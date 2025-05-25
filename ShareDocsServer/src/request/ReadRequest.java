package request;

public class ReadRequest {
    String command = "read";
    Boolean hasArgs = true;
    String docTitle;
    String sectionTitle;

    public Boolean hasArgs() {
        return hasArgs;
    }
    public String getDocTitle() {
        return docTitle;
    }
    public String getSectionTitle() {
        return sectionTitle;
    }
}
