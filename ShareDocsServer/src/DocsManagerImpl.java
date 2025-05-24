import java.io.*;
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
        return configPath;
    }

    @Override
    public CreateResult createDocument(String docTitle, List<String> sectionTitles) {
        Path docPath = baseDir.resolve(docTitle);
        if (Files.exists(docPath)) {
            return CreateResult.ALREADY_EXISTS;
        }

        try {
            Files.createDirectory(docPath);
            int seqNum = 1;  // section 생성 순서 보장
            for (String section : sectionTitles) {
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

                    // 순서 보장
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

    public Path locateSecPath(String docTitle, String secTitleWithoutPrefix) {
        Path docDir = baseDir.resolve(docTitle);

        if (!Files.exists(docDir) || !Files.isDirectory(docDir)) {
            return null;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(docDir, "*.txt")) {
            for (Path file : stream) {
                String filename = file.getFileName().toString();  // 예: "2. TCP 소켓.txt"
                String rawTitle = filename.replaceFirst("\\.txt$", "");  // "2. TCP 소켓"

                String[] parts = rawTitle.split("\\.", 2);  // prefix와 분리
                String actualTitle = parts[1].trim();  // "TCP 소켓"
                if (actualTitle.equals(secTitleWithoutPrefix)) {
                    return file;
                }
            }
        } catch (IOException e) {
            logger.severe("섹션 제목 읽기 중 오류: " + e.getMessage());
            logger.log(Level.SEVERE, "예외 상세:", e);
        }

        return null;  // 못 찾은 경우
    }

    @Override
    public List<String> readSection(String docTitle, String secTitle) {

        Path sectionPath = locateSecPath(docTitle, secTitle);
        if (sectionPath == null) {
            return null;
        }

        try {
            List<String> lines = new ArrayList<>();

            // 문서 제목
            lines.add(docTitle);

            // prefix 포함한 섹션 제목
            String sectionFileName = sectionPath.getFileName().toString();  // "2. TCP 소켓.txt"
            lines.add(sectionFileName.replaceFirst("\\.txt$", ""));  // "2. TCP 소켓"

            // 섹션 내용
            lines.addAll(Files.readAllLines(sectionPath, StandardCharsets.UTF_8));

            return lines;

        } catch (IOException e) {
            logger.severe("섹션 읽기 실패: " + sectionPath + " - " + e.getMessage());
            logger.log(Level.SEVERE, "예외 상세", e);
            return null;
        }
    }

    @Override
    public void writeSection(String docTitle, String sectionTitle, List<String> lines) throws IOException {
        Path sectionPath = locateSecPath(docTitle, sectionTitle);
        if (sectionPath == null) {
            throw new IOException("해당 섹션을 찾을 수 없습니다: " + docTitle + "/" + sectionTitle);
        }

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(sectionPath, StandardCharsets.UTF_8))) {
            for (String line : lines) {
                writer.println(line);
            }
        }
    }
}