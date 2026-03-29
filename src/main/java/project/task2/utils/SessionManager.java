package project.task2.utils;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages session persistence for crash recovery.
 * Automatically restores the previous session on startup.
 */
public class SessionManager {
    private static final String SESSION_FILE = "data/session.dat";
    
    // Store the current state
    private static String currentUsername = null;
    private static String currentFullName = null;
    private static String currentScreen = null;
    private static Map<String, String> screenStates = new HashMap<>();
    
    /**
     * Set the current user session
     */
    public static void setCurrentUser(String username, String fullName) {
        currentUsername = username;
        currentFullName = fullName;
        saveSession();
        System.out.println("👤 User set: " + username);
    }
    
    /**
     * Set the current screen
     */
    public static void setCurrentScreen(String screenName, String additionalData) {
        currentScreen = screenName;
        if (additionalData != null) {
            screenStates.put(screenName, additionalData);
        }
        saveSession();
        System.out.println("📱 Screen set: " + screenName);
    }
    
    /**
     * Save screen state data (like form contents)
     */
    public static void saveScreenState(String screenName, String stateData) {
        if (stateData != null && !stateData.isEmpty()) {
            screenStates.put(screenName, stateData);
            saveSession();
            System.out.println("💾 Screen state saved for: " + screenName);
        }
    }
    
    /**
     * Get saved screen state
     */
    public static String getScreenState(String screenName) {
        return screenStates.get(screenName);
    }
    
    /**
     * Get current username
     */
    public static String getCurrentUsername() {
        return currentUsername;
    }
    
    /**
     * Get current full name
     */
    public static String getCurrentFullName() {
        return currentFullName;
    }
    
    /**
     * Get current screen
     */
    public static String getCurrentScreen() {
        return currentScreen;
    }
    
    /**
     * Check if there's a saved session
     */
    public static boolean hasSavedSession() {
        return currentUsername != null && !currentUsername.isEmpty();
    }
    
    /**
     * Save everything to file
     */
    public static void saveSession() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("USER:").append(currentUsername != null ? currentUsername : "").append("\n");
            sb.append("FULLNAME:").append(currentFullName != null ? currentFullName : "").append("\n");
            sb.append("SCREEN:").append(currentScreen != null ? currentScreen : "").append("\n");
            sb.append("TIMESTAMP:").append(LocalDateTime.now().toString()).append("\n");
            sb.append("STATES:\n");
            for (Map.Entry<String, String> entry : screenStates.entrySet()) {
                sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
            }
            
            Path sessionPath = Paths.get(SESSION_FILE);
            Files.write(sessionPath, sb.toString().getBytes(), 
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            System.out.println("💾 Session saved to file");
        } catch (IOException e) {
            System.err.println("Error saving session: " + e.getMessage());
        }
    }
    
    /**
     * Load everything from file
     */
    public static void loadSession() {
        try {
            Path sessionPath = Paths.get(SESSION_FILE);
            if (Files.exists(sessionPath)) {
                String content = new String(Files.readAllBytes(sessionPath));
                String[] lines = content.split("\n");
                
                screenStates.clear();
                
                boolean inStates = false;
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    
                    if (line.equals("STATES:")) {
                        inStates = true;
                        continue;
                    }
                    
                    if (!inStates) {
                        if (line.startsWith("USER:")) {
                            currentUsername = line.substring(5);
                            if (currentUsername.isEmpty()) currentUsername = null;
                        } else if (line.startsWith("FULLNAME:")) {
                            currentFullName = line.substring(9);
                            if (currentFullName.isEmpty()) currentFullName = null;
                        } else if (line.startsWith("SCREEN:")) {
                            currentScreen = line.substring(7);
                            if (currentScreen.isEmpty()) currentScreen = null;
                            System.out.println("📂 Loaded screen: " + currentScreen);
                        }
                    } else {
                        int eqPos = line.indexOf('=');
                        if (eqPos > 0) {
                            String key = line.substring(0, eqPos);
                            String value = line.substring(eqPos + 1);
                            if (!value.isEmpty()) {
                                screenStates.put(key, value);
                            }
                        }
                    }
                }
                
                if (currentUsername != null) {
                    System.out.println("📂 Session loaded: user=" + currentUsername + ", screen=" + currentScreen);
                } else {
                    System.out.println("📂 No valid session found");
                }
            } else {
                System.out.println("📂 No session file found");
            }
        } catch (IOException e) {
            System.err.println("Error loading session: " + e.getMessage());
        }
    }
    
    /**
     * Clear all session data
     */
    public static void clearSession() {
        currentUsername = null;
        currentFullName = null;
        currentScreen = null;
        screenStates.clear();
        
        try {
            Files.deleteIfExists(Paths.get(SESSION_FILE));
            System.out.println("🧹 Session data cleared");
        } catch (IOException e) {
            System.err.println("Error clearing session: " + e.getMessage());
        }
    }
    
    /**
     * Auto-save is called automatically on changes
     */
    public static void autoSave() {
        if (currentUsername != null) {
            saveSession();
        }
    }
}
