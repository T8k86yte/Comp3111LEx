package project.task2.service;

import project.task2.model.AuthorAccount;
import project.task1.repo.StudentStaffRepository;
import project.task3.repo.LibrarianRepository;
import project.shared.SharedAuthFacade;
import project.task2.repo.AuthorRepository;

import project.task2.model.BookSubmission;
import project.task2.repo.SubmissionRepository;
import project.task2.repo.DraftRepository;
import project.task2.utils.FileHandler;

import java.util.List;

public class AuthorPortalService {
    private final AuthorRepository authorRepository;
    private final SharedAuthFacade sharedAuthFacade;
    private final SubmissionRepository submissionRepository;
    private final DraftRepository draftRepository;

    public AuthorPortalService() {
        this.authorRepository = new AuthorRepository();
        this.sharedAuthFacade = new SharedAuthFacade(
                new StudentStaffRepository(),
                authorRepository,
                new LibrarianRepository()
        );
        this.submissionRepository = new SubmissionRepository();
        this.draftRepository = new DraftRepository();
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
        // Format draft data
        String genresStr = genres != null ? String.join(",", genres) : "";
        String draftData = String.join("|",
            title != null ? title : "",
            genresStr,
            description != null ? description : "",
            filePath != null ? filePath : ""
        );
        
        // Only save if there's some content
        if (!title.isEmpty() || !genresStr.isEmpty() || !description.isEmpty() || !filePath.isEmpty()) {
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
        
        String[] parts = draftData.split("\\|", 4);
        if (parts.length == 4) {
            return parts; // [title, genres, description, filePath]
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

        try {
            // Persist selected genres as a comma-separated string.
            BookSubmission submission = new BookSubmission(
                title, authorUsername, authorFullName, 
                genresStr, description, filePath
            );

            // Save to repository
            submissionRepository.save(submission);
            
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
}
