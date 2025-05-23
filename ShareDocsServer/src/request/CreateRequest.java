package request;

import java.util.ArrayList;
import java.util.List;

public class CreateRequest {
    String command = "create";
    String docTitle;
    Integer sectionCount;
    List<String> sectionTitles = new ArrayList<>();

    public String getDocTitle() {
        return docTitle;
    }
    public List<String> getSectionTitles() {
        return sectionTitles;
    }
}