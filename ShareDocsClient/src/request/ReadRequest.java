package request;

public class ReadRequest {
    String command = "read";
    String docTitle;
    String sectionTitle;

    public void setDocTitle(String docTitle) {
        this.docTitle = docTitle;
    }
    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }
}
