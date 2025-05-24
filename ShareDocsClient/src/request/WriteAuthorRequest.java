package request;

public class WriteAuthorRequest {
    String command = "write";
    String docTitle;
    String sectionTitle;

    public void setDocTitle(String docTitle) {
        this.docTitle = docTitle;
    }
    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }
}
