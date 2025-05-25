import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ShareDocsClient {

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("사용법: ./myclient <client IP> <client port>");
            return;
        }
        String serverIp = args[0];
        int port = Integer.parseInt(args[1]);

        Socket socket = new Socket(serverIp, port);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        // PrintWriter는 버퍼링이 있으므로 autoFlush 진리값을 true로 설정
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

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

        socket.close();  // TCP FIN 전송됨
        in.close();
        out.close();
        System.out.println("클라이언트가 종료됩니다.");
    }
}