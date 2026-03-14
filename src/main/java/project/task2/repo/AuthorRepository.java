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
        System.out.println("\n📂 ===== AUTHOR REPOSITORY CREATED =====");
        createDataDirectory();
        loadFromFile();
        System.out.println("📊 Authors in memory after load: " + authorsByUsername.size());
        if (authorsByUsername.size() > 0) {
            System.out.println("📋 Authors: " + authorsByUsername.keySet());
        }
        System.out.println("========================================\n");dassa
    }

    private void createDataDirectory() {
        try {
            Path dataDir = Paths.get("data");
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
                System.out.println("✅ Created data directory");
            }
        } catch (IOException e) {
            System.err.println("❌ Error creating data directory: " + e.getMessage());
        }
    }

    public void save(AuthorAccount author) {
        System.out.println("\n💾 ===== SAVING AUTHOR =====");
        System.out.println("Saving: " + author.getUsername());
        System.out.println("Before save - Authors in memory: " + authorsByUsername.keySet());
        
        authorsByUsername.put(author.getUsername(), author);
        
        System.out.println("After save - Authors in memory: " + authorsByUsername.keySet());
        saveAllToFile();
    }

    private void saveAllToFile() {
        try {
            List<String> lines = new ArrayList<>();
            for (AuthorAccount author : authorsByUsername.values()) {
                lines.add(author.toString());
            }
            
            Path filePath = Paths.get(AUTHORS_FILE);
            System.out.println("📝 Writing " + lines.size() + " authors to file:");
            for (String line : lines) {
                System.out.println("   " + line);
            }
            
            Files.write(filePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("✅ Saved " + lines.size() + " authors to file");
            System.out.println("==========================\n");
        } catch (IOException e) {
            System.err.println("❌ Error saving authors: " + e.getMessage());
        }
    }

    private void loadFromFile() {
        try {
            Path filePath = Paths.get(AUTHORS_FILE);
            if (Files.exists(filePath)) {
                List<String> lines = Files.readAllLines(filePath);
                System.out.println("📖 Reading from file, found " + lines.size() + " lines");
                
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        AuthorAccount author = AuthorAccount.fromString(line);
                        if (author != null) {
                            authorsByUsername.put(author.getUsername(), author);
                            System.out.println("   ✅ Added to map: " + author.getUsername());
                        } else {
                            System.out.println("   ❌ Failed to parse line");
                        }
                    }
                }
            } else {
                System.out.println("📁 No existing file found");
            }
        } catch (IOException e) {
            System.err.println("❌ Error loading authors: " + e.getMessage());
        }
    }

    public boolean existsByUsername(String username) {
        return authorsByUsername.containsKey(username);
    }

    public List<AuthorAccount> findAll() {
        return new ArrayList<>(authorsByUsername.values());
    }

    public Optional<AuthorAccount> findByUsername(String username) {
        System.out.println("🔍 Looking for: " + username);
        AuthorAccount author = authorsByUsername.get(username);
        if (author != null) {
            System.out.println("   ✅ Found: " + author.getUsername());
        } else {
            System.out.println("   ❌ Not found");
        }
        return Optional.ofNullable(author);
    }

    public int getCount() {
        return authorsByUsername.size();
    }
}
