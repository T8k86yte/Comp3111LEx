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

    public Notification(String authorUsername, String title, String message, String type, String relatedSubmissionId) {
        this.notificationId = "NOTIF_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
        this.authorUsername = authorUsername;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
        this.relatedSubmissionId = relatedSubmissionId;
    }

    public Notification(String notificationId, String authorUsername, String title, String message, 
                        String type, boolean isRead, LocalDateTime createdAt, String relatedSubmissionId) {
        this.notificationId = notificationId;
        this.authorUsername = authorUsername;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = isRead;
        this.createdAt = createdAt;
        this.relatedSubmissionId = relatedSubmissionId;
    }

    public String getNotificationId() { return notificationId; }
    public String getAuthorUsername() { return authorUsername; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public boolean isRead() { return isRead; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getRelatedSubmissionId() { return relatedSubmissionId; }

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
            relatedSubmissionId != null ? relatedSubmissionId : ""
        );
    }

    public static Notification fromString(String data) {
        String[] parts = data.split("\\|");
        if (parts.length >= 8) {
            return new Notification(
                parts[0],
                parts[1],
                parts[2],
                parts[3],
                parts[4],
                Boolean.parseBoolean(parts[5]),
                LocalDateTime.parse(parts[6]),
                parts[7].isEmpty() ? null : parts[7]
            );
        }
        return null;
    }
}
