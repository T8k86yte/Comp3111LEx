package project.task3.service;

import project.task1.model.StudentStaffAccount;
import project.task1.repo.BookRepository;
import project.task1.security.PasswordSecurity;
import project.task1.service.StudentStaffPortalService;
import project.task3.model.LibrarianAccount;
import project.task3.repo.LibrarianRepository;

import java.util.Optional;

public class LibrarianPortalService {
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final LibrarianRepository librarianRepository;
    private final BookRepository bookRepository;

    public LibrarianPortalService(LibrarianRepository librarianRepository, BookRepository bookRepository) {
        this.librarianRepository = librarianRepository;
        this.bookRepository = bookRepository;
    }

    public LibrarianPortalService.OperationResult registerLibrarian(String username, String fullname, String rawPassword, String employeeIDtext) {
        if (username == null || username.isEmpty()) {
            return OperationResult.failure("Registration failed: username is required.");
        }
        if (fullname == null || fullname.isEmpty()) {
            return OperationResult.failure("Registration failed: full name is required.");
        }
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            return OperationResult.failure("Registration failed: password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (!rawPassword.matches(".*[A-Za-z].*") || !rawPassword.matches(".*\\d.*")) {
            return OperationResult.failure("Registration failed: password must include at least one letter and one number.");
        }
        if (librarianRepository.existsByUsername(username)) {
            return OperationResult.failure("Registration failed: username already exists.");
        }
        if (employeeIDtext == null || employeeIDtext.isEmpty()) {
            return OperationResult.failure("Registration failed: employee ID is required.");
        }
        int ID = Integer.parseInt(employeeIDtext);

        // Credentials are never stored as plain text; only salt + hash are persisted.
        String saltBase64 = PasswordSecurity.generateSaltBase64();
        String hashBase64 = PasswordSecurity.hashPasswordBase64(rawPassword, saltBase64);
        LibrarianAccount userAccount = new LibrarianAccount(
                username,
                fullname,
                saltBase64,
                hashBase64,
                ID
        );
        librarianRepository.save(userAccount);
        return OperationResult.success("Registration successful for " + username + ".");
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    public record OperationResult(boolean success, String message) {
        public static OperationResult success(String message) {
            return new OperationResult(true, message);
        }

        public static OperationResult failure(String message) {
            return new OperationResult(false, message);
        }
    }

    public record LoginResult(boolean success, String message, StudentStaffAccount user) {
        public static StudentStaffPortalService.LoginResult success(String message, StudentStaffAccount user) {
            return new StudentStaffPortalService.LoginResult(true, message, user);
        }

        public static StudentStaffPortalService.LoginResult failure(String message) {
            return new StudentStaffPortalService.LoginResult(false, message, null);
        }
    }
}
