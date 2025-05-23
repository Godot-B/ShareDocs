import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShareDocsClient {

    private static BufferedReader in;
    private static PrintWriter out;

    public static void main(String[] args) throws IOException {
        String ServerIP = "localhost";
        int port = 12345;
        Socket socket = new Socket(ServerIP, port);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        // PrintWriter는 버퍼링이 있으므로 autoFlush 진리값을 true로 설정
        out = new PrintWriter(socket.getOutputStream(), true);

        // 환영 메시지 & 안내 response
        System.out.println(getSingleResponse());
        System.out.println(getSingleResponse());

        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
        String userLine;

        while ((userLine = userInput.readLine()) != null) {
            if (userLine.isEmpty()) continue;
            out.println(userLine);

            List<String> tokens = parseTokens(userLine);
            if (tokens.isEmpty()) continue;
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
                    in.close();
                    out.close();
                    socket.close();
                    System.out.println("클라이언트 " + socket.getInetAddress() + ":" + socket.getPort() + " 연결 종료됨.");
                    return;
                default:
                    System.out.println(getSingleResponse());
            }
        }

        in.close();
        out.close();
        socket.close();
        System.out.println("클라이언트 종료.");
    }

    private static List<String> parseTokens(String inputLine) {
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

    private static void handleCreate(List<String> tokens) {
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
        if (tokens.size() < 3 + sectionCount) {
            System.out.println("만들고자 하는 섹션 수가 " + sectionCount + "개보다 작습니다.");
            return;
        }

        boolean isCreated = EncodeAndRequest.create(tokens, out);
        if (!isCreated) {
            return;
        }
        System.out.println(getSingleResponse());
    }

    private static void handleWrite(List<String> tokens) {
        // TODO 0. 대기 여부 처리
        // TODO 1. 클라이언트가 user의 편리한 입력 인터페이스 제공
        // TODO 2. 64KB 줄 단위 out.println, 끝을 알리는 __END__를 붙임
    }

    private static void handleRead(List<String> tokens) throws IOException {
        // read
        if (tokens.size() == 1) {
            String line;
            while ((line = in.readLine()) != null && !line.equals("__END__")) {
                System.out.println(line);  // 문서 제목 출력

                while ((line = in.readLine()) != null && !line.equals("__SEP__")) {
                    if (line.equals("__END__")) break;
                    System.out.println("\t" + line);  // 섹션 제목 출력
                }
            }

            return;
        }

        String response = getSingleResponse();
        // read <d_title> <s_title>>
        if (tokens.size() == 3 && response.equals(tokens.get(1))) {
            System.out.println(response);  // 문서 제목 출력
            String line;
            System.out.println("\t" + getSingleResponse());  // 섹션 제목 출력
            while ((line = in.readLine()) != null && !line.equals("__END__")) { // 섹션 내용 출력
                System.out.println("\t" + "\t" + line);
            }
        } else {  // 잘못된 명령어 - 서버의 에러 메시지 출력
            System.out.println(response);
        }
    }

    private static String getSingleResponse() {
        String response = "";
        try {
            response = in.readLine();
        } catch (IOException e) {
            System.err.println("서버로부터 수신 실패: " + e.getMessage());
        }
        return response;
    }
}