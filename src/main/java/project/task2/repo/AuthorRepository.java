package project.task2.repo;

import project.task2.model.AuthorAccount;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuthorRepository {
    private static final String AUTHORS_FILE = "data/authors.txt";
    private final Map<String, AuthorAccount> authorsByUsername = new ConcurrentHashMap<>();

    public AuthorRepository() {
        // Create data directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get("data"));
        } catch (IOException e) {
            System.err.println("Error creating data directory: " + e.getMessage());
        }
        loadAuthors();
    }

    private void loadAuthors() {
        try {
            Path path = Paths.get(AUTHORS_FILE);
            if (Files.exists(path)) {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        AuthorAccount author = AuthorAccount.fromString(line);
                        if (author != null) {
                            authorsByUsername.put(author.getUsername(), author);
                        }
                    }
                }
                System.out.println("Loaded " + authorsByUsername.size() + " authors from file.");
            }
        } catch (IOException e) {
            System.err.println("Error loading authors: " + e.getMessage());
        }
    }

    private void saveAuthors() {
        try {
            Path path = Paths.get(AUTHORS_FILE);
            List<String> lines = new ArrayList<>();
            for (AuthorAccount author : authorsByUsername.values()) {
                lines.add(author.toString());
            }
            Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Saved " + lines.size() + " authors to file.");
        } catch (IOException e) {
            System.err.println("Error saving authors: " + e.getMessage());
        }
    }

    public boolean existsByUsername(String username) {
        return authorsByUsername.containsKey(username);
    }

    public void save(AuthorAccount author) {
        authorsByUsername.put(author.getUsername(), author);
        saveAuthors();
    }

    public Optional<AuthorAccount> findByUsername(String username) {
        return Optional.ofNullable(authorsByUsername.get(username));
    }

    public List<AuthorAccount> findAll() {
        return new ArrayList<>(authorsByUsername.values());
    }
}