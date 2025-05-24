import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ShareDocsServer {

    static String configPath = "config.txt";

    public static void main(String[] args) throws IOException {
        Logger.getLogger("").setLevel(Level.INFO);

        if (args.length != 2) {
            System.out.println("사용법: ./myserver <server IP> <server port>");
            return;
        }
        String ip = args[0];
        int port = Integer.parseInt(args[1]);
        writeServerInfoToConfig(configPath, ip, port);

        ServerSocket serverSocket = new ServerSocket(port);
        DocsManager docsManager = new DocsManagerImpl(configPath);

        System.out.println("서버가 포트 " + port + "에서 대기 중...");

        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("클라이언트 " + socket.getInetAddress() + ":" + socket.getPort() + " 접속됨");

            // 클라이언트마다 독립 스레드
            new Thread(new ClientSession(socket, docsManager)).start();
        }
    }

    private static void writeServerInfoToConfig(String configPath, String ip, int port) throws IOException {
        Path path = Paths.get(configPath);
        List<String> originalLines = new ArrayList<>();

        if (Files.exists(path)) {
            for (String line : Files.readAllLines(path)) {
                // 기존 docs_server 라인은 제거
                if (!line.trim().startsWith("docs_server")) {
                    originalLines.add(line);
                }
            }
        }

        // 맨 위에 docs_server 라인 추가
        originalLines.add(0, "docs_server = " + ip + " " + port);

        Files.write(path, originalLines);
    }
}