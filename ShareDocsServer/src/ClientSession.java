import com.google.gson.Gson;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClientSession implements Runnable {
    private final Socket socket;
    private final DocsManager docsManager;

    private BufferedReader in;
    private PrintWriter out;

    // 상태 enum
    private enum State { WAIT_COMMAND, WRITING }
    private State state = State.WAIT_COMMAND;

    // 쓰기 관련 임시 변수
    private String currentDoc = null;
    private String currentSec = null;
    private final List<String> writeBuffer = new ArrayList<>();

    public ClientSession(Socket socket, DocsManager docsManager) {
        this.socket = socket;
        this.docsManager = docsManager;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("ShareDocs에 오신 것을 환영합니다." +
                    "\n명령어를 입력하세요. (create, read, write, bye):");

            String line;
            while ((line = in.readLine()) != null) {
                switch (state) {
                    case WAIT_COMMAND -> handleCommand(line);
                    case WRITING -> handleWriting(line);
                }
            }
        } catch (IOException e) {
            System.err.println("내용 저장 중 오류가 발생하였습니다." + e.getMessage());
        } finally {
            // 종료 시 쓰기 중이었으면 안전하게 마무리
            if (state == State.WRITING) {
                SectionLock lock = SectionLockManager.getInstance()
                        .getLock(currentDoc, currentSec);

                if (lock != null && lock.isHeldBy(this)) {
                    lock.release(this);  // 소유자인 경우에만 해제
                }
            }

            try {
                if (in != null) in.close();
                if (out != null) out.close();
                socket.close();
            } catch (IOException ignored) {}
        }
    }

    private void handleCommand(String line) throws IOException {
        JsonObject req;
        try {
            req = JsonParser.parseString(line).getAsJsonObject();
        } catch (Exception e) {
            out.println("status: error");
            out.println("잘못된 명령 형식입니다. JSON 객체를 보내야 합니다.");
            return;
        }

        Gson gson = new Gson();
        String command = req.get("command").getAsString();

        switch (command) {
            case "write" -> {
                if (state == State.WRITING) {
                    out.println("status: error");
                    out.println("이미 쓰기 중입니다. 완료 후 다시 요청하세요.");
                    return;
                }

                WriteAuthorRequest writeAuthReq = gson.fromJson(req, WriteAuthorRequest.class);
                currentDoc = writeAuthReq.getDocTitle();
                currentSec = writeAuthReq.getSectionTitle();

                SectionLock lock = SectionLockManager.getInstance().getLock(currentDoc, currentSec);
                if (lock.tryAcquire(this)) {
                    grantWritePermission();
                } else {
                    sendWaitMessage();
                }
            }
            case "create" -> {
                CreateRequest createReq = gson.fromJson(req, CreateRequest.class);
                handleCreate(createReq);
            }
            case "read" -> {
                ReadRequest readReq = gson.fromJson(req, ReadRequest.class);
                handleRead(readReq);
            }
            case "bye" -> {
                System.out.println("클라이언트 " + socket.getInetAddress() +
                        ":" + socket.getPort() + " 연결 종료됨.");
                socket.close();
            }
            default -> {
                out.println("status: error");
                out.println("잘못된 명령어입니다: " + command);
            }
        }
    }

    private void handleWriting(String line) throws IOException {
        if (line.equals("__END__")) {
            docsManager.writeSection(currentDoc, currentSec, writeBuffer);
            out.println("status: ok");
            out.println("내용이 성공적으로 저장되었습니다.");

            SectionLock lock = SectionLockManager.getInstance().getLock(currentDoc, currentSec);
            if (lock.isHeldBy(this)) {
                lock.release(this);
            }

            resetWriteState();
        } else {
            writeBuffer.add(line);
        }
    }

    public void grantWritePermission() {
        this.state = State.WRITING;
        writeBuffer.clear();
        out.println("status: ok");
        out.println("섹션에 쓸 내용을 입력하세요.");
    }

    public void sendWaitMessage() {
        out.println("status: wait");
    }

    private void resetWriteState() {
        this.state = State.WAIT_COMMAND;
        this.currentDoc = null;
        this.currentSec = null;
        this.writeBuffer.clear();
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
        if (request.getHasArgs()) {
            String docTitle = request.getDocTitle();
            String sectionTitle = request.getSectionTitle();
            List<String> lines = docsManager.readSection(docTitle, sectionTitle);

            if (lines == null) {
                out.println("status: error");
                out.println("문서나 섹션이 존재하지 않습니다.");

            } else {
                out.println("status: ok");
                lines.forEach(out::println);
                out.println("__END__");
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
        out.println("__END__");  // 이스케이프
    }
}