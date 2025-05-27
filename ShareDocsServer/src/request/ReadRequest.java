package request;

public class ReadRequest {
    String command = "read";
    String docTitle;
    String sectionTitle;

    public Boolean hasArgs() {
        return docTitle != null && sectionTitle != null;
    }
    public String getDocTitle() {
        return docTitle;
    }
    public String getSectionTitle() {
        return sectionTitle;
    }
}
