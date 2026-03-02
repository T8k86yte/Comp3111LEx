package project.task2.service;

import project.task2.model.AuthorAccount;
import project.task2.repo.AuthorRepository;

public class AuthorPortalService {
    private static final int MIN_PASSWORD_LENGTH = 8;
    
    private final AuthorRepository authorRepository;

    public AuthorPortalService() {
        this.authorRepository = new AuthorRepository();
    }

    // For testing with dependency injection
    public AuthorPortalService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    /**
     * Register a new author with complete validation
     * @return RegistrationResult containing success/failure status and message
     */
    public RegistrationResult registerAuthor(String username, String fullName, 
                                            String password, String confirmPassword,
                                            String bio) {
        
        // Step 1: Validate required fields (except bio)
        if (isBlank(username)) {
            return RegistrationResult.failure("Username is required.");
        }
        
        if (isBlank(fullName)) {
            return RegistrationResult.failure("Full name is required.");
        }
        
        if (isBlank(password)) {
            return RegistrationResult.failure("Password is required.");
        }

        // Step 2: Validate username format
        if (!isValidUsername(username)) {
            return RegistrationResult.failure("Username must be at least 3 characters and can only contain letters, numbers, and underscores.");
        }

        // Step 3: Check if username already exists
        if (authorRepository.existsByUsername(username.trim())) {
            return RegistrationResult.failure("Username '" + username + "' is already taken. Please choose another.");
        }

        // Step 4: Validate password length
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return RegistrationResult.failure("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.");
        }

        // Step 5: Validate password complexity
        if (!hasLetter(password)) {
            return RegistrationResult.failure("Password must contain at least one letter.");
        }

        if (!hasNumber(password)) {
            return RegistrationResult.failure("Password must contain at least one number.");
        }

        if (!hasUpperCase(password)) {
            return RegistrationResult.failure("Password must contain at least one uppercase letter.");
        }

        // Step 6: Check if passwords match
        if (!password.equals(confirmPassword)) {
            return RegistrationResult.failure("Passwords do not match.");
        }

        // Step 7: Validate bio length (optional but can have max length)
        if (bio != null && bio.length() > 500) {
            return RegistrationResult.failure("Bio is too long. Maximum 500 characters allowed.");
        }

        try {
            // Step 8: Create password hash (simplified for now - we'll add proper hashing later)
            String salt = "temp_salt_" + System.currentTimeMillis(); // TODO: Use proper password hashing
            String hash = "temp_hash_" + password; // TODO: Use proper password hashing

            // Step 9: Create and save author
            AuthorAccount author = new AuthorAccount(
                username.trim(),
                fullName.trim(),
                salt,
                hash,
                bio != null ? bio.trim() : ""
            );

            authorRepository.save(author);

            return RegistrationResult.success(
                "Registration successful! Welcome, " + fullName + "! You can now login."
            );

        } catch (Exception e) {
            return RegistrationResult.failure("Registration failed due to system error: " + e.getMessage());
        }
    }

    /**
     * Simple login validation (will be expanded later)
     */
    public LoginResult login(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            return LoginResult.failure("Username and password are required.");
        }

        var authorOpt = authorRepository.findByUsername(username.trim());
        if (authorOpt.isEmpty()) {
            return LoginResult.failure("Invalid username or password.");
        }

        AuthorAccount author = authorOpt.get();
        
        // TODO: Add proper password verification
        if (!password.equals("password123")) { // Temporary - replace with hash verification
            return LoginResult.failure("Invalid username or password.");
        }

        return LoginResult.success("Login successful! Welcome back, " + author.getFullName() + "!", author);
    }

    // ========== Validation Helper Methods ==========

    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    private boolean isValidUsername(String username) {
        String trimmed = username.trim();
        // Username: 3-20 characters, letters, numbers, underscore only
        return trimmed.length() >= 3 && trimmed.length() <= 20 && 
               trimmed.matches("^[a-zA-Z0-9_]+$");
    }

    private boolean hasLetter(String str) {
        return str.matches(".*[a-zA-Z].*");
    }

    private boolean hasNumber(String str) {
        return str.matches(".*\\d.*");
    }

    private boolean hasUpperCase(String str) {
        return str.matches(".*[A-Z].*");
    }

    // ========== Result Classes ==========

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