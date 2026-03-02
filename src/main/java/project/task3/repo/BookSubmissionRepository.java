package project.task3.repo;

import java.util.*;
import java.time.LocalDate;

import project.task1.repo.BookRepository;
import project.task2.model.BookSubmission;

public class BookSubmissionRepository {
    private final Map<String, BookSubmission> submissionsById = new HashMap<>();

    public BookSubmissionRepository() {
        addBookSubmission(
                "Test1",
                "TestFullName1",
                "TestUsername1",
                "TestSummary1",
                "TestGenre1",
                "TestFilePath1"
        );
        addBookSubmission(
                "Test2",
                "TestFullName2",
                "TestUsername2",
                "TestSummary2",
                "TestGenre2",
                "TestFilePath2"
        );
    }

    public List<BookSubmission> findAll() {
        return submissionsById.values()
                .stream()
                .sorted(Comparator.comparing(BookSubmission::getSubmissionId))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public Optional<BookSubmission> findById(String subId) {
        return Optional.ofNullable(submissionsById.get(subId));
    }

    public boolean approveBookSubmission(String subId, String librarianUsername, BookRepository repo) {
        BookSubmission sub = submissionsById.get(subId);
        if (sub == null) return false;

        sub.approve(librarianUsername);
        repo.addApprovedBook(sub.getTitle(), sub.getAuthorFullName(), LocalDate.now(), sub.getSummary(), sub.getGenre());
        return true;
    }

    public boolean rejectBookSubmission(String subId, String librarianUsername, String reason) {
        BookSubmission sub = submissionsById.get(subId);
        if (sub == null) return false;

        sub.reject(librarianUsername, reason);
        return true;
    }

    public void addBookSubmission(String title, String authorFullName, String authorUsername, String summary, String genre, String filePath) {
        BookSubmission sub = new BookSubmission(title, authorFullName, authorUsername, summary, genre, filePath);
        submissionsById.put(sub.getSubmissionId(), sub);
    }
}
