package project.task2.repo;

import project.task2.model.BookSubmission;
import project.task2.database.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class SubmissionRepository {
    private final Connection conn;

    public SubmissionRepository() {
        this.conn = DatabaseConnection.getConnection();
    }

    public void save(BookSubmission submission) {
        String sql = """
            INSERT INTO submissions 
            (submission_id, title, author_username, author_full_name, genres, 
             description, file_path, submission_date, status, rejection_reason, 
             reviewed_date, reviewed_by, is_draft, last_edited, edit_count)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(submission_id) DO UPDATE SET
            title = excluded.title,
            genres = excluded.genres,
            description = excluded.description,
            file_path = excluded.file_path,
            status = excluded.status,
            rejection_reason = excluded.rejection_reason,
            reviewed_date = excluded.reviewed_date,
            reviewed_by = excluded.reviewed_by,
            is_draft = excluded.is_draft,
            last_edited = excluded.last_edited,
            edit_count = excluded.edit_count
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, submission.getSubmissionId());
            pstmt.setString(2, submission.getTitle());
            pstmt.setString(3, submission.getAuthorUsername());
            pstmt.setString(4, submission.getAuthorFullName());
            pstmt.setString(5, submission.getGenresAsString());
            pstmt.setString(6, submission.getDescription());
            pstmt.setString(7, submission.getFilePath());
            pstmt.setString(8, submission.getSubmissionDate().toString());
            pstmt.setString(9, submission.getStatus());
            pstmt.setString(10, submission.getRejectionReason());
            pstmt.setString(11, submission.getReviewedDate() != null ? submission.getReviewedDate().toString() : null);
            pstmt.setString(12, submission.getReviewedBy());
            pstmt.setInt(13, submission.isDraft() ? 1 : 0);
            pstmt.setString(14, submission.getReviewedDate() != null ? submission.getReviewedDate().toString() : null);
            pstmt.setInt(15, submission.getEditCount());
            
            pstmt.executeUpdate();
            System.out.println("✅ Task2: Submission saved: " + submission.getSubmissionId());
            
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error saving submission: " + e.getMessage());
        }
    }

    public void update(BookSubmission submission) {
        save(submission); // Same as save - will update due to ON CONFLICT clause
    }

    public void delete(String submissionId) {
        String sql = "DELETE FROM submissions WHERE submission_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, submissionId);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("✅ Task2: Submission deleted: " + submissionId);
            } else {
                System.out.println("⚠️ Task2: Submission not found: " + submissionId);
            }
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error deleting submission: " + e.getMessage());
        }
    }

    public List<BookSubmission> findByAuthor(String authorUsername) {
        List<BookSubmission> submissions = new ArrayList<>();
        String sql = "SELECT * FROM submissions WHERE author_username = ? AND is_draft = 0 ORDER BY submission_date DESC";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, authorUsername);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                submissions.add(mapResultSetToSubmission(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error finding submissions: " + e.getMessage());
        }
        return submissions;
    }

    public Optional<BookSubmission> findById(String submissionId) {
        String sql = "SELECT * FROM submissions WHERE submission_id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, submissionId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToSubmission(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error finding submission: " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<BookSubmission> findAll() {
        List<BookSubmission> submissions = new ArrayList<>();
        String sql = "SELECT * FROM submissions WHERE is_draft = 0 ORDER BY submission_date DESC";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                submissions.add(mapResultSetToSubmission(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error finding all submissions: " + e.getMessage());
        }
        return submissions;
    }

    public List<BookSubmission> findPendingSubmissions() {
        List<BookSubmission> submissions = new ArrayList<>();
        String sql = "SELECT * FROM submissions WHERE status = 'PENDING' AND is_draft = 0 ORDER BY submission_date ASC";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                submissions.add(mapResultSetToSubmission(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error finding pending submissions: " + e.getMessage());
        }
        return submissions;
    }

    private BookSubmission mapResultSetToSubmission(ResultSet rs) throws SQLException {
        String genresStr = rs.getString("genres");
        List<String> genres = genresStr != null ? 
            Arrays.asList(genresStr.split(",")) : new ArrayList<>();
        
        // Check if edit_count column exists (for Phase 2)
        int editCount = 0;
        try {
            editCount = rs.getInt("edit_count");
        } catch (SQLException e) {
            // Column might not exist yet, ignore
        }
        
        return new BookSubmission(
            rs.getString("submission_id"),
            rs.getString("title"),
            rs.getString("author_username"),
            rs.getString("author_full_name"),
            genresStr,
            rs.getString("description"),
            rs.getString("file_path"),
            LocalDateTime.parse(rs.getString("submission_date")),
            rs.getString("status"),
            rs.getString("rejection_reason"),
            rs.getString("reviewed_date") != null ? LocalDateTime.parse(rs.getString("reviewed_date")) : null,
            rs.getString("reviewed_by"),
            rs.getInt("is_draft") == 1,
            editCount
        );
    }
}
