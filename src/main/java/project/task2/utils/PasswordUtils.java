package project.task2.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtils {
    
    private static final int SALT_LENGTH = 16;
    private static final int HASH_ITERATIONS = 10000;
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final SecureRandom RANDOM = new SecureRandom();
    
    /**
     * Generate a random salt for password hashing
     * @return Base64 encoded salt
     */
    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    /**
     * Hash a password with the given salt using SHA-256 with multiple iterations
     * @param password The plain text password
     * @param salt Base64 encoded salt
     * @return Base64 encoded hash
     */
    public static String hashPassword(String password, String salt) {
        try {
            byte[] saltBytes = Base64.getDecoder().decode(salt);
            byte[] passwordBytes = password.getBytes("UTF-8");
            
            // Combine password and salt
            byte[] combined = new byte[passwordBytes.length + saltBytes.length];
            System.arraycopy(passwordBytes, 0, combined, 0, passwordBytes.length);
            System.arraycopy(saltBytes, 0, combined, passwordBytes.length, saltBytes.length);
            
            // Apply hashing multiple times for security
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = combined;
            
            for (int i = 0; i < HASH_ITERATIONS; i++) {
                hash = md.digest(hash);
            }
            
            return Base64.getEncoder().encodeToString(hash);
            
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    /**
     * Verify a password against a stored hash and salt
     * @param password The plain text password to verify
     * @param salt The stored salt
     * @param expectedHash The stored hash to compare against
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String salt, String expectedHash) {
        String computedHash = hashPassword(password, salt);
        return MessageDigest.isEqual(
            computedHash.getBytes(),
            expectedHash.getBytes()
        );
    }
    
    /**
     * Check if a password meets minimum strength requirements
     * @param password The password to check
     * @return true if password is strong enough
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasNumber = password.matches(".*\\d.*");
        boolean hasUppercase = password.matches(".*[A-Z].*");
        
        return hasLetter && hasNumber && hasUppercase;
    }
    
    /**
     * Get password requirements message
     * @return String with password requirements
     */
    public static String getPasswordRequirements() {
        return "Password must be:\n" +
               "  • At least 8 characters long\n" +
               "  • Contain at least one letter\n" +
               "  • Contain at least one number\n" +
               "  • Contain at least one uppercase letter";
    }
}
