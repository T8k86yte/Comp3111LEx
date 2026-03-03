package project.task2.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class BookSubmission {
    private final String submissionId;
    private final String title;
    private final String authorFullName;
    private final String authorUsername;
    private final LocalDate submitDate;
    private final String summary;
    private final String genre;
    private final String filePath;
    private String status; // PENDING, APPROVED, REJECTED
    private String rejectionReason;
    private LocalDateTime reviewedDate;
    private String reviewedBy;

    //Used when a new submission is created
    public BookSubmission(
            String title,
            String authorFullName,
            String authorUsername,
            String summary,
            String genre,
            String filePath
    ) {
        this.submissionId = "SUB_" + UUID.randomUUID().toString().substring(0, 8);
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.authorFullName = Objects.requireNonNull(authorFullName, "authorFullName must not be null");
        this.authorUsername = Objects.requireNonNull(authorUsername, "authorUsername must not be null");
        this.submitDate = LocalDate.now();
        this.summary = Objects.requireNonNull(summary, "summary must not be null");
        this.genre = Objects.requireNonNull(genre, "genre must not be null");
        this.filePath = Objects.requireNonNull(filePath, "filePath must not be null");
        this.status = "PENDING";
    }

    //Used when an existing submission is read from the file
    public BookSubmission(
            String submissionId,
            String title,
            String authorFullName,
            String authorUsername,
            LocalDate submitDate,
            String summary,
            String genre,
            String filePath,
            String status,
            String rejectionReason,
            LocalDateTime reviewedDate,
            String reviewedBy
    ) {
        this.submissionId = submissionId;
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.authorFullName = Objects.requireNonNull(authorFullName, "authorFullName must not be null");
        this.authorUsername = Objects.requireNonNull(authorUsername, "authorUsername must not be null");
        this.submitDate = submitDate;
        this.summary = Objects.requireNonNull(summary, "summary must not be null");
        this.genre = Objects.requireNonNull(genre, "genre must not be null");
        this.filePath = Objects.requireNonNull(filePath, "filePath must not be null");
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.reviewedDate = reviewedDate;
        this.reviewedBy = reviewedBy;
    }

    // Getters
    public String getSubmissionId() { return submissionId; }
    public String getTitle() { return title; }
    public String getAuthorFullName() { return authorFullName; }
    public String getAuthorUsername() { return authorUsername; }
    public LocalDate getSubmitDate() { return submitDate; }
    public String getSummary() { return summary; }
    public String getGenre() { return genre; }
    public String getFilePath() { return filePath; }
    public String getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDateTime getReviewedDate() { return reviewedDate; }
    public String getReviewedBy() { return reviewedBy; }

    // Status methods
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

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isApproved() {
        return "APPROVED".equals(status);
    }

    public boolean isRejected() {
        return "REJECTED".equals(status);
    }

    @Override
    public String toString() {
        return String.join("|",
            submissionId,
            title,
            authorFullName,
            authorUsername,
            submitDate.toString(),
            summary,
            genre,
            filePath,
            status,
            rejectionReason != null ? rejectionReason : "",
            reviewedDate != null ? reviewedDate.toString() : "",
            reviewedBy != null ? reviewedBy : ""
        );
    }

    public static BookSubmission fromString(String data) {
        String[] parts = data.split("\\|");
        if (parts.length >= 12) {
            BookSubmission submission = new BookSubmission(
                parts[0], // submissionId
                parts[1], // title
                parts[2], // authorFullName
                parts[3], // authorUsername
                LocalDate.parse(parts[4]), // summary
                parts[5], // summary
                parts[6], // genre
                parts[7], // filePath
                parts[8], // status
                parts[9], // rejectionReason
                LocalDateTime.parse(parts[10]), // reviewedDate
                parts[11] //reviewedBy
            );
            // Use reflection or add setters for these fields if needed
            return submission;
        }
        return null;
    }
}