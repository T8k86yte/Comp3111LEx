package project.shared.notification;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class UnifiedNotificationStore {
    private static final String STORE_FILE = "data/notifications_unified.txt";
    private static final String FIELD_DELIMITER = "|";
    private static final int EXPECTED_PARTS = 11;

    public synchronized UnifiedNotification create(
            String scope,
            String username,
            String title,
            String message,
            String category,
            String type,
            boolean priority,
            String relatedId
    ) {
        UnifiedNotification row = new UnifiedNotification(
                "NTF_" + UUID.randomUUID(),
                normalize(scope),
                normalize(username),
                safe(title),
                safe(message),
                safe(category),
                safe(type),
                false,
                priority,
                safe(relatedId),
                LocalDateTime.now()
        );
        List<UnifiedNotification> all = loadAllInternal();
        all.add(row);
        saveAllInternal(all);
        return row;
    }

    public synchronized void upsert(UnifiedNotification row) {
        List<UnifiedNotification> all = loadAllInternal();
        boolean replaced = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id().equals(row.id()) && all.get(i).scope().equalsIgnoreCase(row.scope())) {
                all.set(i, row);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            all.add(row);
        }
        saveAllInternal(all);
    }

    public synchronized List<UnifiedNotification> findByScopeAndUser(String scope, String username) {
        String normalizedScope = normalize(scope);
        String normalizedUser = normalize(username);
        return loadAllInternal().stream()
                .filter(n -> n.scope().equalsIgnoreCase(normalizedScope))
                .filter(n -> n.username().equals(normalizedUser))
                .sorted(Comparator.comparing(UnifiedNotification::createdAt).reversed())
                .collect(Collectors.toList());
    }

    public synchronized int countByScopeAndUser(String scope, String username, boolean unreadOnly) {
        String normalizedScope = normalize(scope);
        String normalizedUser = normalize(username);
        return (int) loadAllInternal().stream()
                .filter(n -> n.scope().equalsIgnoreCase(normalizedScope))
                .filter(n -> n.username().equals(normalizedUser))
                .filter(n -> !unreadOnly || !n.read())
                .count();
    }

    public synchronized void markRead(String scope, String notificationId) {
        updateMatching(scope, n -> n.id().equals(notificationId), n -> n.withRead(true));
    }

    public synchronized void markAllRead(String scope, String username) {
        String normalizedUser = normalize(username);
        updateMatching(scope, n -> n.username().equals(normalizedUser), n -> n.withRead(true));
    }

    public synchronized void deleteById(String scope, String notificationId) {
        deleteMatching(scope, n -> n.id().equals(notificationId));
    }

    public synchronized void deleteByUser(String scope, String username) {
        String normalizedUser = normalize(username);
        deleteMatching(scope, n -> n.username().equals(normalizedUser));
    }

    public synchronized void deleteReadByUser(String scope, String username) {
        String normalizedUser = normalize(username);
        deleteMatching(scope, n -> n.username().equals(normalizedUser) && n.read());
    }

    private interface RowTransformer {
        UnifiedNotification transform(UnifiedNotification row);
    }

    private void updateMatching(String scope, Predicate<UnifiedNotification> selector, RowTransformer transformer) {
        String normalizedScope = normalize(scope);
        List<UnifiedNotification> all = loadAllInternal();
        for (int i = 0; i < all.size(); i++) {
            UnifiedNotification row = all.get(i);
            if (!row.scope().equalsIgnoreCase(normalizedScope)) {
                continue;
            }
            if (!selector.test(row)) {
                continue;
            }
            all.set(i, transformer.transform(row));
        }
        saveAllInternal(all);
    }

    private void deleteMatching(String scope, Predicate<UnifiedNotification> selector) {
        String normalizedScope = normalize(scope);
        List<UnifiedNotification> remaining = loadAllInternal().stream()
                .filter(row -> !row.scope().equalsIgnoreCase(normalizedScope) || !selector.test(row))
                .collect(Collectors.toCollection(ArrayList::new));
        saveAllInternal(remaining);
    }

    private List<UnifiedNotification> loadAllInternal() {
        ensureDataDir();
        Path path = Paths.get(STORE_FILE);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try {
            List<UnifiedNotification> rows = new ArrayList<>();
            for (String line : Files.readAllLines(path)) {
                UnifiedNotification row = deserialize(line);
                if (row != null) {
                    rows.add(row);
                }
            }
            return rows;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveAllInternal(List<UnifiedNotification> rows) {
        ensureDataDir();
        List<String> lines = rows.stream().map(this::serialize).collect(Collectors.toList());
        try {
            Files.write(
                    Paths.get(STORE_FILE),
                    lines,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (IOException ignored) {
        }
    }

    private void ensureDataDir() {
        try {
            Files.createDirectories(Paths.get("data"));
        } catch (IOException ignored) {
        }
    }

    private String serialize(UnifiedNotification row) {
        return String.join(FIELD_DELIMITER,
                encode(row.id()),
                encode(row.scope()),
                encode(row.username()),
                encode(safe(row.title())),
                encode(safe(row.message())),
                encode(safe(row.category())),
                encode(safe(row.type())),
                Boolean.toString(row.read()),
                Boolean.toString(row.priority()),
                encode(safe(row.relatedId())),
                row.createdAt().toString()
        );
    }

    private UnifiedNotification deserialize(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        String[] parts = line.split("\\|", -1);
        if (parts.length < EXPECTED_PARTS) {
            return null;
        }
        try {
            return new UnifiedNotification(
                    decode(parts[0]),
                    decode(parts[1]),
                    decode(parts[2]),
                    decode(parts[3]),
                    decode(parts[4]),
                    decode(parts[5]),
                    decode(parts[6]),
                    Boolean.parseBoolean(parts[7]),
                    Boolean.parseBoolean(parts[8]),
                    decode(parts[9]),
                    LocalDateTime.parse(parts[10])
            );
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
