package request;

import java.util.ArrayList;
import java.util.List;

public class CreateRequest {
    String command = "create";
    String docTitle;
    Integer sectionCount;
    List<String> sectionTitles = new ArrayList<>();

    public void setDocTitle(String docTitle) {
        this.docTitle = docTitle;
    }

    public void setSectionCount(Integer sectionCount) {
        this.sectionCount = sectionCount;
    }

    public void addSectionTitle(String sectionTitle) {
        sectionTitles.add(sectionTitle);
    }
}