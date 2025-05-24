package request;

public class WriteAuthorRequest {
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
