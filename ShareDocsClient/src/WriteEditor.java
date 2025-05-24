import javax.swing.*;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class WriteEditor {

    public static List<String> openEditor() {
        JTextArea textArea = new JTextArea();
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);

        JButton okButton = new JButton("OK");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(new JLabel("<html><b>섹션 내용을 입력하세요</b><br>" +
                        "최대 10줄, 줄당 64바이트.<br>※ '__END__'는 입력할 수 없습니다.</html>"),
                BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        JDialog dialog = new JDialog((Frame) null, "섹션 입력", true);
        dialog.setContentPane(mainPanel);
        dialog.setSize(600, 400); // 고정 크기 (적당한 기본값)
        dialog.setLocationRelativeTo(null);

        final List<String> result = new ArrayList<>();

        okButton.addActionListener(e -> {
            String text = textArea.getText();
            if (text.contains("__END__")) {
                JOptionPane.showMessageDialog(dialog,
                        "'__END__'는 클라이언트-서버 예약어입니다.\n입력에서 제거해주세요.",
                        "입력 오류", JOptionPane.WARNING_MESSAGE);
                return;
            }

            List<String> processed = splitLinesWithLimit(List.of(text.split("\n")), 64, 10);
            result.addAll(processed);
            dialog.dispose();
        });

        dialog.setVisible(true);
        return result;
    }

    private static List<String> splitLinesWithLimit(List<String> lines, int maxBytes, int maxLines) {
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            int i = 0;
            while (i < line.length()) {
                StringBuilder sb = new StringBuilder();
                int byteCount = 0;
                while (i < line.length()) {
                    char ch = line.charAt(i);
                    int charByteLen = String.valueOf(ch).getBytes(StandardCharsets.UTF_8).length;
                    if (byteCount + charByteLen > maxBytes) break;
                    sb.append(ch);
                    byteCount += charByteLen;
                    i++;
                }
                result.add(sb.toString());
                if (result.size() >= maxLines) return result;
            }
            if (result.size() >= maxLines) break;
        }
        return result;
    }
}