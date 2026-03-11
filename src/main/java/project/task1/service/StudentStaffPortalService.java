package project.task1.service;

import project.task1.model.Book;
import project.task1.model.StudentStaffAccount;
import project.task1.model.UserAccount;
import project.task1.repo.BookRepository;
import project.task1.repo.StudentStaffRepository;
import project.shared.SharedAuthFacade;
import project.task2.repo.AuthorRepository;
import project.task3.repo.LibrarianRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class StudentStaffPortalService {
    private static final int MAX_BORROWED_BOOKS = 5;
    private static final int DEFAULT_BORROW_DAYS = 14;

    private final StudentStaffRepository studentstaffRepository;
    private final BookRepository bookRepository;
    private final SharedAuthFacade sharedAuthFacade;

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
        this.sharedAuthFacade = new SharedAuthFacade(
                studentstaffRepository,
                authorRepository,
                librarianRepository
        );
    }

    public OperationResult registerStaffStudent(String username, String fullName, String rawPassword, String roleText) {
        return registerWithRoleSelection(username, fullName, rawPassword, roleText);
    }

    public OperationResult registerWithRoleSelection(String username, String fullName, String rawPassword, String roleText) {
        SharedAuthFacade.AuthResult authResult = sharedAuthFacade.register(
                username,
                fullName,
                rawPassword,
                null,
                roleText,
                null,
                null
        );
        if (!authResult.success()) {
            return OperationResult.failure(authResult.message());
        }
        if (!"STUDENT".equalsIgnoreCase(authResult.principal().role())
                && !"STAFF".equalsIgnoreCase(authResult.principal().role())) {
            return OperationResult.failure("Registration failed: Task 1 only supports Student and Staff.");
        }
        return OperationResult.success(authResult.message());
    }

    public LoginResult login(String username, String rawPassword) {
        return login(username, rawPassword, "STUDENT");
    }

    public LoginResult login(String username, String rawPassword, String roleText) {
        SharedAuthFacade.AuthResult authResult = sharedAuthFacade.login(username, rawPassword, roleText);
        if (!authResult.success()) {
            return LoginResult.failure(authResult.message());
        }
        String role = authResult.principal().role();
        if (!"STUDENT".equalsIgnoreCase(role) && !"STAFF".equalsIgnoreCase(role)) {
            return LoginResult.failure("Login failed: Task 1 only supports Student and Staff.");
        }
        Optional<StudentStaffAccount> account = studentstaffRepository.findByUsername(authResult.principal().username());
        if (account.isEmpty()) {
            return LoginResult.failure("Login failed: invalid username or password.");
        }
        return LoginResult.success(authResult.message(), account.get());
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

    private long getCurrentBorrowedCount(String username) {
        return bookRepository.findAll()
                .stream()
                .filter(book -> !book.isAvailable())
                .filter(book -> username.equals(book.getBorrowedByUsername()))
                .count();
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
