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
        loadFromFile();
    }

    private void createDataDirectory() {
        try {
            Path dataDir = Paths.get("data");
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
        } catch (IOException e) {
            System.err.println("Error creating data directory: " + e.getMessage());
        }
    }

    public void save(AuthorAccount author) {
        authorsByUsername.put(author.getUsername(), author);
        saveAllToFile();
        System.out.println("✅ Author saved: " + author.getUsername());
    }

    public void update(AuthorAccount author) {
        save(author);
        System.out.println("✅ Author updated: " + author.getUsername());
    }

    private void saveAllToFile() {
        try {
            List<String> lines = new ArrayList<>();
            for (AuthorAccount author : authorsByUsername.values()) {
                lines.add(author.toString());
            }
            
            Path filePath = Paths.get(AUTHORS_FILE);
            Files.write(filePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
        } catch (IOException e) {
            System.err.println("Error saving authors to file: " + e.getMessage());
        }
    }

    private void loadFromFile() {
        try {
            Path filePath = Paths.get(AUTHORS_FILE);
            if (Files.exists(filePath)) {
                List<String> lines = Files.readAllLines(filePath);
                
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        AuthorAccount author = AuthorAccount.fromString(line);
                        if (author != null) {
                            authorsByUsername.put(author.getUsername(), author);
                        }
                    }
                }
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

    public List<AuthorAccount> getAllUsers() {
        return findAll();
    }

    public Optional<AuthorAccount> findByUsername(String username) {
        return Optional.ofNullable(authorsByUsername.get(username));
    }

    public int getCount() {
        return authorsByUsername.size();
    }
}
