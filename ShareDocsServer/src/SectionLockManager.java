import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SectionLockManager {

    // 싱글톤 instance
    private static final SectionLockManager instance = new SectionLockManager();
    protected SectionLockManager() {}

    private final Map<String, SectionLock> lockMap = new ConcurrentHashMap<>();

    public static SectionLockManager getInstance() {
        return instance;
    }

    private String key(String doc, String sec) {
        return doc + "::" + sec;
    }

    public SectionLock getLock(String doc, String sec) {
        return lockMap.computeIfAbsent(key(doc, sec), k -> new SectionLock());
    }
}
