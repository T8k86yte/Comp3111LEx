package project.task2.repo;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DraftRepository {
    private static final String DRAFTS_FILE = "data/drafts.txt";
    private final Map<String, String> draftsByAuthor = new ConcurrentHashMap<>();

    public DraftRepository() {
        createDataDirectory();
        loadFromFile();
    }

    private void createDataDirectory() {
        try {
            Files.createDirectories(Paths.get("data"));
        } catch (IOException e) {
            System.err.println("Error creating data directory: " + e.getMessage());
        }
    }

    private void loadFromFile() {
        try {
            Path filePath = Paths.get(DRAFTS_FILE);
            if (Files.exists(filePath)) {
                List<String> lines = Files.readAllLines(filePath);
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        String[] parts = line.split("\\|", 2);
                        if (parts.length == 2) {
                            draftsByAuthor.put(parts[0], parts[1]);
                        }
                    }
                }
                System.out.println("Loaded " + draftsByAuthor.size() + " drafts from file");
            }
        } catch (IOException e) {
            System.err.println("Error loading drafts: " + e.getMessage());
        }
    }

    private void saveToFile() {
        try {
            List<String> lines = new ArrayList<>();
            for (Map.Entry<String, String> entry : draftsByAuthor.entrySet()) {
                lines.add(entry.getKey() + "|" + entry.getValue());
            }
            
            Path filePath = Paths.get(DRAFTS_FILE);
            Files.write(filePath, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            System.out.println("Saved " + lines.size() + " drafts to file");
        } catch (IOException e) {
            System.err.println("Error saving drafts: " + e.getMessage());
        }
    }

    // Save draft for an author
    public void saveDraft(String authorUsername, String draftData) {
        if (draftData == null || draftData.trim().isEmpty()) {
            // If draft is empty, remove it
            draftsByAuthor.remove(authorUsername);
        } else {
            draftsByAuthor.put(authorUsername, draftData);
        }
        saveToFile();
    }

    // Load draft for an author
    public String loadDraft(String authorUsername) {
        return draftsByAuthor.get(authorUsername);
    }

    // Check if author has a draft
    public boolean hasDraft(String authorUsername) {
        return draftsByAuthor.containsKey(authorUsername);
    }

    // Delete draft for an author
    public void deleteDraft(String authorUsername) {
        draftsByAuthor.remove(authorUsername);
        saveToFile();
    }
}
