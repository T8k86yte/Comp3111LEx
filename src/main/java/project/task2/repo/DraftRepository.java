package project.task2.repo;

import project.task2.database.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;

public class DraftRepository {
    private final Connection conn;

    public DraftRepository() {
        this.conn = DatabaseConnection.getConnection();
    }

    public void saveDraft(String authorUsername, String draftData) {
        String sql = """
            INSERT OR REPLACE INTO drafts (author_username, draft_data, last_updated)
            VALUES (?, ?, ?)
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, authorUsername);
            pstmt.setString(2, draftData);
            pstmt.setString(3, LocalDateTime.now().toString());
            
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error saving draft: " + e.getMessage());
        }
    }

    public String loadDraft(String authorUsername) {
        String sql = "SELECT draft_data FROM drafts WHERE author_username = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, authorUsername);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("draft_data");
            }
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error loading draft: " + e.getMessage());
        }
        return null;
    }

    public boolean hasDraft(String authorUsername) {
        String sql = "SELECT 1 FROM drafts WHERE author_username = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, authorUsername);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error checking draft: " + e.getMessage());
        }
        return false;
    }

    public void deleteDraft(String authorUsername) {
        String sql = "DELETE FROM drafts WHERE author_username = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, authorUsername);
            pstmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error deleting draft: " + e.getMessage());
        }
    }
}
