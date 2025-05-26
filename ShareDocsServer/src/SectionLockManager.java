import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
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
        final ReentrantLock lock = new ReentrantLock();
        final Condition condition = lock.newCondition();

        ClientSession currentOwner = null;
        final Queue<ClientSession> waitingQueue = new ConcurrentLinkedQueue<>();  // Thread-safe
    }

    public void lockHandle(Path sectionPath, ClientSession requester, PrintWriter out) {
        if (requester == null) {
            throw new IllegalArgumentException("requester is null");
        }

        Section section = sectionMap.computeIfAbsent(sectionPath, k -> new Section());
        section.waitingQueue.offer(requester);

        boolean waitSent = false;

        section.lock.lock();
        try {
            // 대기열의 첫 번째가 아닐 경우 condition 대기
            while (section.waitingQueue.peek() != requester || section.currentOwner != null) {
                if (!waitSent) {
                    out.println("status: wait");
                    waitSent = true;
                }
                try {
                    section.condition.await();  // 대기
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            section.waitingQueue.poll();
            section.currentOwner = requester;  // 👈 명시적 소유권 부여

        } finally {
            section.lock.unlock();
        }

        requester.writeSession(sectionPath);

        section.lock.lock();
        try {
            section.currentOwner = null;
            section.condition.signalAll(); // 다음 대기자 깨우기
        } finally {
            section.lock.unlock();
        }
    }
}