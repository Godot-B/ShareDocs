import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

public class SectionLock {
    private final ReentrantLock lock = new ReentrantLock(true); // 공정성 보장
    private ClientSession currentOwner = null;
    private final Queue<ClientSession> waitingQueue = new LinkedList<>();

    public synchronized boolean tryAcquire(ClientSession requester) {
        if (currentOwner == requester || waitingQueue.contains(requester)) {
            return false; // 이미 보유 중이거나 대기 중이면 다시 요청 불가
        }

        if (lock.tryLock()) {
            currentOwner = requester;
            return true;
        } else {
            if (!waitingQueue.contains(requester)) {
                waitingQueue.add(requester);
            }
            return false;
        }
    }

    public synchronized void release(ClientSession requester) {
        if (currentOwner != requester) {
            throw new IllegalStateException("락을 보유한 세션만 해제할 수 있습니다.");
        }
        currentOwner = null;
        lock.unlock();

        if (!waitingQueue.isEmpty()) {
            ClientSession next = waitingQueue.poll();
            boolean acquired = lock.tryLock();
            if (acquired) {
                currentOwner = next;
                next.grantWritePermission();
            } else {
                throw new IllegalStateException("락을 해제했는데 다음 세션이 락을 획득하지 못함");
            }
        }
    }

    public synchronized boolean isHeldBy(ClientSession session) {
        return session == currentOwner;
    }
}
