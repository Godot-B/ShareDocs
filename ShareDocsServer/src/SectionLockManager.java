import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class SectionLockManager {

    // 싱글톤 인스턴스
    private static final SectionLockManager instance = new SectionLockManager();
    protected SectionLockManager() {}

    public static SectionLockManager getInstance() {
        return instance;
    }

    private final Map<Path, Section> sectionMap = new ConcurrentHashMap<>();

    private static class Section {
        ClientSession currentWriter = null;  // 현재 쓰기 중인 클라이언트
        final Queue<ClientSession> waitingQueue = new ConcurrentLinkedQueue<>();  // Thread-safe

        final ReentrantLock lock = new ReentrantLock(true);
        final Condition condition = lock.newCondition();  // 효율적인 wait-awake 수단
    }

    public void lockOrWait(Path sectionPath, ClientSession requester, PrintWriter out) {
        Section section = sectionMap.computeIfAbsent(sectionPath, k -> new Section());
        section.waitingQueue.offer(requester);

        boolean waitSent = false;

        section.lock.lock();
        try {
            while (section.waitingQueue.peek() != requester ||
                    section.currentWriter != null) {
                if (!waitSent) {  // 클라이언트에게 대기할 것을 1번만 알림
                    out.println("status: wait");
                    waitSent = true;
                }
                try {
                    section.condition.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            section.currentWriter = section.waitingQueue.poll();  // 쓰기 권한 획득

        } finally {
            section.lock.unlock();
        }
    }

    public void readyForNextLock(Path sectionPath) {
        Section section = sectionMap.computeIfAbsent(sectionPath, k -> new Section());

        section.lock.lock();
        try {
            section.currentWriter = null;  // 빈 자리로 만듦
            section.condition.signalAll();  // 다음 대기자 깨우기
        } finally {
            section.lock.unlock();
        }
    }
}