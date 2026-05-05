package project.task2.repo;

import project.task2.model.ArchivedNotification;
import project.task2.model.Notification;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ArchivedNotificationRepository {
    private static final String ARCHIVED_FILE = "data/archived_notifications.txt";
    private final Map<String, ArchivedNotification> archivedById = new ConcurrentHashMap<>();

    public ArchivedNotificationRepository() {
        createDataDirectory();
        loadFromFile();
    }

    private void createDataDirectory() {
        try {
            Files.createDirectories(Paths.get("data"));
        } catch (IOException e) {
            System.err.println("Error creating data directory: " + e.getMessage());
        }
    }

    private void loadFromFile() {
        try {
            Path path = Paths.get(ARCHIVED_FILE);
            if (Files.exists(path)) {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        ArchivedNotification archived = ArchivedNotification.fromString(line);
                        if (archived != null) {
                            archivedById.put(archived.getOriginalId(), archived);
                        }
                    }
                }
                System.out.println("📦 Loaded " + archivedById.size() + " archived notifications");
            }
        } catch (IOException e) {
            System.err.println("Error loading archived notifications: " + e.getMessage());
        }
    }

    private void saveToFile() {
        try {
            List<String> lines = new ArrayList<>();
            for (ArchivedNotification archived : archivedById.values()) {
                lines.add(archived.toString());
            }
            Path path = Paths.get(ARCHIVED_FILE);
            Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("💾 Saved " + lines.size() + " archived notifications");
        } catch (IOException e) {
            System.err.println("Error saving archived notifications: " + e.getMessage());
        }
    }

    public void archive(Notification notification) {
        ArchivedNotification archived = new ArchivedNotification(notification);
        archivedById.put(notification.getNotificationId(), archived);
        saveToFile();
        System.out.println("📦 Archived notification: " + notification.getNotificationId());
    }

    public List<ArchivedNotification> findByAuthor(String authorUsername) {
        return archivedById.values().stream()
                .filter(a -> a.getAuthorUsername().equals(authorUsername))
                .sorted((a, b) -> b.getArchivedAt().compareTo(a.getArchivedAt()))
                .collect(Collectors.toList());
    }

    public Optional<ArchivedNotification> findById(String originalId) {
        return Optional.ofNullable(archivedById.get(originalId));
    }

    public boolean unarchive(String originalId) {
        ArchivedNotification removed = archivedById.remove(originalId);
        if (removed != null) {
            saveToFile();
            System.out.println("📤 Unarchived notification: " + originalId);
            return true;
        }
        return false;
    }

    public void deleteArchived(String originalId) {
        archivedById.remove(originalId);
        saveToFile();
        System.out.println("🗑️ Deleted archived notification: " + originalId);
    }

    public void deleteAllByAuthor(String authorUsername) {
        List<String> toDelete = archivedById.values().stream()
                .filter(a -> a.getAuthorUsername().equals(authorUsername))
                .map(ArchivedNotification::getOriginalId)
                .collect(Collectors.toList());
        
        for (String id : toDelete) {
            archivedById.remove(id);
        }
        if (!toDelete.isEmpty()) {
            saveToFile();
            System.out.println("🗑️ Deleted " + toDelete.size() + " archived notifications for " + authorUsername);
        }
    }
}
