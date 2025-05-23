package request;

public class WriteRequest {
    String command = "write";
    String docTitle;
    String sectionTitle;

    public String getDocTitle() {
        return docTitle;
    }
    public String getSectionTitle() {
        return sectionTitle;
    }
}
