package project.task2.model;

import java.time.LocalDateTime;

public class Review {
    private final String reviewId;
    private final String bookId;
    private final String bookTitle;
    private final String reviewerUsername;
    private final String reviewerFullName;
    private final int rating;
    private final String comment;
    private final LocalDateTime createdAt;
    private String authorReply;
    private LocalDateTime replyDate;
    private boolean flagged;
    private String flagReason;

    public Review(String reviewId, String bookId, String bookTitle, String reviewerUsername, 
                  String reviewerFullName, int rating, String comment, LocalDateTime createdAt) {
        this.reviewId = reviewId;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.reviewerUsername = reviewerUsername;
        this.reviewerFullName = reviewerFullName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.authorReply = null;
        this.replyDate = null;
        this.flagged = false;
        this.flagReason = null;
    }

    // Getters
    public String getReviewId() { return reviewId; }
    public String getBookId() { return bookId; }
    public String getBookTitle() { return bookTitle; }
    public String getReviewerUsername() { return reviewerUsername; }
    public String getReviewerFullName() { return reviewerFullName; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getAuthorReply() { return authorReply; }
    public LocalDateTime getReplyDate() { return replyDate; }
    public boolean isFlagged() { return flagged; }
    public String getFlagReason() { return flagReason; }

    // Setters
    public void setAuthorReply(String reply) { 
        this.authorReply = reply; 
        this.replyDate = LocalDateTime.now();
    }
    public void setFlagged(boolean flagged, String reason) { 
        this.flagged = flagged; 
        this.flagReason = reason;
    }
}
