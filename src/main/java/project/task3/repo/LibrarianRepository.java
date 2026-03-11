package project.task3.repo;

import project.task3.model.LibrarianAccount;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class LibrarianRepository {
    private static final String LIBRARIAN_FILE = "data/librarians.txt";
    private final Map<String, LibrarianAccount> librarianByUsername = new ConcurrentHashMap<>();

    public LibrarianRepository() {
        // Create data directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get("data"));
        } catch (IOException e) {
            System.err.println("Error creating data directory: " + e.getMessage());
        }
        loadLibrarians();
    }

    private void loadLibrarians() {
        try {
            Path path = Paths.get(LIBRARIAN_FILE);
            if (Files.exists(path)) {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        LibrarianAccount l = LibrarianAccount.fromString(line);
                        if (l != null) {
                            librarianByUsername.put(l.getUsername(), l);
                        }
                    }
                }
                System.out.println("Loaded " + librarianByUsername.size() + " librarians from file.");
            } else {
                System.out.println("No existing librarian files were found.");
            }
        } catch (IOException e) {
            System.err.println("Error loading librarians: " + e.getMessage());
        }
    }

    private void saveLibrarians() {
        try {
            Path path = Paths.get(LIBRARIAN_FILE);
            List<String> lines = new ArrayList<>();
            for (LibrarianAccount l : librarianByUsername.values()) {
                lines.add(l.toString());
            }
            Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Saved " + lines.size() + " librarians to file.");
        } catch (IOException e) {
            System.err.println("Error saving librarians: " + e.getMessage());
        }
    }

    public boolean existsByUsername(String username) {
        return librarianByUsername.containsKey(username);
    }

    public void save(LibrarianAccount userAccount) {
        librarianByUsername.put(userAccount.getUsername(), userAccount);
        saveLibrarians();//Save all the librarians to file each time it is updated.
    }

    public Optional<LibrarianAccount> findByUsername(String username) {
        return Optional.ofNullable(librarianByUsername.get(username));
    }
}