import java.sql.*;

public class TestSQLite {
    public static void main(String[] args) {
        try {
            // Explicitly load the driver
            Class.forName("org.sqlite.JDBC");
            System.out.println("✅ SQLite JDBC driver loaded!");
            
            // Test connection
            Connection conn = DriverManager.getConnection("jdbc:sqlite:data/test.db");
            System.out.println("✅ Connection successful!");
            conn.close();
            
        } catch (ClassNotFoundException e) {
            System.out.println("❌ Driver not found: " + e.getMessage());
            System.out.println("Make sure sqlite-jdbc.jar is in classpath");
        } catch (SQLException e) {
            System.out.println("❌ SQL Error: " + e.getMessage());
        }
    }
}
