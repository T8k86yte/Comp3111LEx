package project.task3.service;

import project.task1.model.Book;
import project.task1.model.StudentStaffAccount;
import project.task1.repo.BookRepository;
import project.task1.security.PasswordSecurity;
import project.task1.service.StudentStaffPortalService;
import project.task3.model.LibrarianAccount;
import project.task3.repo.LibrarianRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    public LoginResult login(String username, String rawPassword) {
        if (username == null || username.isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
            return LoginResult.failure("Login failed: username and password are required.");
        }

        Optional<LibrarianAccount> userOpt = librarianRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return LoginResult.failure("Login failed: invalid username or password.");
        }

        LibrarianAccount user = userOpt.get();
        boolean matched = PasswordSecurity.verifyPassword(rawPassword, user.getPasswordSaltBase64(), user.getPasswordHashBase64());
        if (!matched) {
            return LoginResult.failure("Login failed: invalid username or password.");
        }

        return LoginResult.success("Login successful. Welcome, " + user.getFullName() + ".", user);
    }

    public OperationResult handleBookSubmission(String subId, LibrarianAccount user, boolean approve) {
        return OperationResult.failure("TBA");
        //TBA
    }

    public record OperationResult(boolean success, String message) {
        public static OperationResult success(String message) {
            return new OperationResult(true, message);
        }

        public static OperationResult failure(String message) {
            return new OperationResult(false, message);
        }
    }

    public record LoginResult(boolean success, String message, LibrarianAccount user) {
        public static LoginResult success(String message, LibrarianAccount user) {
            return new LoginResult(true, message, user);
        }

        public static LoginResult failure(String message) {
            return new LoginResult(false, message, null);
        }
    }
}
