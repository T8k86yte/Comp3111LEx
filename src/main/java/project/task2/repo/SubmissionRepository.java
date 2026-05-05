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

    public void refreshFromFile() {
        System.out.println("🔄 Refreshing submissions from file...");
        loadFromFile();
    }

    private void loadFromFile() {
        try {
            Path filePath = Paths.get(SUBMISSIONS_FILE);
            if (Files.exists(filePath)) {
                List<String> lines = Files.readAllLines(filePath);
                submissionsById.clear();
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        BookSubmission submission = BookSubmission.fromString(line);
                        if (submission != null) {
                            submissionsById.put(submission.getSubmissionId(), submission);
                        }
                    }
                }
                System.out.println("📚 Loaded " + submissionsById.size() + " submissions from file");
            } else {
                System.out.println("📭 No submissions file found");
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
            
            System.out.println("💾 Saved " + lines.size() + " submissions to file");
        } catch (IOException e) {
            System.err.println("Error saving submissions: " + e.getMessage());
        }
    }

    public void save(BookSubmission submission) {
        submissionsById.put(submission.getSubmissionId(), submission);
        saveToFile();
        System.out.println("✅ Submission saved: " + submission.getSubmissionId());
    }

    public void update(BookSubmission submission) {
        submissionsById.put(submission.getSubmissionId(), submission);
        saveToFile();
        System.out.println("✏️ Submission updated: " + submission.getSubmissionId());
    }

    public void delete(String submissionId) {
        submissionsById.remove(submissionId);
        saveToFile();
        System.out.println("🗑️ Submission deleted: " + submissionId);
    }

    public Optional<BookSubmission> findById(String submissionId) {
        return Optional.ofNullable(submissionsById.get(submissionId));
    }

    public List<BookSubmission> findByAuthor(String authorUsername) {
        return submissionsById.values().stream()
                .filter(sub -> sub.getAuthorUsername().equals(authorUsername))
                .collect(Collectors.toList());
    }

    public List<BookSubmission> findPendingSubmissions() {
        return submissionsById.values().stream()
                .filter(BookSubmission::isPending)
                .collect(Collectors.toList());
    }

    public List<BookSubmission> findAll() {
        return new ArrayList<>(submissionsById.values());
    }

    public int getCount() {
        return submissionsById.size();
    }
}
