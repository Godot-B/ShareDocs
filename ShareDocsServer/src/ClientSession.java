import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import request.CreateRequest;
import request.ReadRequest;
import request.WriteAuthorRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientSession implements Runnable {
    private static final Logger logger = Logger.getLogger(ClientSession.class.getName());

    private final Socket socket;
    private final DocsManager docsManager;
    private final SectionLockManager lockManager = SectionLockManager.getInstance();

    private BufferedReader in;
    private PrintWriter out;

    public ClientSession(Socket socket, DocsManager docsManager) {
        this.socket = socket;
        this.docsManager = docsManager;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);  // 즉시 전송 보장

            out.println("ShareDocs에 오신 것을 환영합니다." +
                    "\n명령어를 입력하세요. (create, read, write, bye):");

            Gson gson = new Gson();
            String line;
            while ((line = in.readLine()) != null) {
                JsonElement element = JsonParser.parseString(line);
                if (!element.isJsonObject()) {
                    out.println("status: error");
                    out.println("잘못된 명령 형식입니다. JSON 객체를 보내야 합니다.");
                    continue;
                }

                JsonObject json = element.getAsJsonObject();
                String command = json.get("command").getAsString();

                switch (command) {
                    case "create":
                        CreateRequest createReq = gson.fromJson(json, CreateRequest.class);
                        handleCreate(createReq);
                        break;
                    case "read":
                        ReadRequest readReq = gson.fromJson(json, ReadRequest.class);
                        handleRead(readReq);
                        break;
                    case "write":
                        WriteAuthorRequest writeReq = gson.fromJson(json, WriteAuthorRequest.class);
                        handleWrite(writeReq);
                        break;
                    case "bye":
                        out.close();
                        in.close();
                        socket.close();  // FIN 전송 및 리소스 해제
                        System.out.println("클라이언트 " + socket.getInetAddress() +
                                ":" + socket.getPort() + " 연결 종료됨.");
                        return;
                    default:
                        out.println("status: error");
                        out.println("잘못된 명령어입니다: " + command);
                }
            }

        } catch (IOException e) {
            logger.severe("클라이언트 처리 중 오류: " + e.getMessage());
            logger.log(Level.SEVERE, "예외 상세:", e);
        }
    }

    private void handleCreate(CreateRequest request) {

        String docTitle = request.getDocTitle();
        List<String> sectionTitles = request.getSectionTitles();
        CreateResult result = docsManager.createDocument(docTitle, sectionTitles);

        switch (result) {
            case SUCCESS -> {
                out.println("status: ok");
                out.println("문서 및 섹션이 성공적으로 생성되었습니다.");
            }
            case ALREADY_EXISTS -> {
                out.println("status: error");
                out.println("이미 존재하는 문서입니다.");
            }
            case IO_EXCEPTION -> {
                out.println("status: error");
                out.println("문서 생성 중에 오류가 발생하였습니다.");
            }
        }
    }

    private void handleRead(ReadRequest request) throws IOException {
        if (request.hasArgs()) {
            String docTitle = request.getDocTitle();
            String sectionTitle = request.getSectionTitle();
            List<String> lines = docsManager.readSection(docTitle, sectionTitle);

            if (lines == null) {
                out.println("status: error");
                out.println("문서나 섹션이 존재하지 않습니다.");

            } else {
                out.println("status: ok");
                lines.forEach(out::println);
                out.println("__END__");  // 데이터 전송의 끝
            }

        } else {
            out.println("status: ok");
            sendStructure();
        }
    }

    private void sendStructure() {
        Map<String, List<String>> structure = docsManager.getStructure();

        for (Map.Entry<String, List<String>> entry : structure.entrySet()) {
            String docTitle = entry.getKey();
            List<String> sections = entry.getValue();

            out.println(docTitle);  // 문서 제목
            for (String section : sections) {
                out.println(section);  // prefix 포함한 섹션 제목
            }
            out.println("__SEP__");  // 문서 구분자
        }
        out.println("__END__");  // 데이터 전송의 끝
    }

    private void handleWrite(WriteAuthorRequest request) {
        String docTitle = request.getDocTitle();
        String sectionTitle = request.getSectionTitle();
        Path sectionPath = docsManager.locateSecPath(docTitle, sectionTitle);
        if (sectionPath == null) {
            out.println("status: error");
            out.println("존재하지 않는 섹션입니다: " + docTitle + "/" + sectionTitle);
            return;
        }

        lockManager.lockHandle(sectionPath, this, out);
    }

    public void writeSession(Path sectionPath) {
        out.println("status: ok");
        out.println("섹션에 쓸 내용을 입력하세요.");

        List<String> lines = new ArrayList<>();
        try {
            String line;
            // 클라이언트가 __END__ 줄을 마지막으로 입력하여 끝을 알림.
            while ((line = in.readLine()) != null && !line.equals("__END__")) {
                // 클라이언트의 write에 의해 이미 64바이트 줄 단위 전송됨.
                lines.add(line);
            }

            docsManager.commitWrite(sectionPath, lines);

            out.println("status: ok");
            out.println("내용이 성공적으로 저장되었습니다.");

        } catch (IOException e) {
            out.println("status: error");
            out.println("내용 저장 중 오류가 발생하였습니다.");

            logger.severe("쓰기 중 오류: " + e.getMessage());
            logger.log(Level.SEVERE, "예외 상세:", e);
        }
    }
}
