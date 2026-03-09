package project.task2.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class BookSubmission {
    private final String submissionId;
    private final String title;
    private final String authorUsername;
    private final String authorFullName;
    private List<String> genres;  // Changed to List for multiple genres
    private final String description;
    private final String filePath;
    private final LocalDateTime submissionDate;
    private String status; // PENDING, APPROVED, REJECTED, DRAFT
    private String rejectionReason;
    private LocalDateTime reviewedDate;
    private String reviewedBy;
    private boolean isDraft; // For auto-save feature

    // Constructor for new submissions
    public BookSubmission(String title, String authorUsername, String authorFullName,
                         List<String> genres, String description, String filePath) {
        this.submissionId = generateSubmissionId();
        this.title = title;
        this.authorUsername = authorUsername;
        this.authorFullName = authorFullName;
        this.genres = genres != null ? genres : new ArrayList<>();
        this.description = description;
        this.filePath = filePath;
        this.submissionDate = LocalDateTime.now();
        this.status = "PENDING";
        this.isDraft = false;
    }

    // Constructor for drafts
    public static BookSubmission createDraft(String authorUsername, String authorFullName) {
        BookSubmission draft = new BookSubmission("", authorUsername, authorFullName, 
                                                   new ArrayList<>(), "", "");
        draft.status = "DRAFT";
        draft.isDraft = true;
        return draft;
    }

    // Constructor for loading from file
    public BookSubmission(String submissionId, String title, String authorUsername,
                         String authorFullName, String genresStr, String description,
                         String filePath, LocalDateTime submissionDate, String status,
                         String rejectionReason, LocalDateTime reviewedDate, String reviewedBy,
                         boolean isDraft) {
        this.submissionId = submissionId;
        this.title = title;
        this.authorUsername = authorUsername;
        this.authorFullName = authorFullName;
        this.genres = parseGenres(genresStr);
        this.description = description;
        this.filePath = filePath;
        this.submissionDate = submissionDate;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.reviewedDate = reviewedDate;
        this.reviewedBy = reviewedBy;
        this.isDraft = isDraft;
    }

    private String generateSubmissionId() {
        return "SUB" + System.currentTimeMillis();
    }

    private List<String> parseGenres(String genresStr) {
        if (genresStr == null || genresStr.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(genresStr.split(","));
    }

    private String genresToString() {
        return String.join(",", genres);
    }

    // Getters
    public String getSubmissionId() { return submissionId; }
    public String getTitle() { return title; }
    public String getAuthorUsername() { return authorUsername; }
    public String getAuthorFullName() { return authorFullName; }
    public List<String> getGenres() { return genres; }
    public String getGenresAsString() { return genresToString(); }
    public String getDescription() { return description; }
    public String getFilePath() { return filePath; }
    public LocalDateTime getSubmissionDate() { return submissionDate; }
    public String getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDateTime getReviewedDate() { return reviewedDate; }
    public String getReviewedBy() { return reviewedBy; }
    public boolean isDraft() { return isDraft; }

    // Status setters
    public void approve(String librarianUsername) {
        this.status = "APPROVED";
        this.reviewedDate = LocalDateTime.now();
        this.reviewedBy = librarianUsername;
        this.isDraft = false;
    }

    public void reject(String librarianUsername, String reason) {
        this.status = "REJECTED";
        this.rejectionReason = reason;
        this.reviewedDate = LocalDateTime.now();
        this.reviewedBy = librarianUsername;
        this.isDraft = false;
    }

    // Convert draft to submission
    public void submitForApproval() {
        this.status = "PENDING";
        this.isDraft = false;
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
            genresToString(),
            description,
            filePath,
            submissionDate.format(formatter),
            status,
            rejectionReason != null ? rejectionReason : "",
            reviewedDate != null ? reviewedDate.format(formatter) : "",
            reviewedBy != null ? reviewedBy : "",
            String.valueOf(isDraft)
        );
    }

    // Parse from file string
    public static BookSubmission fromString(String data) {
        String[] parts = data.split("\\|");
        if (parts.length >= 13) {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            
            LocalDateTime submissionDate = LocalDateTime.parse(parts[7], formatter);
            LocalDateTime reviewedDate = parts[10].isEmpty() ? null : 
                LocalDateTime.parse(parts[10], formatter);
            boolean isDraft = Boolean.parseBoolean(parts[12]);
            
            return new BookSubmission(
                parts[0], parts[1], parts[2], parts[3], parts[4], 
                parts[5], parts[6], submissionDate, parts[8],
                parts[9].isEmpty() ? null : parts[9],
                reviewedDate, parts[11].isEmpty() ? null : parts[11],
                isDraft
            );
        }
        return null;
    }
}
