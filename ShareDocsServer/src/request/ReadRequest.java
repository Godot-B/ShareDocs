package request;

public class ReadRequest {
    String command = "read";
    Boolean hasArgs = true;
    String docTitle;
    String sectionTitle;

    public Boolean getHasArgs() {
        return hasArgs;
    }
    public String getDocTitle() {
        return docTitle;
    }
    public String getSectionTitle() {
        return sectionTitle;
    }
}
