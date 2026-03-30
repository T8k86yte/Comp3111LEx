package project.shared;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public final class SessionRecoveryStore {
    private static final Path SESSION_FILE = Paths.get("data/session_recovery.txt");

    private SessionRecoveryStore() {}

    public static void saveLastPortal(String portalKey) {
        saveValue("lastPortal", portalKey);
    }

    public static void saveValue(String key, String value) {
        try {
            Files.createDirectories(Paths.get("data"));
            Properties props = loadProperties();
            props.setProperty(key, value == null ? "" : value.trim());
            storeProperties(props);
        } catch (IOException ignored) {
        }
    }

    public static Optional<String> loadLastPortal() {
        return loadValue("lastPortal");
    }

    public static Optional<String> loadValue(String key) {
        try {
            if (!Files.exists(SESSION_FILE)) {
                return Optional.empty();
            }
            Properties props = loadProperties();
            String value = props.getProperty(key, "").trim();
            return value.isEmpty() ? Optional.empty() : Optional.of(value);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static void removeValue(String key) {
        try {
            Properties props = loadProperties();
            props.remove(key);
            storeProperties(props);
        } catch (IOException ignored) {
        }
    }

    private static Properties loadProperties() throws IOException {
        Properties props = new Properties();
        if (!Files.exists(SESSION_FILE)) {
            return props;
        }
        List<String> lines = Files.readAllLines(SESSION_FILE);
        for (String line : lines) {
            if (line == null || line.trim().isEmpty() || !line.contains("=")) {
                continue;
            }
            int idx = line.indexOf('=');
            String k = line.substring(0, idx).trim();
            String v = line.substring(idx + 1).trim();
            if (!k.isEmpty()) {
                props.setProperty(k, v);
            }
        }
        return props;
    }

    private static void storeProperties(Properties props) throws IOException {
        List<String> lines = props.entrySet()
                .stream()
                .map(e -> e.getKey().toString() + "=" + e.getValue().toString())
                .toList();
        Files.write(
                SESSION_FILE,
                lines,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }
}
