import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface DocsManager {

    CreateResult createDocument(String docTitle, List<String> sectionTitles);

    Map<String, List<String>> getStructure();

    Path locateSecPath(String docTitle, String secTitleWithoutPrefix);

    List<String> readSection(String docTitle, String sectionTitle) throws IOException;

    void commitWrite(Path sectionPath, List<String> lines) throws IOException;
}
