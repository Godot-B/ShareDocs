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

        List<String> tokens = parseTokens(userLine);
        if (tokens.isEmpty()) return true;

        String command = tokens.get(0);

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
                handleBye();
                return false;  // 종료
            default:
                System.out.println("[Error] 알 수 없는 명령어입니다: " + command);
        }
        return true;
    }

    public static List<String> parseTokens(String inputLine) {
        List<String> tokens = new ArrayList<>();

        Matcher matcher = Pattern.compile("\"([^\"]*)\"" +
                        "|" +
                        "(\\S+)")
                .matcher(inputLine);

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                tokens.add(matcher.group(1));  // " " 안의 값
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
        if (tokens.size() != (3 + sectionCount)) {
            System.out.println("만들고자 하는 섹션 수가 " + sectionCount + "개가 아닙니다.");
            return;
        }

        if (forbidWordContained(tokens, sectionCount)) {
            System.out.println("제목에 __END__나 __SEP__는 포함될 수 없습니다.");
            return;
        }

        // JSON 형식 Request 전송
        boolean isCreated = EncodeAndRequest.create(tokens, out);
        if (!isCreated) {
            return;
        }

        String status = ResponseHandler.readStatus(in);
        if (status.equals("ok")) {
            System.out.println(ResponseHandler.getSingleResponse(in));  // 서버의 응답 출력

        } else if (status.equals("error")) {
            System.out.println("[Error] " + ResponseHandler.getSingleResponse(in));  // 서버의 에러 메시지 출력
        }
    }

    private static boolean forbidWordContained(List<String> tokens, int sectionCount) {
        // 금지된 문자열 검사
        List<String> forbidWords = List.of("__END__", "__SEP__");

        // 문서 제목 검사 (tokens[1])
        for (String f : forbidWords) {
            if (tokens.get(1).contains(f)) {
                return true;
            }
        }

        // 섹션 제목들 검사 (tokens[3] ~ tokens[3 + sectionCount - 1])
        for (int i = 3; i < 3 + sectionCount; i++) {
            String title = tokens.get(i);
            for (String f : forbidWords) {
                if (title.contains(f)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void handleRead(List<String> tokens) throws IOException {
        if (tokens.size() == 1) {  // read
            EncodeAndRequest.readNoArgs(out);

            String status = ResponseHandler.readStatus(in);
            if (status.equals("ok")) {
                ResponseHandler.printStructure(in);  // 서버의 응답 출력

            } else if (status.equals("error")) {
                System.out.println("[Error] " + ResponseHandler.getSingleResponse(in));  // 서버의 에러 메시지 출력
            }

        } else if (tokens.size() == 3) {  // read <d_title> <s_title>>
            EncodeAndRequest.readWithArgs(tokens, out);

            String status = ResponseHandler.readStatus(in);
            if (status.equals("ok")) {
                ResponseHandler.printSectionContents(in);  // 서버의 응답 출력

            } else if (status.equals("error")) {
                System.out.println("[Error] " + ResponseHandler.getSingleResponse(in));  // 서버의 에러 메시지 출력
            }

        } else {  // 잘못된 명령어 형식
            System.out.println("사용법: read 또는 read <d_title> <s_title>");
        }
    }

    private void handleWrite(List<String> tokens) throws IOException {
        if (tokens.size() != 3) {
            System.out.println("사용법: write <d_title> <s_title>");
            return;
        }

        // 쓰기 권한 요청
        EncodeAndRequest.writeAuthor(tokens, out);

        String status = ResponseHandler.readStatus(in);
        if (status.equals("wait")) {
            System.out.println("다른 사용자가 쓰기 중입니다. 승인 대기 중...");

            status = ResponseHandler.readStatus(in);
        }

        if (status.equals("ok")) {
            System.out.println(ResponseHandler.getSingleResponse(in));  // 서버의 응답 출력
        } else if (status.equals("error")) {
            System.out.println("[Error] " + ResponseHandler.getSingleResponse(in));  // 서버의 에러 메시지 출력
            return;
        }

        // 사용자로부터 줄 단위 입력 받기
        List<String> lines = WriteEditor.openEditor();

        // 줄 단위 데이터 전송
        for (String line : lines) {
            out.println(line);
        }
        out.println("__END__");

        status = ResponseHandler.readStatus(in);
        if (status.equals("ok")) {
            System.out.println(ResponseHandler.getSingleResponse(in));  // 서버의 응답 출력
        } else if (status.equals("error")) {
            System.out.println("[Error] " + ResponseHandler.getSingleResponse(in));  // 서버의 에러 메시지 출력
        }
    }

    private void handleBye() {
        EncodeAndRequest.bye(out);
        System.out.println("서버에 연결 종료를 요청합니다.");
    }
}
