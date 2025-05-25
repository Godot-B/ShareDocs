import java.io.*;
import java.net.*;

public class ShareDocsServer {
    static String configPath = "config.txt";

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("사용법: ./myserver <server IP> <server port>");
            return;
        }
        String serverIp = args[0];
        int serverPort = Integer.parseInt(args[1]);
        InetSocketAddress socketAddress = new InetSocketAddress(serverIp, serverPort);

        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(socketAddress);
            DocsManager docsManager = new DocsManagerImpl(configPath);

            System.out.println("ShareDocs 서버가 시작됩니다.");
            System.out.printf("서버 주소: %s:%d%n\n", serverIp, serverPort);

            //noinspection InfiniteLoopStatement
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("클라이언트 " + socket.getInetAddress() +
                        ":" + socket.getPort() + " 접속됨");

                // 클라이언트마다 독립 스레드
                new Thread(new ClientSession(socket, docsManager)).start();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}