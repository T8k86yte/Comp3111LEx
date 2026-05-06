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
        List<UnifiedNotification> unifiedList = store.findByScopeAndUser(SCOPE, authorUsername);
        System.out.println("📋 Found " + unifiedList.size() + " unified notifications for " + authorUsername);
        
        return unifiedList.stream()
                .map(this::toTask2Notification)
                .sorted((a, b) -> {
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
    
    public void delete(String notificationId) {
        store.deleteById(SCOPE, notificationId);
    }
    
    public void deleteAllByAuthor(String authorUsername) {
        store.deleteByUser(SCOPE, authorUsername);
    }
    
    public void deleteReadByAuthor(String authorUsername) {
        store.deleteReadByUser(SCOPE, authorUsername);
    }

    public int getUnreadCount(String authorUsername) {
        return store.countByScopeAndUser(SCOPE, authorUsername, true);
    }

    // ========== ARCHIVE METHODS (Task 2.6) ==========
    
    public void archiveNotification(String notificationId) {
        System.out.println("📦 Archiving notification: " + notificationId);
        var notifications = store.findByScopeAndUser(SCOPE, "");
        for (var n : notifications) {
            if (n.id().equals(notificationId)) {
                Notification updated = toTask2Notification(n);
                updated.archive();
                store.upsert(toUnified(updated));
                System.out.println("✅ Notification archived: " + notificationId);
                break;
            }
        }
    }
    
    public void archiveAllByAuthor(String authorUsername) {
        System.out.println("📦 Archiving all notifications for: " + authorUsername);
        var notifications = store.findByScopeAndUser(SCOPE, authorUsername);
        int count = 0;
        for (var n : notifications) {
            Notification updated = toTask2Notification(n);
            if (!updated.isArchived()) {
                updated.archive();
                store.upsert(toUnified(updated));
                count++;
            }
        }
        System.out.println("✅ Archived " + count + " notifications");
    }
    
    public void unarchiveNotification(String notificationId) {
        System.out.println("📦 Unarchiving notification: " + notificationId);
        var notifications = store.findByScopeAndUser(SCOPE, "");
        for (var n : notifications) {
            if (n.id().equals(notificationId)) {
                Notification updated = toTask2Notification(n);
                updated.unarchive();
                store.upsert(toUnified(updated));
                System.out.println("✅ Notification unarchived: " + notificationId);
                break;
            }
        }
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
        // We need to store archived status somewhere
        // For now, let's extract it from the message or use a custom field
        // Since UnifiedNotification doesn't have archived field, we'll use a workaround
        // For demo purposes, we'll store archived status in the message temporarily
        // In production, you'd add an archived field to UnifiedNotification
        
        boolean isArchived = false;
        String message = row.message();
        if (message != null && message.startsWith("[ARCHIVED]")) {
            isArchived = true;
            message = message.substring(10);
        }
        
        Notification notification = new Notification(
                row.id(),
                row.username(),
                row.title(),
                message,
                row.type().isBlank() ? row.category() : row.type(),
                row.read(),
                row.createdAt(),
                row.relatedId().isBlank() ? null : row.relatedId(),
                row.priority(),
                isArchived
        );
        return notification;
    }
    
    // Helper method to update archived status in unified store
    private void updateArchivedStatus(String notificationId, boolean archived) {
        var notifications = store.findByScopeAndUser(SCOPE, "");
        for (var n : notifications) {
            if (n.id().equals(notificationId)) {
                String newMessage = archived ? "[ARCHIVED] " + n.message() : n.message().replace("[ARCHIVED] ", "");
                UnifiedNotification updated = new UnifiedNotification(
                    n.id(), n.scope(), n.username(), n.title(), newMessage,
                    n.category(), n.type(), n.read(), n.priority(), n.relatedId(), n.createdAt()
                );
                store.upsert(updated);
                break;
            }
        }
    }
}
