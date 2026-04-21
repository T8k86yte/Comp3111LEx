package project.task1.repo;

import project.task1.model.Book;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookRepository {
    List<Book> findAll();

    List<Book> findTopRecommended(int limit);

    Optional<Book> findById(String bookId);

    boolean borrowBook(String bookId, String borrowerUsername);

    boolean returnBook(String bookId, String borrowerUsername);

    boolean deleteBook(String bookId);

    boolean modifyBook(String bookId,
                       String newTitle,
                       String newAuthor,
                       String newGenre,
                       String newDescription,
                       String newFilePath,
                       String newCoverPath);

    // Integration hook for Task 2/3:
    // Task 2 submits books, Task 3 approves them, then approved books are added here.
    void addApprovedBook(String title, String author, LocalDate publishDate, String summary, String genre);

    default void addApprovedBook(String title, String author, LocalDate publishDate, String summary, String genre, String coverImagePath) {
        addApprovedBook(title, author, publishDate, summary, genre);
    }
}
