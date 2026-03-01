package project.task1.service;

import project.task1.model.Book;
import project.task1.model.LibrarianAccount;
import project.task1.model.UserAccount;
import project.task1.model.UserRole;
import project.task1.repo.BookRepository;
import project.task1.repo.SubmissionRepository;
import project.task1.repo.UserRepository;
import project.task1.security.PasswordSecurity;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StudentStaffPortalService {
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final SubmissionRepository submissionRepository;

    public StudentStaffPortalService(UserRepository userRepository, BookRepository bookRepository, SubmissionRepository submissionRepository) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.submissionRepository = submissionRepository;
    }

    public OperationResult registerStaffStudent(String username, String fullName, String rawPassword, String roleText) {
        String normalizedUsername = safeTrim(username);
        String normalizedFullName = safeTrim(fullName);
        String normalizedRole = safeTrim(roleText).toUpperCase();

        if (normalizedUsername.isEmpty()) {
            return OperationResult.failure("Registration failed: username is required.");
        }
        if (normalizedFullName.isEmpty()) {
            return OperationResult.failure("Registration failed: full name is required.");
        }
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            return OperationResult.failure("Registration failed: password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (!rawPassword.matches(".*[A-Za-z].*") || !rawPassword.matches(".*\\d.*")) {
            return OperationResult.failure("Registration failed: password must include at least one letter and one number.");
        }
        if (userRepository.existsByUsername(normalizedUsername)) {
            return OperationResult.failure("Registration failed: username already exists.");
        }

        Optional<UserRole> userRole = parseRole(normalizedRole);
        if (userRole.isEmpty()) {
            return OperationResult.failure("Registration failed: role must be Student or Staff.");
        }

        // Credentials are never stored as plain text; only salt + hash are persisted.
        String saltBase64 = PasswordSecurity.generateSaltBase64();
        String hashBase64 = PasswordSecurity.hashPasswordBase64(rawPassword, saltBase64);
        UserAccount userAccount = new UserAccount(
                normalizedUsername,
                normalizedFullName,
                saltBase64,
                hashBase64,
                userRole.get()
        );
        userRepository.save(userAccount);
        return OperationResult.success("Registration successful for " + normalizedUsername + ".");
    }

    public OperationResult registerLibrarian(String username, String fullName, String rawPassword, String employeeIDtext) {
        String normalizedUsername = safeTrim(username);
        String normalizedFullName = safeTrim(fullName);
        int employeeID;
        try {
            employeeID = Integer.parseInt(safeTrim(employeeIDtext));
        } catch (Exception e) {
            return OperationResult.failure("Registration failed: employeeID must be a valid integer.");
        }

        if (normalizedUsername.isEmpty()) {
            return OperationResult.failure("Registration failed: username is required.");
        }
        if (normalizedFullName.isEmpty()) {
            return OperationResult.failure("Registration failed: full name is required.");
        }
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            return OperationResult.failure("Registration failed: password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (!rawPassword.matches(".*[A-Za-z].*") || !rawPassword.matches(".*\\d.*")) {
            return OperationResult.failure("Registration failed: password must include at least one letter and one number.");
        }
        if (userRepository.existsByUsername(normalizedUsername)) {
            return OperationResult.failure("Registration failed: username already exists.");
        }

        // Credentials are never stored as plain text; only salt + hash are persisted.
        String saltBase64 = PasswordSecurity.generateSaltBase64();
        String hashBase64 = PasswordSecurity.hashPasswordBase64(rawPassword, saltBase64);
        UserAccount userAccount = new LibrarianAccount(
                normalizedUsername,
                normalizedFullName,
                saltBase64,
                hashBase64,
                employeeID
        );
        userRepository.save(userAccount);
        return OperationResult.success("Registration successful for " + normalizedUsername + ".");
    }

    public LoginResult login(String username, String rawPassword) {
        String normalizedUsername = safeTrim(username);
        if (normalizedUsername.isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
            return LoginResult.failure("Login failed: username and password are required.");
        }

        Optional<UserAccount> userOpt = userRepository.findByUsername(normalizedUsername);
        if (userOpt.isEmpty()) {
            return LoginResult.failure("Login failed: invalid username or password.");
        }

        UserAccount user = userOpt.get();
        boolean matched = PasswordSecurity.verifyPassword(rawPassword, user.getPasswordSaltBase64(), user.getPasswordHashBase64());
        if (!matched) {
            return LoginResult.failure("Login failed: invalid username or password.");
        }

        return LoginResult.success("Login successful. Welcome, " + user.getFullName() + ".", user);
    }

    public List<Book> getBookScreenData() {
        // Task 1.3 requires an available-book screen, so only available titles are returned.
        return bookRepository.findAll()
                .stream()
                .filter(Book::isAvailable)
                .collect(Collectors.toList());
    }

    public OperationResult borrowBook(String borrowerUsername, String bookId) {
        String normalizedBorrower = safeTrim(borrowerUsername);
        String normalizedBookId = safeTrim(bookId).toUpperCase();

        if (normalizedBorrower.isEmpty()) {
            return OperationResult.failure("Borrow failed: user must be logged in.");
        }
        if (normalizedBookId.isEmpty()) {
            return OperationResult.failure("Borrow failed: book id is required.");
        }

        Optional<Book> bookOpt = bookRepository.findById(normalizedBookId);
        if (bookOpt.isEmpty()) {
            return OperationResult.failure("Borrow failed: book not found.");
        }

        Book book = bookOpt.get();
        if (!book.isAvailable()) {
            return OperationResult.failure("Borrow failed: book is not available.");
        }

        boolean borrowed = bookRepository.borrowBook(normalizedBookId, normalizedBorrower);
        if (!borrowed) {
            return OperationResult.failure("Borrow failed: book is no longer available.");
        }

        return OperationResult.success("Borrow successful: \"" + book.getTitle() + "\" has been borrowed.");
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static Optional<UserRole> parseRole(String normalizedRole) {
        if ("STUDENT".equals(normalizedRole)) {
            return Optional.of(UserRole.STUDENT);
        }
        if ("STAFF".equals(normalizedRole)) {
            return Optional.of(UserRole.STAFF);
        }
        return Optional.empty();
    }

    public record OperationResult(boolean success, String message) {
        public static OperationResult success(String message) {
            return new OperationResult(true, message);
        }

        public static OperationResult failure(String message) {
            return new OperationResult(false, message);
        }
    }

    public record LoginResult(boolean success, String message, UserAccount user) {
        public static LoginResult success(String message, UserAccount user) {
            return new LoginResult(true, message, user);
        }

        public static LoginResult failure(String message) {
            return new LoginResult(false, message, null);
        }
    }
}
