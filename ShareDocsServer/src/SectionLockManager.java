import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SectionLockManager {

    // Singleton 인스턴스
    private static final SectionLockManager instance = new SectionLockManager();
    protected SectionLockManager() {}

    public static SectionLockManager getInstance() {
        return instance;
    }

    private static class Section {
        final Lock lock = new ReentrantLock(true); // 공정성 보장 & 데드락 방지
        final Queue<ClientHandler> waitingQueue = new LinkedList<>();  // FIFO
    }

    // 문서-섹션 key 값으로 구별되는 Section 해시 테이블
    private final Map<String, Section> lockMap = new ConcurrentHashMap<>();  // Thread-safe

    private String key(String docTitle, String secTitle) {
        return docTitle + "_" + secTitle;
    }

    public boolean requestLock(String docTitle, String secTitle, ClientHandler requester) {
        String key = key(docTitle, secTitle);
        Section section = lockMap.computeIfAbsent(key, k -> new Section());

        boolean isLocked = section.lock.tryLock();
        if (isLocked) {
            return true;
        } else {
            // 섹션 별 대기 큐 동시성 처리
            synchronized (section) {
                section.waitingQueue.add(requester);
            }
            return false;
        }
    }

    public void releaseLock(String docTitle, String secTitle) {
        String key = key(docTitle, secTitle);
        Section section = lockMap.get(key);
        if (section == null) {
            return;
        }

        // 섹션 별 대기 큐 동시성 처리
        synchronized (section) {
            if (section.waitingQueue.isEmpty()) {
                section.lock.unlock();
                lockMap.remove(key);
            } else {
                // 섹션 내 대기 중인 다음 클라이언트(Thread)
                ClientHandler next = section.waitingQueue.poll();

                // 다음 클라이언트에게 write 권한 주기
                next.grantWritePermission(docTitle, secTitle);

                // 락은 유지됨
            }
        }
    }
}
