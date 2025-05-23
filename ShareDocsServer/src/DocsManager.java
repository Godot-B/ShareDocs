import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface DocsManager {

    CreateResult createDocument(String docTitle, List<String> sectionTitles);

    Map<String, List<String>> getStructure();

    List<String> readSection(String docTitle, String sectionTitle) throws IOException;

    void commitWrite(String docTitle, String sectionTitle, List<String> newLines);
}
