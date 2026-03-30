package project.database;

import java.sql.*;

public class DatabaseConnection {
    private static final String DATABASE_URL = "jdbc:sqlite:data/author.db";
    private static Connection connection = null;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            System.out.println("✅ SQLite JDBC driver loaded");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ SQLite JDBC driver not found: " + e.getMessage());
        }
    }

    public static Connection getConnection() {
        if (connection == null) {
            try {
                new java.io.File("data").mkdirs();
                
                connection = DriverManager.getConnection(DATABASE_URL);
                System.out.println("✅ Connected to Author database");
                
                createTables();
                
            } catch (SQLException e) {
                System.err.println("❌ Database connection error: " + e.getMessage());
            }
        }
        return connection;
    }

    private static void createTables() {
        // Authors table for Task 2
        String createAuthorsTable = """
            CREATE TABLE IF NOT EXISTS authors (
                username TEXT PRIMARY KEY,
                full_name TEXT NOT NULL,
                password_salt TEXT NOT NULL,
                password_hash TEXT NOT NULL,
                bio TEXT,
                registration_date TEXT NOT NULL
            )
            """;

        // Book submissions table
        String createSubmissionsTable = """
            CREATE TABLE IF NOT EXISTS submissions (
                submission_id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                author_username TEXT NOT NULL,
                author_full_name TEXT NOT NULL,
                genres TEXT,
                description TEXT NOT NULL,
                file_path TEXT NOT NULL,
                submission_date TEXT NOT NULL,
                status TEXT DEFAULT 'PENDING',
                rejection_reason TEXT,
                reviewed_date TEXT,
                reviewed_by TEXT,
                is_draft INTEGER DEFAULT 0,
                FOREIGN KEY (author_username) REFERENCES authors(username) ON DELETE CASCADE
            )
            """;

        // Drafts table for auto-save feature
        String createDraftsTable = """
            CREATE TABLE IF NOT EXISTS drafts (
                author_username TEXT PRIMARY KEY,
                draft_data TEXT,
                last_updated TEXT,
                FOREIGN KEY (author_username) REFERENCES authors(username) ON DELETE CASCADE
            )
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createAuthorsTable);
            stmt.execute(createSubmissionsTable);
            stmt.execute(createDraftsTable);
            
            System.out.println("✅ Author database tables created/verified");
            System.out.println("   - authors: author accounts");
            System.out.println("   - submissions: book submissions");
            System.out.println("   - drafts: auto-save drafts");
            
        } catch (SQLException e) {
            System.err.println("❌ Error creating tables: " + e.getMessage());
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🔒 Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}
