package project.task1.repo;

import project.task1.model.Book;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBookRepository implements BookRepository {
    private final Map<String, Book> booksById = new ConcurrentHashMap<>();

    public InMemoryBookRepository() {
        // Sample approved books to demonstrate Task 1.3 and 1.4.
        addApprovedBook(
                "B001",
                "Effective Java",
                "Joshua Bloch",
                LocalDate.now().minusDays(20),
                "A practical guide to best practices in Java development."
        );
        addApprovedBook(
                "B002",
                "Clean Code",
                "Robert C. Martin",
                LocalDate.now().minusDays(12),
                "A handbook of software craftsmanship and clean coding principles."
        );
    }

    @Override
    public List<Book> findAll() {
        return booksById.values()
                .stream()
                .sorted(Comparator.comparing(Book::getId))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    @Override
    public Optional<Book> findById(String bookId) {
        return Optional.ofNullable(booksById.get(bookId));
    }

    @Override
    public boolean borrowBook(String bookId, String borrowerUsername) {
        Book book = booksById.get(bookId);
        if (book == null) {
            return false;
        }
        return book.borrowBy(borrowerUsername);
    }

    @Override
    public void addApprovedBook(String id, String title, String author, LocalDate publishDate, String summary) {
        booksById.put(id, new Book(id, title, author, publishDate, summary, true));
    }
}
