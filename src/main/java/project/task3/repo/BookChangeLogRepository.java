package project.task3.repo;

import project.task3.model.BookChangeLog;
import project.task1.model.Book;
import project.task3.model.LibrarianAccount;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BookChangeLogRepository {
    private static final String LOG_FILE = "data/booklogs.txt";
    private final Map<String, ArrayList<BookChangeLog>> logs = new ConcurrentHashMap<>();

    public BookChangeLogRepository() {
        // Create data directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get("data"));
        } catch (IOException e) {
            System.err.println("Error creating data directory: " + e.getMessage());
        }
        loadLogs();
    }

    public void logChange(Book book) {//Should be called before changing the book
        BookChangeLog log = new BookChangeLog(book.getId(), book.getTitle(), book.getAuthor(), book.getGenre(),
                book.getPdfFilePath(), book.getCoverImagePath(), LocalDate.now(), book.getSummary());
        if (logs.containsKey(book.getId())) logs.get(book.getId()).add(log);
        else {
            ArrayList<BookChangeLog> list = new ArrayList<>(1);
            list.add(log);
            logs.put(book.getId(), list);
        }
        saveLogs();
    }

    public String getChangeLogs(String bookId) {
        if (!logs.containsKey(bookId)) return null;
        StringBuilder b = new StringBuilder();
        for (BookChangeLog log : logs.get(bookId)) {
            b.append(log.description());
            b.append("\n\n");
        }
        b.delete(b.length() - 2, b.length());
        return b.toString();
    }

    private void loadLogs() {
        try {
            Path path = Paths.get(LOG_FILE);
            if (Files.exists(path)) {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        BookChangeLog l = BookChangeLog.fromString(line);
                        if (l != null) {
                            if (logs.containsKey(l.getId())) logs.get(l.getId()).add(l);
                            else {
                                ArrayList<BookChangeLog> list = new ArrayList<>(1);
                                list.add(l);
                                logs.put(l.getId(), list);
                            }
                        }
                    }
                }
                System.out.println("Loaded " + logs.size() + " book logs from file.");
            } else {
                System.out.println("No existing book log files were found.");
            }
        } catch (IOException e) {
            System.err.println("Error loading book logs: " + e.getMessage());
        }
    }

    private void saveLogs() {
        try {
            Path path = Paths.get(LOG_FILE);
            List<String> lines = new ArrayList<>();
            for (ArrayList<BookChangeLog> list : logs.values()) {
                for (BookChangeLog l : list) lines.add(l.toString());
            }
            Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Saved " + lines.size() + " book log to file.");
        } catch (IOException e) {
            System.err.println("Error saving book logs: " + e.getMessage());
        }
    }
}
