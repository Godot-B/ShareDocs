import com.google.gson.Gson;
import request.CreateRequest;
import request.ReadRequest;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class EncodeAndRequest {
    private static final Gson gson = new Gson();

    public static boolean isOverMaxBytes(String input, int maxBytes) {
        return input.getBytes(StandardCharsets.UTF_8).length > maxBytes;
    }

    // create <d_title> <s_#> <s1_title> ... <sk_title>
    public static boolean create(List<String> tokens, PrintWriter out) {
        CreateRequest request = new CreateRequest();

        // 문서 제목
        String docTitle = tokens.get(1);
        if (isOverMaxBytes(docTitle, 64)) {
            System.out.println("문서 제목이 64바이트를 초과했습니다.");
            return false;
        }

        request.setDocTitle(docTitle);

        // 섹션 제목
        int sectionCount = Integer.parseInt(tokens.get(2));
        request.setSectionCount(sectionCount);

        Set<String> sectionTitleSet = new HashSet<>();

        for (int i = 0; i < sectionCount; i++) {
            String sectionTitle = tokens.get(3 + i);

            if (isOverMaxBytes(sectionTitle, 64)) {
                System.out.println("섹션 제목이 64바이트를 초과했습니다.");
                return false;
            }
            if (!sectionTitleSet.add(sectionTitle)) {
                System.out.println("섹션 제목이 중복되었습니다: " + sectionTitle);
                return false;
            }

            request.addSectionTitle(sectionTitle);
        }

        String json = gson.toJson(request);
        out.println(json);

        return true;
    }

    // read
    public static void readNoArgs(PrintWriter out) {
        ReadRequest request = new ReadRequest();
        request.setHasArgs(false);

        String json = gson.toJson(request);
        out.println(json);
    }

    // read <d_title> <s_title>
    public static void read(List<String> tokens, PrintWriter out) {
        String docTitle = tokens.get(1);
        String sectionTitle = tokens.get(2);

        ReadRequest request = new ReadRequest();
        request.setDocTitle(docTitle);
        request.setSectionTitle(sectionTitle);

        String json = gson.toJson(request);
        out.println(json);
    }

    public static void write(List<String> tokens) {
    }
}
