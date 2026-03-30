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
    private List<String> genres;
    private final String description;
    private final String filePath;
    private final String coverImagePath;
    private final LocalDateTime submissionDate;
    private String status;
    private String rejectionReason;
    private LocalDateTime reviewedDate;
    private String reviewedBy;
    private boolean isDraft;
    private int editCount;
    private int totalBorrowedCount;      // Total times borrowed (historical)
    private int currentlyBorrowedCount;   // Currently borrowed (not returned)

    public BookSubmission(String title, String authorUsername, String authorFullName,
                         List<String> genres, String description, String filePath,
                         String coverImagePath) {
        this.submissionId = "SUB" + System.currentTimeMillis();
        this.title = title;
        this.authorUsername = authorUsername;
        this.authorFullName = authorFullName;
        this.genres = genres != null ? genres : new ArrayList<>();
        this.description = description;
        this.filePath = filePath;
        this.coverImagePath = coverImagePath != null ? coverImagePath : "";
        this.submissionDate = LocalDateTime.now();
        this.status = "PENDING";
        this.isDraft = false;
        this.editCount = 0;
        this.totalBorrowedCount = 0;
        this.currentlyBorrowedCount = 0;
    }

    public BookSubmission(String submissionId, String title, String authorUsername,
                         String authorFullName, String genresStr, String description,
                         String filePath, String coverImagePath, LocalDateTime submissionDate, 
                         String status, String rejectionReason, LocalDateTime reviewedDate, 
                         String reviewedBy, boolean isDraft, int editCount, 
                         int totalBorrowedCount, int currentlyBorrowedCount) {
        this.submissionId = submissionId;
        this.title = title;
        this.authorUsername = authorUsername;
        this.authorFullName = authorFullName;
        this.genres = parseGenres(genresStr);
        this.description = description;
        this.filePath = filePath;
        this.coverImagePath = coverImagePath != null ? coverImagePath : "";
        this.submissionDate = submissionDate;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.reviewedDate = reviewedDate;
        this.reviewedBy = reviewedBy;
        this.isDraft = isDraft;
        this.editCount = editCount;
        this.totalBorrowedCount = totalBorrowedCount;
        this.currentlyBorrowedCount = currentlyBorrowedCount;
    }

    private List<String> parseGenres(String genresStr) {
        if (genresStr == null || genresStr.isEmpty()) return new ArrayList<>();
        return Arrays.asList(genresStr.split(","));
    }

    private String genresToString() { return String.join(",", genres); }

    // Getters
    public String getSubmissionId() { return submissionId; }
    public String getTitle() { return title; }
    public String getAuthorUsername() { return authorUsername; }
    public String getAuthorFullName() { return authorFullName; }
    public List<String> getGenres() { return genres; }
    public String getGenresAsString() { return genresToString(); }
    public String getGenre() { return genresToString(); }
    public String getDescription() { return description; }
    public String getFilePath() { return filePath; }
    public String getCoverImagePath() { return coverImagePath; }
    public LocalDateTime getSubmissionDate() { return submissionDate; }
    public String getStatus() { return status; }
    public String getRejectionReason() { return rejectionReason; }
    public LocalDateTime getReviewedDate() { return reviewedDate; }
    public String getReviewedBy() { return reviewedBy; }
    public boolean isDraft() { return isDraft; }
    public int getEditCount() { return editCount; }
    public int getTotalBorrowedCount() { return totalBorrowedCount; }
    public int getCurrentlyBorrowedCount() { return currentlyBorrowedCount; }
    public boolean isCurrentlyBorrowed() { return currentlyBorrowedCount > 0; }

    // Setters for borrow/return
    public void incrementBorrowedCount() { 
        this.totalBorrowedCount++; 
        this.currentlyBorrowedCount++;
    }
    
    public void decrementBorrowedCount() { 
        if (this.currentlyBorrowedCount > 0) {
            this.currentlyBorrowedCount--;
        }
    }
    
    public void setGenres(List<String> newGenres) { this.genres = newGenres; this.editCount++; }
    
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
    
    public boolean isPending() { return "PENDING".equals(status); }
    public boolean isApproved() { return "APPROVED".equals(status); }
    public boolean isRejected() { return "REJECTED".equals(status); }
    
    public boolean canBeDeleted() {
        // Can delete if:
        // 1. Status is PENDING, OR
        // 2. Status is APPROVED and currentlyBorrowedCount == 0 (no books currently borrowed)
        if (isPending()) return true;
        if (isApproved() && currentlyBorrowedCount == 0) return true;
        return false;
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return String.join("|",
            submissionId, title, authorUsername, authorFullName, genresToString(),
            description, filePath, coverImagePath, submissionDate.format(formatter), status,
            rejectionReason != null ? rejectionReason : "",
            reviewedDate != null ? reviewedDate.format(formatter) : "",
            reviewedBy != null ? reviewedBy : "",
            String.valueOf(isDraft), String.valueOf(editCount), 
            String.valueOf(totalBorrowedCount), String.valueOf(currentlyBorrowedCount)
        );
    }

    public static BookSubmission fromString(String data) {
        String[] parts = data.split("\\|", -1);
        if (parts.length >= 17) {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            LocalDateTime submissionDate = LocalDateTime.parse(parts[8], formatter);
            LocalDateTime reviewedDate = parts[11].isEmpty() ? null : LocalDateTime.parse(parts[11], formatter);
            boolean isDraft = Boolean.parseBoolean(parts[13]);
            int editCount = Integer.parseInt(parts[14]);
            int totalBorrowedCount = Integer.parseInt(parts[15]);
            int currentlyBorrowedCount = Integer.parseInt(parts[16]);
            return new BookSubmission(
                parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6], parts[7],
                submissionDate, parts[9], parts[10].isEmpty() ? null : parts[10],
                reviewedDate, parts[12].isEmpty() ? null : parts[12], isDraft, editCount,
                totalBorrowedCount, currentlyBorrowedCount
            );
        }
        return null;
    }
}
