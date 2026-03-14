package project.task1.service;

import project.task1.model.Book;
import project.task1.model.StudentStaffAccount;
import project.task1.model.UserAccount;
import project.task1.model.UserRole;
import project.task1.repo.BookRepository;
import project.task1.repo.StudentStaffRepository;
import project.task1.security.PasswordSecurity;
import project.task2.model.AuthorAccount;
import project.task2.repo.AuthorRepository;
import project.task3.model.LibrarianAccount;
import project.task3.repo.LibrarianRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StudentStaffPortalService {
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_BORROWED_BOOKS = 5;
    private static final int DEFAULT_BORROW_DAYS = 14;

    private final StudentStaffRepository studentstaffRepository;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final LibrarianRepository librarianRepository;

    public StudentStaffPortalService(StudentStaffRepository studentstaffRepository, BookRepository bookRepository) {
        this(studentstaffRepository, bookRepository, new AuthorRepository(), new LibrarianRepository());
    }

    public StudentStaffPortalService(
            StudentStaffRepository studentstaffRepository,
            BookRepository bookRepository,
            AuthorRepository authorRepository,
            LibrarianRepository librarianRepository
    ) {
        this.studentstaffRepository = studentstaffRepository;
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.librarianRepository = librarianRepository;
    }

    public OperationResult registerStaffStudent(String username, String fullName, String rawPassword, String roleText) {
        return registerWithRoleSelection(username, fullName, rawPassword, roleText);
    }

    public OperationResult registerWithRoleSelection(String username, String fullName, String rawPassword, String roleText) {
        String normalizedUsername = safeTrim(username);
        String normalizedFullName = safeTrim(fullName);
        String normalizedRoleText = safeTrim(roleText).toUpperCase();

        if (normalizedUsername.isEmpty()) {
            return OperationResult.failure("Registration failed: username is required.");
        }
        if (normalizedFullName.isEmpty()) {
            return OperationResult.failure("Registration failed: full name is required.");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            return OperationResult.failure("Registration failed: password is required.");
        }

        PasswordStrength passwordStrength = evaluatePasswordStrength(rawPassword);
        if (passwordStrength == PasswordStrength.WEAK) {
            return OperationResult.failure(
                    "Registration failed: weak password. Use at least " + MIN_PASSWORD_LENGTH
                            + " chars with letters and numbers."
            );
        }

        if (existsByUsernameAcrossUserTypes(normalizedUsername)) {
            return OperationResult.failure("Registration failed: username already exists across user types.");
        }

        Optional<UserRole> userRole = parseRole(normalizedRoleText);
        if (userRole.isEmpty()) {
            return OperationResult.failure("Registration failed: role must be Student, Staff, Author, or Librarian.");
        }

        if (userRole.get() == UserRole.AUTHOR) {
            return OperationResult.failure("Registration failed: Author registration is handled in Task 2.");
        }
        if (userRole.get() == UserRole.LIBRARIAN) {
            return OperationResult.failure("Registration failed: Librarian registration is handled in Task 3.");
        }

        String saltBase64 = PasswordSecurity.generateSaltBase64();
        String hashBase64 = PasswordSecurity.hashPasswordBase64(rawPassword, saltBase64);
        StudentStaffAccount userAccount = new StudentStaffAccount(
                normalizedUsername,
                normalizedFullName,
                saltBase64,
                hashBase64,
                userRole.get()
        );
        studentstaffRepository.save(userAccount);
        return OperationResult.success("Registration successful for " + normalizedUsername + ".");
    }

    public LoginResult login(String username, String rawPassword) {
        return login(username, rawPassword, "STUDENT");
    }

    public LoginResult login(String username, String rawPassword, String roleText) {
        String normalizedUsername = safeTrim(username);
        String normalizedRoleText = safeTrim(roleText).toUpperCase();
        if (normalizedUsername.isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
            return LoginResult.failure("Login failed: username and password are required.");
        }

        Optional<UserRole> selectedRoleOpt = parseRole(normalizedRoleText);
        if (selectedRoleOpt.isEmpty()) {
            return LoginResult.failure("Login failed: role must be Student, Staff, Author, or Librarian.");
        }
        UserRole selectedRole = selectedRoleOpt.get();

        Optional<? extends UserAccount> userOpt = findAccountByRole(selectedRole, normalizedUsername);
        if (userOpt.isEmpty()) {
            Optional<UserRole> existingRole = findExistingRoleByUsername(normalizedUsername);
            if (existingRole.isPresent()) {
                return LoginResult.failure("Login failed: username belongs to " + existingRole.get().name() + " account.");
            }
            return LoginResult.failure("Login failed: invalid username or password.");
        }

        UserAccount user = userOpt.get();
        boolean matched = PasswordSecurity.verifyPassword(rawPassword, user.getPasswordSaltBase64(), user.getPasswordHashBase64());
        if (!matched) {
            return LoginResult.failure("Login failed: invalid username or password.");
        }
        if (user.getRole() != selectedRole) {
            return LoginResult.failure("Login failed: selected role does not match this username.");
        }
        return LoginResult.success("Login successful. Welcome, " + user.getFullName() + ".", user);
    }

    public List<Book> getBookScreenData() {
        return bookRepository.findAll();
    }

    public List<Book> getRecommendedBooks(int limit) {
        List<Book> popular = bookRepository.findTopRecommended(limit)
                .stream()
                .filter(book -> book.getBorrowCount() > 0)
                .collect(Collectors.toList());
        if (!popular.isEmpty()) {
            return popular;
        }

        return bookRepository.findAll()
                .stream()
                .filter(Book::isAvailable)
                .limit(Math.max(0, limit))
                .collect(Collectors.toList());
    }

    public String buildBorrowConfirmation(String borrowerUsername, String bookId) {
        String normalizedBorrower = safeTrim(borrowerUsername);
        String normalizedBookId = safeTrim(bookId).toUpperCase();
        Optional<Book> bookOpt = bookRepository.findById(normalizedBookId);
        if (bookOpt.isEmpty()) {
            return "Book not found.";
        }
        Book book = bookOpt.get();
        int borrowedCount = (int) getCurrentBorrowedCount(normalizedBorrower);
        int remaining = Math.max(0, MAX_BORROWED_BOOKS - borrowedCount);
        LocalDate dueDate = LocalDate.now().plusDays(DEFAULT_BORROW_DAYS);
        String warning = remaining <= 1
                ? "Warning: borrow limit is nearly reached."
                : "No borrow limit warning.";
        return "Book: " + book.getTitle()
                + "\nBook ID: " + book.getId()
                + "\nBorrow duration: " + DEFAULT_BORROW_DAYS + " days"
                + "\nDue date: " + dueDate
                + "\nCurrent borrowed: " + borrowedCount + "/" + MAX_BORROWED_BOOKS
                + "\n" + warning;
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
        if (getCurrentBorrowedCount(normalizedBorrower) >= MAX_BORROWED_BOOKS) {
            return OperationResult.failure("Borrow failed: reached max limit of " + MAX_BORROWED_BOOKS + " books.");
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

    public OperationResult returnBook(String borrowerUsername, String bookId) {
        String normalizedBorrower = safeTrim(borrowerUsername);
        String normalizedBookId = safeTrim(bookId).toUpperCase();
        if (normalizedBorrower.isEmpty()) {
            return OperationResult.failure("Return failed: user must be logged in.");
        }
        if (normalizedBookId.isEmpty()) {
            return OperationResult.failure("Return failed: book id is required.");
        }

        Optional<Book> bookOpt = bookRepository.findById(normalizedBookId);
        if (bookOpt.isEmpty()) {
            return OperationResult.failure("Return failed: book not found.");
        }
        Book book = bookOpt.get();
        if (book.isAvailable()) {
            return OperationResult.failure("Return failed: book is already available.");
        }
        if (!normalizedBorrower.equals(book.getBorrowedByUsername())) {
            return OperationResult.failure("Return failed: this book is borrowed by another user.");
        }

        boolean returned = bookRepository.returnBook(normalizedBookId, normalizedBorrower);
        if (!returned) {
            return OperationResult.failure("Return failed: unable to complete return.");
        }
        return OperationResult.success("Return successful: \"" + book.getTitle() + "\" has been returned.");
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
        if ("AUTHOR".equals(normalizedRole)) {
            return Optional.of(UserRole.AUTHOR);
        }
        if ("LIBRARIAN".equals(normalizedRole)) {
            return Optional.of(UserRole.LIBRARIAN);
        }
        return Optional.empty();
    }

    private Optional<? extends UserAccount> findAccountByRole(UserRole role, String username) {
        return switch (role) {
            case STUDENT, STAFF -> studentstaffRepository.findByUsername(username);
            case AUTHOR -> authorRepository.findByUsername(username);
            case LIBRARIAN -> librarianRepository.findByUsername(username);
        };
    }

    private Optional<UserRole> findExistingRoleByUsername(String username) {
        Optional<StudentStaffAccount> ss = studentstaffRepository.findByUsername(username);
        if (ss.isPresent()) {
            return Optional.of(ss.get().getRole());
        }
        Optional<AuthorAccount> author = authorRepository.findByUsername(username);
        if (author.isPresent()) {
            return Optional.of(UserRole.AUTHOR);
        }
        Optional<LibrarianAccount> librarian = librarianRepository.findByUsername(username);
        if (librarian.isPresent()) {
            return Optional.of(UserRole.LIBRARIAN);
        }
        return Optional.empty();
    }

    private boolean existsByUsernameAcrossUserTypes(String username) {
        return studentstaffRepository.existsByUsername(username)
                || authorRepository.existsByUsername(username)
                || librarianRepository.existsByUsername(username);
    }

    private long getCurrentBorrowedCount(String username) {
        return bookRepository.findAll()
                .stream()
                .filter(book -> !book.isAvailable())
                .filter(book -> username.equals(book.getBorrowedByUsername()))
                .count();
    }

    private static PasswordStrength evaluatePasswordStrength(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            return PasswordStrength.EMPTY;
        }
        if (rawPassword.length() < MIN_PASSWORD_LENGTH) {
            return PasswordStrength.WEAK;
        }
        boolean hasLetter = rawPassword.matches(".*[A-Za-z].*");
        boolean hasNumber = rawPassword.matches(".*\\d.*");
        return (hasLetter && hasNumber) ? PasswordStrength.STRONG : PasswordStrength.WEAK;
    }

    private enum PasswordStrength {
        EMPTY,
        WEAK,
        STRONG
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
