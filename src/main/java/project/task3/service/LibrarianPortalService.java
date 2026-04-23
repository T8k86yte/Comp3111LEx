package project.task3.service;

import org.apache.poi.hssf.usermodel.helpers.HSSFRowShifter;
import project.task1.model.StudentStaffAccount;
import project.task1.model.UserAccount;
import project.task1.model.UserRole;
import project.task1.model.Book;
import project.task1.repo.BookRepository;
import project.task1.repo.StudentStaffRepository;
import project.shared.SharedAuthFacade;
import project.task1.security.PasswordSecurity;
import project.task1.service.StudentStaffPortalService;
import project.task2.model.AuthorAccount;
import project.task2.model.BookSubmission;
import project.task2.repo.AuthorRepository;
import project.task2.repo.SubmissionRepository;
import project.task3.model.LibrarianAccount;
import project.task3.repo.LibrarianRepository;
import project.shared.notification.UnifiedNotification;
import project.shared.notification.UnifiedNotificationStore;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.time.chrono.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.hssf.usermodel.*;

public class LibrarianPortalService {
    private final LibrarianRepository librarianRepository;
    private final StudentStaffRepository studentStaffRepository;
    private final AuthorRepository authorRepository;
    private final SharedAuthFacade sharedAuthFacade;
    private final BookRepository bookRepository;
    private final SubmissionRepository bookSubmissionRepository;
    private final UnifiedNotificationStore notificationStore;

    private static final String LEGACY_TASK3_NOTIFICATIONS_FILE = "data/task3/notifications.txt";
    private static final String TASK1_NOTIFICATION_SCOPE = "TASK1";
    private static final String TASK2_NOTIFICATION_SCOPE = "TASK2";
    private static final String TASK3_NOTIFICATION_SCOPE = "TASK3";
    private static final String BORROW_RECORDS_FILE = "data/task1/borrow_records.txt";
    private static final String TASK1_BOOK_REQUESTS_FILE = "data/task1/book_requests.txt";
    private static final DateTimeFormatter HISTORY_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public LibrarianPortalService(LibrarianRepository librarianRepository, StudentStaffRepository studentStaffRepository, AuthorRepository authorRepository, BookRepository bookRepository, SubmissionRepository bookSubmissionRepository) {
        this.librarianRepository = librarianRepository;
        this.studentStaffRepository = studentStaffRepository;
        this.authorRepository = authorRepository;
        this.sharedAuthFacade = new SharedAuthFacade(studentStaffRepository, authorRepository, librarianRepository);
        this.bookRepository = bookRepository;
        this.bookSubmissionRepository = bookSubmissionRepository;
        this.notificationStore = new UnifiedNotificationStore();
        migrateLegacyTask3Notifications();
    }

    private void migrateLegacyTask3Notifications() {
        Path path = Paths.get(LEGACY_TASK3_NOTIFICATIONS_FILE);
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
                String legacyId = "LEGACY_TASK3_" + Integer.toUnsignedString(line.hashCode());
                notificationStore.upsert(new UnifiedNotification(
                        legacyId,
                        TASK3_NOTIFICATION_SCOPE,
                        username,
                        category,
                        message,
                        category,
                        category,
                        false,
                        "RESPONSE".equalsIgnoreCase(category),
                        "",
                        createdAt
                ));
            }
        } catch (Exception ignored) {
        }
    }

    public LibrarianPortalService.OperationResult registerLibrarian(String username, String fullname, String rawPassword, String employeeIDtext) {
        SharedAuthFacade.AuthResult authResult = sharedAuthFacade.register(
                username,
                fullname,
                rawPassword,
                null,
                "Librarian",
                null,
                employeeIDtext
        );
        if (!authResult.success()) {
            return OperationResult.failure(authResult.message());
        }
        return OperationResult.success(authResult.message());
    }

    public LoginResult login(String username, String rawPassword) {
        SharedAuthFacade.AuthResult authResult = sharedAuthFacade.login(username, rawPassword, "Librarian");
        if (!authResult.success()) {
            return LoginResult.failure(authResult.message());
        }
        LibrarianAccount user = librarianRepository.findByUsername(authResult.principal().username()).orElse(null);
        if (user == null) {
            return LoginResult.failure("Login failed: invalid username or password.");
        }

        return LoginResult.success(authResult.message(), user);
    }

    private static boolean filterBookSubmission(BookSubmission sub,
                                                Pattern titleFilter,
                                                Pattern authorUsernameFilter,
                                                Pattern genreFilter,
                                                LocalDateTime submissionMin,
                                                LocalDateTime submissionMax,
                                                String statusFilter) {
        if (!titleFilter.matcher(sub.getTitle()).matches()) return false;
        if (!authorUsernameFilter.matcher(sub.getAuthorUsername()).matches()) return false;
        if (!genreFilter.matcher(sub.getGenre()).matches()) return false;
        if (submissionMin != null && sub.getSubmissionDate().isBefore(submissionMin)) return false;
        if (submissionMax != null && sub.getSubmissionDate().isAfter(submissionMax)) return false;
        return statusFilter.equals("ALL") || sub.getStatus().equals(statusFilter);
    }

    public List<BookSubmission> getBookSubmissionScreenData(String titleFilter,
                                                            String authorUsernameFilter,
                                                            String genreFilter,
                                                            LocalDateTime submissionMin,
                                                            LocalDateTime submissionMax,
                                                            String statusFilter) {
        Pattern titleP = Pattern.compile("[\\s\\S]*" + titleFilter + "[\\s\\S]*", Pattern.CASE_INSENSITIVE);
        Pattern authorUsernameP = Pattern.compile("[\\s\\S]*" + authorUsernameFilter + "[\\s\\S]*", Pattern.CASE_INSENSITIVE);
        Pattern genreP = Pattern.compile("[\\s\\S]*" + genreFilter + "[\\s\\S]*", Pattern.CASE_INSENSITIVE);
        return bookSubmissionRepository.findAll()
                .stream()
                .filter(s -> filterBookSubmission(s, titleP, authorUsernameP, genreP, submissionMin, submissionMax, statusFilter))
                .collect(Collectors.toList());
    }
    public List<BookSubmission> getBookSubmissionScreenData() {
        return bookSubmissionRepository.findAll()
                .stream()
                .filter(BookSubmission::isPending)
                .collect(Collectors.toList());
    }

    public List<UserAccount> getUsersScreenData(String type) {
        switch (type) {
            case "Student/Staff":
                return new ArrayList<UserAccount>(studentStaffRepository.getAllUsers());
            case "Author":
                return new ArrayList<UserAccount>(authorRepository.getAllUsers());
            case "Librarian":
                return new ArrayList<UserAccount>(librarianRepository.getAllUsers());
            case "All":
                ArrayList<UserAccount> l = new ArrayList<>(studentStaffRepository.getAllUsers());
                l.addAll(authorRepository.getAllUsers());
                l.addAll(librarianRepository.getAllUsers());
                return l;
        }
        return new ArrayList<>();
    }

    private static boolean filterBorrowedBook(Book book,
                                              Pattern titleFilter,
                                              Pattern authorUsernameFilter,
                                              ChronoLocalDate publishedMin,
                                              ChronoLocalDate publishedMax,
                                              Pattern summaryFilter,
                                              Pattern borrowedByFilter) {
        if (book.isAvailable()) return false;
        if (!titleFilter.matcher(book.getTitle()).matches()) return false;
        if (!authorUsernameFilter.matcher(book.getAuthor()).matches()) return false;
        if (publishedMin != null && book.getPublishDate().isBefore(publishedMin)) return false;
        if (publishedMax != null && book.getPublishDate().isAfter(publishedMax)) return false;
        if (!summaryFilter.matcher(book.getSummary()).matches()) return false;
        return borrowedByFilter.matcher(book.getBorrowedByUsername()).matches();
    }

    public List<Book> getBorrowedBooksScreenData(String titleFilter,
                                                 String authorUsernameFilter,
                                                 LocalDate publishedMin,
                                                 LocalDate publishedMax,
                                                 String summaryFilter,
                                                 String borrowedByFilter) {
        Pattern titleP = Pattern.compile("[\\s\\S]*" + titleFilter + "[\\s\\S]*", Pattern.CASE_INSENSITIVE);
        Pattern authorUsernameP = Pattern.compile("[\\s\\S]*" + authorUsernameFilter + "[\\s\\S]*", Pattern.CASE_INSENSITIVE);
        Pattern summaryP = Pattern.compile("[\\s\\S]*" + summaryFilter + "[\\s\\S]*", Pattern.CASE_INSENSITIVE);
        Pattern borrowedByP = Pattern.compile("[\\s\\S]*" + borrowedByFilter + "[\\s\\S]*", Pattern.CASE_INSENSITIVE);
        return bookRepository.findAll()
                .stream()
                .filter(s -> filterBorrowedBook(s, titleP, authorUsernameP, publishedMin, publishedMax, summaryP, borrowedByP))
                .collect(Collectors.toList());
    }
    public List<BorrowedBookRecordView> getBorrowedBookRecords(
            String titleFilter,
            String borrowerFilter,
            String statusFilter
    ) {
        String title = safeTrim(titleFilter).toLowerCase();
        String borrower = safeTrim(borrowerFilter).toLowerCase();
        String status = safeTrim(statusFilter).toUpperCase();
        List<BorrowedBookRecordView> records = new ArrayList<>();
        Path path = Paths.get(BORROW_RECORDS_FILE);
        if (!Files.exists(path)) {
            return records;
        }
        try {
            for (String line : Files.readAllLines(path)) {
                if (line == null || line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length < 7) continue;
                String username = decode(parts[0]);
                String bookId = decode(parts[1]);
                String bookTitle = decode(parts[2]);
                int offset = parts.length >= 9 ? 2 : 0;
                LocalDate borrowDate = LocalDate.parse(parts[3 + offset]);
                LocalDate dueDate = LocalDate.parse(parts[4 + offset]);
                LocalDate returnDate = parts[5 + offset].isBlank() ? null : LocalDate.parse(parts[5 + offset]);
                String rowStatus = returnDate == null ? (dueDate.isBefore(LocalDate.now()) ? "OVERDUE" : "BORROWED") : "RETURNED";
                if (!title.isEmpty() && !bookTitle.toLowerCase().contains(title)) continue;
                if (!borrower.isEmpty() && !username.toLowerCase().contains(borrower)) continue;
                if (!status.isEmpty() && !"ALL".equals(status) && !rowStatus.equals(status)) continue;
                records.add(new BorrowedBookRecordView(bookId, bookTitle, username, borrowDate, returnDate, rowStatus, dueDate.isBefore(LocalDate.now()) && returnDate == null));
            }
        } catch (Exception ignored) {
            return List.of();
        }
        records.sort(Comparator.comparing(BorrowedBookRecordView::borrowDate).reversed());
        return records;
    }

    public List<BookRequestView> getBookRequests(String statusFilter, String keywordFilter) {
        String status = safeTrim(statusFilter).toUpperCase();
        String keyword = safeTrim(keywordFilter).toLowerCase();
        return loadBookRequests().stream()
                .filter(r -> status.isEmpty() || "ALL".equals(status) || status.equalsIgnoreCase(r.status()))
                .filter(r -> keyword.isEmpty()
                        || r.requestId().toLowerCase().contains(keyword)
                        || r.username().toLowerCase().contains(keyword)
                        || r.title().toLowerCase().contains(keyword)
                        || r.author().toLowerCase().contains(keyword)
                        || r.genre().toLowerCase().contains(keyword))
                .sorted(Comparator.comparing(BookRequestView::createdAt).reversed())
                .collect(Collectors.toList());
    }

    public OperationResult approveBookRequest(String requestId, String librarianUsername, String comment) {
        String normalizedRequestId = safeTrim(requestId);
        String normalizedLibrarian = safeTrim(librarianUsername);
        if (normalizedRequestId.isEmpty() || normalizedLibrarian.isEmpty()) {
            return OperationResult.failure("Approve failed: invalid request or librarian.");
        }
        List<BookRequestView> rows = loadBookRequests();
        for (int i = 0; i < rows.size(); i++) {
            BookRequestView request = rows.get(i);
            if (!normalizedRequestId.equalsIgnoreCase(request.requestId())) {
                continue;
            }
            if (!"PENDING".equalsIgnoreCase(request.status())) {
                return OperationResult.failure("Approve failed: request is already " + request.status() + ".");
            }
            String summary = "Added from student/staff request. Reason: " + request.reason();
            bookRepository.addApprovedBook(
                    request.title(),
                    request.author(),
                    LocalDate.now(),
                    summary,
                    request.genre(),
                    "",
                    ""
            );
            BookRequestView decided = new BookRequestView(
                    request.requestId(),
                    request.username(),
                    request.title(),
                    request.author(),
                    request.genre(),
                    request.reason(),
                    "APPROVED",
                    safeTrim(comment).isEmpty() ? "Approved by " + normalizedLibrarian : comment.trim(),
                    request.createdAt(),
                    LocalDateTime.now()
            );
            rows.set(i, decided);
            saveBookRequests(rows);
            appendNotificationTo(
                    request.username(),
                    "BOOK_REQUEST_APPROVED",
                    "Your request \"" + request.title() + "\" was approved and uploaded to the library.",
                    UserRole.STUDENT
            );
            return OperationResult.success("Book request approved and uploaded: " + request.title());
        }
        return OperationResult.failure("Approve failed: request not found.");
    }

    public OperationResult rejectBookRequest(String requestId, String librarianUsername, String comment) {
        String normalizedRequestId = safeTrim(requestId);
        String normalizedLibrarian = safeTrim(librarianUsername);
        if (normalizedRequestId.isEmpty() || normalizedLibrarian.isEmpty()) {
            return OperationResult.failure("Reject failed: invalid request or librarian.");
        }
        List<BookRequestView> rows = loadBookRequests();
        for (int i = 0; i < rows.size(); i++) {
            BookRequestView request = rows.get(i);
            if (!normalizedRequestId.equalsIgnoreCase(request.requestId())) {
                continue;
            }
            if (!"PENDING".equalsIgnoreCase(request.status())) {
                return OperationResult.failure("Reject failed: request is already " + request.status() + ".");
            }
            BookRequestView decided = new BookRequestView(
                    request.requestId(),
                    request.username(),
                    request.title(),
                    request.author(),
                    request.genre(),
                    request.reason(),
                    "REJECTED",
                    safeTrim(comment).isEmpty() ? "Rejected by " + normalizedLibrarian : comment.trim(),
                    request.createdAt(),
                    LocalDateTime.now()
            );
            rows.set(i, decided);
            saveBookRequests(rows);
            appendNotificationTo(
                    request.username(),
                    "BOOK_REQUEST_REJECTED",
                    "Your request \"" + request.title() + "\" was rejected. "
                            + (safeTrim(comment).isEmpty() ? "" : ("Reason: " + comment.trim())),
                    UserRole.STUDENT
            );
            return OperationResult.success("Book request rejected: " + request.title());
        }
        return OperationResult.failure("Reject failed: request not found.");
    }
    public OperationResult exportBorrowedBooksData(File file,
                                                   String titleFilter,
                                                   String borrowedByFilter,
                                                   String statusFilter) {
        List<BorrowedBookRecordView> data = getBorrowedBookRecords(
                titleFilter,
                borrowedByFilter,
                statusFilter
        );

        try (HSSFWorkbook book = new HSSFWorkbook(); FileOutputStream fos = new FileOutputStream(file)) {
            HSSFSheet sheet = book.createSheet("Borrowed Books");

            HSSFRow headerRow = sheet.createRow(0);
            String[] headers = new String[]{ "Id", "Title", "Borrowed By", "Borrowed Date", "Return Date", "Status", "Overdue" };
            for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);

            int j = 1;
            for (BorrowedBookRecordView b : data) {
                HSSFRow thisRow = sheet.createRow(j);
                thisRow.createCell(0).setCellValue(b.bookId());//ID
                thisRow.createCell(1).setCellValue(b.bookTitle());//Title
                thisRow.createCell(2).setCellValue(b.borrowerUsername());//Borrowed By
                thisRow.createCell(3).setCellValue(b.borrowDate() == null ? "" : b.borrowDate().toString());//Borrowed Date
                thisRow.createCell(4).setCellValue(b.returnDate == null ? "" : b.returnDate().toString());//Due Date
                thisRow.createCell(5).setCellValue(b.status());//Status
                thisRow.createCell(6).setCellValue(b.overdue());//Overdue
                j++;
            }

            book.write(fos);
        } catch (Exception e) {
            return OperationResult.failure("Export failed: directory not found or resource issue.");
        }

        return OperationResult.success("Successfully stored filtered borrowed books data to \"" + file.getName() + "\".");
    }

    public OperationResult exportBorrowedRecordsCsv(Path outputPath, List<BorrowedBookRecordView> rows) {
        if (outputPath == null) {
            return OperationResult.failure("Export failed: invalid output path.");
        }
        try {
            List<String> lines = new ArrayList<>();
            lines.add("Book ID,Book Title,Borrower Username,Borrow Date,Return Date,Status,Overdue");
            for (BorrowedBookRecordView row : rows) {
                lines.add(csv(row.bookId()) + ","
                        + csv(row.bookTitle()) + ","
                        + csv(row.borrowerUsername()) + ","
                        + csv(row.borrowDate().toString()) + ","
                        + csv(row.returnDate() == null ? "" : row.returnDate().toString()) + ","
                        + csv(row.status()) + ","
                        + csv(row.overdue() ? "Yes" : "No"));
            }
            Files.write(outputPath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return OperationResult.success("Export successful: " + outputPath);
        } catch (Exception e) {
            return OperationResult.failure("Export failed: " + e.getMessage());
        }
    }

    public List<Book> getPublishedBooksScreenData() {
        return bookRepository.findAll();
    }


    public OperationResult modifyBook(String bookId,
                                      String newTitle,
                                      String newAuthor,
                                      String newGenre,
                                      String newDescription,
                                      String newFilePath,
                                      String newCoverPath) {
        if (bookId.isEmpty()) return OperationResult.failure("Modification failed: Id should not be empty.");
        if (bookRepository.findById(bookId).isEmpty()) return OperationResult.failure("Modification failed: Invalid book Id.");

        boolean success = bookRepository.modifyBook(bookId, newTitle, newAuthor, newGenre, newDescription, newFilePath, newCoverPath);
        return success ? OperationResult.success("Modification successful: Modified book with Id \"" + bookId + "\".") :
                OperationResult.failure("Modification failed: Invalid parameters.");
    }
    public OperationResult createBook(String title,
                                      String author,
                                      String genre,
                                      String description,
                                      String filePath,
                                      String coverPath) {
        bookRepository.addApprovedBook(title, author, LocalDate.now(), description, genre, filePath, coverPath);
        return OperationResult.success("Creation successful: Created the book + \"" + title + "\".");
    }


    private static String csv(String value) {
        String v = value == null ? "" : value;
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }


    public OperationResult validateBookSubmissionId(String subId) {
        if (bookSubmissionRepository.findById(subId).isEmpty()) return OperationResult.failure("Invalid book submission Id: " + subId);
        else return OperationResult.success("");
    }

    public OperationResult approveBookSubmission(String subId, LibrarianAccount user) {
        Optional<BookSubmission> sub = bookSubmissionRepository.findById(subId);
        if (sub.isEmpty()) return OperationResult.failure("Approve failed: Invalid submission ID.");
        if (user == null) return OperationResult.failure("Approve failed: No user logged in.");

        BookSubmission s = sub.get();
        if (!s.isPending()) return OperationResult.failure("Approve failed: \"" + s.getTitle() + "\" is not a pending book submission.");

        s.approve(user.getUsername());
        bookRepository.addApprovedBook(
                s.getTitle(),
                s.getAuthorFullName(),
                LocalDate.now(),
                s.getDescription(),
                s.getGenre(),
                s.getFilePath(),
                s.getCoverImagePath()
        );//Note that description is just an alias of summary for book
        bookSubmissionRepository.update(s);//Changes should be saved once there are updates

        appendNotificationTo(s.getAuthorUsername(), "BOOK_APPROVED", "Your Book Submission \"" + s.getTitle() + "\" was approved.", UserRole.AUTHOR);//Send notification to the author
        return OperationResult.success("Approve successful: \"" + s.getTitle() + "\" is approved and created.");
    }
    public OperationResult approveBookSubmission(String subId, String Username) {
        Optional<LibrarianAccount> l = librarianRepository.findByUsername(Username);
        if (l.isEmpty()) return OperationResult.failure("Error: Current user does not exist.");
        return approveBookSubmission(subId, l.get());
    }

    public OperationResult rejectBookSubmission(String subId, LibrarianAccount user, String reason) {
        Optional<BookSubmission> sub = bookSubmissionRepository.findById(subId);
        if (sub.isEmpty()) return OperationResult.failure("Rejection failed: Invalid submission ID.");
        if (user == null) return OperationResult.failure("Rejection failed: No user logged in.");

        BookSubmission s = sub.get();
        if (!s.isPending()) return OperationResult.failure("Rejection failed: \"" + s.getTitle() + "\" is not a pending book submission.");

        s.reject(user.getUsername(), reason);
        bookSubmissionRepository.update(s);

        appendNotificationTo(s.getAuthorUsername(), "BOOK_REJECTED", "Your Book Submission \"" + s.getTitle() + "\" was rejected.\nRejection reason:" + reason, UserRole.AUTHOR);//Send notification to the author
        return OperationResult.success("Rejection successful: \"" + s.getTitle() + "\" is rejected.");
    }
    public OperationResult rejectBookSubmission(String subId, String Username, String reason) {
        Optional<LibrarianAccount> l = librarianRepository.findByUsername(Username);
        if (l.isEmpty()) return OperationResult.failure("Error: Current user does not exist.");
        return rejectBookSubmission(subId, l.get(), reason);
    }

    public OperationResult previewBookSubmission(String subId) {
        Optional<BookSubmission> sub = bookSubmissionRepository.findById(subId);
        if (sub.isEmpty()) return OperationResult.failure("Preview failed: Invalid submission ID.");
        String path = sub.get().getFilePath();
        if (path.isEmpty()) return OperationResult.failure("Preview failed: No file was included for this submission. submission ID: " + subId);

        File pdfFile = new File(path);

        if (!pdfFile.exists()) return OperationResult.failure("Preview failed: File path does not exist.");
        if (!Desktop.isDesktopSupported()) return OperationResult.failure("Preview failed: File cannot be opened.");
        try {
            Desktop.getDesktop().open(pdfFile);
        } catch (IOException e) {
            return OperationResult.failure("Preview failed: cannot open target file. Error message: " + e.getMessage());
        }

        return OperationResult.success("Successfully opened file: " + path);
    }

    public OperationResult updateProfile(String username, String newFullName, String oldPassword, String newPassword, String confirmNewPassword, String newEmployeeID) {
        String normalizedUsername = safeTrim(username);
        String normalizedFullName = safeTrim(newFullName);
        String pwd = newPassword == null ? "" : newPassword;
        String confirm = confirmNewPassword == null ? "" : confirmNewPassword;
        String normalizedEmployeeID = safeTrim(newEmployeeID);

        if (normalizedUsername.isEmpty()) return OperationResult.failure("Profile update failed: invalid user.");
        Optional<LibrarianAccount> existingOpt = librarianRepository.findByUsername(normalizedUsername);
        if (existingOpt.isEmpty()) return OperationResult.failure("Profile update failed: account not found.");
        LibrarianAccount existing = existingOpt.get();

        if (!PasswordSecurity.verifyPassword(
                oldPassword,
                existing.getPasswordSaltBase64(),
                existing.getPasswordHashBase64()
        )) return OperationResult.failure("Profile update failed: the old password is wrong.");


        if (normalizedFullName.isEmpty()) return OperationResult.failure("Profile update failed: full name is required.");

        String salt = existing.getPasswordSaltBase64();
        String hash = existing.getPasswordHashBase64();
        boolean passwordUpdated = !pwd.isBlank() || !confirm.isBlank();
        if (passwordUpdated) {
            if (!pwd.equals(confirm)) return OperationResult.failure("Profile update failed: passwords do not match.");
            if (!isStrongPassword(pwd)) return OperationResult.failure("Profile update failed: weak password.");
            salt = project.task1.security.PasswordSecurity.generateSaltBase64();
            hash = project.task1.security.PasswordSecurity.hashPasswordBase64(pwd, salt);
        }

        int eID = existing.getEmployeeID();
        if (!normalizedEmployeeID.isBlank()) {
            try {
                eID = Integer.parseInt(normalizedEmployeeID);
            } catch (Exception e) {
                return OperationResult.failure("Profile update failed: employee ID must be an integer");
            }
        }

        librarianRepository.save(new LibrarianAccount(
                existing.getUsername(),
                normalizedFullName,
                salt,
                hash,
                existing.isDisabled(),
                eID
        ));
        appendNotification(normalizedUsername, "ANNOUNCEMENT", "Your profile was updated successfully.");
        return OperationResult.success(passwordUpdated ? "Profile updated successfully with password changed." : "Profile updated successfully.");
    }
    /*
    public ProfileUpdateResult updateProfile(String username, String currentPassword, String newFullName, String newPassword, String confirmNewPassword, String newEmployeeID) {
        String normalizedUsername = safeTrim(username);
        String normalizedCurrentPassword = currentPassword == null ? "" : currentPassword;
        String normalizedFullName = safeTrim(newFullName);
        String pwd = newPassword == null ? "" : newPassword;
        String confirm = confirmNewPassword == null ? "" : confirmNewPassword;
        String normalizedEmployeeID = safeTrim(newEmployeeID);

        if (normalizedUsername.isEmpty()) return ProfileUpdateResult.failure("Profile update failed: invalid user.", false);
        Optional<LibrarianAccount> existingOpt = librarianRepository.findByUsername(normalizedUsername);
        if (existingOpt.isEmpty()) return ProfileUpdateResult.failure("Profile update failed: account not found.", false);
        LibrarianAccount existing = existingOpt.get();
        if (normalizedFullName.isEmpty()) return ProfileUpdateResult.failure("Profile update failed: full name is required.", false);
        if (normalizedCurrentPassword.isBlank()) return ProfileUpdateResult.failure("Profile update failed: current password is required.", false);
        boolean currentPwdOk = project.task1.security.PasswordSecurity.verifyPassword(
                normalizedCurrentPassword,
                existing.getPasswordSaltBase64(),
                existing.getPasswordHashBase64()
        );
        if (!currentPwdOk) return ProfileUpdateResult.failure("Profile update failed: current password is incorrect.", false);

        String salt = existing.getPasswordSaltBase64();
        String hash = existing.getPasswordHashBase64();
        boolean passwordChanged = false;
        if (!pwd.isBlank() || !confirm.isBlank()) {
            if (!pwd.equals(confirm)) return ProfileUpdateResult.failure("Profile update failed: passwords do not match.", false);
            if (!isStrongPassword(pwd)) return ProfileUpdateResult.failure("Profile update failed: weak password.", false);
            salt = project.task1.security.PasswordSecurity.generateSaltBase64();
            hash = project.task1.security.PasswordSecurity.hashPasswordBase64(pwd, salt);
            passwordChanged = true;
        }

        int eID = existing.getEmployeeID();
        if (!normalizedEmployeeID.isBlank()) {
            try {
                eID = Integer.parseInt(normalizedEmployeeID);
            } catch (Exception e) {
                return ProfileUpdateResult.failure("Profile update failed: employee ID must be an integer", false);
            }
        }

        librarianRepository.save(new LibrarianAccount(
                existing.getUsername(),
                normalizedFullName,
                salt,
                hash,
                existing.isDisabled(),
                eID
        ));
        appendNotification(normalizedUsername, "ANNOUNCEMENT", "Your profile was updated successfully.");
        return ProfileUpdateResult.success("Profile updated successfully.", passwordChanged);
    }
    */

    public OperationResult editUserAccount(String username, String newFullName, String newPassword, String confirmNewPassword) {
        String normalizedUsername = safeTrim(username);
        String normalizedFullName = safeTrim(newFullName);
        String pwd = newPassword == null ? "" : newPassword;
        String confirm = confirmNewPassword == null ? "" : confirmNewPassword;

        if (normalizedUsername.isEmpty()) return OperationResult.failure("Edit failed: invalid username.");

        UserAccount user;
        Optional<StudentStaffAccount> userStudentStaff = Optional.empty();
        Optional<AuthorAccount> userAuthor = Optional.empty();
        Optional<LibrarianAccount> userLibrarian = Optional.empty();
        UserRole role;
        do
        {
            userStudentStaff = studentStaffRepository.findByUsername(normalizedUsername);
            if (userStudentStaff.isPresent()) {
                role = UserRole.STUDENT;//Use STUDENT to represent both student and staff here
                user = userStudentStaff.get();
                break;
            }

            userAuthor = authorRepository.findByUsername(normalizedUsername);
            if (userAuthor.isPresent()) {
                role = UserRole.AUTHOR;
                user = userAuthor.get();
                break;
            }

            userLibrarian = librarianRepository.findByUsername(normalizedUsername);
            if (userLibrarian.isPresent()) {
                role = UserRole.LIBRARIAN;
                user = userLibrarian.get();
                break;
            }

            return OperationResult.failure("Edit failed: account not found.");
        } while (false);

        if (normalizedFullName.isEmpty()) return OperationResult.failure("Edit failed: full name is required.");

        String salt = user.getPasswordSaltBase64();
        String hash = user.getPasswordHashBase64();
        if (!pwd.isBlank() || !confirm.isBlank()) {
            if (!pwd.equals(confirm)) return OperationResult.failure("Edit failed: passwords do not match.");
            if (!isStrongPassword(pwd)) return OperationResult.failure("Edit failed: weak password.");
            salt = project.task1.security.PasswordSecurity.generateSaltBase64();
            hash = project.task1.security.PasswordSecurity.hashPasswordBase64(pwd, salt);
        }

        switch (role) {
            case STUDENT:
                studentStaffRepository.save(new StudentStaffAccount(
                        user.getUsername(),
                        normalizedFullName,
                        salt,
                        hash,
                        userStudentStaff.get().getRole(),
                        user.isDisabled()//Preserve the disabled state
                ));
                break;
            case AUTHOR:
                authorRepository.save(new AuthorAccount(
                        user.getUsername(),
                        normalizedFullName,
                        salt,
                        hash,
                        user.isDisabled(),
                        userAuthor.get().getBio()
                ));
                break;
            case LIBRARIAN:
                librarianRepository.save(new LibrarianAccount(
                        user.getUsername(),
                        normalizedFullName,
                        salt,
                        hash,
                        user.isDisabled(),
                        userLibrarian.get().getEmployeeID()
                ));
                break;
        }
        appendNotificationTo(normalizedUsername, "USER_ACCOUNT_UPDATE", "Your profile was edited.", role);

        return OperationResult.success("Successfully edited target user account.");
    }
    public String getUserEditConfirmDetail(String username, String newFullName) {
        String normalizedUsername = safeTrim(username);

        UserAccount user;
        do
        {
            Optional<StudentStaffAccount> userStudentStaff = studentStaffRepository.findByUsername(normalizedUsername);
            if (userStudentStaff.isPresent()) {
                user = userStudentStaff.get();
                break;
            }

            Optional<AuthorAccount> userAuthor = authorRepository.findByUsername(normalizedUsername);
            if (userAuthor.isPresent()) {
                user = userAuthor.get();
                break;
            }

            Optional<LibrarianAccount> userLibrarian = librarianRepository.findByUsername(normalizedUsername);
            if (userLibrarian.isPresent()) {
                user = userLibrarian.get();
                break;
            }

            return "";
        } while (false);

        return "Username: " + user.getUsername() + "\nFull Name: "+ user.getFullName() + " -> " + newFullName + "\nRole: " + user.getRole();
    }
    public String getUserDisableConfirmDetail(String username) {
        String normalizedUsername = safeTrim(username);

        UserAccount user;
        do
        {
            Optional<StudentStaffAccount> userStudentStaff = studentStaffRepository.findByUsername(normalizedUsername);
            if (userStudentStaff.isPresent()) {
                user = userStudentStaff.get();
                break;
            }

            Optional<AuthorAccount> userAuthor = authorRepository.findByUsername(normalizedUsername);
            if (userAuthor.isPresent()) {
                user = userAuthor.get();
                break;
            }

            Optional<LibrarianAccount> userLibrarian = librarianRepository.findByUsername(normalizedUsername);
            if (userLibrarian.isPresent()) {
                user = userLibrarian.get();
                break;
            }

            return "";
        } while (false);

        return "Username: " + user.getUsername() + "\nFull Name: "+ user.getFullName() + "\nRole: " + user.getRole();
    }
    public OperationResult validateUsername(String username) {//Validate username used for editing
        String normalizedUsername = safeTrim(username);
        do
        {
            if (studentStaffRepository.findByUsername(normalizedUsername).isPresent()) break;
            if (authorRepository.findByUsername(normalizedUsername).isPresent()) break;
            if (librarianRepository.findByUsername(normalizedUsername).isPresent()) break;

            return OperationResult.failure("User does not exist.");
        } while (false);
        return OperationResult.success("");
    }

    public OperationResult validateDisabledUsername(String username) {//Validate username, then check whether it is not disabled
        String normalizedUsername = safeTrim(username);

        UserAccount user;
        Optional<StudentStaffAccount> userStudentStaff;
        Optional<AuthorAccount> userAuthor;
        Optional<LibrarianAccount> userLibrarian;
        do
        {
            userStudentStaff = studentStaffRepository.findByUsername(normalizedUsername);
            if (userStudentStaff.isPresent()) {
                user = userStudentStaff.get();
                break;
            }

            userAuthor = authorRepository.findByUsername(normalizedUsername);
            if (userAuthor.isPresent()) {
                user = userAuthor.get();
                break;
            }

            userLibrarian = librarianRepository.findByUsername(normalizedUsername);
            if (userLibrarian.isPresent()) {
                user = userLibrarian.get();
                break;
            }

            return OperationResult.failure("User does not exist.");
        } while (false);

        if (user.isDisabled()) return OperationResult.failure("The user account is already disabled.");

        return OperationResult.success("");
    }
    public OperationResult disableUser(String username) {
        String normalizedUsername = safeTrim(username);

        if (normalizedUsername.isEmpty()) return OperationResult.failure("Disable failed: invalid username.");

        UserAccount user;
        Optional<StudentStaffAccount> userStudentStaff = Optional.empty();
        Optional<AuthorAccount> userAuthor = Optional.empty();
        Optional<LibrarianAccount> userLibrarian = Optional.empty();
        UserRole role;
        do
        {
            userStudentStaff = studentStaffRepository.findByUsername(normalizedUsername);
            if (userStudentStaff.isPresent()) {
                role = UserRole.STUDENT;//Use STUDENT to represent both student and staff here
                user = userStudentStaff.get();
                break;
            }

            userAuthor = authorRepository.findByUsername(normalizedUsername);
            if (userAuthor.isPresent()) {
                role = UserRole.AUTHOR;
                user = userAuthor.get();
                break;
            }

            userLibrarian = librarianRepository.findByUsername(normalizedUsername);
            if (userLibrarian.isPresent()) {
                role = UserRole.LIBRARIAN;
                user = userLibrarian.get();
                break;
            }

            return OperationResult.failure("Edit failed: account not found.");
        } while (false);

        user.setDisabled(true);

        switch (role) {
            case STUDENT:
                studentStaffRepository.save(userStudentStaff.get());
                break;
            case AUTHOR:
                authorRepository.save(userAuthor.get());
                break;
            case LIBRARIAN:
                librarianRepository.save(userLibrarian.get());
                break;
        }
        appendNotificationTo(normalizedUsername, "USER_ACCOUNT_UPDATE", "Your account was disabled.", role);

        return OperationResult.success("Successfully disabled target user account.");
    }
    public OperationResult validateActivatedUsername(String username) {//Validate username, then check whether it is disabled
        String normalizedUsername = safeTrim(username);

        UserAccount user;
        Optional<StudentStaffAccount> userStudentStaff;
        Optional<AuthorAccount> userAuthor;
        Optional<LibrarianAccount> userLibrarian;
        do
        {
            userStudentStaff = studentStaffRepository.findByUsername(normalizedUsername);
            if (userStudentStaff.isPresent()) {
                user = userStudentStaff.get();
                break;
            }

            userAuthor = authorRepository.findByUsername(normalizedUsername);
            if (userAuthor.isPresent()) {
                user = userAuthor.get();
                break;
            }

            userLibrarian = librarianRepository.findByUsername(normalizedUsername);
            if (userLibrarian.isPresent()) {
                user = userLibrarian.get();
                break;
            }

            return OperationResult.failure("User does not exist.");
        } while (false);

        if (!user.isDisabled()) return OperationResult.failure("The user account is not disabled.");

        return OperationResult.success("");
    }
    public OperationResult activateUser(String username) {
        String normalizedUsername = safeTrim(username);

        if (normalizedUsername.isEmpty()) return OperationResult.failure("Activation failed: invalid username.");

        UserAccount user;
        Optional<StudentStaffAccount> userStudentStaff = Optional.empty();
        Optional<AuthorAccount> userAuthor = Optional.empty();
        Optional<LibrarianAccount> userLibrarian = Optional.empty();
        UserRole role;
        do
        {
            userStudentStaff = studentStaffRepository.findByUsername(normalizedUsername);
            if (userStudentStaff.isPresent()) {
                role = UserRole.STUDENT;//Use STUDENT to represent both student and staff here
                user = userStudentStaff.get();
                break;
            }

            userAuthor = authorRepository.findByUsername(normalizedUsername);
            if (userAuthor.isPresent()) {
                role = UserRole.AUTHOR;
                user = userAuthor.get();
                break;
            }

            userLibrarian = librarianRepository.findByUsername(normalizedUsername);
            if (userLibrarian.isPresent()) {
                role = UserRole.LIBRARIAN;
                user = userLibrarian.get();
                break;
            }

            return OperationResult.failure("Edit failed: account not found.");
        } while (false);

        user.setDisabled(false);

        switch (role) {
            case STUDENT:
                studentStaffRepository.save(userStudentStaff.get());
                break;
            case AUTHOR:
                authorRepository.save(userAuthor.get());
                break;
            case LIBRARIAN:
                librarianRepository.save(userLibrarian.get());
                break;
        }
        appendNotificationTo(normalizedUsername, "USER_ACCOUNT_UPDATE", "Your account was activated.", role);

        return OperationResult.success("Successfully activated target user account.");
    }

    public OperationResult createUser(String type, String username, String fullName, String password, String passwordConfirm, String additional) {//Use the parameter additional to contain Bio for authors or employee id for librarians
        SharedAuthFacade.AuthResult authResult = sharedAuthFacade.register(
                username,
                fullName,
                password,
                passwordConfirm,
                type,
                additional,
                additional
        );
        if (!authResult.success()) return OperationResult.failure(authResult.message());
        return OperationResult.success(authResult.message());
    }

    public String getConfirmDetail(String subId) {
        BookSubmission sub = bookSubmissionRepository.findById(subId).get();
        return "Title: " + sub.getTitle() + "\nAuthor Username: "+ sub.getAuthorUsername() + "\nDescription: " + sub.getDescription() + "\nSubmission Time: " + sub.getSubmissionDate() + "\n";
    }

    private List<BookRequestView> loadBookRequests() {
        Path path = Paths.get(TASK1_BOOK_REQUESTS_FILE);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try {
            List<BookRequestView> list = new ArrayList<>();
            for (String line : Files.readAllLines(path)) {
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                if (parts.length < 10) {
                    continue;
                }
                list.add(new BookRequestView(
                        decode(parts[0]),
                        decode(parts[1]),
                        decode(parts[2]),
                        decode(parts[3]),
                        decode(parts[4]),
                        decode(parts[5]),
                        decode(parts[6]),
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

    private void saveBookRequests(List<BookRequestView> rows) {
        try {
            Files.createDirectories(Paths.get("data/task1"));
            List<String> lines = new ArrayList<>();
            for (BookRequestView r : rows) {
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
                        r.decidedAt() == null ? "" : r.decidedAt().format(HISTORY_TIME_FORMAT)
                ));
            }
            Files.write(Paths.get(TASK1_BOOK_REQUESTS_FILE), lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ignored) {
        }
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

    public record LoginResult(boolean success, String message, LibrarianAccount user) {
        public static LoginResult success(String message, LibrarianAccount user) {
            return new LoginResult(true, message, user);
        }

        public static LoginResult failure(String message) {
            return new LoginResult(false, message, null);
        }
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        return password.matches(".*[A-Za-z].*")
                && password.matches(".*\\d.*")
                && password.matches(".*[A-Z].*");
    }


    private static boolean filterNotification(NotificationView notification,
                                              String categoryFilter,
                                              LocalDateTime timeMin,
                                              LocalDateTime timeMax,
                                              String urgencyFilter) {
        if (!(categoryFilter.equals("ALL") || categoryFilter.equals(notification.category()))) return false;
        if (timeMin != null && notification.timestamp().isBefore(timeMin)) return false;
        if (timeMax != null && notification.timestamp().isAfter(timeMax)) return false;
        if ("URGENT".equalsIgnoreCase(urgencyFilter) && !notification.isUrgent()) return false;
        if ("NORMAL".equalsIgnoreCase(urgencyFilter) && notification.isUrgent()) return false;
        return true;
    }

    public List<NotificationView> getNotificationBoard(String username) {
        return getNotificationBoard(username, "ALL", null, null, "ALL");
    }
    public List<NotificationView> getNotificationBoard(
            String username,
            String categoryFilter,
            LocalDateTime dateMin,
            LocalDateTime dateMax,
            String urgencyFilter) {
        String normalized = safeTrim(username);
        if (normalized.isEmpty()) return List.of();

        List<NotificationView> notifications = loadStoredNotifications(normalized);
        notifications.sort(Comparator.comparing(NotificationView::timestamp).reversed());
        notifications.sort((n1, n2) -> Boolean.compare(n2.isUrgent(), n1.isUrgent()));
        return notifications
                .stream()
                .filter(n -> filterNotification(n, categoryFilter, dateMin, dateMax, urgencyFilter))
                .collect(Collectors.toList());
    }

    public List<NotificationView> getUnreadNotifications(String username, int limit) {
        String normalized = safeTrim(username);
        if (normalized.isEmpty()) {
            return List.of();
        }
        return loadStoredNotifications(normalized).stream()
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

    public void markNotificationAsRead(String username, String notificationId) {
        String normalized = safeTrim(username);
        String normalizedId = safeTrim(notificationId);
        if (normalized.isEmpty() || normalizedId.isEmpty()) {
            return;
        }
        boolean owned = notificationStore.findByScopeAndUser(TASK3_NOTIFICATION_SCOPE, normalized).stream()
                .anyMatch(n -> n.id().equals(normalizedId));
        if (owned) {
            notificationStore.markRead(TASK3_NOTIFICATION_SCOPE, normalizedId);
        }
    }

    public void deleteNotification(String username, String notificationId) {
        String normalized = safeTrim(username);
        String normalizedId = safeTrim(notificationId);
        if (normalized.isEmpty() || normalizedId.isEmpty()) {
            return;
        }
        boolean owned = notificationStore.findByScopeAndUser(TASK3_NOTIFICATION_SCOPE, normalized).stream()
                .anyMatch(n -> n.id().equals(normalizedId));
        if (owned) {
            notificationStore.deleteById(TASK3_NOTIFICATION_SCOPE, normalizedId);
        }
    }

    public void deleteReadNotifications(String username) {
        String normalized = safeTrim(username);
        if (normalized.isEmpty()) {
            return;
        }
        notificationStore.deleteReadByUser(TASK3_NOTIFICATION_SCOPE, normalized);
    }

    private void appendNotification(String username, String category, String message) {
        notificationStore.create(
                TASK3_NOTIFICATION_SCOPE,
                username,
                category,
                message,
                category,
                category,
                isUrgentNotificationCategory(category),
                ""
        );
    }

    private void appendNotificationTo(String username, String category, String message, UserRole role) {
        String scope = switch (role) {
            case STUDENT, STAFF -> TASK1_NOTIFICATION_SCOPE;
            case AUTHOR -> TASK2_NOTIFICATION_SCOPE;
            case LIBRARIAN -> TASK3_NOTIFICATION_SCOPE;
        };
        notificationStore.create(
                scope,
                username,
                category,
                message,
                category,
                category,
                isUrgentNotificationCategory(category),
                ""
        );
    }

    private boolean isUrgentNotificationCategory(String category) {
        String c = safeTrim(category).toUpperCase();
        return "BOOK_REJECTED".equals(c)
                || "NEW_BOOK_SUBMISSION".equals(c)
                || "NEW_BOOK_REQUEST".equals(c)
                || "USER_ACCOUNT_UPDATE".equals(c)
                || "RESPONSE".equals(c);
    }

    private List<NotificationView> loadStoredNotifications(String username) {
        return notificationStore.findByScopeAndUser(TASK3_NOTIFICATION_SCOPE, username).stream()
                .map(n -> new NotificationView(
                        n.id(),
                        n.createdAt(),
                        n.category().isBlank() ? n.type() : n.category(),
                        n.message(),
                        n.read()
                ))
                .collect(Collectors.toList());
    }

    public record NotificationView(
            String notificationId,
            LocalDateTime timestamp,
            String category,
            String message,
            boolean read
    )
    {
        public boolean isUrgent() {
            String c = safeTrim(category).toUpperCase();
            return c.equals("NEW_BOOK_SUBMISSION")
                    || c.equals("USER_ACCOUNT_UPDATE")
                    || c.equals("BOOK_REJECTED")
                    || c.equals("RESPONSE")
                    || c.contains("URGENT");
        }
    }

    public SharedAuthFacade.UserPrincipal getLibrarianPrinciple(String username) {
        if (username == null) return null;
        LibrarianAccount user = librarianRepository.findByUsername(username).orElse(null);
        if (user == null) return null;
        return new SharedAuthFacade.UserPrincipal(username, user.getFullName(), user.getRole().name());
    }

    public record ProfileUpdateResult(boolean success, String message, boolean passwordChanged) {
        public static ProfileUpdateResult success(String message, boolean passwordChanged) {
            return new ProfileUpdateResult(true, message, passwordChanged);
        }

        public static ProfileUpdateResult failure(String message, boolean passwordChanged) {
            return new ProfileUpdateResult(false, message, passwordChanged);
        }
    }

    public record BorrowedBookRecordView(
            String bookId,
            String bookTitle,
            String borrowerUsername,
            LocalDate borrowDate,
            LocalDate returnDate,
            String status,
            boolean overdue
    )
    {
        public String getBookId() { return bookId; }
        public String getBookTitle() { return bookTitle; }
        public String getBorrowerUsername() { return borrowerUsername; }
        public LocalDate getBorrowDate() { return borrowDate; }
        public LocalDate getReturnDate() { return returnDate; }
        public String getStatusAlt() { return status; }
        public boolean getOverdue() { return overdue; }
    }

    public record BookRequestView(
            String requestId,
            String username,
            String title,
            String author,
            String genre,
            String reason,
            String status,
            String librarianComment,
            LocalDateTime createdAt,
            LocalDateTime decidedAt
    ) {
        public String getRequestId() { return requestId; }
        public String getUsername() { return username; }
        public String getTitle() { return title; }
        public String getAuthor() { return author; }
        public String getGenre() { return genre; }
        public String getReason() { return reason; }
        public String getStatus() { return status; }
    }
}
