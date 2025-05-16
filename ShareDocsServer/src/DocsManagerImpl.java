import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DocsManagerImpl implements DocsManager {

    private final Path baseDir;
    private static final Logger logger = Logger.getLogger(DocsManagerImpl.class.getName());

    public DocsManagerImpl(String configPath) {
        String directoryPath = findDirectoryFromConfig(configPath);
        this.baseDir = Paths.get(directoryPath);

        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new RuntimeException("공유 문서 저장 디렉토리 생성 실패: " + baseDir, e);
        }
    }

    private String findDirectoryFromConfig(String configPath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(configPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // 주석이나 빈 줄은 무시
                if (line.isEmpty() || line.startsWith("#")) continue;

                // docs_directory 설정 찾기
                if (line.startsWith("docs_directory")) {
                    String[] tokens = line.split("=", 2);
                    if (tokens.length == 2) {
                        return tokens[1].trim();
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("config 파일 읽기 실패: " + configPath, e);
        }
        throw new RuntimeException("docs_directory 설정이 config에 없습니다.");
    }

    @Override
    public CreateResult createDocument(String docTitle, List<String> secTitles) {
        Path docPath = baseDir.resolve(docTitle);
        if (Files.exists(docPath)) {
            return CreateResult.ALREADY_EXISTS;
        }

        try {
            Files.createDirectory(docPath);
            int seqNum = 1;
            for (String section : secTitles) {
                String prefix = String.valueOf(seqNum++);
                Path sectionFile = docPath.resolve(prefix + ". " + section + ".txt");
                Files.createFile(sectionFile);
            }
            return CreateResult.SUCCESS;
        } catch (IOException e) {
            return CreateResult.IO_EXCEPTION;
        }
    }

    @Override
    public Map<String, List<String>> getStructure() {
        Map<String, List<String>> result = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir)) {
            for (Path docDir : stream) {
                if (Files.isDirectory(docDir)) {
                    List<String> sections = new ArrayList<>();

                    try (DirectoryStream<Path> sectionStream = Files.newDirectoryStream(docDir)) {
                        for (Path section : sectionStream) {
                            String name = section.getFileName().toString().replaceFirst("\\.txt$", "");
                            sections.add(name);
                        }
                    } catch (IOException e) {
                        logger.severe("섹션 디렉토리 읽기 중 오류: " + e.getMessage());
                        logger.log(Level.SEVERE, "예외 상세:", e);
                    }

                    sections.sort(Comparator.comparingInt(s -> {
                        String prefix = s.split("\\.", 2)[0].trim();  // "1. 개요" → "1"
                        return Integer.parseInt(prefix);
                    }));

                    result.put(docDir.getFileName().toString(), sections);
                }
            }
        } catch (IOException e) {
            logger.severe("문서 디렉토리 목록 읽기 중 오류: " + e.getMessage());
            logger.log(Level.SEVERE, "예외 상세:", e);
        }
        return result;
    }

    @Override
    public List<String> readSection(String docTitle, String secTitle) {
        Path sectionPath = baseDir.resolve(docTitle).resolve(secTitle + ".txt");
        if (!Files.exists(sectionPath)) {
            return null;
        }
        try {
            // 클라이언트의 write에 의해 이미 64바이트 줄 단위 작성된 파일
            return Files.readAllLines(sectionPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.severe("섹션 읽기 실패: " + sectionPath + " - " + e.getMessage());
            logger.log(Level.SEVERE, "예외 상세", e);
            return null;
        }
    }

    @Override
    public void commitWrite(String docTitle, String secTitle, List<String> newLines) {
        Path sectionPath = baseDir.resolve(docTitle).resolve(secTitle + ".txt");

        try (BufferedWriter writer = Files.newBufferedWriter(sectionPath, StandardCharsets.UTF_8)) {
            for (String line : newLines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            logger.severe("쓰기 저장 중 실패: " + e.getMessage());
            logger.log(Level.SEVERE, "예외 상세", e);
        }
    }
}