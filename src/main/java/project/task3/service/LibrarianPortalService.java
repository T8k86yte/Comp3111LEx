package project.task3.service;

import project.task1.model.Book;
import project.task1.repo.BookRepository;
import project.task1.security.PasswordSecurity;
import project.task2.model.BookSubmission;
import project.task2.repo.SubmissionRepository;
import project.task3.model.LibrarianAccount;
import project.task3.repo.LibrarianRepository;

import java.util.regex.Pattern;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class LibrarianPortalService {
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final LibrarianRepository librarianRepository;
    private final BookRepository bookRepository;
    private final SubmissionRepository bookSubmissionRepository;

    public LibrarianPortalService(LibrarianRepository librarianRepository, BookRepository bookRepository, SubmissionRepository bookSubmissionRepository) {
        this.librarianRepository = librarianRepository;
        this.bookRepository = bookRepository;
        this.bookSubmissionRepository = bookSubmissionRepository;

        bookSubmissionRepository.save(new BookSubmission(
                "Test1",
                "TestFullName1",
                "TestUsername1",
                "TestSummary1",
                "TestGenre1",
                "TestFilePath1"
                )
        );
        bookSubmissionRepository.save(new BookSubmission(
                "Test2",
                "TestFullName2",
                "TestUsername2",
                "TestSummary2",
                "TestGenre2",
                "TestFilePath2"
                )
        );
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
        return sub.getStatus().equals(statusFilter);
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
        return OperationResult.success("Approve successful: \"" + sub.get().getTitle() + "\" is approved and created.");
    }

    public OperationResult rejectBookSubmission(String subId, LibrarianAccount user, String reason) {
        Optional<BookSubmission> sub = bookSubmissionRepository.findById(subId);
        if (sub.isEmpty()) return OperationResult.failure("Approve failed: Invalid submission ID.");
        if (user == null) return OperationResult.failure("Approve failed: No user logged in.");

        BookSubmission s = sub.get();
        s.reject(user.getUsername(), reason);
        bookSubmissionRepository.update(s);
        return OperationResult.success("Reject successful: \"" + sub.get().getTitle() + "\" is rejected.");
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
