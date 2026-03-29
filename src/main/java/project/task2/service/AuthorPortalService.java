package project.task2.service;

import project.task2.model.AuthorAccount;
import project.task1.model.Book;
import project.task1.repo.StudentStaffRepository;
import project.task3.repo.LibrarianRepository;
import project.shared.SharedAuthFacade;
import project.task2.repo.AuthorRepository;
import project.task1.repo.InMemoryBookRepository;

import project.task2.model.BookSubmission;
import project.task2.repo.SubmissionRepository;
import project.task2.repo.DraftRepository;
import project.task2.utils.FileHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AuthorPortalService {
    private static final String TASK2_NOTIFICATIONS_FILE = "data/task2/notifications.txt";
    private static final DateTimeFormatter HISTORY_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final AuthorRepository authorRepository;
    private final SharedAuthFacade sharedAuthFacade;
    private final SubmissionRepository submissionRepository;
    private final DraftRepository draftRepository;
    private final InMemoryBookRepository bookRepository;

    public AuthorPortalService() {
        this.authorRepository = new AuthorRepository();
        this.sharedAuthFacade = new SharedAuthFacade(
                new StudentStaffRepository(),
                authorRepository,
                new LibrarianRepository()
        );
        this.submissionRepository = new SubmissionRepository();
        this.draftRepository = new DraftRepository();
        this.bookRepository = new InMemoryBookRepository();
    }

    public AuthorPortalService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
        this.sharedAuthFacade = new SharedAuthFacade(
                new StudentStaffRepository(),
                authorRepository,
                new LibrarianRepository()
        );
        this.submissionRepository = new SubmissionRepository();
        this.draftRepository = new DraftRepository();
        this.bookRepository = new InMemoryBookRepository();
    }

    // ========== REGISTRATION (AUTHOR)==========
    public RegistrationResult registerAuthor(String username, String fullName, 
                                            String password, String confirmPassword,
                                            String bio) {
        // Keep author-specific input validation in service.
        if (!isValidUsername(username)) {
            return RegistrationResult.failure("Username must be at least 3 characters and can only contain letters, numbers, and underscores.");
        }
        if (bio != null && bio.length() > 500) {
            return RegistrationResult.failure("Bio is too long. Maximum 500 characters allowed.");
        }

        SharedAuthFacade.AuthResult authResult = sharedAuthFacade.register(
                username,
                fullName,
                password,
                confirmPassword,
                "Author",
                bio,
                null
        );
        if (!authResult.success()) {
            return RegistrationResult.failure(authResult.message());
        }
        return RegistrationResult.success(authResult.message());
    }

    // ========== AUTHORLOGIN ==========
    public LoginResult login(String username, String password) {
        SharedAuthFacade.AuthResult authResult = sharedAuthFacade.login(username, password, "Author");
        if (!authResult.success()) {
            return LoginResult.failure(authResult.message());
        }
        AuthorAccount author = authorRepository.findByUsername(authResult.principal().username()).orElse(null);
        if (author == null) {
            return LoginResult.failure("Invalid username or password.");
        }
        return LoginResult.success(authResult.message(), author);
    }

    // ========== DRAFT METHODS ==========
    public void saveDraft(String authorUsername, String title, List<String> genres, 
                          String description, String filePath) {
        saveDraft(authorUsername, title, genres, description, filePath, "");
    }

    public void saveDraft(
            String authorUsername,
            String title,
            List<String> genres,
            String description,
            String filePath,
            String coverImagePath
    ) {
        // Format draft data
        String genresStr = genres != null ? String.join(",", genres) : "";
        String draftData = String.join("|",
            title != null ? title : "",
            genresStr,
            description != null ? description : "",
            filePath != null ? filePath : "",
            coverImagePath != null ? coverImagePath : ""
        );
        
        // Only save if there's some content
        if (!title.isEmpty() || !genresStr.isEmpty() || !description.isEmpty() || !filePath.isEmpty()
                || (coverImagePath != null && !coverImagePath.isEmpty())) {
            draftRepository.saveDraft(authorUsername, draftData);
        } else {
            // If all empty, delete any existing draft
            draftRepository.deleteDraft(authorUsername);
        }
    }

    public String[] loadDraft(String authorUsername) {
        String draftData = draftRepository.loadDraft(authorUsername);
        if (draftData == null || draftData.isEmpty()) {
            return null;
        }
        
        String[] parts = draftData.split("\\|", -1);
        if (parts.length >= 5) {
            return new String[]{parts[0], parts[1], parts[2], parts[3], parts[4]};
        }
        if (parts.length == 4) {
            return new String[]{parts[0], parts[1], parts[2], parts[3], ""};
        }
        return null;
    }

    public boolean hasDraft(String authorUsername) {
        return draftRepository.hasDraft(authorUsername);
    }

    public void clearDraft(String authorUsername) {
        draftRepository.deleteDraft(authorUsername);
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

    // ========== TASK 2.3: BOOK SUBMISSION RESULT CLASS ==========
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

    // ========== TASK 2.3: BOOK SUBMISSION METHODS ==========
    public SubmissionResult submitBookForApproval(String authorUsername, String authorFullName,
                                              String title, String genresStr, 
                                              String description, String filePath) {
        return submitBookForApproval(authorUsername, authorFullName, title, genresStr, description, filePath, "");
    }

    public SubmissionResult submitBookForApproval(
            String authorUsername,
            String authorFullName,
            String title,
            String genresStr,
            String description,
            String filePath,
            String coverImagePath
    ) {
    
        // Validation
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

        // Validate file type
        if (!FileHandler.isValidFileType(filePath)) {
            return SubmissionResult.failure("Invalid file type. Allowed: " + 
                FileHandler.getAllowedFileTypes());
        }
        if (!FileHandler.isWithinBookFileSizeLimit(Path.of(filePath))) {
            return SubmissionResult.failure("Book file is too large. Max allowed: "
                    + (FileHandler.getMaxBookFileSizeBytes() / (1024 * 1024)) + "MB.");
        }
        String normalizedCover = coverImagePath == null ? "" : coverImagePath.trim();
        if (!normalizedCover.isEmpty()) {
            if (!FileHandler.isValidImageType(normalizedCover)) {
                return SubmissionResult.failure("Invalid cover image type. Allowed: " + FileHandler.getAllowedImageTypes());
            }
            if (!FileHandler.isWithinImageFileSizeLimit(Path.of(normalizedCover))) {
                return SubmissionResult.failure("Cover image is too large. Max allowed: "
                        + (FileHandler.getMaxImageFileSizeBytes() / (1024 * 1024)) + "MB.");
            }
        }

        try {
            // Persist selected genres as a comma-separated string.
            BookSubmission submission = new BookSubmission(
                title, authorUsername, authorFullName, 
                genresStr, description, filePath, normalizedCover
            );

            // Save to repository
            submissionRepository.save(submission);
            appendSystemNotificationForLibrarians(
                    "SUBMISSION_REQUEST",
                    "New submission received: \"" + title + "\" by " + authorUsername + "."
            );
            
            // Clear draft after successful submission
            clearDraft(authorUsername);

            return SubmissionResult.success(
                "Book '" + title + "' submitted successfully!\n" +
                "Submission ID: " + submission.getSubmissionId()
            );

        } catch (Exception e) {
            return SubmissionResult.failure("Submission failed: " + e.getMessage());
        }
    }

    // Get all user book submission result (both approved and rejected)
    public List<BookSubmission> getAuthorSubmissions(String authorUsername) {
        return submissionRepository.findByAuthor(authorUsername);
    }

    public SubmissionResult editPendingSubmission(
            String authorUsername,
            String submissionId,
            String title,
            String genresStr,
            String description,
            String filePath,
            String coverImagePath
    ) {
        Optional<BookSubmission> existingOpt = submissionRepository.findById(submissionId);
        if (existingOpt.isEmpty()) {
            return SubmissionResult.failure("Edit failed: submission not found.");
        }
        BookSubmission existing = existingOpt.get();
        if (!existing.getAuthorUsername().equals(authorUsername)) {
            return SubmissionResult.failure("Edit failed: submission does not belong to current author.");
        }
        if (!existing.isPending()) {
            return SubmissionResult.failure("Edit failed: only pending submissions can be edited.");
        }
        if (isBlank(title) || isBlank(genresStr) || isBlank(description) || isBlank(filePath)) {
            return SubmissionResult.failure("Edit failed: title, genre, description, and file are required.");
        }
        if (!FileHandler.isValidFileType(filePath)) {
            return SubmissionResult.failure("Edit failed: invalid file type. Allowed: " + FileHandler.getAllowedFileTypes());
        }
        if (!FileHandler.isWithinBookFileSizeLimit(Path.of(filePath))) {
            return SubmissionResult.failure("Edit failed: book file exceeds size limit.");
        }
        String normalizedCover = coverImagePath == null ? "" : coverImagePath.trim();
        if (!normalizedCover.isEmpty()) {
            if (!FileHandler.isValidImageType(normalizedCover)) {
                return SubmissionResult.failure("Edit failed: invalid cover image type. Allowed: " + FileHandler.getAllowedImageTypes());
            }
            if (!FileHandler.isWithinImageFileSizeLimit(Path.of(normalizedCover))) {
                return SubmissionResult.failure("Edit failed: cover image exceeds size limit.");
            }
        }
        BookSubmission updated = new BookSubmission(
                existing.getSubmissionId(),
                title.trim(),
                existing.getAuthorUsername(),
                existing.getAuthorFullName(),
                genresStr.trim(),
                description.trim(),
                filePath.trim(),
                normalizedCover,
                existing.getSubmissionDate(),
                existing.getStatus(),
                existing.getRejectionReason(),
                existing.getReviewedDate(),
                existing.getReviewedBy()
        );
        submissionRepository.update(updated);
        return SubmissionResult.success("Submission updated successfully.");
    }

    public SubmissionResult deleteSubmission(String authorUsername, String submissionId) {
        Optional<BookSubmission> existingOpt = submissionRepository.findById(submissionId);
        if (existingOpt.isEmpty()) {
            return SubmissionResult.failure("Delete failed: submission not found.");
        }
        BookSubmission existing = existingOpt.get();
        if (!existing.getAuthorUsername().equals(authorUsername)) {
            return SubmissionResult.failure("Delete failed: submission does not belong to current author.");
        }
        if (existing.isApproved()) {
            boolean borrowed = bookRepository.findAll()
                    .stream()
                    .filter(b -> b.getTitle().equalsIgnoreCase(existing.getTitle()))
                    .filter(b -> b.getAuthor().equalsIgnoreCase(existing.getAuthorFullName()))
                    .anyMatch(b -> !b.isAvailable());
            if (borrowed) {
                return SubmissionResult.failure("Delete failed: approved book is currently borrowed.");
            }
        } else if (!existing.isPending()) {
            return SubmissionResult.failure("Delete failed: rejected submissions cannot be deleted.");
        }
        boolean deleted = submissionRepository.deleteById(submissionId);
        if (!deleted) {
            return SubmissionResult.failure("Delete failed: cannot remove submission.");
        }
        return SubmissionResult.success("Submission deleted successfully.");
    }

    public RegistrationResult updateProfile(
            String username,
            String newFullName,
            String newPassword,
            String confirmPassword,
            String newBio,
            String currentPassword
    ) {
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedFullName = newFullName == null ? "" : newFullName.trim();
        String pwd = newPassword == null ? "" : newPassword;
        String confirm = confirmPassword == null ? "" : confirmPassword;
        String normalizedBio = newBio == null ? "" : newBio.trim();
        String currentPwd = currentPassword == null ? "" : currentPassword;
        if (normalizedUsername.isEmpty()) {
            return RegistrationResult.failure("Profile update failed: invalid user.");
        }
        Optional<AuthorAccount> existingOpt = authorRepository.findByUsername(normalizedUsername);
        if (existingOpt.isEmpty()) {
            return RegistrationResult.failure("Profile update failed: account not found.");
        }
        AuthorAccount existing = existingOpt.get();
        if (normalizedFullName.isEmpty()) {
            return RegistrationResult.failure("Profile update failed: full name is required.");
        }
        if (normalizedBio.length() > 500) {
            return RegistrationResult.failure("Profile update failed: bio is too long.");
        }
        boolean fullNameChanged = !normalizedFullName.equals(existing.getFullName());
        boolean bioChanged = !normalizedBio.equals(existing.getBio());
        boolean passwordChanged = !pwd.isBlank() || !confirm.isBlank();
        boolean requiresReAuth = fullNameChanged || bioChanged || passwordChanged;
        if (requiresReAuth) {
            SharedAuthFacade.AuthResult reauth = sharedAuthFacade.login(normalizedUsername, currentPwd, "Author");
            if (!reauth.success()) {
                return RegistrationResult.failure("Profile update failed: current password is incorrect.");
            }
        }
        String salt = existing.getPasswordSaltBase64();
        String hash = existing.getPasswordHashBase64();
        if (passwordChanged) {
            if (!pwd.equals(confirm)) {
                return RegistrationResult.failure("Profile update failed: passwords do not match.");
            }
            if (!project.task2.utils.PasswordUtils.isStrongPassword(pwd)) {
                return RegistrationResult.failure("Profile update failed: weak password.");
            }
            salt = project.task2.utils.PasswordUtils.generateSalt();
            hash = project.task2.utils.PasswordUtils.hashPassword(pwd, salt);
        }
        authorRepository.save(new AuthorAccount(
                existing.getUsername(),
                normalizedFullName,
                salt,
                hash,
                existing.isDisabled(),
                normalizedBio
        ));
        appendNotification(normalizedUsername, "ANNOUNCEMENT", "Your author profile was updated successfully.");
        return RegistrationResult.success("Profile updated successfully.");
    }

    public List<NotificationView> getNotificationBoard(String username, String categoryFilter, String keyword, boolean unreadOnly) {
        String normalizedUser = username == null ? "" : username.trim();
        if (normalizedUser.isEmpty()) {
            return List.of();
        }
        String normalizedCategory = categoryFilter == null ? "ALL" : categoryFilter.trim().toUpperCase();
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        return loadAllNotifications().stream()
                .filter(n -> normalizedUser.equals(n.username()))
                .filter(n -> "ALL".equals(normalizedCategory) || n.category().equalsIgnoreCase(normalizedCategory))
                .filter(n -> normalizedKeyword.isEmpty()
                        || n.message().toLowerCase().contains(normalizedKeyword)
                        || n.category().toLowerCase().contains(normalizedKeyword))
                .filter(n -> !unreadOnly || !n.read())
                .sorted((a, b) -> {
                    boolean aUrgent = isUrgentCategory(a.category());
                    boolean bUrgent = isUrgentCategory(b.category());
                    if (aUrgent != bUrgent) {
                        return Boolean.compare(bUrgent, aUrgent);
                    }
                    return b.timestamp().compareTo(a.timestamp());
                })
                .collect(Collectors.toList());
    }

    public SubmissionResult markNotificationRead(String username, String notificationId) {
        return updateNotificationState(username, notificationId, true, false);
    }

    public SubmissionResult deleteNotification(String username, String notificationId) {
        return updateNotificationState(username, notificationId, false, true);
    }

    private SubmissionResult updateNotificationState(String username, String notificationId, boolean markRead, boolean delete) {
        String normalizedUser = username == null ? "" : username.trim();
        String normalizedId = notificationId == null ? "" : notificationId.trim();
        if (normalizedUser.isEmpty() || normalizedId.isEmpty()) {
            return SubmissionResult.failure("Notification update failed: invalid input.");
        }
        List<NotificationView> rows = loadAllNotifications();
        List<NotificationView> updated = new ArrayList<>();
        boolean matched = false;
        for (NotificationView row : rows) {
            if (!normalizedUser.equals(row.username()) || !row.id().equals(normalizedId)) {
                updated.add(row);
                continue;
            }
            matched = true;
            if (!delete) {
                updated.add(new NotificationView(row.id(), row.username(), row.timestamp(), row.category(), row.message(), markRead || row.read()));
            }
        }
        if (!matched) {
            return SubmissionResult.failure("Notification update failed: target not found.");
        }
        saveNotifications(updated);
        return SubmissionResult.success(delete ? "Notification deleted." : "Notification marked as read.");
    }

    private List<NotificationView> loadAllNotifications() {
        try {
            if (!Files.exists(Paths.get(TASK2_NOTIFICATIONS_FILE))) {
                return List.of();
            }
            List<NotificationView> rows = new ArrayList<>();
            for (String line : Files.readAllLines(Paths.get(TASK2_NOTIFICATIONS_FILE))) {
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                if (parts.length < 4) {
                    continue;
                }
                String u = decode(parts[0]);
                String category = decode(parts[1]);
                String message = decode(parts[2]);
                LocalDateTime timestamp = LocalDateTime.parse(parts[3], HISTORY_TIME_FORMAT);
                boolean read = parts.length >= 5 && "1".equals(parts[4]);
                String id = parts.length >= 6 && !parts[5].isBlank()
                        ? parts[5]
                        : Integer.toHexString((u + "|" + category + "|" + message + "|" + timestamp).hashCode());
                rows.add(new NotificationView(id, u, timestamp, category, message, read));
            }
            return rows;
        } catch (Exception e) {
            return List.of();
        }
    }

    private void saveNotifications(List<NotificationView> rows) {
        try {
            Files.createDirectories(Paths.get("data/task2"));
            List<String> lines = rows.stream()
                    .map(n -> String.join("|",
                            encode(n.username()),
                            encode(n.category()),
                            encode(n.message()),
                            n.timestamp().format(HISTORY_TIME_FORMAT),
                            n.read() ? "1" : "0",
                            n.id()
                    ))
                    .collect(Collectors.toList());
            Files.write(Paths.get(TASK2_NOTIFICATIONS_FILE), lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    private void appendNotification(String username, String category, String message) {
        try {
            Files.createDirectories(Paths.get("data/task2"));
            String id = username + ":" + Long.toHexString(System.nanoTime());
            String line = String.join("|",
                    encode(username),
                    encode(category),
                    encode(message),
                    LocalDateTime.now().format(HISTORY_TIME_FORMAT),
                    "0",
                    id
            );
            Files.write(Paths.get(TASK2_NOTIFICATIONS_FILE), List.of(line), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    private void appendSystemNotificationForLibrarians(String category, String message) {
        try {
            Files.createDirectories(Paths.get("data/task3"));
            String line = String.join("|",
                    encode("SYSTEM"),
                    encode(category),
                    encode(message),
                    LocalDateTime.now().format(HISTORY_TIME_FORMAT)
            );
            Files.write(Paths.get("data/task3/notifications.txt"), List.of(line), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    private static boolean isUrgentCategory(String category) {
        if (category == null) {
            return false;
        }
        String normalized = category.toUpperCase();
        return normalized.contains("REJECT")
                || normalized.contains("BOOK_DELETION")
                || normalized.contains("URGENT");
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    public record NotificationView(
            String id,
            String username,
            LocalDateTime timestamp,
            String category,
            String message,
            boolean read
    ) {}
}
