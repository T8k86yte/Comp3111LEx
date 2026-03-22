package project.task2.database;

import java.sql.*;

public class DatabaseConnection {
    private static final String DATABASE_URL = "jdbc:sqlite:data/task2.db";
    private static Connection connection = null;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
            System.out.println("✅ Task2: SQLite JDBC driver loaded");
        } catch (ClassNotFoundException e) {
            System.err.println("❌ Task2: SQLite JDBC driver not found: " + e.getMessage());
        }
    }

    public static Connection getConnection() {
        if (connection == null) {
            try {
                new java.io.File("data").mkdirs();
                
                connection = DriverManager.getConnection(DATABASE_URL);
                System.out.println("✅ Task2: Connected to database");
                
                createTables();
                upgradeTables();
                
            } catch (SQLException e) {
                System.err.println("❌ Task2: Database connection error: " + e.getMessage());
            }
        }
        return connection;
    }

    private static void createTables() {
        // Authors table
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
                last_edited TEXT,
                edit_count INTEGER DEFAULT 0
            )
            """;

        // Drafts table for auto-save
        String createDraftsTable = """
            CREATE TABLE IF NOT EXISTS drafts (
                author_username TEXT PRIMARY KEY,
                draft_data TEXT,
                last_updated TEXT,
                FOREIGN KEY (author_username) REFERENCES authors(username) ON DELETE CASCADE
            )
            """;

        // Notifications table
        String createNotificationsTable = """
            CREATE TABLE IF NOT EXISTS notifications (
                notification_id TEXT PRIMARY KEY,
                author_username TEXT NOT NULL,
                title TEXT NOT NULL,
                message TEXT NOT NULL,
                type TEXT NOT NULL,
                is_read INTEGER DEFAULT 0,
                created_at TEXT NOT NULL,
                related_submission_id TEXT,
                FOREIGN KEY (author_username) REFERENCES authors(username) ON DELETE CASCADE
            )
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createAuthorsTable);
            stmt.execute(createSubmissionsTable);
            stmt.execute(createDraftsTable);
            stmt.execute(createNotificationsTable);
            
            System.out.println("✅ Task2: Database tables created/verified");
            
        } catch (SQLException e) {
            System.err.println("❌ Task2: Error creating tables: " + e.getMessage());
        }
    }

    private static void upgradeTables() {
        try {
            String addLastEdited = "ALTER TABLE submissions ADD COLUMN last_edited TEXT";
            connection.createStatement().execute(addLastEdited);
        } catch (SQLException e) {
            // Column already exists
        }
        
        try {
            String addEditCount = "ALTER TABLE submissions ADD COLUMN edit_count INTEGER DEFAULT 0";
            connection.createStatement().execute(addEditCount);
        } catch (SQLException e) {
            // Column already exists
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("🔒 Task2: Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Task2: Error closing connection: " + e.getMessage());
        }
    }
}
