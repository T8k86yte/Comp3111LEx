package project.task1.model;

import java.time.LocalDate;
import java.util.Objects;
import project.task1.repo.BookRepository;

public class NewBookSubmission {
    private final String title;
    private final String author;
    private final LocalDate submitDate;//Note that this is not the same as publishDate of books
    private final String summary;
    private final String genre;

    public NewBookSubmission(String title, String author, LocalDate submitDate, String summary, String genre) {
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.author = Objects.requireNonNull(author, "author must not be null");
        this.submitDate = Objects.requireNonNull(submitDate, "submitDate must not be null");
        this.summary = Objects.requireNonNull(summary, "summary must not be null");
        this.genre = Objects.requireNonNull(genre, "genre must not be null");
    }

    public String getTitle() {
        return title;
    }
    public String getAuthor() {
        return author;
    }
    public LocalDate getSubmitDate() {
        return submitDate;
    }
    public String getSummary() {
        return summary;
    }
    public String getGenre() {
        return genre;
    }

    //add the approved book to repo
    public void approve(BookRepository repo) {
        repo.addApprovedBook(title, author, LocalDate.now(), summary, genre);
    }

    //Reject method does not exist because it does nothing other than deleting the submission itself, which cannot be done here
}
