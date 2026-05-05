package project.task2.model;

import java.time.LocalDateTime;

public class ArchivedNotification {
    private final String originalId;
    private final String authorUsername;
    private final String title;
    private final String message;
    private final String type;
    private final LocalDateTime createdAt;
    private final LocalDateTime archivedAt;
    private final String relatedSubmissionId;

    public ArchivedNotification(Notification notification) {
        this.originalId = notification.getNotificationId();
        this.authorUsername = notification.getAuthorUsername();
        this.title = notification.getTitle();
        this.message = notification.getMessage();
        this.type = notification.getType();
        this.createdAt = notification.getCreatedAt();
        this.archivedAt = LocalDateTime.now();
        this.relatedSubmissionId = notification.getRelatedSubmissionId();
    }

    public ArchivedNotification(String originalId, String authorUsername, String title, String message,
                                 String type, LocalDateTime createdAt, LocalDateTime archivedAt,
                                 String relatedSubmissionId) {
        this.originalId = originalId;
        this.authorUsername = authorUsername;
        this.title = title;
        this.message = message;
        this.type = type;
        this.createdAt = createdAt;
        this.archivedAt = archivedAt;
        this.relatedSubmissionId = relatedSubmissionId;
    }

    public String getOriginalId() { return originalId; }
    public String getAuthorUsername() { return authorUsername; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getType() { return type; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getArchivedAt() { return archivedAt; }
    public String getRelatedSubmissionId() { return relatedSubmissionId; }

    @Override
    public String toString() {
        return String.join("|",
            originalId,
            authorUsername,
            title,
            message,
            type,
            createdAt.toString(),
            archivedAt.toString(),
            relatedSubmissionId != null ? relatedSubmissionId : ""
        );
    }

    public static ArchivedNotification fromString(String data) {
        String[] parts = data.split("\\|", -1);
        if (parts.length >= 8) {
            return new ArchivedNotification(
                parts[0], parts[1], parts[2], parts[3], parts[4],
                LocalDateTime.parse(parts[5]),
                LocalDateTime.parse(parts[6]),
                parts[7].isEmpty() ? null : parts[7]
            );
        }
        return null;
    }
}
