package project.task2.service.llm;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * LLM Service for generating book summaries.
 * 
 * This service generates AI-like summaries based on book title and genre.
 * For production, you can replace this with actual LLM API calls (OpenAI, Gemini, etc.)
 */
public class LLMService {
    
    private static final Random RANDOM = new Random();
    
    /**
     * Generate a book summary based on the book title, genre, and content preview
     */
    public String generateSummary(String title, String genre, String contentPreview) {
        return generateMockSummary(title, genre, contentPreview);
    }
    
    /**
     * Generate a book summary based on the book title and genre
     */
    public String generateSummary(String title, String genre) {
        return generateMockSummary(title, genre, "");
    }
    
    /**
     * Generate summary from uploaded file content
     */
    public String generateSummaryFromFile(String filePath, String title, String genre) {
        String contentPreview = extractFilePreview(filePath);
        return generateMockSummary(title, genre, contentPreview);
    }
    
    /**
     * Mock summary generation - creates template-based summaries
     * This simulates LLM output for testing without API keys
     */
    private String generateMockSummary(String title, String genre, String contentPreview) {
        // Generate different summary styles based on genre
        String genreSummary = getGenreBasedSummary(genre);
        String theme = getGenericTheme();
        String conclusion = getConcludingStatement();
        
        return String.format(
            "\"%s\" is a compelling %s that explores %s. %s %s",
            title,
            getGenreDescription(genre),
            theme,
            genreSummary,
            conclusion
        );
    }
    
    private String getGenreDescription(String genre) {
        if (genre == null || genre.isEmpty()) return "literary work";
        
        String g = genre.toLowerCase();
        if (g.contains("fiction")) return "work of fiction";
        if (g.contains("non-fiction")) return "non-fictional narrative";
        if (g.contains("science") || g.contains("sci-fi")) return "science fiction novel";
        if (g.contains("fantasy")) return "fantasy adventure";
        if (g.contains("mystery")) return "mystery thriller";
        if (g.contains("biography")) return "biographical account";
        if (g.contains("history")) return "historical narrative";
        if (g.contains("technology")) return "technology-focused analysis";
        if (g.contains("romance")) return "romantic tale";
        if (g.contains("thriller")) return "suspenseful thriller";
        if (g.contains("poetry")) return "poetic collection";
        if (g.contains("children")) return "children's story";
        return "literary work";
    }
    
    private String getGenreBasedSummary(String genre) {
        if (genre == null || genre.isEmpty()) {
            return "The author presents a unique perspective on contemporary issues.";
        }
        
        String g = genre.toLowerCase();
        if (g.contains("science") || g.contains("tech")) {
            return "It delves into cutting-edge concepts and technological innovations that shape our future.";
        }
        if (g.contains("fantasy")) {
            return "Readers will be transported to a richly imagined world filled with magic and wonder.";
        }
        if (g.contains("mystery") || g.contains("thriller")) {
            return "The plot keeps readers guessing with unexpected twists and turns until the very end.";
        }
        if (g.contains("romance")) {
            return "The emotional journey explores the complexities of human connection and love.";
        }
        if (g.contains("history")) {
            return "The author provides deep insights into historical events and their lasting impact.";
        }
        if (g.contains("biography")) {
            return "This intimate portrait reveals the triumphs and struggles of a remarkable individual.";
        }
        if (g.contains("poetry")) {
            return "The verses flow with emotion and imagery, touching the reader's soul.";
        }
        if (g.contains("children")) {
            return "This delightful story will captivate young readers with its engaging narrative.";
        }
        
        return "The writing is engaging and accessible, making complex ideas understandable to all readers.";
    }
    
    private String getGenericTheme() {
        String[] themes = {
            "themes of growth, resilience, and human connection",
            "complex relationships between characters and their environment",
            "universal truths about the human condition",
            "timeless questions about identity and purpose",
            "the delicate balance between tradition and progress",
            "the pursuit of knowledge and self-discovery",
            "the power of hope in challenging times",
            "the importance of community and belonging"
        };
        return themes[RANDOM.nextInt(themes.length)];
    }
    
    private String getConcludingStatement() {
        String[] conclusions = {
            "This book is recommended for readers seeking an enriching literary experience.",
            "A must-read for anyone interested in this genre.",
            "The author's voice is distinctive and memorable.",
            "This work will leave a lasting impression on its readers.",
            "It offers valuable insights that resonate long after reading.",
            "A compelling addition to any library collection.",
            "Readers will find themselves returning to this book again and again.",
            "This book stands out as a noteworthy contribution to its field."
        };
        return conclusions[RANDOM.nextInt(conclusions.length)];
    }
    
    /**
     * Extract preview text from file (first few hundred characters)
     */
    private String extractFilePreview(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return "";
            }
            
            String content;
            String lowerPath = filePath.toLowerCase();
            
            if (lowerPath.endsWith(".txt")) {
                content = Files.readString(path);
            } else if (lowerPath.endsWith(".pdf")) {
                // For PDF, return a note
                return "PDF content - AI summary generated based on title and genre.";
            } else if (lowerPath.endsWith(".doc") || lowerPath.endsWith(".docx")) {
                return "Document content - AI summary generated based on title and genre.";
            } else {
                return "";
            }
            
            if (content.length() > 300) {
                content = content.substring(0, 300) + "...";
            }
            return content;
            
        } catch (IOException e) {
            return "";
        }
    }
}
