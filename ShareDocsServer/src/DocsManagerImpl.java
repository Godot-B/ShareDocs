import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocsManagerImpl implements DocsManager {

    private final Path baseDir;

    public DocsManagerImpl(String configPath) {
        String directoryPath = findDirectoryFromConfig(configPath);
        this.baseDir = Paths.get(directoryPath);
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create base directory", e);
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
            throw new RuntimeException("Failed to read config file: " + configPath, e);
        }
        throw new RuntimeException("docs_directory not found in config file");
    }


    @Override
    public boolean createDocument(String docTitle, List<String> secTitles) {
        Path docPath = baseDir.resolve(docTitle);
        if (Files.exists(docPath)) return false;

        try {
            Files.createDirectory(docPath);
            for (String section : secTitles) {
                Path sectionFile = docPath.resolve(section + ".txt");
                Files.createFile(sectionFile);
            }
            return true;
        } catch (IOException e) {
            return false;
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
                    }
                    result.put(docDir.getFileName().toString(), sections);
                }
            }
        } catch (IOException e) {
            // 로그 출력만
        }
        return result;
    }

    @Override
    public List<String> readSection(String docTitle, String secTitle) {
        Path sectionPath = baseDir.resolve(docTitle).resolve(secTitle + ".txt");
        if (!Files.exists(sectionPath)) return null;
        try {
            return Files.readAllLines(sectionPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public synchronized boolean requestWriteLock(String docTitle, String sectionTitle) {
//        // 실제 락은 이후 고급 구현에서 구현 (파일 잠금 또는 메모리 기반 queue)
//        Path sectionPath = baseDir.resolve(docTitle).resolve(sectionTitle + ".txt");
//        return Files.exists(sectionPath); // 존재하면 "락 허용" 가정

        return true;
    }

    @Override
    public void commitWrite(String docTitle, String sectionTitle, List<String> newLines) {
        Path sectionPath = baseDir.resolve(docTitle).resolve(sectionTitle + ".txt");
        try (BufferedWriter writer = Files.newBufferedWriter(sectionPath, StandardCharsets.UTF_8)) {
            for (String line : newLines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("IOException 발생!!: " + e.getMessage());
        }
    }
}