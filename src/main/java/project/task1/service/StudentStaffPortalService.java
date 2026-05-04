package project.task1.service;

import project.task1.model.Book;
import project.task1.model.StudentStaffAccount;
import project.task1.model.UserAccount;
import project.task1.repo.BookRepository;
import project.task1.repo.StudentStaffRepository;
import project.shared.SharedAuthFacade;
import project.shared.notification.UnifiedNotification;
import project.shared.notification.UnifiedNotificationStore;
import project.task2.repo.AuthorRepository;
import project.task2.repo.SubmissionRepository;
import project.task3.repo.LibrarianRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;

public class StudentStaffPortalService {
    private static final int MAX_BORROWED_BOOKS = 5;
    private static final int MIN_BORROW_DAYS = 1;
    private static final int MAX_BORROW_DAYS = 14;
    private static final int DEFAULT_BORROW_DAYS = 14;
    private static final String BORROW_HISTORY_FILE = "data/borrow_history.txt";
    private static final String BORROW_RECORDS_FILE = "data/task1/borrow_records.txt";
    private static final String LEGACY_TASK1_NOTIFICATIONS_FILE = "data/task1/notifications.txt";
    private static final String TASK1_NOTIFICATION_SCOPE = "TASK1";
    private static final String TASK1_READING_PROGRESS_FILE = "data/task1/reading_progress.txt";
    private static final String TASK1_BOOK_REVIEWS_FILE = "data/task1/book_reviews.txt";
    private static final String TASK1_BOOK_REQUESTS_FILE = "data/task1/book_requests.txt";
    private static final String TASK1_REVIEW_HELPFUL_FILE = "data/task1/review_helpful_votes.txt";
    private static final String TASK1_NOTIFICATION_ARCHIVE_FILE = "data/task1/notification_archives.txt";
    private static final String TASK1_PROFILE_PICTURE_FILE = "data/task1/profile_pictures.txt";
    private static final String TASK1_PROFILE_PICTURE_DIR = "data/task1/profile_pictures";
    private static final long MAX_PROFILE_PICTURE_SIZE_BYTES = 2L * 1024L * 1024L;
    private static final DateTimeFormatter HISTORY_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final StudentStaffRepository studentstaffRepository;
    private final BookRepository bookRepository;
    private final SharedAuthFacade sharedAuthFacade;
    private final SubmissionRepository submissionRepository;
    private final UnifiedNotificationStore notificationStore;
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
        this.sharedAuthFacade = new SharedAuthFacade(
                studentstaffRepository,
                authorRepository,
                librarianRepository
        );
        this.librarianRepository = librarianRepository;
        this.submissionRepository = new SubmissionRepository();
        this.notificationStore = new UnifiedNotificationStore();
        migrateLegacyTask1Notifications();
    }

    private void migrateLegacyTask1Notifications() {
        Path path = Paths.get(LEGACY_TASK1_NOTIFICATIONS_FILE);
        if (!Files.exists(path)) {
            return;
        }
        try {
            for (String line : Files.readAllLines(path)) {
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                if (parts.length < 4) {
                    continue;
                }
                String username = decode(parts[0]);
                String category = decode(parts[1]);
                String message = decode(parts[2]);
                LocalDateTime createdAt = LocalDateTime.parse(parts[3], HISTORY_TIME_FORMAT);
                String legacyId = "LEGACY_TASK1_" + Integer.toUnsignedString(line.hashCode());
                notificationStore.upsert(new UnifiedNotification(
                        legacyId,
                        TASK1_NOTIFICATION_SCOPE,
                        username,
                        category,
                        message,
                        category,
                        category,
                        false,
                        isPriorityCategory(category),
                        "",
                        createdAt
                ));
            }
        } catch (Exception ignored) {
        }
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

    public List<Book> getBookScreenData(
            String titleFilter,
            String authorFilter,
            String genreFilter,
            LocalDate publishDateFilter,
            String availabilityFilter
    ) {
        String title = safeTrim(titleFilter).toLowerCase();
        String author = safeTrim(authorFilter).toLowerCase();
        String genre = safeTrim(genreFilter).toLowerCase();
        String availability = safeTrim(availabilityFilter).toUpperCase();
        return bookRepository.findAll()
                .stream()
                .filter(book -> title.isEmpty() || book.getTitle().toLowerCase().contains(title))
                .filter(book -> author.isEmpty() || book.getAuthor().toLowerCase().contains(author))
                .filter(book -> genre.isEmpty() || book.getGenre().toLowerCase().contains(genre))
                .filter(book -> publishDateFilter == null || publishDateFilter.equals(book.getPublishDate()))
                .filter(book -> {
                    if (availability.isEmpty() || "ALL".equals(availability)) return true;
                    if ("AVAILABLE".equals(availability)) return book.isAvailable();
                    if ("UNAVAILABLE".equals(availability)) return !book.isAvailable();
                    return true;
                })
                .collect(Collectors.toList());
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
        return buildBorrowConfirmation(borrowerUsername, bookId, DEFAULT_BORROW_DAYS);
    }

    public String buildBorrowConfirmation(String borrowerUsername, String bookId, int borrowDays) {
        String normalizedBorrower = safeTrim(borrowerUsername);
        String normalizedBookId = safeTrim(bookId).toUpperCase();
        Optional<Book> bookOpt = bookRepository.findById(normalizedBookId);
        if (bookOpt.isEmpty()) {
            return "Book not found.";
        }
        if (borrowDays < MIN_BORROW_DAYS || borrowDays > MAX_BORROW_DAYS) {
            return "Invalid borrow duration. Please choose between " + MIN_BORROW_DAYS + " and " + MAX_BORROW_DAYS + " day(s).";
        }
        Book book = bookOpt.get();
        int borrowedCount = (int) getCurrentBorrowedCount(normalizedBorrower);
        int remaining = Math.max(0, MAX_BORROWED_BOOKS - borrowedCount);
        LocalDate dueDate = LocalDate.now().plusDays(borrowDays);
        String warning = remaining <= 1
                ? "Warning: borrow limit is nearly reached."
                : "No borrow limit warning.";
        return "Book: " + book.getTitle()
                + "\nBook ID: " + book.getId()
                + "\nBorrow duration: " + borrowDays + " days"
                + "\nDue date: " + dueDate
                + "\nCurrent borrowed: " + borrowedCount + "/" + MAX_BORROWED_BOOKS
                + "\n" + warning;
    }

    public OperationResult borrowBook(String borrowerUsername, String bookId) {
        return borrowBook(borrowerUsername, bookId, DEFAULT_BORROW_DAYS);
    }

    public OperationResult borrowBook(String borrowerUsername, String bookId, int borrowDays) {
        String normalizedBorrower = safeTrim(borrowerUsername);
        String normalizedBookId = safeTrim(bookId).toUpperCase();

        if (normalizedBorrower.isEmpty()) {
            return OperationResult.failure("Borrow failed: user must be logged in.");
        }
        if (normalizedBookId.isEmpty()) {
            return OperationResult.failure("Borrow failed: book id is required.");
        }
        if (borrowDays < MIN_BORROW_DAYS || borrowDays > MAX_BORROW_DAYS) {
            return OperationResult.failure(
                    "Borrow failed: duration must be between " + MIN_BORROW_DAYS + " and " + MAX_BORROW_DAYS + " day(s)."
            );
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
                book.getAuthor(),
                book.getGenre(),
                LocalDate.now(),
                LocalDate.now().plusDays(borrowDays)
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
        clearReadingProgressForUserBook(normalizedBorrower, normalizedBookId);
        appendNotification(
                normalizedBorrower,
                "BOOK_RETURN",
                "You returned \"" + book.getTitle() + "\" successfully."
        );
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
        LifecycleProcessingResult result = processBorrowLifecycleEvents();
        return OperationResult.success("Auto-return completed. Returned "
                + result.autoReturnedCount()
                + " overdue book(s), and sent "
                + result.reminderCount()
                + " reminder(s).");
    }

    public LifecycleProcessingResult processBorrowLifecycleEvents() {
        List<BorrowRecord> records = loadBorrowRecords();
        if (records.isEmpty()) {
            return new LifecycleProcessingResult(0, 0);
        }
        LocalDate today = LocalDate.now();
        int returnedCount = 0;
        int reminderCount = 0;
        boolean recordsChanged = false;
        for (int i = 0; i < records.size(); i++) {
            BorrowRecord record = records.get(i);
            if (record.returnDate() != null) {
                continue;
            }
            if (record.dueDate().isBefore(today)) {
                boolean returned = bookRepository.returnBook(record.bookId(), record.username());
                if (returned) {
                    records.set(i, record.withReturnDate(today, "AUTO_RETURN"));
                    clearReadingProgressForUserBook(record.username(), record.bookId());
                    appendNotification(
                            record.username(),
                            "AUTO_RETURN",
                            "Book \"" + record.bookTitle() + "\" was auto-returned after due date (" + record.dueDate() + ")."
                    );
                    returnedCount++;
                    recordsChanged = true;
                }
                continue;
            }
            long daysLeft = ChronoUnit.DAYS.between(today, record.dueDate());
            if (daysLeft >= 0 && daysLeft <= 3) {
                upsertDueReminder(record, today, daysLeft);
                reminderCount++;
            }
        }
        if (recordsChanged) {
            saveBorrowRecords(records);
        }
        return new LifecycleProcessingResult(returnedCount, reminderCount);
    }

    public Optional<LocalDate> getActiveBorrowDueDate(String username, String bookId) {
        String normalizedUser = safeTrim(username);
        String normalizedBookId = safeTrim(bookId).toUpperCase();
        if (normalizedUser.isEmpty() || normalizedBookId.isEmpty()) {
            return Optional.empty();
        }
        return loadBorrowRecords().stream()
                .filter(r -> r.returnDate() == null)
                .filter(r -> normalizedUser.equals(r.username()) && normalizedBookId.equalsIgnoreCase(r.bookId()))
                .map(BorrowRecord::dueDate)
                .findFirst();
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
            boolean sameAsCurrent = project.task1.security.PasswordSecurity.verifyPassword(
                    pwd,
                    existing.getPasswordSaltBase64(),
                    existing.getPasswordHashBase64()
            );
            if (sameAsCurrent) {
                return OperationResult.failure("Profile update failed: new password cannot be the same as the current password.");
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
                existing.isDisabled()
        );
        studentstaffRepository.save(updated);
        appendNotification(normalizedUsername, "ANNOUNCEMENT", "Your profile was updated successfully.");
        return OperationResult.success("Profile updated successfully.");
    }

    public ProfileUpdateResult updateProfileWithReAuth(
            String username,
            String newFullName,
            String currentPassword,
            String newPassword,
            String confirmPassword
    ) {
        String normalizedUsername = safeTrim(username);
        String normalizedFullName = safeTrim(newFullName);
        String pwd = newPassword == null ? "" : newPassword;
        String confirm = confirmPassword == null ? "" : confirmPassword;
        String oldPwd = currentPassword == null ? "" : currentPassword;
        if (normalizedUsername.isEmpty()) {
            return ProfileUpdateResult.failure("Profile update failed: invalid user.", false);
        }
        Optional<StudentStaffAccount> existingOpt = studentstaffRepository.findByUsername(normalizedUsername);
        if (existingOpt.isEmpty()) {
            return ProfileUpdateResult.failure("Profile update failed: account not found.", false);
        }
        StudentStaffAccount existing = existingOpt.get();
        if (normalizedFullName.isEmpty()) {
            return ProfileUpdateResult.failure("Profile update failed: full name is required.", false);
        }

        if (oldPwd.isBlank()) {
            return ProfileUpdateResult.failure("Profile update failed: current password is required.", false);
        }
        boolean currentPwdOk = project.task1.security.PasswordSecurity.verifyPassword(
                oldPwd,
                existing.getPasswordSaltBase64(),
                existing.getPasswordHashBase64()
        );
        if (!currentPwdOk) {
            return ProfileUpdateResult.failure("Profile update failed: current password is incorrect.", false);
        }

        String salt = existing.getPasswordSaltBase64();
        String hash = existing.getPasswordHashBase64();
        boolean passwordChanged = false;
        if (!pwd.isBlank() || !confirm.isBlank()) {
            if (!pwd.equals(confirm)) {
                return ProfileUpdateResult.failure("Profile update failed: passwords do not match.", false);
            }
            if (!isStrongPassword(pwd)) {
                return ProfileUpdateResult.failure("Profile update failed: weak password.", false);
            }
            boolean sameAsCurrent = project.task1.security.PasswordSecurity.verifyPassword(
                    pwd,
                    existing.getPasswordSaltBase64(),
                    existing.getPasswordHashBase64()
            );
            if (sameAsCurrent) {
                return ProfileUpdateResult.failure(
                        "Profile update failed: new password cannot be the same as the current password.",
                        false
                );
            }
            salt = project.task1.security.PasswordSecurity.generateSaltBase64();
            hash = project.task1.security.PasswordSecurity.hashPasswordBase64(pwd, salt);
            passwordChanged = true;
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
        return ProfileUpdateResult.success("Profile updated successfully.", passwordChanged);
    }

    public OperationResult updateProfilePicture(String username, String sourcePath) {
        String normalizedUser = safeTrim(username);
        String normalizedPath = safeTrim(sourcePath);
        if (normalizedUser.isEmpty() || normalizedPath.isEmpty()) {
            return OperationResult.failure("Profile picture update failed: invalid input.");
        }
        Path source = Paths.get(normalizedPath);
        if (!Files.exists(source) || !Files.isRegularFile(source)) {
            return OperationResult.failure("Profile picture update failed: file not found.");
        }
        String lowerName = source.getFileName().toString().toLowerCase();
        if (!(lowerName.endsWith(".png")
                || lowerName.endsWith(".jpg")
                || lowerName.endsWith(".jpeg")
                || lowerName.endsWith(".gif")
                || lowerName.endsWith(".webp"))) {
            return OperationResult.failure("Profile picture update failed: allowed formats are png/jpg/jpeg/gif/webp.");
        }
        try {
            long size = Files.size(source);
            if (size > MAX_PROFILE_PICTURE_SIZE_BYTES) {
                return OperationResult.failure("Profile picture update failed: file size must be <= 2MB.");
            }
            Files.createDirectories(Paths.get(TASK1_PROFILE_PICTURE_DIR));
            String ext = lowerName.substring(lowerName.lastIndexOf('.'));
            String safeUser = normalizedUser.replaceAll("[^A-Za-z0-9_-]", "_");
            String filename = safeUser + "_" + System.currentTimeMillis() + ext;
            Path target = Paths.get(TASK1_PROFILE_PICTURE_DIR, filename);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            saveProfilePicturePath(normalizedUser, target.toAbsolutePath().toString());
            appendNotification(normalizedUser, "ANNOUNCEMENT", "Your profile picture was updated.");
            return OperationResult.success("Profile picture updated.");
        } catch (Exception e) {
            return OperationResult.failure("Profile picture update failed: " + e.getMessage());
        }
    }

    public String getProfilePicturePath(String username) {
        String normalized = safeTrim(username);
        if (normalized.isEmpty()) {
            return "";
        }
        return loadProfilePictures().stream()
                .filter(row -> normalized.equals(row.username()))
                .map(ProfilePictureRow::path)
                .findFirst()
                .orElse("");
    }

    public List<NotificationView> getNotificationBoard(String username) {
        return getNotificationBoard(username, "", "ALL", "ACTIVE");
    }

    public List<NotificationView> getNotificationBoard(String username, String searchFilter, String categoryFilter) {
        return getNotificationBoard(username, searchFilter, categoryFilter, "ACTIVE");
    }

    public List<NotificationView> getNotificationBoard(String username, String searchFilter, String categoryFilter, String archiveFilter) {
        String normalized = safeTrim(username);
        if (normalized.isEmpty()) {
            return List.of();
        }
        String search = safeTrim(searchFilter).toLowerCase();
        String category = safeTrim(categoryFilter).toUpperCase();
        String archiveMode = safeTrim(archiveFilter).toUpperCase();
        List<NotificationView> notifications = loadStoredNotifications(normalized);
        return notifications.stream()
                .filter(n -> {
                    if ("ARCHIVED".equals(archiveMode)) {
                        return n.archived();
                    }
                    if ("ALL".equals(archiveMode)) {
                        return true;
                    }
                    return !n.archived();
                })
                .filter(n -> search.isEmpty()
                        || n.message().toLowerCase().contains(search)
                        || n.category().toLowerCase().contains(search))
                .filter(n -> category.isEmpty()
                        || "ALL".equals(category)
                        || n.category().equalsIgnoreCase(category))
                .sorted((a, b) -> {
                    boolean ap = a.isUrgent();
                    boolean bp = b.isUrgent();
                    if (ap != bp) return ap ? -1 : 1;
                    return b.timestamp().compareTo(a.timestamp());
                })
                .collect(Collectors.toList());
    }

    public List<NotificationView> getUnreadNotifications(String username, int limit) {
        String normalized = safeTrim(username);
        if (normalized.isEmpty()) {
            return List.of();
        }
        return loadStoredNotifications(normalized).stream()
                .filter(n -> !n.archived())
                .filter(n -> !n.read())
                .sorted((a, b) -> {
                    if (a.isUrgent() != b.isUrgent()) {
                        return a.isUrgent() ? -1 : 1;
                    }
                    return b.timestamp().compareTo(a.timestamp());
                })
                .limit(Math.max(0, limit))
                .collect(Collectors.toList());
    }

    public int getUnreadNotificationCount(String username) {
        String normalized = safeTrim(username);
        if (normalized.isEmpty()) {
            return 0;
        }
        return (int) loadStoredNotifications(normalized).stream()
                .filter(n -> !n.archived())
                .filter(n -> !n.read())
                .count();
    }

    public void markNotificationAsRead(String username, String notificationId) {
        String normalized = safeTrim(username);
        String normalizedId = safeTrim(notificationId);
        if (normalized.isEmpty() || normalizedId.isEmpty()) {
            return;
        }
        boolean owned = notificationStore.findByScopeAndUser(TASK1_NOTIFICATION_SCOPE, normalized).stream()
                .anyMatch(n -> n.id().equals(normalizedId));
        if (owned) {
            notificationStore.markRead(TASK1_NOTIFICATION_SCOPE, normalizedId);
        }
    }

    public void deleteNotification(String username, String notificationId) {
        String normalized = safeTrim(username);
        String normalizedId = safeTrim(notificationId);
        if (normalized.isEmpty() || normalizedId.isEmpty()) {
            return;
        }
        boolean owned = notificationStore.findByScopeAndUser(TASK1_NOTIFICATION_SCOPE, normalized).stream()
                .anyMatch(n -> n.id().equals(normalizedId));
        if (owned) {
            notificationStore.deleteById(TASK1_NOTIFICATION_SCOPE, normalizedId);
            List<NotificationArchiveEntry> rows = loadNotificationArchives().stream()
                    .filter(a -> !(normalized.equals(a.username()) && normalizedId.equals(a.notificationId())))
                    .collect(Collectors.toCollection(ArrayList::new));
            saveNotificationArchives(rows);
        }
    }

    public void deleteReadNotifications(String username) {
        String normalized = safeTrim(username);
        if (normalized.isEmpty()) {
            return;
        }
        List<String> deletedIds = notificationStore.findByScopeAndUser(TASK1_NOTIFICATION_SCOPE, normalized).stream()
                .filter(UnifiedNotification::read)
                .map(UnifiedNotification::id)
                .collect(Collectors.toCollection(ArrayList::new));
        notificationStore.deleteReadByUser(TASK1_NOTIFICATION_SCOPE, normalized);
        if (deletedIds.isEmpty()) {
            return;
        }
        List<NotificationArchiveEntry> remaining = loadNotificationArchives().stream()
                .filter(a -> !normalized.equals(a.username()) || !deletedIds.contains(a.notificationId()))
                .collect(Collectors.toCollection(ArrayList::new));
        saveNotificationArchives(remaining);
    }

    public void archiveNotification(String username, String notificationId) {
        String normalized = safeTrim(username);
        String normalizedId = safeTrim(notificationId);
        if (normalized.isEmpty() || normalizedId.isEmpty()) {
            return;
        }
        boolean owned = notificationStore.findByScopeAndUser(TASK1_NOTIFICATION_SCOPE, normalized).stream()
                .anyMatch(n -> n.id().equals(normalizedId));
        if (!owned) {
            return;
        }
        List<NotificationArchiveEntry> rows = loadNotificationArchives();
        boolean exists = rows.stream().anyMatch(r -> normalized.equals(r.username()) && normalizedId.equals(r.notificationId()));
        if (!exists) {
            rows.add(new NotificationArchiveEntry(normalized, normalizedId));
            saveNotificationArchives(rows);
        }
    }

    public void restoreArchivedNotification(String username, String notificationId) {
        String normalized = safeTrim(username);
        String normalizedId = safeTrim(notificationId);
        if (normalized.isEmpty() || normalizedId.isEmpty()) {
            return;
        }
        List<NotificationArchiveEntry> rows = loadNotificationArchives().stream()
                .filter(r -> !(normalized.equals(r.username()) && normalizedId.equals(r.notificationId())))
                .collect(Collectors.toCollection(ArrayList::new));
        saveNotificationArchives(rows);
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

    public void handleBookDeleted(String bookId, String bookTitle) {
        String normalizedBookId = safeTrim(bookId).toUpperCase();
        if (normalizedBookId.isEmpty()) {
            return;
        }
        notifyBookDeletedForBorrowers(normalizedBookId, bookTitle);
        List<BorrowRecord> records = loadBorrowRecords();
        boolean changed = false;
        LocalDate today = LocalDate.now();
        for (int i = 0; i < records.size(); i++) {
            BorrowRecord record = records.get(i);
            if (record.returnDate() != null) {
                continue;
            }
            if (!normalizedBookId.equalsIgnoreCase(record.bookId())) {
                continue;
            }
            records.set(i, record.withReturnDate(today, "BOOK_DELETED"));
            changed = true;
        }
        if (changed) {
            saveBorrowRecords(records);
        }
        clearReadingProgressForAllUsersForBook(normalizedBookId);
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
        String bookOwnedPath = bookRepository.findById(normalizedBookId)
                .map(Book::getPdfFilePath)
                .map(StudentStaffPortalService::safeTrim)
                .orElse("");
        if (!bookOwnedPath.isBlank()) {
            return bookOwnedPath;
        }
        return loadReadingProgress()
                .stream()
                .filter(r -> normalizedUser.equals(r.username()) && normalizedBookId.equalsIgnoreCase(r.bookId()))
                .map(ReadingProgress::pdfPath)
                .map(StudentStaffPortalService::safeTrim)
                .filter(path -> !path.isBlank())
                .findFirst()
                .orElse("");
    }

    public String resolveBorrowedBookPdfPath(String username, String bookId) {
        String normalizedUser = safeTrim(username);
        String normalizedBookId = safeTrim(bookId).toUpperCase();
        if (normalizedUser.isEmpty() || normalizedBookId.isEmpty()) {
            return "";
        }

        String linkedPath = getBorrowedBookPdfPath(normalizedUser, normalizedBookId);
        if (!linkedPath.isBlank()) {
            File linkedFile = new File(linkedPath);
            if (linkedFile.exists()) {
                return linkedPath;
            }
        }

        Optional<Book> borrowedBook = bookRepository.findById(normalizedBookId);
        if (borrowedBook.isEmpty()) {
            return "";
        }

        String fallbackPath = findApprovedSubmissionPdfPath(
                borrowedBook.get().getTitle(),
                borrowedBook.get().getAuthor()
        );
        if (fallbackPath.isBlank()) {
            return "";
        }

        File fallbackFile = new File(fallbackPath);
        if (!fallbackFile.exists()) {
            return "";
        }

        // Keep legacy mapping for older books that do not yet store pdf in Book entity.
        setBorrowedBookPdfPath(normalizedUser, normalizedBookId, fallbackPath);
        return fallbackPath;
    }

    private String findApprovedSubmissionPdfPath(String bookTitle, String authorFullName) {
        String normalizedTitle = safeTrim(bookTitle).toLowerCase();
        String normalizedAuthor = safeTrim(authorFullName).toLowerCase();
        if (normalizedTitle.isEmpty() || normalizedAuthor.isEmpty()) {
            return "";
        }

        submissionRepository.refreshFromFile();
        return submissionRepository.findAll()
                .stream()
                .filter(sub -> sub != null && sub.isApproved())
                .filter(sub -> normalizedTitle.equals(safeTrim(sub.getTitle()).toLowerCase()))
                .filter(sub -> normalizedAuthor.equals(safeTrim(sub.getAuthorFullName()).toLowerCase()))
                .map(sub -> safeTrim(sub.getFilePath()))
                .filter(path -> !path.isBlank())
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

    public OperationResult setReadingProgress(String username, String bookId, String bookmark, String highlightNotes) {
        String normalizedUser = safeTrim(username);
        String normalizedBookId = safeTrim(bookId).toUpperCase();
        if (normalizedUser.isEmpty() || normalizedBookId.isEmpty()) {
            return OperationResult.failure("Reading update failed: invalid user/book.");
        }
        String normalizedBookmark = bookmark == null ? "" : bookmark.trim();
        String normalizedHighlights = highlightNotes == null ? "" : highlightNotes.trim();

        List<ReadingProgress> rows = loadReadingProgress();
        boolean updated = false;
        for (int i = 0; i < rows.size(); i++) {
            ReadingProgress row = rows.get(i);
            if (normalizedUser.equals(row.username()) && normalizedBookId.equalsIgnoreCase(row.bookId())) {
                rows.set(i, new ReadingProgress(
                        row.username(),
                        row.bookId(),
                        row.pdfPath(),
                        normalizedBookmark,
                        normalizedHighlights
                ));
                updated = true;
                break;
            }
        }
        if (!updated) {
            rows.add(new ReadingProgress(
                    normalizedUser,
                    normalizedBookId,
                    "",
                    normalizedBookmark,
                    normalizedHighlights
            ));
        }
        saveReadingProgress(rows);
        return OperationResult.success("Reading progress updated.");
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

    public List<ReadingHistoryView> getReadingHistory(
            String username,
            String titleFilter,
            String authorFilter,
            String genreFilter,
            LocalDate borrowDateFrom,
            LocalDate borrowDateTo
    ) {
        String normalizedUser = safeTrim(username);
        if (normalizedUser.isEmpty()) {
            return List.of();
        }
        String title = safeTrim(titleFilter).toLowerCase();
        String author = safeTrim(authorFilter).toLowerCase();
        String genre = safeTrim(genreFilter).toLowerCase();

        Map<String, ReadingProgress> progressByBook = loadReadingProgress().stream()
                .filter(r -> normalizedUser.equals(r.username()))
                .collect(Collectors.toMap(
                        r -> r.bookId().toUpperCase(),
                        r -> r,
                        (a, b) -> b,
                        HashMap::new
                ));

        List<ReadingHistoryView> rows = new ArrayList<>();
        for (BorrowRecord record : loadBorrowRecords()) {
            if (!normalizedUser.equals(record.username())) {
                continue;
            }
            if (!title.isEmpty() && !record.bookTitle().toLowerCase().contains(title)) {
                continue;
            }
            if (!author.isEmpty() && !safeTrim(record.bookAuthor()).toLowerCase().contains(author)) {
                continue;
            }
            if (!genre.isEmpty() && !safeTrim(record.bookGenre()).toLowerCase().contains(genre)) {
                continue;
            }
            if (borrowDateFrom != null && record.borrowDate().isBefore(borrowDateFrom)) {
                continue;
            }
            if (borrowDateTo != null && record.borrowDate().isAfter(borrowDateTo)) {
                continue;
            }
            LocalDate end = record.returnDate() == null ? LocalDate.now() : record.returnDate();
            long durationDays = Math.max(0, ChronoUnit.DAYS.between(record.borrowDate(), end));
            ReadingProgress progress = progressByBook.getOrDefault(record.bookId().toUpperCase(), null);
            String bookmark = progress == null ? "" : safeTrim(progress.bookmark());
            String progressText = progress == null
                    ? ""
                    : buildProgressSummary(progress.bookmark(), progress.highlightNotes());
            rows.add(new ReadingHistoryView(
                    record.bookId(),
                    record.bookTitle(),
                    safeTrim(record.bookAuthor()),
                    safeTrim(record.bookGenre()),
                    record.borrowDate(),
                    record.returnDate(),
                    durationDays,
                    bookmark,
                    progressText
            ));
        }
        rows.sort(Comparator.comparing(ReadingHistoryView::borrowDate).reversed());
        return rows;
    }

    public OperationResult exportReadingHistoryCsv(String username, Path outputPath) {
        if (outputPath == null) {
            return OperationResult.failure("Export failed: output path is required.");
        }
        List<ReadingHistoryView> rows = getReadingHistory(username, "", "", "", null, null);
        try {
            List<String> lines = new ArrayList<>();
            lines.add("Book ID,Book Title,Author,Genre,Borrow Date,Return Date,Reading Duration Days,Bookmark,Reading Progress");
            for (ReadingHistoryView row : rows) {
                lines.add(csv(row.bookId()) + ","
                        + csv(row.bookTitle()) + ","
                        + csv(row.author()) + ","
                        + csv(row.genre()) + ","
                        + csv(row.borrowDate() == null ? "" : row.borrowDate().toString()) + ","
                        + csv(row.returnDate() == null ? "" : row.returnDate().toString()) + ","
                        + csv(Long.toString(row.readingDurationDays())) + ","
                        + csv(row.bookmark()) + ","
                        + csv(row.progressSummary()));
            }
            Files.write(outputPath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return OperationResult.success("Reading history exported to CSV: " + outputPath);
        } catch (Exception e) {
            return OperationResult.failure("Export failed: " + e.getMessage());
        }
    }

    public OperationResult exportReadingHistoryPdf(String username, Path outputPath) {
        if (outputPath == null) {
            return OperationResult.failure("Export failed: output path is required.");
        }
        List<ReadingHistoryView> rows = getReadingHistory(username, "", "", "", null, null);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDPageContentStream stream = new PDPageContentStream(document, page);
            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
            float y = 800;
            stream.beginText();
            stream.newLineAtOffset(40, y);
            stream.showText("Reading History Export - " + safeTrim(username));
            stream.endText();
            y -= 22;
            for (ReadingHistoryView row : rows) {
                String line = row.borrowDate() + " | " + row.bookTitle()
                        + " | " + row.genre()
                        + " | Duration " + row.readingDurationDays() + " day(s)"
                        + " | Bookmark: " + (row.bookmark().isBlank() ? "-" : row.bookmark());
                if (y < 60) {
                    stream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    stream = new PDPageContentStream(document, page);
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                    y = 800;
                }
                stream.beginText();
                stream.newLineAtOffset(40, y);
                stream.showText(line.length() > 120 ? line.substring(0, 120) : line);
                stream.endText();
                y -= 16;
            }
            stream.close();
            document.save(outputPath.toFile());
            return OperationResult.success("Reading history exported to PDF: " + outputPath);
        } catch (Exception e) {
            return OperationResult.failure("Export failed: " + e.getMessage());
        }
    }

    public ReadingHistoryInsights getReadingHistoryInsights(String username) {
        List<ReadingHistoryView> rows = getReadingHistory(username, "", "", "", null, null);
        if (rows.isEmpty()) {
            return new ReadingHistoryInsights(Map.of(), 0.0, 0);
        }
        Map<String, Integer> genreCounts = new HashMap<>();
        long totalDuration = 0;
        for (ReadingHistoryView row : rows) {
            genreCounts.merge(safeTrim(row.genre()).isBlank() ? "Unknown" : row.genre(), 1, Integer::sum);
            totalDuration += row.readingDurationDays();
        }
        double averageDuration = totalDuration / (double) rows.size();
        return new ReadingHistoryInsights(genreCounts, averageDuration, rows.size());
    }

    public List<String> getReadingBadges(String username) {
        List<ReadingHistoryView> rows = getReadingHistory(username, "", "", "", null, null);
        long completed = rows.stream().filter(r -> r.returnDate() != null).count();
        List<String> badges = new ArrayList<>();
        if (completed >= 1) badges.add("First Book Finished");
        if (completed >= 5) badges.add("Book Explorer: Read 5 books");
        if (completed >= 10) badges.add("Semester Milestone: Read 10 books");
        if (rows.stream().map(ReadingHistoryView::genre).filter(g -> g != null && !g.isBlank()).distinct().count() >= 3) {
            badges.add("Genre Hopper: 3+ genres explored");
        }
        if (badges.isEmpty()) {
            badges.add("Start reading to unlock badges");
        }
        return badges;
    }

    public OperationResult submitBookReview(String username, String bookId, int rating, String reviewText) {
        return submitBookReview(username, bookId, rating, reviewText, false);
    }

    public OperationResult submitBookReview(String username, String bookId, int rating, String reviewText, boolean anonymous) {
        String normalizedUser = safeTrim(username);
        String normalizedBookId = safeTrim(bookId).toUpperCase();
        String normalizedReview = reviewText == null ? "" : reviewText.trim();
        if (normalizedUser.isEmpty() || normalizedBookId.isEmpty()) {
            return OperationResult.failure("Review failed: invalid user/book.");
        }
        if (rating < 1 || rating > 5) {
            return OperationResult.failure("Review failed: rating must be from 1 to 5.");
        }
        if (!hasBorrowedBook(normalizedUser, normalizedBookId)) {
            return OperationResult.failure("Review failed: you can only review books you have borrowed.");
        }

        Optional<Book> bookOpt = bookRepository.findById(normalizedBookId);
        String title = bookOpt.map(Book::getTitle).orElse(normalizedBookId);
        String author = bookOpt.map(Book::getAuthor).orElse("");
        String genre = bookOpt.map(Book::getGenre).orElse("");

        List<BookReview> reviews = loadBookReviews();
        BookReview row = new BookReview(
                normalizedUser,
                normalizedBookId,
                title,
                author,
                genre,
                rating,
                normalizedReview,
                anonymous,
                LocalDateTime.now()
        );
        boolean replaced = false;
        for (int i = 0; i < reviews.size(); i++) {
            BookReview existing = reviews.get(i);
            if (normalizedUser.equals(existing.username())
                    && normalizedBookId.equalsIgnoreCase(existing.bookId())) {
                reviews.set(i, row);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            reviews.add(row);
        }
        saveBookReviews(reviews);
        return OperationResult.success("Review saved successfully.");
    }

    public List<BookReviewView> getBookReviews(String bookId) {
        return getBookReviews(bookId, "MOST_RECENT");
    }

    public List<BookReviewView> getBookReviews(String bookId, String sortMode) {
        String normalizedBookId = safeTrim(bookId).toUpperCase();
        if (normalizedBookId.isEmpty()) {
            return List.of();
        }
        String sort = safeTrim(sortMode).toUpperCase();
        Map<String, Integer> helpfulCounts = buildHelpfulVoteCountByReview();
        Comparator<BookReview> comparator = "MOST_HELPFUL".equals(sort)
                ? Comparator.<BookReview>comparingInt(r -> helpfulCounts.getOrDefault(reviewKey(r.bookId(), r.username()), 0)).reversed()
                .thenComparing(BookReview::updatedAt, Comparator.reverseOrder())
                : Comparator.comparing(BookReview::updatedAt).reversed();
        return loadBookReviews().stream()
                .filter(r -> normalizedBookId.equalsIgnoreCase(r.bookId()))
                .sorted(comparator)
                .map(r -> new BookReviewView(
                        r.username(),
                        r.anonymous(),
                        r.bookId(),
                        r.bookTitle(),
                        r.bookAuthor(),
                        r.bookGenre(),
                        r.rating(),
                        r.reviewText(),
                        helpfulCounts.getOrDefault(reviewKey(r.bookId(), r.username()), 0),
                        r.updatedAt()
                ))
                .collect(Collectors.toList());
    }

    public OperationResult markReviewHelpful(String voterUsername, String bookId, String reviewOwnerUsername) {
        String voter = safeTrim(voterUsername);
        String normalizedBookId = safeTrim(bookId).toUpperCase();
        String owner = safeTrim(reviewOwnerUsername);
        if (voter.isEmpty() || normalizedBookId.isEmpty() || owner.isEmpty()) {
            return OperationResult.failure("Helpful vote failed: invalid input.");
        }
        if (voter.equalsIgnoreCase(owner)) {
            return OperationResult.failure("Helpful vote failed: you cannot vote your own review.");
        }
        boolean reviewExists = loadBookReviews().stream()
                .anyMatch(r -> normalizedBookId.equalsIgnoreCase(r.bookId()) && owner.equalsIgnoreCase(r.username()));
        if (!reviewExists) {
            return OperationResult.failure("Helpful vote failed: review not found.");
        }
        List<ReviewHelpfulVote> votes = loadReviewHelpfulVotes();
        boolean exists = votes.stream().anyMatch(v -> voter.equals(v.voterUsername())
                && normalizedBookId.equalsIgnoreCase(v.bookId())
                && owner.equalsIgnoreCase(v.reviewOwnerUsername()));
        if (exists) {
            return OperationResult.failure("Helpful vote already submitted.");
        }
        votes.add(new ReviewHelpfulVote(voter, normalizedBookId, owner, LocalDateTime.now()));
        saveReviewHelpfulVotes(votes);
        return OperationResult.success("Marked as helpful.");
    }

    public BookRatingSummary getBookRatingSummary(String bookId) {
        List<BookReviewView> rows = getBookReviews(bookId);
        if (rows.isEmpty()) {
            return new BookRatingSummary(0.0, 0);
        }
        double avg = rows.stream().mapToInt(BookReviewView::rating).average().orElse(0.0);
        return new BookRatingSummary(avg, rows.size());
    }

    public Map<String, BookRatingSummary> getRatingSummaryByBookId() {
        Map<String, List<BookReview>> grouped = loadBookReviews().stream()
                .collect(Collectors.groupingBy(r -> r.bookId().toUpperCase()));
        Map<String, BookRatingSummary> map = new HashMap<>();
        for (Map.Entry<String, List<BookReview>> entry : grouped.entrySet()) {
            List<BookReview> rows = entry.getValue();
            double avg = rows.stream().mapToInt(BookReview::rating).average().orElse(0.0);
            map.put(entry.getKey(), new BookRatingSummary(avg, rows.size()));
        }
        return map;
    }

    public OperationResult submitBookRequest(
            String username,
            String title,
            String author,
            String genre,
            String reason
    ) {
        String normalizedUser = safeTrim(username);
        String normalizedTitle = safeTrim(title);
        String normalizedAuthor = safeTrim(author);
        String normalizedGenre = safeTrim(genre);
        String normalizedReason = safeTrim(reason);
        if (normalizedUser.isEmpty()) {
            return OperationResult.failure("Request failed: invalid user.");
        }
        if (normalizedTitle.isEmpty() || normalizedAuthor.isEmpty() || normalizedGenre.isEmpty() || normalizedReason.isEmpty()) {
            return OperationResult.failure("Request failed: title, author, genre, and reason are all required.");
        }
        Optional<BookRequest> duplicate = loadBookRequests().stream()
                .filter(r -> "PENDING".equalsIgnoreCase(r.status()) || "APPROVED".equalsIgnoreCase(r.status()))
                .filter(r -> normalizedTitle.equalsIgnoreCase(r.title()))
                .filter(r -> normalizedAuthor.equalsIgnoreCase(r.author()))
                .findFirst();
        if (duplicate.isPresent()) {
            return OperationResult.failure("Duplicate request detected: existing request ID " + duplicate.get().requestId()
                    + " is already " + duplicate.get().status() + ".");
        }
        String requestId = "REQ" + System.currentTimeMillis();
        List<BookRequest> rows = loadBookRequests();
        rows.add(new BookRequest(
                requestId,
                normalizedUser,
                normalizedTitle,
                normalizedAuthor,
                normalizedGenre,
                normalizedReason,
                "PENDING",
                false,
                "",
                LocalDateTime.now(),
                null
        ));
        saveBookRequests(rows);
        notifyLibrariansOfBookRequest(requestId, normalizedUser, normalizedTitle, normalizedAuthor);
        return OperationResult.success("Request submitted successfully. Request ID: " + requestId);
    }

    public List<BookRequestView> getMyBookRequests(String username) {
        String normalizedUser = safeTrim(username);
        if (normalizedUser.isEmpty()) {
            return List.of();
        }
        return loadBookRequests().stream()
                .filter(r -> normalizedUser.equals(r.username()))
                .sorted(Comparator.comparing(BookRequest::createdAt).reversed())
                .map(r -> new BookRequestView(
                        r.requestId(),
                        r.username(),
                        r.title(),
                        r.author(),
                        r.genre(),
                        r.reason(),
                        r.status(),
                        r.urgent(),
                        r.librarianComment(),
                        r.createdAt(),
                        r.decidedAt()
                ))
                .collect(Collectors.toList());
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

    private void recordBorrowRecord(
            String username,
            String bookId,
            String bookTitle,
            String bookAuthor,
            String bookGenre,
            LocalDate borrowDate,
            LocalDate dueDate
    ) {
        List<BorrowRecord> records = loadBorrowRecords();
        records.add(new BorrowRecord(username, bookId, bookTitle, bookAuthor, bookGenre, borrowDate, dueDate, null, ""));
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
                        parts.length >= 9 ? decode(parts[3]) : "",
                        parts.length >= 9 ? decode(parts[4]) : "",
                        LocalDate.parse(parts[3 + (parts.length >= 9 ? 2 : 0)]),
                        LocalDate.parse(parts[4 + (parts.length >= 9 ? 2 : 0)]),
                        parts[5 + (parts.length >= 9 ? 2 : 0)].isBlank()
                                ? null
                                : LocalDate.parse(parts[5 + (parts.length >= 9 ? 2 : 0)]),
                        decode(parts[6 + (parts.length >= 9 ? 2 : 0)])
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
                        encode(r.bookAuthor()),
                        encode(r.bookGenre()),
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
        notificationStore.create(
                TASK1_NOTIFICATION_SCOPE,
                username,
                category,
                message,
                category,
                category,
                isPriorityCategory(category),
                ""
        );
    }

    private List<NotificationView> loadStoredNotifications(String username) {
        List<String> archivedIds = loadNotificationArchives().stream()
                .filter(a -> username.equals(a.username()))
                .map(NotificationArchiveEntry::notificationId)
                .collect(Collectors.toCollection(ArrayList::new));
        return notificationStore.findByScopeAndUser(TASK1_NOTIFICATION_SCOPE, username)
                .stream()
                .map(n -> new NotificationView(
                        n.id(),
                        n.createdAt(),
                        n.category().isBlank() ? n.type() : n.category(),
                        n.message(),
                        n.read(),
                        n.priority(),
                        archivedIds.contains(n.id())
                ))
                .collect(Collectors.toList());
    }

    private void upsertDueReminder(BorrowRecord record, LocalDate today, long daysLeft) {
        String message = daysLeft == 0
                ? "Book \"" + record.bookTitle() + "\" is due today (" + record.dueDate() + ")."
                : "Book \"" + record.bookTitle() + "\" is due on " + record.dueDate()
                + " (" + daysLeft + " day(s) left).";
        String id = "DUE_TASK1_" + record.username() + "_" + record.bookId() + "_" + today;
        notificationStore.upsert(new UnifiedNotification(
                id,
                TASK1_NOTIFICATION_SCOPE,
                record.username(),
                "DUE_REMINDER",
                message,
                "DUE_REMINDER",
                "DUE_REMINDER",
                false,
                daysLeft <= 1,
                record.bookId(),
                LocalDateTime.now()
        ));
    }

    private List<NotificationArchiveEntry> loadNotificationArchives() {
        Path path = Paths.get(TASK1_NOTIFICATION_ARCHIVE_FILE);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try {
            List<NotificationArchiveEntry> rows = new ArrayList<>();
            for (String line : Files.readAllLines(path)) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                if (parts.length < 2) {
                    continue;
                }
                rows.add(new NotificationArchiveEntry(decode(parts[0]), decode(parts[1])));
            }
            return rows;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveNotificationArchives(List<NotificationArchiveEntry> rows) {
        try {
            Files.createDirectories(Paths.get("data/task1"));
            List<String> lines = new ArrayList<>();
            for (NotificationArchiveEntry row : rows) {
                lines.add(encode(row.username()) + "|" + encode(row.notificationId()));
            }
            Files.write(Paths.get(TASK1_NOTIFICATION_ARCHIVE_FILE), lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ignored) {
        }
    }

    private List<ProfilePictureRow> loadProfilePictures() {
        Path path = Paths.get(TASK1_PROFILE_PICTURE_FILE);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try {
            List<ProfilePictureRow> rows = new ArrayList<>();
            for (String line : Files.readAllLines(path)) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                if (parts.length < 2) {
                    continue;
                }
                rows.add(new ProfilePictureRow(decode(parts[0]), decode(parts[1])));
            }
            return rows;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveProfilePicturePath(String username, String path) {
        List<ProfilePictureRow> rows = loadProfilePictures();
        boolean updated = false;
        for (int i = 0; i < rows.size(); i++) {
            ProfilePictureRow row = rows.get(i);
            if (username.equals(row.username())) {
                rows.set(i, new ProfilePictureRow(username, path));
                updated = true;
                break;
            }
        }
        if (!updated) {
            rows.add(new ProfilePictureRow(username, path));
        }
        try {
            Files.createDirectories(Paths.get("data/task1"));
            List<String> lines = new ArrayList<>();
            for (ProfilePictureRow row : rows) {
                lines.add(encode(row.username()) + "|" + encode(row.path()));
            }
            Files.write(Paths.get(TASK1_PROFILE_PICTURE_FILE), lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ignored) {
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

    private boolean hasBorrowedBook(String username, String bookId) {
        return loadBorrowRecords().stream()
                .anyMatch(r -> username.equals(r.username()) && bookId.equalsIgnoreCase(r.bookId()));
    }

    private String buildProgressSummary(String bookmark, String highlights) {
        List<String> parts = new ArrayList<>();
        if (bookmark != null && !bookmark.isBlank()) {
            parts.add("Bookmark: " + bookmark);
        }
        if (highlights != null && !highlights.isBlank()) {
            int count = (int) highlights.lines().filter(line -> !line.isBlank()).count();
            parts.add("Highlights: " + Math.max(1, count) + " note(s)");
        }
        return String.join(" | ", parts);
    }

    private List<BookReview> loadBookReviews() {
        Path path = Paths.get(TASK1_BOOK_REVIEWS_FILE);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try {
            List<BookReview> list = new ArrayList<>();
            for (String line : Files.readAllLines(path)) {
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                if (parts.length < 8) {
                    continue;
                }
                boolean anonymous = parts.length >= 9 && Boolean.parseBoolean(parts[8]);
                list.add(new BookReview(
                        decode(parts[0]),
                        decode(parts[1]),
                        decode(parts[2]),
                        decode(parts[3]),
                        decode(parts[4]),
                        Integer.parseInt(parts[5]),
                        decode(parts[6]),
                        anonymous,
                        LocalDateTime.parse(parts[7], HISTORY_TIME_FORMAT)
                ));
            }
            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveBookReviews(List<BookReview> rows) {
        try {
            Files.createDirectories(Paths.get("data/task1"));
            List<String> lines = new ArrayList<>();
            for (BookReview r : rows) {
                lines.add(String.join("|",
                        encode(r.username()),
                        encode(r.bookId()),
                        encode(r.bookTitle()),
                        encode(r.bookAuthor()),
                        encode(r.bookGenre()),
                        Integer.toString(r.rating()),
                        encode(r.reviewText() == null ? "" : r.reviewText()),
                        r.updatedAt().format(HISTORY_TIME_FORMAT),
                        Boolean.toString(r.anonymous())
                ));
            }
            Files.write(Paths.get(TASK1_BOOK_REVIEWS_FILE), lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    private List<ReviewHelpfulVote> loadReviewHelpfulVotes() {
        Path path = Paths.get(TASK1_REVIEW_HELPFUL_FILE);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try {
            List<ReviewHelpfulVote> rows = new ArrayList<>();
            for (String line : Files.readAllLines(path)) {
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                if (parts.length < 4) {
                    continue;
                }
                rows.add(new ReviewHelpfulVote(
                        decode(parts[0]),
                        decode(parts[1]),
                        decode(parts[2]),
                        LocalDateTime.parse(parts[3], HISTORY_TIME_FORMAT)
                ));
            }
            return rows;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveReviewHelpfulVotes(List<ReviewHelpfulVote> votes) {
        try {
            Files.createDirectories(Paths.get("data/task1"));
            List<String> lines = new ArrayList<>();
            for (ReviewHelpfulVote v : votes) {
                lines.add(String.join("|",
                        encode(v.voterUsername()),
                        encode(v.bookId()),
                        encode(v.reviewOwnerUsername()),
                        v.votedAt().format(HISTORY_TIME_FORMAT)
                ));
            }
            Files.write(Paths.get(TASK1_REVIEW_HELPFUL_FILE), lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ignored) {
        }
    }

    private Map<String, Integer> buildHelpfulVoteCountByReview() {
        Map<String, Integer> map = new HashMap<>();
        for (ReviewHelpfulVote vote : loadReviewHelpfulVotes()) {
            map.merge(reviewKey(vote.bookId(), vote.reviewOwnerUsername()), 1, Integer::sum);
        }
        return map;
    }

    private String reviewKey(String bookId, String owner) {
        return safeTrim(bookId).toUpperCase() + "::" + safeTrim(owner).toLowerCase();
    }

    private List<BookRequest> loadBookRequests() {
        Path path = Paths.get(TASK1_BOOK_REQUESTS_FILE);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try {
            List<BookRequest> list = new ArrayList<>();
            for (String line : Files.readAllLines(path)) {
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                if (parts.length < 10) {
                    continue;
                }
                boolean urgent = parts.length >= 11 && Boolean.parseBoolean(parts[10]);
                list.add(new BookRequest(
                        decode(parts[0]),
                        decode(parts[1]),
                        decode(parts[2]),
                        decode(parts[3]),
                        decode(parts[4]),
                        decode(parts[5]),
                        decode(parts[6]),
                        urgent,
                        decode(parts[7]),
                        LocalDateTime.parse(parts[8], HISTORY_TIME_FORMAT),
                        parts[9].isBlank() ? null : LocalDateTime.parse(parts[9], HISTORY_TIME_FORMAT)
                ));
            }
            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveBookRequests(List<BookRequest> rows) {
        try {
            Files.createDirectories(Paths.get("data/task1"));
            List<String> lines = new ArrayList<>();
            for (BookRequest r : rows) {
                lines.add(String.join("|",
                        encode(r.requestId()),
                        encode(r.username()),
                        encode(r.title()),
                        encode(r.author()),
                        encode(r.genre()),
                        encode(r.reason()),
                        encode(r.status()),
                        encode(r.librarianComment() == null ? "" : r.librarianComment()),
                        r.createdAt().format(HISTORY_TIME_FORMAT),
                        r.decidedAt() == null ? "" : r.decidedAt().format(HISTORY_TIME_FORMAT),
                        Boolean.toString(r.urgent())
                ));
            }
            Files.write(Paths.get(TASK1_BOOK_REQUESTS_FILE), lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    private void notifyLibrariansOfBookRequest(String requestId, String requester, String title, String author) {
        for (var librarian : librarianRepository.getAllUsers()) {
            notificationStore.create(
                    "TASK3",
                    librarian.getUsername(),
                    "NEW_BOOK_REQUEST",
                    "New request " + requestId + ": \"" + title + "\" by " + author + " (requested by " + requester + ").",
                    "NEW_BOOK_REQUEST",
                    "NEW_BOOK_REQUEST",
                    true,
                    requestId
            );
        }
    }

    private void clearReadingProgressForUserBook(String username, String bookId) {
        String normalizedUser = safeTrim(username);
        String normalizedBookId = safeTrim(bookId).toUpperCase();
        if (normalizedUser.isEmpty() || normalizedBookId.isEmpty()) {
            return;
        }
        List<ReadingProgress> remaining = loadReadingProgress()
                .stream()
                .filter(r -> !(normalizedUser.equals(r.username()) && normalizedBookId.equalsIgnoreCase(r.bookId())))
                .collect(Collectors.toCollection(ArrayList::new));
        saveReadingProgress(remaining);
    }

    private void clearReadingProgressForAllUsersForBook(String bookId) {
        String normalizedBookId = safeTrim(bookId).toUpperCase();
        if (normalizedBookId.isEmpty()) {
            return;
        }
        List<ReadingProgress> remaining = loadReadingProgress()
                .stream()
                .filter(r -> !normalizedBookId.equalsIgnoreCase(r.bookId()))
                .collect(Collectors.toCollection(ArrayList::new));
        saveReadingProgress(remaining);
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

    private static String csv(String value) {
        String v = value == null ? "" : value;
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    private static boolean isPriorityCategory(String category) {
        String c = safeTrim(category).toUpperCase();
        return c.contains("DELETION") || c.contains("AUTO_RETURN") || c.contains("URGENT");
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

    public record ProfileUpdateResult(boolean success, String message, boolean passwordChanged) {
        public static ProfileUpdateResult success(String message, boolean passwordChanged) {
            return new ProfileUpdateResult(true, message, passwordChanged);
        }

        public static ProfileUpdateResult failure(String message, boolean passwordChanged) {
            return new ProfileUpdateResult(false, message, passwordChanged);
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
            String bookAuthor,
            String bookGenre,
            LocalDate borrowDate,
            LocalDate dueDate,
            LocalDate returnDate,
            String returnMode
    ) {
        private BorrowRecord withReturnDate(LocalDate date, String mode) {
            return new BorrowRecord(username, bookId, bookTitle, bookAuthor, bookGenre, borrowDate, dueDate, date, mode);
        }
    }

    public record BorrowRecordView(
            String bookId,
            String bookTitle,
            LocalDate borrowDate,
            LocalDate dueDate,
            String status
    ) {}

    public record ReadingHistoryView(
            String bookId,
            String bookTitle,
            String author,
            String genre,
            LocalDate borrowDate,
            LocalDate returnDate,
            long readingDurationDays,
            String bookmark,
            String progressSummary
    ) {}

    private record BookReview(
            String username,
            String bookId,
            String bookTitle,
            String bookAuthor,
            String bookGenre,
            int rating,
            String reviewText,
            boolean anonymous,
            LocalDateTime updatedAt
    ) {}

    public record BookReviewView(
            String username,
            boolean anonymous,
            String bookId,
            String bookTitle,
            String bookAuthor,
            String bookGenre,
            int rating,
            String reviewText,
            int helpfulVotes,
            LocalDateTime updatedAt
    ) {}

    public record BookRatingSummary(double averageRating, int reviewCount) {}

    private record BookRequest(
            String requestId,
            String username,
            String title,
            String author,
            String genre,
            String reason,
            String status,
            boolean urgent,
            String librarianComment,
            LocalDateTime createdAt,
            LocalDateTime decidedAt
    ) {}

    public record BookRequestView(
            String requestId,
            String username,
            String title,
            String author,
            String genre,
            String reason,
            String status,
            boolean urgent,
            String librarianComment,
            LocalDateTime createdAt,
            LocalDateTime decidedAt
    ) {}

    public record NotificationView(
            String notificationId,
            LocalDateTime timestamp,
            String category,
            String message,
            boolean read,
            boolean urgent,
            boolean archived
    ) {
        public boolean isUrgent() {
            return urgent;
        }
    }

    public record LifecycleProcessingResult(int autoReturnedCount, int reminderCount) {}

    private record NotificationArchiveEntry(String username, String notificationId) {}
    private record ProfilePictureRow(String username, String path) {}

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

    public record ReadingHistoryInsights(
            Map<String, Integer> genreReadCounts,
            double averageReadingDurationDays,
            int totalRecords
    ) {}

    private record ReviewHelpfulVote(
            String voterUsername,
            String bookId,
            String reviewOwnerUsername,
            LocalDateTime votedAt
    ) {}
}
