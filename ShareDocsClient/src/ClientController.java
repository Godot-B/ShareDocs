import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientController {
    private final BufferedReader in;
    private final PrintWriter out;

    public ClientController(BufferedReader in, PrintWriter out) {
        this.in = in;
        this.out = out;
    }

    public boolean handleInput(String userLine) throws IOException {
        if (userLine == null || userLine.isEmpty()) return true;

        out.println(userLine);
        List<String> tokens = parseTokens(userLine);
        if (tokens.isEmpty()) return true;

        String command = tokens.get(0).toLowerCase();

        switch (command) {
            case "create":
                handleCreate(tokens);
                break;
            case "read":
                handleRead(tokens);
                break;
            case "write":
                handleWrite(tokens);
                break;
            case "bye":
                return false; // 종료
            default:
                System.out.println(ResponseHandler.getSingleResponse(in));
        }
        return true;
    }

    public static List<String> parseTokens(String inputLine) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = Pattern.compile("\"([^\"]*)\"|(\\S+)").matcher(inputLine);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                tokens.add(matcher.group(1));  // "..." 안의 값
            } else {
                tokens.add(matcher.group(2));  // 숫자 또는 단일 단어
            }
        }
        return tokens;
    }

    private void handleCreate(List<String> tokens) throws IOException {
        if (tokens.size() < 4) {
            System.out.println("사용법: create <d_title> <s_#> <s1_title> ... <sk_title>");
            return;
        }

        int sectionCount;
        try {
            sectionCount = Integer.parseInt(tokens.get(2));
        } catch (NumberFormatException e) {
            System.out.println("s_#에는 숫자를 입력해주세요.");
            return;
        }
        if (sectionCount > 10) {
            System.out.println("문서 하나 당 섹션 수는 최대 10개입니다.");
            return;
        }
        if (tokens.size() != 3 + sectionCount) {
            System.out.println("만들고자 하는 섹션 수가 " + sectionCount + "개가 아닙니다.");
            return;
        }

        // JSON 형식 Request 전송
        boolean isCreated = EncodeAndRequest.create(tokens, out);
        if (!isCreated) {
            return;
        }

        String status = ResponseHandler.readStatus(in);
        if (status.equals("ok")) {
            ResponseHandler.getSingleResponse(in);  // 서버의 응답 출력

        } else if (status.equals("error")) {
            System.out.println("[Error] ");
            ResponseHandler.getSingleResponse(in);  // 서버의 에러 메시지 출력
        } else {
            System.out.println("[Unknown Error]");
        }
    }

    private void handleRead(List<String> tokens) throws IOException {

        if (tokens.size() == 1) {  // read
            EncodeAndRequest.readNoArgs(out);

            String status = ResponseHandler.readStatus(in);
            if (status.equals("ok")) {
                ResponseHandler.printStructure(in);  // 서버의 응답 출력

            } else if (status.equals("error")) {
                System.out.println("[Error] ");
                ResponseHandler.getSingleResponse(in);  // 서버의 에러 메시지 출력
            } else {
                System.out.println("[Unknown Error]");
            }

        } else if (tokens.size() == 3) {  // read <d_title> <s_title>>
            EncodeAndRequest.read(tokens, out);

            String status = ResponseHandler.readStatus(in);
            if (status.equals("ok")) {
                ResponseHandler.printSectionContents(in);  // 서버의 응답 출력

            } else if (status.equals("error")) {
                System.out.println("[Error] ");
                ResponseHandler.getSingleResponse(in); // 서버의 에러 메시지 출력
            } else {
                System.out.println("[Unknown Error]");
            }

        } else {  // 잘못된 명령어 형식
            System.out.println("사용법: read 또는 read <d_title> <s_title>");
        }
    }

    private void handleWrite(List<String> tokens) {

        // TODO 0. 대기 여부 처리
        // TODO 1. 클라이언트가 user의 편리한 입력 인터페이스 제공
        // TODO 2. 64KB 줄 단위 out.println, 끝을 알리는 __END__를 붙임
    }
}
