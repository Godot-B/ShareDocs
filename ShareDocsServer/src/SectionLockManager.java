import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class SectionLockManager {

    // Singleton 인스턴스
    private static final SectionLockManager instance = new SectionLockManager();
    protected SectionLockManager() {}

    public static SectionLockManager getInstance() {
        return instance;
    }

    private static class LockState {
        final ReentrantLock lock = new ReentrantLock(true);
        boolean isLocked = false;
        final Queue<ClientHandler> waitingQueue = new LinkedList<>();
    }

    private final Map<String, LockState> lockMap = new ConcurrentHashMap<>();

    private String key(String docTitle, String secTitle) {
        return docTitle + "_" + secTitle;
    }

    public synchronized boolean requestLock(String docTitle, String secTitle, ClientHandler requester) {
        String key = key(docTitle, secTitle);
        LockState state = lockMap.computeIfAbsent(key, k -> new LockState());

        if (!state.isLocked) {
            state.lock.lock();
            state.isLocked = true;
            return true;
        } else {
            state.waitingQueue.add(requester);
            return false;
        }
    }

    public synchronized void releaseLock(String docTitle, String secTitle) {
        String key = key(docTitle, secTitle);
        LockState state = lockMap.get(key);

        if (state == null) return;

        if (state.waitingQueue.isEmpty()) {
            state.isLocked = false;
            state.lock.unlock();
        } else {
            // 대기 중인 다음 클라이언트 스레드
            ClientHandler next = state.waitingQueue.poll();

            // 다음 클라이언트에게 write 권한 주기
            next.grantWritePermission(docTitle, secTitle);

            // 락은 유지됨
        }
    }
}
