package project.task2.repo;

import project.task2.model.Notification;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NotificationRepository {
    private static final String NOTIFICATIONS_FILE = "data/notifications.txt";
    private final Map<String, Notification> notificationsById = new ConcurrentHashMap<>();

    public NotificationRepository() {
        createDataDirectory();
        loadFromFile();
    }

    private void createDataDirectory() {
        try {
            Path dataDir = Paths.get("data");
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
        } catch (IOException e) {
            System.err.println("Error creating data directory: " + e.getMessage());
        }
    }

    private void loadFromFile() {
        try {
            Path filePath = Paths.get(NOTIFICATIONS_FILE);
            if (Files.exists(filePath)) {
                List<String> lines = Files.readAllLines(filePath);
                notificationsById.clear();
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        Notification notification = Notification.fromString(line);
                        if (notification != null) {
                            notificationsById.put(notification.getNotificationId(), notification);
                        }
                    }
                }
                System.out.println("Loaded " + notificationsById.size() + " notifications from file");
            }
        } catch (IOException e) {
            System.err.println("Error loading notifications: " + e.getMessage());
        }
    }

    private void saveToFile() {
        try {
            List<String> lines = new ArrayList<>();
            for (Notification notification : notificationsById.values()) {
                lines.add(notification.toString());
            }
            
            Path filePath = Paths.get(NOTIFICATIONS_FILE);
            Files.write(filePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            System.out.println("Saved " + lines.size() + " notifications to file");
        } catch (IOException e) {
            System.err.println("Error saving notifications: " + e.getMessage());
        }
    }

    public void save(Notification notification) {
        notificationsById.put(notification.getNotificationId(), notification);
        saveToFile();
    }

    public List<Notification> findByAuthor(String authorUsername) {
        return notificationsById.values().stream()
                .filter(n -> n.getAuthorUsername().equals(authorUsername))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public List<Notification> findUnreadByAuthor(String authorUsername) {
        return notificationsById.values().stream()
                .filter(n -> n.getAuthorUsername().equals(authorUsername) && !n.isRead())
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public void markAsRead(String notificationId) {
        Notification notification = notificationsById.get(notificationId);
        if (notification != null) {
            notification.markAsRead();
            saveToFile();
        }
    }

    public void markAllAsRead(String authorUsername) {
        for (Notification notification : notificationsById.values()) {
            if (notification.getAuthorUsername().equals(authorUsername) && !notification.isRead()) {
                notification.markAsRead();
            }
        }
        saveToFile();
    }

    public int getUnreadCount(String authorUsername) {
        return (int) notificationsById.values().stream()
                .filter(n -> n.getAuthorUsername().equals(authorUsername) && !n.isRead())
                .count();
    }
}
