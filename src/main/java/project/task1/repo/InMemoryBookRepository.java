package project.task1.repo;

import project.task1.model.Book;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryBookRepository implements BookRepository {
    private static final String BOOKS_FILE = "data/books.txt";
    private final Map<String, Book> booksById = new ConcurrentHashMap<>();
    private int nextid;

    public InMemoryBookRepository() {
        nextid = 1;
        try {
            Files.createDirectories(Paths.get("data"));
        } catch (IOException e) {
            System.err.println("Error creating data directory: " + e.getMessage());
        }

        loadBooks();
        if (booksById.isEmpty()) {
            addApprovedBook(
                    "Effective Java",
                    "Joshua Bloch",
                    LocalDate.now().minusDays(20),
                    "A practical guide to best practices in Java development.",
                    "PLACEHOLDER"
            );
            addApprovedBook(
                    "Clean Code",
                    "Robert C. Martin",
                    LocalDate.now().minusDays(12),
                    "A handbook of software craftsmanship and clean coding principles.",
                    "PLACEHOLDER"
            );
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
        boolean success = book.borrowBy(borrowerUsername);
        if (success) {
            saveBooks();
        }
        return success;
    }

    @Override
    public void addApprovedBook(String title, String author, LocalDate publishDate, String summary, String genre) {
        String idstr = Integer.toString(nextid);
        if (idstr.length() < 3) idstr = "0".repeat(3 - idstr.length()).concat(idstr);
        idstr = "B".concat(idstr);
        booksById.put(idstr, new Book(idstr, title, author, publishDate, summary, true));
        nextid++;
        saveBooks();
    }

    private void loadBooks() {
        try {
            Path path = Paths.get(BOOKS_FILE);
            if (!Files.exists(path)) {
                return;
            }

            List<String> lines = Files.readAllLines(path);
            int maxId = 0;
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                Book book = parseBook(line);
                if (book != null) {
                    booksById.put(book.getId(), book);
                    if (book.getId().startsWith("B")) {
                        try {
                            int numericId = Integer.parseInt(book.getId().substring(1));
                            maxId = Math.max(maxId, numericId);
                        } catch (NumberFormatException ignored) {
                            // Keep loading even when one malformed id is encountered.
                        }
                    }
                }
            }
            nextid = maxId + 1;
            System.out.println("Loaded " + booksById.size() + " books from file.");
        } catch (IOException e) {
            System.err.println("Error loading books: " + e.getMessage());
        }
    }

    private void saveBooks() {
        try {
            Path path = Paths.get(BOOKS_FILE);
            List<String> lines = new ArrayList<>();
            for (Book book : findAll()) {
                lines.add(serializeBook(book));
            }
            Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Saved " + lines.size() + " books to file.");
        } catch (IOException e) {
            System.err.println("Error saving books: " + e.getMessage());
        }
    }

    private static String serializeBook(Book book) {
        String borrowedBy = book.getBorrowedByUsername() == null ? "" : book.getBorrowedByUsername();
        return String.join("|",
                encodeField(book.getId()),
                encodeField(book.getTitle()),
                encodeField(book.getAuthor()),
                book.getPublishDate().toString(),
                encodeField(book.getSummary()),
                Boolean.toString(book.isAvailable()),
                encodeField(borrowedBy)
        );
    }

    private static Book parseBook(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length < 7) {
            return null;
        }

        try {
            String id = decodeField(parts[0]);
            String title = decodeField(parts[1]);
            String author = decodeField(parts[2]);
            LocalDate publishDate = LocalDate.parse(parts[3]);
            String summary = decodeField(parts[4]);
            boolean available = Boolean.parseBoolean(parts[5]);
            String borrowedBy = decodeField(parts[6]);
            if (borrowedBy.isEmpty()) {
                borrowedBy = null;
            }
            return new Book(id, title, author, publishDate, summary, available, borrowedBy);
        } catch (Exception e) {
            return null;
        }
    }

    private static String encodeField(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeField(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}