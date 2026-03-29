package project.task2.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Notification {
    private final String notificationId;
    private final String authorUsername;
    private final String title;
    private final String message;
    private final String type;
    private boolean isRead;
    private final LocalDateTime createdAt;
    private final String relatedSubmissionId;
    private final boolean isPriority;  // NEW: priority flag for urgent notifications

    // Constructor for new notifications (default priority = false)
    public Notification(String authorUsername, String title, String message, String type, String relatedSubmissionId) {
        this.notificationId = "NOTIF_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
        this.authorUsername = authorUsername;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
        this.relatedSubmissionId = relatedSubmissionId;
        this.isPriority = isPriorityType(type);
    }

    // Constructor for loading from file
    public Notification(String notificationId, String authorUsername, String title, String message, 
                        String type, boolean isRead, LocalDateTime createdAt, 
                        String relatedSubmissionId, boolean isPriority) {
        this.notificationId = notificationId;
        this.authorUsername = authorUsername;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = isRead;
        this.createdAt = createdAt;
        this.relatedSubmissionId = relatedSubmissionId;
        this.isPriority = isPriority;
    }

    // Determine if a notification type should be priority
    private boolean isPriorityType(String type) {
        return type.equals("BOOK_DELETED") || 
               type.equals("BOOK_REJECTED") ||
               type.equals("URGENT_ANNOUNCEMENT");
    }

    public String getNotificationId() { return notificationId; }
    public String getAuthorUsername() { return authorUsername; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public boolean isRead() { return isRead; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getRelatedSubmissionId() { return relatedSubmissionId; }
    public boolean isPriority() { return isPriority; }

    public void markAsRead() { this.isRead = true; }

    public String getFormattedDate() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return createdAt.format(formatter);
    }

    public String getTypeIcon() {
        return switch (type) {
            case "BOOK_APPROVED" -> "✅";
            case "BOOK_REJECTED" -> "❌";
            case "BOOK_SUBMITTED" -> "📝";
            case "BOOK_PENDING" -> "⏳";
            case "BOOK_DELETED" -> "🗑️";
            case "URGENT_ANNOUNCEMENT" -> "⚠️";
            default -> "📌";
        };
    }

    @Override
    public String toString() {
        return String.join("|",
            notificationId,
            authorUsername,
            title,
            message,
            type,
            String.valueOf(isRead),
            createdAt.toString(),
            relatedSubmissionId != null ? relatedSubmissionId : "",
            String.valueOf(isPriority)
        );
    }

    public static Notification fromString(String data) {
        String[] parts = data.split("\\|");
        if (parts.length >= 9) {
            return new Notification(
                parts[0],
                parts[1],
                parts[2],
                parts[3],
                parts[4],
                Boolean.parseBoolean(parts[5]),
                LocalDateTime.parse(parts[6]),
                parts[7].isEmpty() ? null : parts[7],
                Boolean.parseBoolean(parts[8])
            );
        }
        return null;
    }
}
