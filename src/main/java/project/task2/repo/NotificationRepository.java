package project.task2.repo;

import project.task2.model.Notification;
import project.shared.notification.UnifiedNotification;
import project.shared.notification.UnifiedNotificationStore;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationRepository {
    private static final String LEGACY_NOTIFICATIONS_FILE = "data/notifications.txt";
    private static final String SCOPE = "TASK2";
    private final UnifiedNotificationStore store = new UnifiedNotificationStore();

    public NotificationRepository() {
        migrateLegacyData();
    }

    private void migrateLegacyData() {
        try {
            Path legacyPath = Paths.get(LEGACY_NOTIFICATIONS_FILE);
            if (!Files.exists(legacyPath)) {
                return;
            }
            for (String line : Files.readAllLines(legacyPath)) {
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                Notification legacy = Notification.fromString(line);
                if (legacy == null) {
                    continue;
                }
                store.upsert(toUnified(legacy));
            }
        } catch (Exception ignored) {
        }
    }

    public void save(Notification notification) {
        store.upsert(toUnified(notification));
    }

    public List<Notification> findByAuthor(String authorUsername) {
        return store.findByScopeAndUser(SCOPE, authorUsername).stream()
                .map(this::toTask2Notification)
                .sorted((a, b) -> {
                    // Priority notifications come first, then by date (newest first)
                    if (a.isPriority() && !b.isPriority()) return -1;
                    if (!a.isPriority() && b.isPriority()) return 1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .collect(Collectors.toList());
    }

    public List<Notification> findUnreadByAuthor(String authorUsername) {
        return store.findByScopeAndUser(SCOPE, authorUsername).stream()
                .map(this::toTask2Notification)
                .filter(n -> !n.isRead())
                .sorted((a, b) -> {
                    if (a.isPriority() && !b.isPriority()) return -1;
                    if (!a.isPriority() && b.isPriority()) return 1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .collect(Collectors.toList());
    }

    public void markAsRead(String notificationId) {
        store.markRead(SCOPE, notificationId);
    }

    public void markAllAsRead(String authorUsername) {
        store.markAllRead(SCOPE, authorUsername);
    }
    
    // NEW: Delete a single notification
    public void delete(String notificationId) {
        store.deleteById(SCOPE, notificationId);
    }
    
    // NEW: Delete all notifications for an author
    public void deleteAllByAuthor(String authorUsername) {
        store.deleteByUser(SCOPE, authorUsername);
    }
    
    // NEW: Delete read notifications for an author
    public void deleteReadByAuthor(String authorUsername) {
        store.deleteReadByUser(SCOPE, authorUsername);
    }

    public int getUnreadCount(String authorUsername) {
        return store.countByScopeAndUser(SCOPE, authorUsername, true);
    }

    private UnifiedNotification toUnified(Notification n) {
        return new UnifiedNotification(
                n.getNotificationId(),
                SCOPE,
                n.getAuthorUsername(),
                n.getTitle(),
                n.getMessage(),
                n.getType(),
                n.getType(),
                n.isRead(),
                n.isPriority(),
                n.getRelatedSubmissionId() == null ? "" : n.getRelatedSubmissionId(),
                n.getCreatedAt()
        );
    }

    private Notification toTask2Notification(UnifiedNotification row) {
        return new Notification(
                row.id(),
                row.username(),
                row.title(),
                row.message(),
                row.type().isBlank() ? row.category() : row.type(),
                row.read(),
                row.createdAt(),
                row.relatedId().isBlank() ? null : row.relatedId(),
                row.priority()
        );
    }
}
