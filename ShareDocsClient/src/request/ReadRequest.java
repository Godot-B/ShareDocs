package request;

public class ReadRequest {
    String command = "read";
    Boolean hasArgs = true;
    String docTitle;
    String sectionTitle;

    public void setHasArgs(Boolean hasArgs) {
        this.hasArgs = hasArgs;
    }
    public void setDocTitle(String docTitle) {
        this.docTitle = docTitle;
    }
    public void setSectionTitle(String sectionTitle) {
        this.sectionTitle = sectionTitle;
    }
}
