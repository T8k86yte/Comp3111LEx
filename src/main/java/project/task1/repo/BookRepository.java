package project.task1.repo;

import project.task1.model.Book;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface BookRepository {
    List<Book> findAll();

    Optional<Book> findById(String bookId);

    boolean borrowBook(String bookId, String borrowerUsername);

    // Integration hook for Task 2/3:
    // Task 2 submits books, Task 3 approves them, then approved books are added here.
    void addApprovedBook(String id, String title, String author, LocalDate publishDate, String summary, String genre);
}
