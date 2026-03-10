package project.task1.model;

import java.time.LocalDate;
import java.util.Objects;

public final class Book {
    private final String id;
    private final String title;
    private final String author;
    private final LocalDate publishDate;
    private final String summary;
    private boolean available;
    private String borrowedByUsername;
    private int borrowCount;

    public Book(String id, String title, String author, LocalDate publishDate, String summary, boolean available) {
        this(id, title, author, publishDate, summary, available, null, 0);
    }

    public Book(
            String id,
            String title,
            String author,
            LocalDate publishDate,
            String summary,
            boolean available,
            String borrowedByUsername
    ) {
        this(id, title, author, publishDate, summary, available, borrowedByUsername, 0);
    }

    public Book(
            String id,
            String title,
            String author,
            LocalDate publishDate,
            String summary,
            boolean available,
            String borrowedByUsername,
            int borrowCount
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.author = Objects.requireNonNull(author, "author must not be null");
        this.publishDate = Objects.requireNonNull(publishDate, "publishDate must not be null");
        this.summary = Objects.requireNonNull(summary, "summary must not be null");
        this.available = available;
        this.borrowedByUsername = borrowedByUsername;
        this.borrowCount = Math.max(0, borrowCount);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public LocalDate getPublishDate() {
        return publishDate;
    }

    public String getSummary() {
        return summary;
    }

    public synchronized boolean isAvailable() {
        return available;
    }

    public synchronized String getBorrowedByUsername() {
        return borrowedByUsername;
    }

    public synchronized boolean borrowBy(String username) {
        if (!available) {
            return false;
        }
        available = false;
        borrowedByUsername = username;
        borrowCount++;
        return true;
    }

    public synchronized boolean returnBy(String username) {
        if (available) {
            return false;
        }
        if (borrowedByUsername == null || !borrowedByUsername.equals(username)) {
            return false;
        }
        available = true;
        borrowedByUsername = null;
        return true;
    }

    public synchronized int getBorrowCount() {
        return borrowCount;
    }
}
