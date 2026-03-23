package project.task2.repo;

import project.task2.model.AuthorAccount;
import project.task2.database.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class AuthorRepository {
    private final Connection conn;

    public AuthorRepository() {
        this.conn = DatabaseConnection.getConnection();
    }

    public void save(AuthorAccount author) {
        String sql = """
            INSERT OR REPLACE INTO authors 
            (username, full_name, password_salt, password_hash, bio, registration_date)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, author.getUsername());
            pstmt.setString(2, author.getFullName());
            pstmt.setString(3, author.getPasswordSalt());
            pstmt.setString(4, author.getPasswordHash());
            pstmt.setString(5, author.getBio());
            pstmt.setString(6, LocalDateTime.now().toString());
            
            pstmt.executeUpdate();
            System.out.println("✅ Task2: Author saved: " + author.getUsername());
            
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error saving author: " + e.getMessage());
        }
    }

    public void update(AuthorAccount author) {
        save(author);
        System.out.println("✅ Task2: Author updated: " + author.getUsername());
    }

    public Optional<AuthorAccount> findByUsername(String username) {
        String sql = "SELECT * FROM authors WHERE username = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                AuthorAccount author = new AuthorAccount(
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("password_salt"),
                    rs.getString("password_hash"),
                    rs.getString("bio")
                );
                return Optional.of(author);
            }
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error finding author: " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM authors WHERE username = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error checking username: " + e.getMessage());
        }
        return false;
    }

    public List<AuthorAccount> findAll() {
        List<AuthorAccount> authors = new ArrayList<>();
        String sql = "SELECT * FROM authors ORDER BY username";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                authors.add(new AuthorAccount(
                    rs.getString("username"),
                    rs.getString("full_name"),
                    rs.getString("password_salt"),
                    rs.getString("password_hash"),
                    rs.getString("bio")
                ));
            }
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error finding all authors: " + e.getMessage());
        }
        return authors;
    }

    public int getCount() {
        String sql = "SELECT COUNT(*) FROM authors";
        
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error counting authors: " + e.getMessage());
        }
        return 0;
    }
}
