package project.shared.notification;

import java.time.LocalDateTime;

public record UnifiedNotification(
        String id,
        String scope,
        String username,
        String title,
        String message,
        String category,
        String type,
        boolean read,
        boolean priority,
        String relatedId,
        LocalDateTime createdAt
) {
    public UnifiedNotification {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Notification id is required");
        }
        if (scope == null || scope.isBlank()) {
            throw new IllegalArgumentException("Notification scope is required");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Notification username is required");
        }
        if (message == null) {
            throw new IllegalArgumentException("Notification message is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Notification timestamp is required");
        }
    }

    public UnifiedNotification withRead(boolean nextRead) {
        return new UnifiedNotification(
                id,
                scope,
                username,
                title,
                message,
                category,
                type,
                nextRead,
                priority,
                relatedId,
                createdAt
        );
    }
}
