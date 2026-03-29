package project.task3.service;

import project.task1.model.StudentStaffAccount;
import project.task1.model.UserAccount;
import project.task1.model.UserRole;
import project.task1.model.Book;
import project.task1.repo.BookRepository;
import project.task1.repo.StudentStaffRepository;
import project.shared.SharedAuthFacade;
import project.task1.service.StudentStaffPortalService;
import project.task2.model.AuthorAccount;
import project.task2.model.BookSubmission;
import project.task2.repo.AuthorRepository;
import project.task2.repo.SubmissionRepository;
import project.task3.model.LibrarianAccount;
import project.task3.repo.LibrarianRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.time.chrono.*;

public class LibrarianPortalService {
    private final LibrarianRepository librarianRepository;
    private final StudentStaffRepository studentStaffRepository;
    private final AuthorRepository authorRepository;
    private final SharedAuthFacade sharedAuthFacade;
    private final BookRepository bookRepository;
    private final SubmissionRepository bookSubmissionRepository;
    private final StudentStaffPortalService studentStaffPortalService;

    private static final String TASK1_NOTIFICATIONS_FILE = "data/task1/notifications.txt";
    private static final String TASK2_NOTIFICATIONS_FILE = "data/task2/notifications.txt";
    private static final String TASK3_NOTIFICATIONS_FILE = "data/task3/notifications.txt";
    private static final DateTimeFormatter HISTORY_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public LibrarianPortalService(LibrarianRepository librarianRepository, StudentStaffRepository studentStaffRepository, AuthorRepository authorRepository, BookRepository bookRepository, SubmissionRepository bookSubmissionRepository) {
        this.librarianRepository = librarianRepository;
        this.studentStaffRepository = studentStaffRepository;
        this.authorRepository = authorRepository;
        this.sharedAuthFacade = new SharedAuthFacade(studentStaffRepository, authorRepository, librarianRepository);
        this.bookRepository = bookRepository;
        this.bookSubmissionRepository = bookSubmissionRepository;
        this.studentStaffPortalService = new StudentStaffPortalService(
                studentStaffRepository,
                bookRepository,
                authorRepository,
                librarianRepository
        );
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

    public OperationResult validateBookSubmissionId(String subId) {
        if (bookSubmissionRepository.findById(subId).isEmpty()) return OperationResult.failure("Invalid book submission Id.");
        else return OperationResult.success("");
    }

    public OperationResult approveBookSubmission(String subId, LibrarianAccount user) {
        Optional<BookSubmission> sub = bookSubmissionRepository.findById(subId);
        if (sub.isEmpty()) return OperationResult.failure("Approve failed: Invalid submission ID.");
        if (user == null) return OperationResult.failure("Approve failed: No user logged in.");

        BookSubmission s = sub.get();
        s.approve(user.getUsername());
        bookRepository.addApprovedBook(s.getTitle(), s.getAuthorFullName(), LocalDate.now(), s.getDescription(), s.getGenre());//Note that description is just an alias of summary for book
        bookSubmissionRepository.update(s);//Changes should be saved once there are updates

        appendNotificationTo(s.getAuthorUsername(), "ANNOUNCEMENT", "Your Book Submission \"" + s.getTitle() + "\" was approved.", UserRole.AUTHOR);//Send notification to the author
        return OperationResult.success("Approve successful: \"" + sub.get().getTitle() + "\" is approved and created.");
    }
    public OperationResult approveBookSubmission(String subId, String Username) {
        Optional<LibrarianAccount> l = librarianRepository.findByUsername(Username);
        if (l.isEmpty()) return OperationResult.failure("Error: Current user does not exist.");
        return approveBookSubmission(subId, l.get());
    }

    public OperationResult rejectBookSubmission(String subId, LibrarianAccount user, String reason) {
        Optional<BookSubmission> sub = bookSubmissionRepository.findById(subId);
        if (sub.isEmpty()) return OperationResult.failure("Approve failed: Invalid submission ID.");
        if (user == null) return OperationResult.failure("Approve failed: No user logged in.");

        BookSubmission s = sub.get();
        s.reject(user.getUsername(), reason);
        bookSubmissionRepository.update(s);

        appendNotificationTo(s.getAuthorUsername(), "ANNOUNCEMENT", "Your Book Submission \"" + s.getTitle() + "\" was rejected.\nRejection reason:" + reason, UserRole.AUTHOR);//Send notification to the author
        return OperationResult.success("Reject successful: \"" + sub.get().getTitle() + "\" is rejected.");
    }
    public OperationResult rejectBookSubmission(String subId, String Username, String reason) {
        Optional<LibrarianAccount> l = librarianRepository.findByUsername(Username);
        if (l.isEmpty()) return OperationResult.failure("Error: Current user does not exist.");
        return rejectBookSubmission(subId, l.get(), reason);
    }

    public OperationResult approveBookSubmissionsBulk(List<String> submissionIds, String username) {
        if (submissionIds == null || submissionIds.isEmpty()) {
            return OperationResult.failure("Bulk approve failed: no submissions selected.");
        }
        int ok = 0;
        List<String> errors = new ArrayList<>();
        for (String subId : submissionIds) {
            OperationResult result = approveBookSubmission(subId, username);
            if (result.success()) {
                ok++;
            } else {
                errors.add(subId + ": " + result.message());
            }
        }
        if (errors.isEmpty()) {
            return OperationResult.success("Bulk approve completed: " + ok + " submission(s).");
        }
        return OperationResult.failure("Bulk approve partial: " + ok + " approved.\n" + String.join("\n", errors));
    }

    public OperationResult rejectBookSubmissionsBulk(List<String> submissionIds, String username, String reason) {
        if (submissionIds == null || submissionIds.isEmpty()) {
            return OperationResult.failure("Bulk reject failed: no submissions selected.");
        }
        int ok = 0;
        List<String> errors = new ArrayList<>();
        for (String subId : submissionIds) {
            OperationResult result = rejectBookSubmission(subId, username, reason);
            if (result.success()) {
                ok++;
            } else {
                errors.add(subId + ": " + result.message());
            }
        }
        if (errors.isEmpty()) {
            return OperationResult.success("Bulk reject completed: " + ok + " submission(s).");
        }
        return OperationResult.failure("Bulk reject partial: " + ok + " rejected.\n" + String.join("\n", errors));
    }

    public OperationResult updateProfile(String username, String newFullName, String newPassword, String confirmNewPassword, String newEmployeeID) {
        return updateProfile(username, newFullName, newPassword, confirmNewPassword, newEmployeeID, "");
    }

    public OperationResult updateProfile(
            String username,
            String newFullName,
            String newPassword,
            String confirmNewPassword,
            String newEmployeeID,
            String currentPassword
    ) {
        String normalizedUsername = safeTrim(username);
        String normalizedFullName = safeTrim(newFullName);
        String pwd = newPassword == null ? "" : newPassword;
        String confirm = confirmNewPassword == null ? "" : confirmNewPassword;
        String normalizedEmployeeID = safeTrim(newEmployeeID);
        String currentPwd = currentPassword == null ? "" : currentPassword;

        if (normalizedUsername.isEmpty()) return OperationResult.failure("Profile update failed: invalid user.");
        Optional<LibrarianAccount> existingOpt = librarianRepository.findByUsername(normalizedUsername);
        if (existingOpt.isEmpty()) return OperationResult.failure("Profile update failed: account not found.");
        LibrarianAccount existing = existingOpt.get();
        if (normalizedFullName.isEmpty()) return OperationResult.failure("Profile update failed: full name is required.");
        boolean fullNameChanged = !normalizedFullName.equals(existing.getFullName());
        boolean employeeIdChanged = !normalizedEmployeeID.isBlank()
                && !normalizedEmployeeID.equals(Integer.toString(existing.getEmployeeID()));
        boolean passwordChanged = !pwd.isBlank() || !confirm.isBlank();
        if ((fullNameChanged || employeeIdChanged || passwordChanged)
                && !project.task1.security.PasswordSecurity.verifyPassword(
                currentPwd,
                existing.getPasswordSaltBase64(),
                existing.getPasswordHashBase64()
        )) {
            return OperationResult.failure("Profile update failed: current password is incorrect.");
        }

        String salt = existing.getPasswordSaltBase64();
        String hash = existing.getPasswordHashBase64();
        if (passwordChanged) {
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
        return OperationResult.success("Profile updated successfully.");
    }

    public OperationResult addNewUser(
            String roleText,
            String username,
            String fullName,
            String password,
            String confirmPassword,
            String bio,
            String employeeIdText
    ) {
        SharedAuthFacade.AuthResult result = sharedAuthFacade.register(
                username,
                fullName,
                password,
                confirmPassword,
                roleText,
                bio,
                employeeIdText
        );
        if (!result.success()) {
            return OperationResult.failure(result.message());
        }
        appendNotificationTo(result.principal().username(), "ANNOUNCEMENT", "Your account was created by librarian.", mapRole(result.principal().role()));
        appendNotification("SYSTEM", "USER_UPDATE", "New user created: " + result.principal().username() + " (" + result.principal().role() + ").");
        return OperationResult.success("Successfully created user account.");
    }

    public OperationResult disableUsers(List<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return OperationResult.failure("Bulk disable failed: no usernames provided.");
        }
        int ok = 0;
        List<String> errors = new ArrayList<>();
        for (String username : usernames) {
            OperationResult result = disableUser(username);
            if (result.success()) {
                ok++;
            } else {
                errors.add(username + ": " + result.message());
            }
        }
        if (errors.isEmpty()) {
            return OperationResult.success("Bulk disable successful: " + ok + " account(s) disabled.");
        }
        return OperationResult.failure("Bulk disable partial success: " + ok + " disabled.\n" + String.join("\n", errors));
    }

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
        appendNotificationTo(normalizedUsername, "ANNOUNCEMENT", "Your profile was edited.", role);

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
        appendNotificationTo(normalizedUsername, "ANNOUNCEMENT", "Your account was disabled.", role);

        return OperationResult.success("Successfully disabled target user account.");
    }

    public String getConfirmDetail(String subId) {
        BookSubmission sub = bookSubmissionRepository.findById(subId).get();
        return "Title: " + sub.getTitle()
                + "\nAuthor Username: " + sub.getAuthorUsername()
                + "\nDescription: " + sub.getDescription()
                + "\nBook File: " + sub.getFilePath()
                + "\nCover Image: " + (sub.getCoverImagePath().isBlank() ? "None" : sub.getCoverImagePath())
                + "\nSubmission Time: " + sub.getSubmissionDate() + "\n";
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


    public List<NotificationView> getNotificationBoard(String username) {
        return getNotificationBoard(username, "ALL", "", true);
    }

    public List<NotificationView> getNotificationBoard(String username, String categoryFilter, String keyword, boolean urgentFirst) {
        String normalized = safeTrim(username);
        if (normalized.isEmpty()) return List.of();

        List<NotificationView> notifications = loadStoredNotifications(normalized);
        String normalizedCategory = safeTrim(categoryFilter).toUpperCase();
        String normalizedKeyword = safeTrim(keyword).toLowerCase();
        List<NotificationView> filtered = notifications.stream()
                .filter(n -> "ALL".equals(normalizedCategory) || n.category().equalsIgnoreCase(normalizedCategory))
                .filter(n -> normalizedKeyword.isEmpty()
                        || n.message().toLowerCase().contains(normalizedKeyword)
                        || n.category().toLowerCase().contains(normalizedKeyword))
                .collect(Collectors.toList());
        Comparator<NotificationView> byTime = Comparator.comparing(NotificationView::timestamp).reversed();
        if (urgentFirst) {
            filtered.sort(Comparator.comparing((NotificationView n) -> isUrgentCategory(n.category())).reversed().thenComparing(byTime));
        } else {
            filtered.sort(byTime);
        }
        return filtered;
    }

    public OperationResult exportBorrowedBooksCsv(String outputPath) {
        String normalizedPath = safeTrim(outputPath);
        if (normalizedPath.isEmpty()) {
            return OperationResult.failure("Export failed: output path is required.");
        }
        try {
            List<String> lines = new ArrayList<>();
            lines.add("BookTitle,BorrowerUsername,PublishDate,Status");
            for (Book book : bookRepository.findAll()) {
                if (book.isAvailable()) {
                    continue;
                }
                String title = escapeCsv(book.getTitle());
                String borrower = escapeCsv(book.getBorrowedByUsername());
                String publish = book.getPublishDate().toString();
                String status = "BORROWED";
                lines.add(title + "," + borrower + "," + publish + "," + status);
            }
            Path path = Paths.get(normalizedPath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return OperationResult.success("Export successful: " + normalizedPath);
        } catch (Exception e) {
            return OperationResult.failure("Export failed: " + e.getMessage());
        }
    }

    public OperationResult deleteBookAndNotify(String bookId) {
        String normalizedBookId = safeTrim(bookId).toUpperCase();
        if (normalizedBookId.isEmpty()) {
            return OperationResult.failure("Delete failed: invalid book ID.");
        }
        Optional<Book> bookOpt = bookRepository.findById(normalizedBookId);
        if (bookOpt.isEmpty()) {
            return OperationResult.failure("Delete failed: book not found.");
        }
        String title = bookOpt.get().getTitle();
        studentStaffPortalService.notifyBookDeletedForBorrowers(normalizedBookId, title);
        boolean deleted = bookRepository.deleteBook(normalizedBookId);
        if (!deleted) {
            return OperationResult.failure("Delete failed: unable to remove target book.");
        }
        appendNotification("SYSTEM", "ANNOUNCEMENT", "Book deleted: " + normalizedBookId + " - " + title);
        return OperationResult.success("Book deleted successfully: " + normalizedBookId);
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
        String file = "";
        String folder = "";
        switch (role) {
            case STUDENT, STAFF:
                file = TASK1_NOTIFICATIONS_FILE;
                folder = "data/task1";
                break;
            case AUTHOR:
                file = TASK2_NOTIFICATIONS_FILE;
                folder = "data/task2";
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
                if (!username.equals(u) && !"SYSTEM".equalsIgnoreCase(u)) continue;
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

    private static UserRole mapRole(String role) {
        if (role == null) {
            return UserRole.STUDENT;
        }
        return switch (role.toUpperCase()) {
            case "STAFF" -> UserRole.STAFF;
            case "AUTHOR" -> UserRole.AUTHOR;
            case "LIBRARIAN" -> UserRole.LIBRARIAN;
            default -> UserRole.STUDENT;
        };
    }

    private static boolean isUrgentCategory(String category) {
        if (category == null) {
            return false;
        }
        String normalized = category.toUpperCase();
        return normalized.contains("URGENT")
                || normalized.contains("REQUEST")
                || normalized.contains("USER_UPDATE")
                || normalized.contains("SUBMISSION");
    }

    private static String escapeCsv(String value) {
        String normalized = value == null ? "" : value;
        String escaped = normalized.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    public record NotificationView(
            LocalDateTime timestamp,
            String category,
            String message
    ) {}
}
