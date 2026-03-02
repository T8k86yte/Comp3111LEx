package project.task2.repo;

import project.task2.model.AuthorAccount;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class AuthorRepository {
    private static final String AUTHORS_FILE = "data/authors.txt";
    private final Map<String, AuthorAccount> authorsByUsername = new HashMap<>();

    public AuthorRepository() {
        // Create data directory if it doesn't exist
        createDataDirectory();
        // Load existing authors from file
        loadFromFile();
    }

    // Create data directory
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

    // Save a new author
    public void save(AuthorAccount author) {
        authorsByUsername.put(author.getUsername(), author);
        saveToFile();
        System.out.println("Author saved: " + author.getUsername());
    }

    // Save all authors to file
    private void saveToFile() {
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

    // Load authors from file
    private void loadFromFile() {
        try {
            Path filePath = Paths.get(AUTHORS_FILE);
            if (Files.exists(filePath)) {
                List<String> lines = Files.readAllLines(filePath);
                
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        // We'll implement fromString later when needed
                        System.out.println("Found author in file: " + line);
                    }
                }
                System.out.println("Loaded " + lines.size() + " authors from file");
            } else {
                System.out.println("No existing authors file found, starting fresh");
            }
        } catch (IOException e) {
            System.err.println("Error loading authors from file: " + e.getMessage());
        }
    }

    // Check if username already exists
    public boolean existsByUsername(String username) {
        return authorsByUsername.containsKey(username);
    }

    // Get all authors
    public List<AuthorAccount> findAll() {
        return new ArrayList<>(authorsByUsername.values());
    }

    // Find author by username
    public Optional<AuthorAccount> findByUsername(String username) {
        return Optional.ofNullable(authorsByUsername.get(username));
    }

    // Get total count of authors
    public int getCount() {
        return authorsByUsername.size();
    }
}