import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final DocsManager docsManager;

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
            System.err.println("IOException 발생!!: " + e.getMessage());
        }
    }

    private void handleCreate(String[] tokens, PrintWriter out) {

    }

    private void handleRead(String[] tokens, PrintWriter out) {

    }

    private void handleWrite(String[] tokens, PrintWriter out) {

    }
}
