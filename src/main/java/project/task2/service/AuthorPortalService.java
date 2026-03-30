package project.task2.service;

import project.task2.model.AuthorAccount;
import project.task2.repo.AuthorRepository;
import project.task2.utils.PasswordUtils;
import project.task2.model.BookSubmission;
import project.task2.repo.SubmissionRepository;
import project.task2.repo.DraftRepository;
import project.task2.repo.NotificationRepository;
import project.task2.model.Notification;
import project.task2.utils.FileHandler;

import java.util.List;
import java.util.Arrays;

public class AuthorPortalService {
    private final AuthorRepository authorRepository;
    private final SubmissionRepository submissionRepository;
    private final DraftRepository draftRepository;
    private final NotificationRepository notificationRepository;

    public AuthorPortalService() {
        this.authorRepository = new AuthorRepository();
        this.submissionRepository = new SubmissionRepository();
        this.draftRepository = new DraftRepository();
        this.notificationRepository = new NotificationRepository();
    }

    // ========== REGISTRATION ==========
    public RegistrationResult registerAuthor(String username, String fullName, 
                                            String password, String confirmPassword,
                                            String bio) {
        
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
            String salt = PasswordUtils.generateSalt();
            String hash = PasswordUtils.hashPassword(password, salt);

            AuthorAccount author = new AuthorAccount(
                username.trim(),
                fullName.trim(),
                salt,
                hash,
                false,
                bio != null ? bio.trim() : ""
            );

            authorRepository.save(author);
            return RegistrationResult.success("Registration successful! Welcome, " + fullName + "!");

        } catch (Exception e) {
            return RegistrationResult.failure("Registration failed: " + e.getMessage());
        }
    }

    // ========== LOGIN ==========
    public LoginResult login(String username, String password) {
        if (isBlank(username)) {
            return LoginResult.failure("Username is required.");
        }
        if (isBlank(password)) {
            return LoginResult.failure("Password is required.");
        }

        var authorOpt = authorRepository.findByUsername(username.trim());
        
        if (authorOpt.isEmpty()) {
            return LoginResult.failure("Invalid username or password.");
        }

        AuthorAccount author = authorOpt.get();
        
        boolean passwordMatches = PasswordUtils.verifyPassword(
            password,
            author.getPasswordSalt(),
            author.getPasswordHash()
        );

        if (!passwordMatches) {
            return LoginResult.failure("Invalid username or password.");
        }

        return LoginResult.success("Login successful! Welcome back, " + author.getFullName() + "!", author);
    }

    // ========== DRAFT METHODS ==========
    public void saveDraft(String authorUsername, String title, List<String> genres, 
                          String description, String filePath, String coverImagePath) {
        String genresStr = genres != null ? String.join(",", genres) : "";
        String draftData = String.join("|",
            title != null ? title : "",
            genresStr,
            description != null ? description : "",
            filePath != null ? filePath : "",
            coverImagePath != null ? coverImagePath : ""
        );
        
        if (!title.isEmpty() || !genresStr.isEmpty() || !description.isEmpty() || 
            !filePath.isEmpty() || !coverImagePath.isEmpty()) {
            draftRepository.saveDraft(authorUsername, draftData);
        } else {
            draftRepository.deleteDraft(authorUsername);
        }
    }

    public String[] loadDraft(String authorUsername) {
        String draftData = draftRepository.loadDraft(authorUsername);
        if (draftData == null || draftData.isEmpty()) {
            return null;
        }
        
        String[] parts = draftData.split("\\|", 5);
        if (parts.length >= 4) {
            return parts; // [title, genres, description, filePath, coverImagePath]
        }
        return null;
    }

    public void clearDraft(String authorUsername) {
        draftRepository.deleteDraft(authorUsername);
    }

    // ========== BOOK SUBMISSION ==========
    public SubmissionResult submitBookForApproval(String authorUsername, String authorFullName,
                                              String title, String genresStr, 
                                              String description, String filePath,
                                              String coverImagePath) {
    
        if (isBlank(title)) {
            return SubmissionResult.failure("Book title is required.");
        }
        if (isBlank(genresStr)) {
            return SubmissionResult.failure("Please select at least one genre.");
        }
        if (isBlank(description)) {
            return SubmissionResult.failure("Description is required.");
        }
        if (isBlank(filePath)) {
            return SubmissionResult.failure("Book file is required.");
        }

        if (!FileHandler.isValidFileType(filePath)) {
            return SubmissionResult.failure("Invalid file type. Allowed: " + 
                FileHandler.getAllowedFileTypes());
        }

        // Validate cover image if provided
        if (coverImagePath != null && !coverImagePath.isEmpty()) {
            if (!isValidCoverImage(coverImagePath)) {
                return SubmissionResult.failure("Invalid cover image. Allowed: JPG, PNG (max 5MB)");
            }
        }

        try {
            List<String> genres = Arrays.asList(genresStr.split(","));
            
            BookSubmission submission = new BookSubmission(
                title, authorUsername, authorFullName, 
                genres, description, filePath, coverImagePath
            );

            submissionRepository.save(submission);
            clearDraft(authorUsername);
            
            // Send notification for submission
            sendNotification(authorUsername, 
                "📝 Book Submitted: " + title,
                "Your book '" + title + "' has been submitted and is pending review by a librarian.",
                "BOOK_SUBMITTED",
                submission.getSubmissionId());

            return SubmissionResult.success(
                "Book '" + title + "' submitted successfully!\n" +
                "Submission ID: " + submission.getSubmissionId()
            );

        } catch (Exception e) {
            return SubmissionResult.failure("Submission failed: " + e.getMessage());
        }
    }

    private boolean isValidCoverImage(String filePath) {
        String lower = filePath.toLowerCase();
        // Check extension
        if (!lower.endsWith(".jpg") && !lower.endsWith(".jpeg") && !lower.endsWith(".png")) {
            return false;
        }
        // Check file size (5MB limit)
        try {
            java.io.File file = new java.io.File(filePath);
            if (file.exists() && file.length() > 5 * 1024 * 1024) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public List<BookSubmission> getAuthorSubmissions(String authorUsername) {
        submissionRepository.refreshFromFile();
        return submissionRepository.findByAuthor(authorUsername);
    }

    // ========== PROFILE MANAGEMENT ==========
    public boolean updateProfile(AuthorAccount author) {
        try {
            authorRepository.update(author);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Task2: Error updating profile: " + e.getMessage());
            return false;
        }
    }

    // ========== BOOK EDITING & DELETION ==========
    public boolean updateSubmission(BookSubmission submission) {
        try {
            submissionRepository.update(submission);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Task2: Error updating submission: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteSubmission(String submissionId) {
        try {
            submissionRepository.delete(submissionId);
            return true;
        } catch (Exception e) {
            System.err.println("❌ Task2: Error deleting submission: " + e.getMessage());
            return false;
        }
    }

    // ========== NOTIFICATION METHODS ==========
    public void sendNotification(String authorUsername, String title, String message, 
                                  String type, String relatedSubmissionId) {
        Notification notification = new Notification(authorUsername, title, message, type, relatedSubmissionId);
        notificationRepository.save(notification);
    }

    public List<Notification> getNotifications(String authorUsername) {
        return notificationRepository.findByAuthor(authorUsername);
    }

    public int getUnreadNotificationCount(String authorUsername) {
        return notificationRepository.getUnreadCount(authorUsername);
    }

    public void markNotificationAsRead(String notificationId) {
        notificationRepository.markAsRead(notificationId);
    }

    public void markAllNotificationsAsRead(String authorUsername) {
        notificationRepository.markAllAsRead(authorUsername);
    }

    // ========== NOTIFICATION DELETE METHODS ==========
    public void deleteNotification(String notificationId) {
        notificationRepository.delete(notificationId);
    }
    
    public void deleteAllNotifications(String authorUsername) {
        notificationRepository.deleteAllByAuthor(authorUsername);
    }
    
    public void deleteReadNotifications(String authorUsername) {
        notificationRepository.deleteReadByAuthor(authorUsername);
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

    public static class SubmissionResult {
        private final boolean success;
        private final String message;

        private SubmissionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static SubmissionResult success(String message) {
            return new SubmissionResult(true, message);
        }

        public static SubmissionResult failure(String message) {
            return new SubmissionResult(false, message);
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}
