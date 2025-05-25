import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ShareDocsClient {
    static String configPath = "config.txt";
    static String serverIp = null;
    static int serverPort = -1;

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("사용법: ./myclient <client IP> <client port>");
            return;
        }
        String clientIp = args[0];
        int clientPort = Integer.parseInt(args[1]);

        readConfig();
        if (serverIp == null || serverPort == -1) {
            System.out.println("config.txt에서 서버 주소를 읽지 못했습니다.");
            return;
        }

        // 바인드용 로컬 주소
        InetSocketAddress address = new InetSocketAddress(clientIp, clientPort);
        Socket socket = new Socket();
        socket.bind(address);

        // 서버에 연결
        socket.connect(new InetSocketAddress(serverIp, serverPort));

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);  // 즉시 전송 보장

        // 환영 메시지 & 안내 response
        System.out.println(ResponseHandler.getSingleResponse(in));
        System.out.println(ResponseHandler.getSingleResponse(in));

        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
        ClientController controller = new ClientController(in, out);

        String userLine;
        while ((userLine = userInput.readLine()) != null) {
            boolean keepRunning = controller.handleInput(userLine);
            if (!keepRunning) break;
        }

        out.close();
        in.close();
        socket.close();  // FIN 전송 및 리소스 해제
        System.out.println("클라이언트가 종료됩니다.");
    }

    private static void readConfig() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(configPath));
        for (String line : lines) {
            if (line.startsWith("docs_server")) {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String[] address = parts[1].trim().split("\\s+");
                    if (address.length == 2) {
                        serverIp = address[0];
                        serverPort = Integer.parseInt(address[1]);
                    }
                }
            }
        }
    }
}