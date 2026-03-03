package project.task1.repo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

final class RepositoryFileStorageSupport {
    private RepositoryFileStorageSupport() {
    }

    static List<String> readLines(Path path) {
        if (!Files.exists(path)) {
            return Collections.emptyList();
        }
        try {
            return Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read repository file: " + path, ex);
        }
    }

    static void writeLines(Path path, List<String> lines) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(
                    path,
                    lines,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write repository file: " + path, ex);
        }
    }

    static String encode(String raw) {
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static String decode(String encoded) {
        return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    static List<String> splitRecord(String line, int expectedColumns) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != expectedColumns) {
            throw new IllegalArgumentException("Invalid record column count: " + line);
        }
        List<String> result = new ArrayList<>(parts.length);
        Collections.addAll(result, parts);
        return result;
    }
}
