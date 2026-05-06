package project.task2.model;

public class BookStats {
    private final String bookId;
    private final String title;
    private final int borrowCount;
    private final double averageRating;
    private final int reviewCount;
    private final int totalReads;
    private final String status;

    public BookStats(String bookId, String title, int borrowCount, double averageRating, 
                     int reviewCount, int totalReads, String status) {
        this.bookId = bookId;
        this.title = title;
        this.borrowCount = borrowCount;
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
        this.totalReads = totalReads;
        this.status = status;
    }

    public String getBookId() { return bookId; }
    public String getTitle() { return title; }
    public int getBorrowCount() { return borrowCount; }
    public double getAverageRating() { return averageRating; }
    public int getReviewCount() { return reviewCount; }
    public int getTotalReads() { return totalReads; }
    public String getStatus() { return status; }
}
