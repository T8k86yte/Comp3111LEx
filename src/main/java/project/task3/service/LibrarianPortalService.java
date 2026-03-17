package project.task3.service;

import project.task1.repo.BookRepository;
import project.task1.repo.StudentStaffRepository;
import project.shared.SharedAuthFacade;
import project.task2.model.BookSubmission;
import project.task2.repo.AuthorRepository;
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
    private final LibrarianRepository librarianRepository;
    private final SharedAuthFacade sharedAuthFacade;
    private final BookRepository bookRepository;
    private final SubmissionRepository bookSubmissionRepository;

    public LibrarianPortalService(LibrarianRepository librarianRepository, BookRepository bookRepository, SubmissionRepository bookSubmissionRepository) {
        this.librarianRepository = librarianRepository;
        this.sharedAuthFacade = new SharedAuthFacade(new StudentStaffRepository(), new AuthorRepository(), librarianRepository);
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

    public String getConfirmDetail(String subId) {
        BookSubmission sub = bookSubmissionRepository.findById(subId).get();
        return "Title: " + sub.getTitle() + "\nAuthor Username: "+ sub.getAuthorUsername() + "\nDescription: " + sub.getDescription() + "\nSubmission Time: " + sub.getSubmissionDate() + "\n";
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
