package project.task1.model;

import java.time.LocalDate;
import java.util.Objects;

public final class Book {
    private final String id;
    private final String title;
    private final String author;
    private final LocalDate publishDate;
    private final String summary;
    private final String genre;
    private boolean available;
    private String borrowedByUsername;

    public Book(String id, String title, String author, LocalDate publishDate, String summary, String genre, boolean available) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.author = Objects.requireNonNull(author, "author must not be null");
        this.publishDate = Objects.requireNonNull(publishDate, "publishDate must not be null");
        this.summary = Objects.requireNonNull(summary, "summary must not be null");
        this.genre = Objects.requireNonNull(genre, "genre must not be null");
        this.available = available;
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

    public String getGenre() {
        return genre;
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
        return true;
    }
}
