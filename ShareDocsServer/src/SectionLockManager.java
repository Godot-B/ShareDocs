import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SectionLockManager {

    // 싱글톤 인스턴스
    private static final SectionLockManager instance = new SectionLockManager();
    protected SectionLockManager() {}

    public static SectionLockManager getInstance() {
        return instance;
    }

    private static class Section {
        final ReentrantLock lock = new ReentrantLock(true); // 공정성 보장 Lock
        final Condition condition = lock.newCondition();  // 대기열 전용 condition

        ClientSession currentOwner = null;
        final Queue<ClientSession> waitingQueue = new LinkedList<>();
    }

    private static final Map<Path, Section> sectionMap = new HashMap<>();

    /**
     * 클라이언트가 락 요청할 때 호출됨
     * @return true면 즉시 락 획득 성공, false면 대기해야 함
     */
    public synchronized boolean requestLock(Path sectionPath, ClientSession requester) {
        Section section;
        synchronized (this) {
            section = sectionMap.computeIfAbsent(sectionPath, k -> new Section());
        }

        section.lock.lock();
        try {
            if (section.currentOwner == requester) {
                // 이미 내 차례가 된 경우
                return true;
            }

            // 대기열도 비어있어야
            if (section.currentOwner == null && section.waitingQueue.isEmpty()) {
                section.currentOwner = requester;  // 락 점유
                return true;
            } else {
                // 대기열에 사람이 있는 경우에 공정성 보장
                section.waitingQueue.offer(requester);
                return false;
            }
        } finally {
            section.lock.unlock();
        }
    }

    /**
     * 락 해제 시 호출됨. 다음 대기자에게 권한을 넘기고 알림.
     */
    public synchronized void releaseLock(Path sectionPath) {
        Section section;
        synchronized (this) {
            section = sectionMap.get(sectionPath);
        }
        if (section == null) return;

        section.lock.lock();
        try {
            section.currentOwner = null;

            if (section.waitingQueue.isEmpty()) {
                synchronized (this) {
                    sectionMap.remove(sectionPath);
                }
                return;
            }

            // 다음 대기자에게 권한 위임
            section.currentOwner = section.waitingQueue.poll();
            section.condition.signalAll();

        } finally {
            section.lock.unlock();
        }
    }

    /**
     * 락을 얻기 위해 자신의 차례가 될 때까지 대기
     */
    public void waitForTurn(Path sectionPath, ClientSession requester) {
        Section section;
        synchronized (this) {
            section = sectionMap.get(sectionPath);
        }
        if (section == null) return;

        section.lock.lock();
        try {
            while (section.currentOwner != requester) {
                try {
                    section.condition.await();  // 대기
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        } finally {
            section.lock.unlock();
        }
    }
}