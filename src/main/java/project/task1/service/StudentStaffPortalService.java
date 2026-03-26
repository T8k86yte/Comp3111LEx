package project.task1.service;

import project.task1.model.Book;
import project.task1.model.StudentStaffAccount;
import project.task1.model.UserAccount;
import project.task1.repo.BookRepository;
import project.task1.repo.StudentStaffRepository;
import project.shared.SharedAuthFacade;
import project.task2.repo.AuthorRepository;
import project.task3.repo.LibrarianRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

public class StudentStaffPortalService {
    private static final int MAX_BORROWED_BOOKS = 5;
    private static final int DEFAULT_BORROW_DAYS = 14;
    private static final String BORROW_HISTORY_FILE = "data/borrow_history.txt";
    private static final String BORROW_RECORDS_FILE = "data/task1/borrow_records.txt";
    private static final String TASK1_NOTIFICATIONS_FILE = "data/task1/notifications.txt";
    private static final String TASK1_READING_PROGRESS_FILE = "data/task1/reading_progress.txt";
    private static final DateTimeFormatter HISTORY_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

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

        recordBorrowRecord(
                normalizedBorrower,
                book.getId(),
                book.getTitle(),
                LocalDate.now(),
                LocalDate.now().plusDays(DEFAULT_BORROW_DAYS)
        );
        recordBorrowHistory(normalizedBorrower, book.getId(), book.getTitle());
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
        closeBorrowRecord(normalizedBorrower, normalizedBookId, LocalDate.now(), "SELF_RETURN");
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

    public OperationResult autoReturnExpiredBooks() {
        List<BorrowRecord> records = loadBorrowRecords();
        if (records.isEmpty()) {
            return OperationResult.success("No borrowed books to process.");
        }
        LocalDate today = LocalDate.now();
        int returnedCount = 0;
        for (int i = 0; i < records.size(); i++) {
            BorrowRecord record = records.get(i);
            if (record.returnDate() != null) {
                continue;
            }
            if (record.dueDate().isBefore(today)) {
                boolean returned = bookRepository.returnBook(record.bookId(), record.username());
                if (returned) {
                    records.set(i, record.withReturnDate(today, "AUTO_RETURN"));
                    appendNotification(
                            record.username(),
                            "DUE_REMINDER",
                            "Book \"" + record.bookTitle() + "\" was auto-returned after due date (" + record.dueDate() + ")."
                    );
                    returnedCount++;
                }
            }
        }
        saveBorrowRecords(records);
        return OperationResult.success("Auto-return completed. Returned " + returnedCount + " overdue book(s).");
    }

    public List<BorrowRecordView> getBorrowedBookRecords(String username) {
        String normalized = safeTrim(username);
        if (normalized.isEmpty()) {
            return List.of();
        }
        autoReturnExpiredBooks();
        return loadBorrowRecords()
                .stream()
                .filter(r -> normalized.equals(r.username()))
                .filter(r -> r.returnDate() == null)
                .sorted(Comparator.comparing(BorrowRecord::borrowDate).reversed())
                .map(r -> new BorrowRecordView(r.bookId(), r.bookTitle(), r.borrowDate(), r.dueDate(), "BORROWED"))
                .collect(Collectors.toList());
    }

    public OperationResult updateProfile(String username, String newFullName, String newPassword, String confirmPassword) {
        String normalizedUsername = safeTrim(username);
        String normalizedFullName = safeTrim(newFullName);
        String pwd = newPassword == null ? "" : newPassword;
        String confirm = confirmPassword == null ? "" : confirmPassword;
        if (normalizedUsername.isEmpty()) {
            return OperationResult.failure("Profile update failed: invalid user.");
        }
        Optional<StudentStaffAccount> existingOpt = studentstaffRepository.findByUsername(normalizedUsername);
        if (existingOpt.isEmpty()) {
            return OperationResult.failure("Profile update failed: account not found.");
        }
        StudentStaffAccount existing = existingOpt.get();
        if (normalizedFullName.isEmpty()) {
            return OperationResult.failure("Profile update failed: full name is required.");
        }

        String salt = existing.getPasswordSaltBase64();
        String hash = existing.getPasswordHashBase64();
        if (!pwd.isBlank() || !confirm.isBlank()) {
            if (!pwd.equals(confirm)) {
                return OperationResult.failure("Profile update failed: passwords do not match.");
            }
            if (!isStrongPassword(pwd)) {
                return OperationResult.failure("Profile update failed: weak password.");
            }
            salt = project.task1.security.PasswordSecurity.generateSaltBase64();
            hash = project.task1.security.PasswordSecurity.hashPasswordBase64(pwd, salt);
        }
        StudentStaffAccount updated = new StudentStaffAccount(
                existing.getUsername(),
                normalizedFullName,
                salt,
                hash,
                existing.getRole(),
                existing.isDisabled()//Preserve the disabled state
        );
        studentstaffRepository.save(updated);
        appendNotification(normalizedUsername, "ANNOUNCEMENT", "Your profile was updated successfully.");
        return OperationResult.success("Profile updated successfully.");
    }

    public List<NotificationView> getNotificationBoard(String username) {
        String normalized = safeTrim(username);
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<NotificationView> notifications = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (BorrowRecord record : loadBorrowRecords()) {
            if (!normalized.equals(record.username()) || record.returnDate() != null) {
                continue;
            }
            long daysLeft = today.until(record.dueDate()).getDays();
            if (daysLeft <= 3) {
                String msg = daysLeft < 0
                        ? "Book \"" + record.bookTitle() + "\" is overdue since " + record.dueDate() + "."
                        : "Book \"" + record.bookTitle() + "\" is due on " + record.dueDate() + " (" + daysLeft + " day(s) left).";
                notifications.add(new NotificationView(LocalDateTime.now(), "DUE_REMINDER", msg));
            }
        }

        notifications.addAll(loadStoredNotifications(normalized));
        notifications.sort(Comparator.comparing(NotificationView::timestamp).reversed());
        return notifications;
    }

    public void notifyBookDeletedForBorrowers(String bookId, String bookTitle) {
        String normalizedBookId = safeTrim(bookId).toUpperCase();
        if (normalizedBookId.isEmpty()) {
            return;
        }
        String title = safeTrim(bookTitle).isEmpty() ? normalizedBookId : bookTitle;
        List<BorrowRecord> records = loadBorrowRecords();
        records.stream()
                .filter(r -> normalizedBookId.equalsIgnoreCase(r.bookId()))
                .map(BorrowRecord::username)
                .distinct()
                .forEach(user -> appendNotification(
                        user,
                        "BOOK_DELETION",
                        "Book \"" + title + "\" (" + normalizedBookId + ") was deleted from library."
                ));
    }

    public OperationResult setBorrowedBookPdfPath(String username, String bookId, String pdfPath) {
        String normalizedUser = safeTrim(username);
        String normalizedBookId = safeTrim(bookId).toUpperCase();
        String normalizedPath = safeTrim(pdfPath);
        if (normalizedUser.isEmpty() || normalizedBookId.isEmpty() || normalizedPath.isEmpty()) {
            return OperationResult.failure("Reading setup failed: invalid input.");
        }
        List<ReadingProgress> rows = loadReadingProgress();
        boolean updated = false;
        for (int i = 0; i < rows.size(); i++) {
            ReadingProgress r = rows.get(i);
            if (normalizedUser.equals(r.username()) && normalizedBookId.equalsIgnoreCase(r.bookId())) {
                rows.set(i, new ReadingProgress(r.username(), r.bookId(), normalizedPath, r.bookmark(), r.highlightNotes()));
                updated = true;
                break;
            }
        }
        if (!updated) {
            rows.add(new ReadingProgress(normalizedUser, normalizedBookId, normalizedPath, "", ""));
        }
        saveReadingProgress(rows);
        return OperationResult.success("PDF linked successfully.");
    }

    public String getBorrowedBookPdfPath(String username, String bookId) {
        String normalizedUser = safeTrim(username);
        String normalizedBookId = safeTrim(bookId).toUpperCase();
        return loadReadingProgress()
                .stream()
                .filter(r -> normalizedUser.equals(r.username()) && normalizedBookId.equalsIgnoreCase(r.bookId()))
                .map(ReadingProgress::pdfPath)
                .filter(p -> p != null && !p.isBlank())
                .findFirst()
                .orElse("");
    }

    public OperationResult saveBookmark(String username, String bookId, String bookmark) {
        return updateReadingProgress(username, bookId, bookmark, null);
    }

    public OperationResult saveHighlight(String username, String bookId, String highlightText) {
        return updateReadingProgress(username, bookId, null, highlightText);
    }

    public ReadingProgressView getReadingProgress(String username, String bookId) {
        String normalizedUser = safeTrim(username);
        String normalizedBookId = safeTrim(bookId).toUpperCase();
        return loadReadingProgress()
                .stream()
                .filter(r -> normalizedUser.equals(r.username()) && normalizedBookId.equalsIgnoreCase(r.bookId()))
                .findFirst()
                .map(r -> new ReadingProgressView(r.pdfPath(), r.bookmark(), r.highlightNotes()))
                .orElse(new ReadingProgressView("", "", ""));
    }

    public List<String> getBorrowHistory(String username) {
        String normalizedUser = safeTrim(username);
        if (normalizedUser.isEmpty()) {
            return List.of();
        }
        Path path = Paths.get(BORROW_HISTORY_FILE);
        if (!Files.exists(path)) {
            return List.of();
        }
        try {
            List<String> lines = Files.readAllLines(path);
            List<String> history = new ArrayList<>();
            for (String line : lines) {
                String[] parts = line.split("\\|", -1);
                if (parts.length < 4) {
                    continue;
                }
                String rowUser = decode(parts[0]);
                if (!normalizedUser.equals(rowUser)) {
                    continue;
                }
                String bookId = decode(parts[1]);
                String bookTitle = decode(parts[2]);
                LocalDateTime time = LocalDateTime.parse(parts[3], HISTORY_TIME_FORMAT);
                history.add(time.toLocalDate() + " " + time.toLocalTime().withNano(0) + " - " + bookId + " - " + bookTitle);
            }
            return history;
        } catch (Exception e) {
            return List.of();
        }
    }

    private void recordBorrowHistory(String username, String bookId, String bookTitle) {
        try {
            Files.createDirectories(Paths.get("data"));
            String line = String.join("|",
                    encode(username),
                    encode(bookId),
                    encode(bookTitle),
                    LocalDateTime.now().format(HISTORY_TIME_FORMAT)
            );
            Files.write(
                    Paths.get(BORROW_HISTORY_FILE),
                    List.of(line),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
            // Avoid blocking the borrow operation if history persistence fails.
        }
    }

    private void recordBorrowRecord(String username, String bookId, String bookTitle, LocalDate borrowDate, LocalDate dueDate) {
        List<BorrowRecord> records = loadBorrowRecords();
        records.add(new BorrowRecord(username, bookId, bookTitle, borrowDate, dueDate, null, ""));
        saveBorrowRecords(records);
    }

    private void closeBorrowRecord(String username, String bookId, LocalDate returnDate, String returnMode) {
        List<BorrowRecord> records = loadBorrowRecords();
        for (int i = records.size() - 1; i >= 0; i--) {
            BorrowRecord r = records.get(i);
            if (r.returnDate() == null
                    && username.equals(r.username())
                    && bookId.equalsIgnoreCase(r.bookId())) {
                records.set(i, r.withReturnDate(returnDate, returnMode));
                break;
            }
        }
        saveBorrowRecords(records);
    }

    private List<BorrowRecord> loadBorrowRecords() {
        Path path = Paths.get(BORROW_RECORDS_FILE);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try {
            List<BorrowRecord> records = new ArrayList<>();
            for (String line : Files.readAllLines(path)) {
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                if (parts.length < 7) {
                    continue;
                }
                records.add(new BorrowRecord(
                        decode(parts[0]),
                        decode(parts[1]),
                        decode(parts[2]),
                        LocalDate.parse(parts[3]),
                        LocalDate.parse(parts[4]),
                        parts[5].isBlank() ? null : LocalDate.parse(parts[5]),
                        decode(parts[6])
                ));
            }
            return records;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveBorrowRecords(List<BorrowRecord> records) {
        try {
            Files.createDirectories(Paths.get("data/task1"));
            List<String> lines = new ArrayList<>();
            for (BorrowRecord r : records) {
                lines.add(String.join("|",
                        encode(r.username()),
                        encode(r.bookId()),
                        encode(r.bookTitle()),
                        r.borrowDate().toString(),
                        r.dueDate().toString(),
                        r.returnDate() == null ? "" : r.returnDate().toString(),
                        encode(r.returnMode() == null ? "" : r.returnMode())
                ));
            }
            Files.write(Paths.get(BORROW_RECORDS_FILE), lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    private void appendNotification(String username, String category, String message) {
        try {
            Files.createDirectories(Paths.get("data/task1"));
            String line = String.join("|",
                    encode(username),
                    encode(category),
                    encode(message),
                    LocalDateTime.now().format(HISTORY_TIME_FORMAT)
            );
            Files.write(
                    Paths.get(TASK1_NOTIFICATIONS_FILE),
                    List.of(line),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
        }
    }

    private List<NotificationView> loadStoredNotifications(String username) {
        Path path = Paths.get(TASK1_NOTIFICATIONS_FILE);
        if (!Files.exists(path)) {
            return List.of();
        }
        try {
            List<NotificationView> list = new ArrayList<>();
            for (String line : Files.readAllLines(path)) {
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                if (parts.length < 4) {
                    continue;
                }
                String u = decode(parts[0]);
                if (!username.equals(u)) {
                    continue;
                }
                list.add(new NotificationView(
                        LocalDateTime.parse(parts[3], HISTORY_TIME_FORMAT),
                        decode(parts[1]),
                        decode(parts[2])
                ));
            }
            return list;
        } catch (Exception e) {
            return List.of();
        }
    }

    private OperationResult updateReadingProgress(String username, String bookId, String bookmark, String highlight) {
        String normalizedUser = safeTrim(username);
        String normalizedBookId = safeTrim(bookId).toUpperCase();
        if (normalizedUser.isEmpty() || normalizedBookId.isEmpty()) {
            return OperationResult.failure("Reading update failed: invalid user/book.");
        }

        List<ReadingProgress> rows = loadReadingProgress();
        boolean updated = false;
        for (int i = 0; i < rows.size(); i++) {
            ReadingProgress r = rows.get(i);
            if (normalizedUser.equals(r.username()) && normalizedBookId.equalsIgnoreCase(r.bookId())) {
                String newBookmark = bookmark == null ? r.bookmark() : bookmark;
                String newHighlight = highlight == null
                        ? r.highlightNotes()
                        : (r.highlightNotes().isBlank() ? highlight : r.highlightNotes() + "\n- " + highlight);
                rows.set(i, new ReadingProgress(r.username(), r.bookId(), r.pdfPath(), newBookmark, newHighlight));
                updated = true;
                break;
            }
        }
        if (!updated) {
            rows.add(new ReadingProgress(
                    normalizedUser,
                    normalizedBookId,
                    "",
                    bookmark == null ? "" : bookmark,
                    highlight == null ? "" : highlight
            ));
        }
        saveReadingProgress(rows);
        return OperationResult.success("Reading progress updated.");
    }

    private List<ReadingProgress> loadReadingProgress() {
        Path path = Paths.get(TASK1_READING_PROGRESS_FILE);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try {
            List<ReadingProgress> list = new ArrayList<>();
            for (String line : Files.readAllLines(path)) {
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                if (parts.length < 5) {
                    continue;
                }
                list.add(new ReadingProgress(
                        decode(parts[0]),
                        decode(parts[1]),
                        decode(parts[2]),
                        decode(parts[3]),
                        decode(parts[4])
                ));
            }
            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveReadingProgress(List<ReadingProgress> rows) {
        try {
            Files.createDirectories(Paths.get("data/task1"));
            List<String> lines = new ArrayList<>();
            for (ReadingProgress r : rows) {
                lines.add(String.join("|",
                        encode(r.username()),
                        encode(r.bookId()),
                        encode(r.pdfPath()),
                        encode(r.bookmark()),
                        encode(r.highlightNotes())
                ));
            }
            Files.write(Paths.get(TASK1_READING_PROGRESS_FILE), lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    private static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        return password.matches(".*[A-Za-z].*")
                && password.matches(".*\\d.*")
                && password.matches(".*[A-Z].*");
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
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

    private record BorrowRecord(
            String username,
            String bookId,
            String bookTitle,
            LocalDate borrowDate,
            LocalDate dueDate,
            LocalDate returnDate,
            String returnMode
    ) {
        private BorrowRecord withReturnDate(LocalDate date, String mode) {
            return new BorrowRecord(username, bookId, bookTitle, borrowDate, dueDate, date, mode);
        }
    }

    public record BorrowRecordView(
            String bookId,
            String bookTitle,
            LocalDate borrowDate,
            LocalDate dueDate,
            String status
    ) {}

    public record NotificationView(
            LocalDateTime timestamp,
            String category,
            String message
    ) {}

    private record ReadingProgress(
            String username,
            String bookId,
            String pdfPath,
            String bookmark,
            String highlightNotes
    ) {}

    public record ReadingProgressView(
            String pdfPath,
            String bookmark,
            String highlightNotes
    ) {}
}
