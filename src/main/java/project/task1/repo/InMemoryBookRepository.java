package project.task1.repo;

import project.task1.model.Book;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBookRepository implements BookRepository {
    private static final Path STORAGE_PATH = Paths.get("data", "task1", "books.db");
    private final Map<String, Book> booksById = new ConcurrentHashMap<>();

    public InMemoryBookRepository() {
        loadFromFile();
        if (booksById.isEmpty()) {
            seedDefaultBooks();
            persistToFile();
        }
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
        boolean borrowed = book.borrowBy(borrowerUsername);
        if (borrowed) {
            persistToFile();
        }
        return borrowed;
    }

    @Override
    public void addApprovedBook(String id, String title, String author, LocalDate publishDate, String summary) {
        booksById.put(id, new Book(id, title, author, publishDate, summary, true));
        persistToFile();
    }

    private void seedDefaultBooks() {
        booksById.put(
                "B001",
                new Book(
                        "B001",
                        "Effective Java",
                        "Joshua Bloch",
                        LocalDate.now().minusDays(20),
                        "A practical guide to best practices in Java development.",
                        true
                )
        );
        booksById.put(
                "B002",
                new Book(
                        "B002",
                        "Clean Code",
                        "Robert C. Martin",
                        LocalDate.now().minusDays(12),
                        "A handbook of software craftsmanship and clean coding principles.",
                        true
                )
        );
    }

    private void loadFromFile() {
        List<String> lines = RepositoryFileStorageSupport.readLines(STORAGE_PATH);
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            List<String> parts = RepositoryFileStorageSupport.splitRecord(line, 7);
            String id = parts.get(0);
            String title = RepositoryFileStorageSupport.decode(parts.get(1));
            String author = RepositoryFileStorageSupport.decode(parts.get(2));
            LocalDate publishDate = LocalDate.parse(parts.get(3));
            boolean available = Boolean.parseBoolean(parts.get(4));
            String borrowedByEncoded = parts.get(5);
            String borrowedBy = borrowedByEncoded.isEmpty()
                    ? null
                    : RepositoryFileStorageSupport.decode(borrowedByEncoded);
            String summary = RepositoryFileStorageSupport.decode(parts.get(6));
            booksById.put(
                    id,
                    new Book(id, title, author, publishDate, summary, available, borrowedBy)
            );
        }
    }

    private void persistToFile() {
        List<String> lines = booksById.values()
                .stream()
                .sorted(Comparator.comparing(Book::getId))
                .collect(ArrayList::new, (acc, book) -> acc.add(serialize(book)), ArrayList::addAll);
        RepositoryFileStorageSupport.writeLines(STORAGE_PATH, lines);
    }

    private static String serialize(Book book) {
        String borrowedBy = book.getBorrowedByUsername() == null
                ? ""
                : RepositoryFileStorageSupport.encode(book.getBorrowedByUsername());
        return book.getId()
                + "|"
                + RepositoryFileStorageSupport.encode(book.getTitle())
                + "|"
                + RepositoryFileStorageSupport.encode(book.getAuthor())
                + "|"
                + book.getPublishDate()
                + "|"
                + book.isAvailable()
                + "|"
                + borrowedBy
                + "|"
                + RepositoryFileStorageSupport.encode(book.getSummary());
    }
}
