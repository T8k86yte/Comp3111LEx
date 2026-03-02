package project.task2.repo;

import project.task2.model.AuthorAccount;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class AuthorRepository {
    private static final String AUTHORS_FILE = "data/authors.txt";
    private final Map<String, AuthorAccount> authorsByUsername = new HashMap<>();

    public AuthorRepository() {
        createDataDirectory();
        loadFromFile();  // Load existing authors when repository is created
    }

    private void createDataDirectory() {
        try {
            Path dataDir = Paths.get("data");
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
                System.out.println("Created data directory");
            }
        } catch (IOException e) {
            System.err.println("Error creating data directory: " + e.getMessage());
        }
    }

    public void save(AuthorAccount author) {
        // Add to in-memory map
        authorsByUsername.put(author.getUsername(), author);
        // Save ALL authors to file
        saveAllToFile();
        System.out.println("Author saved: " + author.getUsername());
    }

    private void saveAllToFile() {
        try {
            List<String> lines = new ArrayList<>();
            for (AuthorAccount author : authorsByUsername.values()) {
                lines.add(author.toString());
            }
            
            Path filePath = Paths.get(AUTHORS_FILE);
            Files.write(filePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            System.out.println("Saved " + lines.size() + " authors to " + AUTHORS_FILE);
        } catch (IOException e) {
            System.err.println("Error saving authors to file: " + e.getMessage());
        }
    }

    private void loadFromFile() {
        try {
            Path filePath = Paths.get(AUTHORS_FILE);
            if (Files.exists(filePath)) {
                List<String> lines = Files.readAllLines(filePath);
                int loadedCount = 0;
                
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        // Parse the line back into an AuthorAccount object
                        AuthorAccount author = AuthorAccount.fromString(line);
                        if (author != null) {
                            // IMPORTANT: Add to the map!
                            authorsByUsername.put(author.getUsername(), author);
                            loadedCount++;
                            System.out.println("Loaded author: " + author.getUsername());
                        }
                    }
                }
                System.out.println("Loaded " + loadedCount + " authors from " + AUTHORS_FILE);
            } else {
                System.out.println("No existing authors file found, starting fresh");
            }
        } catch (IOException e) {
            System.err.println("Error loading authors from file: " + e.getMessage());
        }
    }

    public boolean existsByUsername(String username) {
        return authorsByUsername.containsKey(username);
    }

    public List<AuthorAccount> findAll() {
        return new ArrayList<>(authorsByUsername.values());
    }

    public Optional<AuthorAccount> findByUsername(String username) {
        return Optional.ofNullable(authorsByUsername.get(username));
    }

    public int getCount() {
        return authorsByUsername.size();
    }

    // For debugging - show all authors in memory
    public void printAllAuthors() {
        System.out.println("\n📚 Authors in memory (" + authorsByUsername.size() + "):");
        for (AuthorAccount author : authorsByUsername.values()) {
            System.out.println("  • " + author.getUsername() + " | " + author.getFullName());
        }
    }
}
