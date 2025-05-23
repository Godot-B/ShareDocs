import com.google.gson.Gson;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class EncodeAndRequest {
    private static final Gson gson = new Gson();

    // 토큰 분리 함수
    private static String[] tokenizeQuoted(String input) {
        return input.split(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
    }

    public static boolean isOverMaxBytes(String input, int maxBytes) {
        return input.getBytes(StandardCharsets.UTF_8).length > maxBytes;
    }

    // create <d_title> <s_#> <s1_title> ... <sk_title>
    public static boolean create(List<String> tokens, PrintWriter out) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("command", "create");

        if (tokens.size() < 4) {
            System.out.println("사용법: create <d_title> <s_#> <s1_title> ... <sk_title>");
            return false;
        }

        // 문서 제목
        String docTitle = tokens.get(1);
        if (isOverMaxBytes(docTitle, 64)) {
            System.out.println("문서 제목이 64바이트를 초과했습니다.");
            return false;
        }

        message.put("d_title", docTitle);

        // 섹션 제목
        int sectionCount = Integer.parseInt(tokens.get(2));
        message.put("section_count", sectionCount);

        Set<String> sectionTitleSet = new HashSet<>();

        for (int i = 0; i < sectionCount; i++) {
            String secTitle = tokens.get(3 + i);

            if (isOverMaxBytes(secTitle, 64)) {
                System.out.println("섹션 제목이 64바이트를 초과했습니다.");
                return false;
            }
            if (!sectionTitleSet.add(secTitle)) {
                System.out.println("섹션 제목이 중복되었습니다: " + secTitle);
                return false;
            }

            message.put("s_title_" + (i + 1), secTitle);
        }

        String json = gson.toJson(message);
        out.println(json);

        return true;
    }

    // read
    // read <d_title> <s_title>
    public static void read(List<String> tokens) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("command", "read");

        int size = tokens.size();
        switch (size) {
            case 1:

                break;
            case 2:

                break;
        }

    }

    public static void write(List<String> tokens) {
    }
}
