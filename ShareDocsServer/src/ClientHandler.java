import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final DocsManager docsManager;
    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    public ClientHandler(Socket clientSocket, DocsManager docsManager) {
        this.socket = clientSocket;
        this.docsManager = docsManager;
    }

    public void run() {
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            String inputLine;
            out.println("ShareDocs에 오신 것을 환영합니다.");

            while ((inputLine = in.readLine()) != null) {
                out.println("명령어를 입력하세요. (create, read, write, bye):");
                inputLine = inputLine.trim();   // 명령문 앞뒤 공백 제거
                if (inputLine.isEmpty()) continue;

                String[] tokens = inputLine.split("\\s+");  // 명령문을 공백 기준으로 분리
                String command = tokens[0].toLowerCase();

                switch (command) {
                    case "create":
                        handleCreate(tokens, out);
                        break;
                    case "read":
                        handleRead(tokens, out);
                        break;
                    case "write":
                        handleWrite(tokens, in, out);
                        break;
                    case "bye":
                        in.close();
                        out.close();
                        socket.close();
                        System.out.println("클라이언트 " + socket.getInetAddress() + ":" + socket.getPort() + " 연결 종료됨.");
                        return;
                    default:
                        out.println("잘못된 명령어입니다: " + command);
                }
            }
        } catch (IOException e) {
            logger.severe("클라이언트 처리 중 오류: " + e.getMessage());
            logger.log(Level.SEVERE, "예외 상세:", e);
        }
    }

    public boolean isOverMaxBytes(String input, int maxBytes) {
        return input.getBytes(StandardCharsets.UTF_8).length > maxBytes;
    }

    private void handleCreate(String[] tokens, PrintWriter out) {
        if (tokens.length < 4) {
            out.println("사용법: create <d_title> <s_#> <s1_title> ... <sk_title>");
            return;
        }

        int sectionCount;
        try {
            sectionCount = Integer.parseInt(tokens[2]);
        } catch (NumberFormatException e) {
            out.println("s_#에는 숫자를 입력해주세요.");
            return;
        }
        if (sectionCount > 10) {
            out.println("문서 하나 당 섹션 수는 최대 10개입니다.");
            return;
        }
        if (tokens.length < 3 + sectionCount) {
            out.println("만들고자 하는 섹션 수가 " + sectionCount + "개보다 작습니다.");
            return;
        }

        List<String> sectionTitles = new ArrayList<>();
        for (int i = 0; i < sectionCount; i++) {
            if (isOverMaxBytes(tokens[3 + i], 64)) {
                out.println("섹션 제목이 64바이트를 초과했습니다.");
                return;
            }
            sectionTitles.add(tokens[3 + i]);
        }

        String docTitle = tokens[1];
        if (isOverMaxBytes(docTitle, 64)) {
            out.println("문서 제목이 64바이트를 초과했습니다.");
            return;
        }

        CreateResult result = docsManager.createDocument(docTitle, sectionTitles);
        switch (result) {
            case SUCCESS -> out.println("문서 및 섹션이 성공적으로 생성되었습니다.");
            case ALREADY_EXISTS -> out.println("이미 존재하는 문서입니다.");
            case IO_EXCEPTION -> out.println("문서 생성 중에 오류가 발생하였습니다.");
        }
    }

    private void handleRead(String[] tokens, PrintWriter out) {
        if (tokens.length == 1) {
            sendStructure(out);
        } else if (tokens.length == 3) {
            String docTitle = tokens[1];
            String sectionTitle = tokens[2];
            List<String> lines = docsManager.readSection(docTitle, sectionTitle);
            if (lines == null) {
                out.println("문서나 섹션이 존재하지 않습니다.");
            } else {
                lines.forEach(out::println);
            }
        } else {
            out.println("사용법: read 또는 read <d_title> <s_title>");
        }
    }

    private void sendStructure(PrintWriter out) {
        Map<String, List<String>> structure = docsManager.getStructure();

        for (Map.Entry<String, List<String>> entry : structure.entrySet()) {
            String docTitle = entry.getKey();
            List<String> sections = entry.getValue();

            out.println(docTitle);  // 문서 제목
            for (String section : sections) {
                out.println(section);  // 번호 포함된 섹션 제목
            }
            out.println();  // 문서 간 줄바꿈
        }
    }

    private void handleWrite(String[] tokens, PrintWriter out) {

    }
}
