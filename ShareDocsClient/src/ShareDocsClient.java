import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ShareDocsClient {
    public static void main(String[] args) throws IOException {
        String ServerIP = "localhost";
        int port = 12345;
        Socket serverSocket = new Socket(ServerIP, port);

        BufferedReader in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
        // PrintWriter는 버퍼링이 있으므로 autoFlush 진리값을 true로 설정
        PrintWriter out = new PrintWriter(serverSocket.getOutputStream(), true);

        BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));
        String userLine;
        System.out.println("메시지를 입력하세요 (bye 입력 시 종료):");
        while ((userLine = userInput.readLine()) != null) {
            out.println(userLine);
            String response = in.readLine();
            System.out.println("서버 응답: " + response);
            if (userLine.equalsIgnoreCase("bye")) break;
        }

        in.close();
        out.close();
        serverSocket.close();
        System.out.println("클라이언트 종료.");
    }
}