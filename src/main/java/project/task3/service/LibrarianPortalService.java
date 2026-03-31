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
import project.task2.model.Notification;
import project.task2.repo.AuthorRepository;
import project.task2.repo.SubmissionRepository;
import project.task3.model.LibrarianAccount;
import project.task3.repo.LibrarianRepository;

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

    private static final String TASK1_NOTIFICATIONS_FILE = "data/task1/notifications.txt";
    private static final String TASK2_NOTIFICATIONS_FILE = "data/notifications.txt";
    private static final String TASK3_NOTIFICATIONS_FILE = "data/task3/notifications.txt";
    private static final String BORROW_RECORDS_FILE = "data/task1/borrow_records.txt";
    private static final DateTimeFormatter HISTORY_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public LibrarianPortalService(LibrarianRepository librarianRepository, StudentStaffRepository studentStaffRepository, AuthorRepository authorRepository, BookRepository bookRepository, SubmissionRepository bookSubmissionRepository) {
        this.librarianRepository = librarianRepository;
        this.studentStaffRepository = studentStaffRepository;
        this.authorRepository = authorRepository;
        this.sharedAuthFacade = new SharedAuthFacade(studentStaffRepository, authorRepository, librarianRepository);
        this.bookRepository = bookRepository;
        this.bookSubmissionRepository = bookSubmissionRepository;
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
                LocalDate borrowDate = LocalDate.parse(parts[3]);
                LocalDate dueDate = LocalDate.parse(parts[4]);
                LocalDate returnDate = parts[5].isBlank() ? null : LocalDate.parse(parts[5]);
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
        bookRepository.addApprovedBook(s.getTitle(), s.getAuthorFullName(), LocalDate.now(), s.getDescription(), s.getGenre());//Note that description is just an alias of summary for book
        bookSubmissionRepository.update(s);//Changes should be saved once there are updates

        appendNotificationForAuthors(s.getAuthorUsername(), "Submission approved", "Your Book Submission \"" + s.getTitle() + "\" was approved.", "Book Approved", s.getSubmissionId());//Send notification to the author
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

        appendNotificationForAuthors(s.getAuthorUsername(), "Submission rejected", "Your Book Submission \"" + s.getTitle() + "\" was rejected. Rejection reason:" + reason, "Book Rejected", s.getSubmissionId());//Send notification to the author
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
        if (role == UserRole.AUTHOR) appendNotificationForAuthors(normalizedUsername, "Profile edited", "Your profile was edited.", "Urgent", "");//Send notification to the author
        else appendNotificationTo(normalizedUsername, "ANNOUNCEMENT", "Your profile was edited.", role);

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
        if (role == UserRole.AUTHOR) appendNotificationForAuthors(normalizedUsername, "Account disabled", "Your account was disabled.", "Urgent", "");//Send notification to the author
        else appendNotificationTo(normalizedUsername, "ANNOUNCEMENT", "Your account was disabled.", role);

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
        if (role == UserRole.AUTHOR) appendNotificationForAuthors(normalizedUsername, "Account activated", "Your account was activated.", "Urgent", "");//Send notification to the author
        else appendNotificationTo(normalizedUsername, "ANNOUNCEMENT", "Your account was activated.", role);

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
        return true;//TO DO: Add urgency to the NotificationView class, and replace this by urgencyFilter.equals("ALL") || urgencyFilter.equals(notification.urgency());
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
        notifications.sort((n1, n2) -> (n1.isUrgent() ? 1 : 0) - (n2.isUrgent() ? 1 : 0));
        return notifications
                .stream()
                .filter(n -> filterNotification(n, categoryFilter, dateMin, dateMax, urgencyFilter))
                .collect(Collectors.toList());
    }

    private void appendNotification(String username, String category, String message) {
        try {
            Files.createDirectories(Paths.get("data/task3"));
            String line = String.join("|",
                    encode(username),
                    encode(category),
                    encode(message),
                    LocalDateTime.now().format(HISTORY_TIME_FORMAT)
            );
            Files.write(
                    Paths.get(TASK3_NOTIFICATIONS_FILE),
                    List.of(line),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
        }
    }

    private void appendNotificationTo(String username, String category, String message, UserRole role) {
        if (role == UserRole.AUTHOR) return;

        String file = "";
        String folder = "";
        switch (role) {
            case STUDENT, STAFF:
                file = TASK1_NOTIFICATIONS_FILE;
                folder = "data/task1";
                break;
            case LIBRARIAN:
                file = TASK3_NOTIFICATIONS_FILE;
                folder = "data/task3";
        }
        try {
            Files.createDirectories(Paths.get(folder));
            String line = String.join("|",
                    encode(username),
                    encode(category),
                    encode(message),
                    LocalDateTime.now().format(HISTORY_TIME_FORMAT)
            );
            Files.write(
                    Paths.get(file),
                    List.of(line),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
        }
    }
    private void appendNotificationForAuthors(String username, String title, String message, String type, String subId) {
        try {
            Files.createDirectories(Paths.get("data"));
            String line = new Notification(username, title, message, type, subId).toString();
            Files.write(
                    Paths.get("data/notifications.txt"),
                    List.of(line),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
        }
    }

    private List<NotificationView> loadStoredNotifications(String username) {
        Path path = Paths.get(TASK3_NOTIFICATIONS_FILE);
        if (!Files.exists(path)) return List.of();
        try {
            List<NotificationView> list = new ArrayList<>();
            for (String line : Files.readAllLines(path)) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length < 4) continue;
                String u = decode(parts[0]);
                if (!username.equals(u)) continue;
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

    public record NotificationView(
            LocalDateTime timestamp,
            String category,
            String message
    )
    {
        public boolean isUrgent() {
            return category.equals("Profile Change");
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
}
