
import java.io.BufferedReader;
import java.io.IOException;

public class ResponseHandler {

    public static String readStatus(BufferedReader in) throws IOException {
        String line = in.readLine();
        if (line == null || !line.startsWith("status: ")) {
            throw new IOException("잘못된 응답 형식: " + line);
        }
        return line.substring(8).trim(); // "ok", "error", "wait", ...
    }

    public static String getSingleResponse(BufferedReader in) {
        String response = "";
        try {
            response = in.readLine();
        } catch (IOException e) {
            System.err.println("서버로부터 수신 실패: " + e.getMessage());
        }
        return response;
    }

    public static void printStructure(BufferedReader in) throws IOException {
        String line;
        while ((line = in.readLine()) != null && !line.equals("__END__")) {
            System.out.println(line);  // 문서 제목 출력

            while ((line = in.readLine()) != null && !line.equals("__SEP__")) {
                if (line.equals("__END__")) break;
                System.out.println("\t" + line);  // 섹션 제목 출력
            }
        }
    }

    public static void printSectionContents(BufferedReader in) throws IOException {
        // 문서 제목 및 섹션 제목 출력
        String line;
        if ((line = in.readLine()) != null && !line.equals("__END__"))
            System.out.println(line);
        if ((line = in.readLine()) != null && !line.equals("__END__"))
            System.out.println("\t" + line);

        // 섹션 내용 출력
        while ((line = in.readLine()) != null && !line.equals("__END__")) {
            System.out.println("\t\t" + line);
        }
    }
}