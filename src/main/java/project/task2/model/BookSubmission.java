package project.task2.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BookSubmission {
    private final String submissionId;
    private final String title;
    private final String authorUsername;
    private final String authorFullName;
    private final String genre;
    private final String description;
    private final String filePath;
    private final LocalDateTime submissionDate;
    private String status; // PENDING, APPROVED, REJECTED
    private String rejectionReason;
    private LocalDateTime reviewedDate;
    private String reviewedBy;

    // Constructor for new submissions
    public BookSubmission(String title, String authorUsername, String authorFullName,
                         String genre, String description, String filePath) {
        this.submissionId = generateSubmissionId();
        this.title = title;
        this.authorUsername = authorUsername;
        this.authorFullName = authorFullName;
        this.genre = genre;
        this.description = description;
        this.filePath = filePath;
        this.submissionDate = LocalDateTime.now();
        this.status = "PENDING";
    }

    // Constructor for loading from file
    public BookSubmission(String submissionId, String title, String authorUsername,
                         String authorFullName, String genre, String description,
                         String filePath, LocalDateTime submissionDate, String status,
                         String rejectionReason, LocalDateTime reviewedDate, String reviewedBy) {
        this.submissionId = submissionId;
        this.title = title;
        this.authorUsername = authorUsername;
        this.authorFullName = authorFullName;
        this.genre = genre;
        this.description = description;
        this.filePath = filePath;
        this.submissionDate = submissionDate;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.reviewedDate = reviewedDate;
        this.reviewedBy = reviewedBy;
    }

    private String generateSubmissionId() {
        return "SUB" + System.currentTimeMillis();
    }

    // Getters
    public String getSubmissionId() { return submissionId; }
    public String getTitle() { return title; }
    public String getAuthorUsername() { return authorUsername; }
    public String getAuthorFullName() { return authorFullName; }
    public String getGenre() { return genre; }
    public String getDescription() { return description; }
    public String getFilePath() { return filePath; }
    public LocalDateTime getSubmissionDate() { return submissionDate; }
    public String getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDateTime getReviewedDate() { return reviewedDate; }
    public String getReviewedBy() { return reviewedBy; }

    // Status setters (for librarian)
    public void approve(String librarianUsername) {
        this.status = "APPROVED";
        this.reviewedDate = LocalDateTime.now();
        this.reviewedBy = librarianUsername;
    }

    public void reject(String librarianUsername, String reason) {
        this.status = "REJECTED";
        this.rejectionReason = reason;
        this.reviewedDate = LocalDateTime.now();
        this.reviewedBy = librarianUsername;
    }

    // Helper methods
    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isApproved() {
        return "APPROVED".equals(status);
    }

    public boolean isRejected() {
        return "REJECTED".equals(status);
    }

    // Format for file storage
    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return String.join("|",
            submissionId,
            title,
            authorUsername,
            authorFullName,
            genre,
            description,
            filePath,
            submissionDate.format(formatter),
            status,
            rejectionReason != null ? rejectionReason : "",
            reviewedDate != null ? reviewedDate.format(formatter) : "",
            reviewedBy != null ? reviewedBy : ""
        );
    }

    // Parse from file string
    public static BookSubmission fromString(String data) {
        String[] parts = data.split("\\|");
        if (parts.length >= 12) {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

            LocalDateTime submissionDate = LocalDateTime.parse(parts[7], formatter);
            LocalDateTime reviewedDate = parts[10].isEmpty() ? null :
                LocalDateTime.parse(parts[10], formatter);

            return new BookSubmission(
                parts[0], parts[1], parts[2], parts[3], parts[4],
                parts[5], parts[6], submissionDate, parts[8],
                parts[9].isEmpty() ? null : parts[9],
                reviewedDate, parts[11].isEmpty() ? null : parts[11]
            );
        }
        return null;
    }
}