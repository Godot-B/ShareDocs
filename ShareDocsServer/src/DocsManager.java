import java.util.List;
import java.util.Map;

public interface DocsManager {

    CreateResult createDocument(String docTitle, List<String> secTitles);

    Map<String, List<String>> getStructure();

    List<String> readSection(String docTitle, String secTitle);

    boolean requestWriteLock(String docTitle, String secTitle);

    void commitWrite(String docTitle, String secTitle, List<String> newLines);
}
