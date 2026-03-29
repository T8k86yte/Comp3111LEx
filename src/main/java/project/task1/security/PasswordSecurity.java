package project.task1.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordSecurity {
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordSecurity() {
    }

    public static String generateSaltBase64() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hashPasswordBase64(String rawPassword, String saltBase64) {
        byte[] salt = Base64.getDecoder().decode(saltBase64.getBytes(StandardCharsets.UTF_8));
        PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);
        try {
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = keyFactory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Unable to hash password securely", ex);
        } finally {
            spec.clearPassword();
        }
    }

    public static boolean verifyPassword(String rawPassword, String saltBase64, String expectedHashBase64) {
        String candidateHashBase64 = hashPasswordBase64(rawPassword, saltBase64);
        byte[] candidate = Base64.getDecoder().decode(candidateHashBase64.getBytes(StandardCharsets.UTF_8));
        byte[] expected = Base64.getDecoder().decode(expectedHashBase64.getBytes(StandardCharsets.UTF_8));
        return MessageDigest.isEqual(candidate, expected);
    }
}
