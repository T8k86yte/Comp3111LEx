package project.task2.repo;

import project.task2.model.Notification;
import project.task2.database.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class NotificationRepository {
    private final Connection conn;

    public NotificationRepository() {
        this.conn = DatabaseConnection.getConnection();
    }

    public void save(Notification notification) {
        String sql = """
            INSERT OR REPLACE INTO notifications 
            (notification_id, author_username, title, message, type, is_read, created_at, related_submission_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, notification.getNotificationId());
            pstmt.setString(2, notification.getAuthorUsername());
            pstmt.setString(3, notification.getTitle());
            pstmt.setString(4, notification.getMessage());
            pstmt.setString(5, notification.getType());
            pstmt.setInt(6, notification.isRead() ? 1 : 0);
            pstmt.setString(7, notification.getCreatedAt().toString());
            pstmt.setString(8, notification.getRelatedSubmissionId());
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error saving notification: " + e.getMessage());
        }
    }

    public List<Notification> findByAuthor(String authorUsername) {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE author_username = ? ORDER BY created_at DESC";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, authorUsername);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                notifications.add(mapResultSetToNotification(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error finding notifications: " + e.getMessage());
        }
        return notifications;
    }

    public List<Notification> findUnreadByAuthor(String authorUsername) {
        List<Notification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE author_username = ? AND is_read = 0 ORDER BY created_at DESC";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, authorUsername);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                notifications.add(mapResultSetToNotification(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error finding unread notifications: " + e.getMessage());
        }
        return notifications;
    }

    public void markAsRead(String notificationId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE notification_id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, notificationId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error marking notification as read: " + e.getMessage());
        }
    }

    public void markAllAsRead(String authorUsername) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE author_username = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, authorUsername);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error marking all notifications as read: " + e.getMessage());
        }
    }

    public void delete(String notificationId) {
        String sql = "DELETE FROM notifications WHERE notification_id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, notificationId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error deleting notification: " + e.getMessage());
        }
    }

    public int getUnreadCount(String authorUsername) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE author_username = ? AND is_read = 0";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, authorUsername);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error getting unread count: " + e.getMessage());
        }
        return 0;
    }

    private Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        return new Notification(
            rs.getString("notification_id"),
            rs.getString("author_username"),
            rs.getString("title"),
            rs.getString("message"),
            rs.getString("type"),
            rs.getInt("is_read") == 1,
            LocalDateTime.parse(rs.getString("created_at")),
            rs.getString("related_submission_id")
        );
    }
}
