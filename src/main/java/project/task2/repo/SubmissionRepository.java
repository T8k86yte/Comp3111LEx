package project.task2.repo;

import project.task2.model.BookSubmission;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SubmissionRepository {
    private static final String SUBMISSIONS_FILE = "data/submissions.txt";
    private final Map<String, BookSubmission> submissionsById = new ConcurrentHashMap<>();

    public SubmissionRepository() {
        createDataDirectory();
        loadFromFile();
    }

    private void createDataDirectory() {
        try {
            Files.createDirectories(Paths.get("data"));
        } catch (IOException e) {
            System.err.println("Error creating data directory: " + e.getMessage());
        }
    }

    private void loadFromFile() {
        try {
            Path filePath = Paths.get(SUBMISSIONS_FILE);
            if (Files.exists(filePath)) {
                List<String> lines = Files.readAllLines(filePath);
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        BookSubmission submission = BookSubmission.fromString(line);
                        if (submission != null) {
                            submissionsById.put(submission.getSubmissionId(), submission);
                        }
                    }
                }
                System.out.println("Loaded " + submissionsById.size() + " submissions from file");
            }
        } catch (IOException e) {
            System.err.println("Error loading submissions: " + e.getMessage());
        }
    }

    private void saveToFile() {
        try {
            List<String> lines = new ArrayList<>();
            for (BookSubmission submission : submissionsById.values()) {
                lines.add(submission.toString());
            }
            
            Path filePath = Paths.get(SUBMISSIONS_FILE);
            Files.write(filePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            System.out.println("Saved " + lines.size() + " submissions to file");
        } catch (IOException e) {
            System.err.println("Error saving submissions: " + e.getMessage());
        }
    }

    // Save a new submission
    public void save(BookSubmission submission) {
        submissionsById.put(submission.getSubmissionId(), submission);
        saveToFile();
    }

    // Update an existing submission
    public void update(BookSubmission submission) {
        submissionsById.put(submission.getSubmissionId(), submission);
        saveToFile();
    }

    // Find by ID
    public Optional<BookSubmission> findById(String submissionId) {
        return Optional.ofNullable(submissionsById.get(submissionId));
    }

    // Find all submissions by an author
    public List<BookSubmission> findByAuthor(String authorUsername) {
        return submissionsById.values().stream()
                .filter(sub -> sub.getAuthorUsername().equals(authorUsername))
                .collect(Collectors.toList());
    }

    // Find all pending submissions (for librarian)
    public List<BookSubmission> findPendingSubmissions() {
        return submissionsById.values().stream()
                .filter(BookSubmission::isPending)
                .collect(Collectors.toList());
    }

    // Find all submissions
    public List<BookSubmission> findAll() {
        return new ArrayList<>(submissionsById.values());
    }

    // Get count
    public int getCount() {
        return submissionsById.size();
    }
}
