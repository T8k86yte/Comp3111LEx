package project.task3.repo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.time.LocalDate;

import project.task1.repo.BookRepository;
import project.task2.model.BookSubmission;

public class BookSubmissionRepository {
    private static final String SUBMISSION_FILE = "data/booksubmissions.txt";
    private final Map<String, BookSubmission> submissionsById = new HashMap<>();

    public BookSubmissionRepository() {
        /*
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
        */

        loadBookSubmissions();
    }

    private void loadBookSubmissions() {
        try {
            Path path = Paths.get(SUBMISSION_FILE);
            if (Files.exists(path)) {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        BookSubmission sub = BookSubmission.fromString(line);
                        if (sub != null) {
                            submissionsById.put(sub.getSubmissionId(), sub);
                        }
                    }
                }
                System.out.println("Loaded " + submissionsById.size() + " book submissions from file.");
            } else {
                System.out.println("No existing book submission files were found.");
            }
        } catch (IOException e) {
            System.err.println("Error loading book submissions: " + e.getMessage());
        }
    }

    public void saveBookSubmissions() {
        try {
            Path path = Paths.get(SUBMISSION_FILE);
            List<String> lines = new ArrayList<>();
            for (BookSubmission sub : submissionsById.values()) {
                lines.add(sub.toString());
            }
            Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Saved " + lines.size() + " book submissions to file.");
        } catch (IOException e) {
            System.err.println("Error saving book submissions: " + e.getMessage());
        }
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
        repo.addApprovedBook(sub.getTitle(), sub.getAuthorFullName(), LocalDate.now(), sub.getDescription(), sub.getGenre());
        saveBookSubmissions();//Changes should be saved once there are updates
        return true;
    }

    public boolean rejectBookSubmission(String subId, String librarianUsername, String reason) {
        BookSubmission sub = submissionsById.get(subId);
        if (sub == null) return false;

        sub.reject(librarianUsername, reason);
        saveBookSubmissions();
        return true;
    }

    public void addBookSubmission(String title, String authorFullName, String authorUsername, String summary, String genre, String filePath) {
        BookSubmission sub = new BookSubmission(title, authorFullName, authorUsername, summary, genre, filePath);
        submissionsById.put(sub.getSubmissionId(), sub);
        saveBookSubmissions();
    }
}
