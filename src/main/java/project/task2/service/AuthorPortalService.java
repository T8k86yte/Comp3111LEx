package project.task2.service;

import project.task2.model.AuthorAccount;
import project.task2.repo.AuthorRepository;
import project.task2.utils.PasswordUtils;

public class AuthorPortalService {
    private final AuthorRepository authorRepository;

    public AuthorPortalService() {
        this.authorRepository = new AuthorRepository();
    }

    public AuthorPortalService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    // ========== REGISTRATION (AUTHOR)==========
    public RegistrationResult registerAuthor(String username, String fullName, 
                                            String password, String confirmPassword,
                                            String bio) {
        
        // Validation
        if (isBlank(username)) {
            return RegistrationResult.failure("Username is required.");
        }
        
        if (isBlank(fullName)) {
            return RegistrationResult.failure("Full name is required.");
        }
        
        if (isBlank(password)) {
            return RegistrationResult.failure("Password is required.");
        }

        if (!isValidUsername(username)) {
            return RegistrationResult.failure("Username must be at least 3 characters and can only contain letters, numbers, and underscores.");
        }

        if (authorRepository.existsByUsername(username.trim())) {
            return RegistrationResult.failure("Username '" + username + "' is already taken.");
        }

        // Use PasswordUtils for password validation
        if (!PasswordUtils.isStrongPassword(password)) {
            return RegistrationResult.failure(PasswordUtils.getPasswordRequirements());
        }

        if (!password.equals(confirmPassword)) {
            return RegistrationResult.failure("Passwords do not match.");
        }

        if (bio != null && bio.length() > 500) {
            return RegistrationResult.failure("Bio is too long. Maximum 500 characters allowed.");
        }

        try {
            // PROPER HASHING: Generate salt and hash using PasswordUtils
            String salt = PasswordUtils.generateSalt();
            String hash = PasswordUtils.hashPassword(password, salt);

            AuthorAccount author = new AuthorAccount(
                username.trim(),
                fullName.trim(),
                salt,
                hash,
                bio != null ? bio.trim() : ""
            );

            authorRepository.save(author);

            return RegistrationResult.success(
                "Registration successful! Welcome, " + fullName + "!"
            );

        } catch (Exception e) {
            return RegistrationResult.failure("Registration failed: " + e.getMessage());
        }
    }

    // ========== AUTHORLOGIN ==========
    public LoginResult login(String username, String password) {
        // Validation
        if (isBlank(username)) {
            return LoginResult.failure("Username is required.");
        }
        
        if (isBlank(password)) {
            return LoginResult.failure("Password is required.");
        }

        // Find author by username
        var authorOpt = authorRepository.findByUsername(username.trim());
        
        if (authorOpt.isEmpty()) {
            // Use same message for security (don't reveal if username exists)
            return LoginResult.failure("Invalid username or password.");
        }

        AuthorAccount author = authorOpt.get();
        
        // PROPER VERIFICATION: Use PasswordUtils to verify
        boolean passwordMatches = PasswordUtils.verifyPassword(
            password,
            author.getPasswordSalt(),
            author.getPasswordHash()
        );

        if (!passwordMatches) {
            return LoginResult.failure("Invalid username or password.");
        }

        // Login successful
        return LoginResult.success(
            "Login successful! Welcome back, " + author.getFullName() + "!",
            author
        );
    }

    // Get author by username (for session management)
    public AuthorAccount getAuthorByUsername(String username) {
        return authorRepository.findByUsername(username).orElse(null);
    }

    // Check if username exists
    public boolean usernameExists(String username) {
        return authorRepository.existsByUsername(username);
    }

    // ========== VALIDATION HELPER METHODS ==========
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private boolean isValidUsername(String username) {
        String trimmed = username.trim();
        return trimmed.length() >= 3 && trimmed.length() <= 20 && 
               trimmed.matches("^[a-zA-Z0-9_]+$");
    }

    // ========== RESULT CLASSES ==========
    public static class RegistrationResult {
        private final boolean success;
        private final String message;

        private RegistrationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static RegistrationResult success(String message) {
            return new RegistrationResult(true, message);
        }

        public static RegistrationResult failure(String message) {
            return new RegistrationResult(false, message);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    public static class LoginResult {
        private final boolean success;
        private final String message;
        private final AuthorAccount author;

        private LoginResult(boolean success, String message, AuthorAccount author) {
            this.success = success;
            this.message = message;
            this.author = author;
        }

        public static LoginResult success(String message, AuthorAccount author) {
            return new LoginResult(true, message, author);
        }

        public static LoginResult failure(String message) {
            return new LoginResult(false, message, null);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public AuthorAccount getAuthor() { return author; }
    }
}
