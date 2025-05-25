import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class SectionLockManager {
    private static final Logger logger = Logger.getLogger(SectionLockManager.class.getName());

    // 싱글톤 인스턴스
    private static final SectionLockManager instance = new SectionLockManager();
    protected SectionLockManager() {}

    public static SectionLockManager getInstance() {
        return instance;
    }

    private static class Section {
        final ReentrantLock lock = new ReentrantLock(true); // 공정성 보장
        ClientSession currentOwner = null;
        final Queue<ClientSession> waitingQueue = new LinkedList<>();
        final Object monitor = new Object();  // wait-notify 전용 객체
    }

    private static final Map<Path, Section> sectionMap = new HashMap<>();

    /**
     * 클라이언트가 락 요청할 때 호출됨
     * @return true면 즉시 락 획득 성공, false면 대기해야 함
     */
    public synchronized boolean requestLock(Path sectionPath, ClientSession requester) {
        Section section = sectionMap.computeIfAbsent(sectionPath, k -> new Section());

        // 대기열도 비어있어야
        if (section.waitingQueue.isEmpty() && !section.lock.isLocked()) {
            section.lock.lock();  // 락 점유
            section.currentOwner = requester;
            return true;
        } else {
            // 대기열에 사람이 있는 경우에 공정성 보장
            section.waitingQueue.add(requester);
            return false;
        }
    }

    /**
     * 락 해제 시 호출됨. 다음 대기자에게 권한을 넘기고 알림.
     */
    public synchronized void releaseLock(Path sectionPath) {
        Section section = sectionMap.get(sectionPath);
        if (section == null) return;

        section.lock.unlock();
        section.currentOwner = null;

        if (section.waitingQueue.isEmpty()) {
            sectionMap.remove(sectionPath);

        } else {
            // 다음 대기자 (ClientSession)
            section.currentOwner = section.waitingQueue.poll();

            // 다음 대기자에게 알림
            synchronized (section.monitor) {
                section.monitor.notifyAll();  // currentOwner == session 조건 확인하게 함
            }
        }
    }

    /**
     * 락을 얻기 위해 자신의 차례가 될 때까지 대기
     */
    public static void waitForTurn(Path sectionPath, ClientSession session) {
        Section section = sectionMap.get(sectionPath);
        if (section == null) {
            logger.warning("releaseLock(): 섹션이 존재하지 않습니다. 이미 해제되었거나 잘못된 요청: " + sectionPath);
            return;
        }

        synchronized (section.monitor) {
            while (section.currentOwner != session) {
                try {
                    section.monitor.wait(); // 다른 스레드가 notify 해줄 때까지 대기
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // 인터럽트 대응
                    return;
                }
            }
        }
    }
}