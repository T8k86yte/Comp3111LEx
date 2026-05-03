package project.task2.service.llm;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * LLM Service for generating book summaries with multiple styles.
 * Supports: Short, Medium, and Detailed summaries.
 */
public class LLMService {
    
    private static final Random RANDOM = new Random();
    
    public enum SummaryStyle {
        SHORT("Short (1-2 sentences)", "Brief overview for quick preview"),
        MEDIUM("Medium (2-3 sentences)", "Standard summary with key details"),
        DETAILED("Detailed (4-5 sentences)", "Comprehensive summary with themes and analysis");
        
        private final String displayName;
        private final String description;
        
        SummaryStyle(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    /**
     * Generate a book summary with specified style
     */
    public String generateSummary(String title, String genre, String contentPreview, SummaryStyle style) {
        switch (style) {
            case SHORT:
                return generateShortSummary(title, genre, contentPreview);
            case DETAILED:
                return generateDetailedSummary(title, genre, contentPreview);
            default:
                return generateMediumSummary(title, genre, contentPreview);
        }
    }
    
    /**
     * Generate summary from uploaded file with specified style
     */
    public String generateSummaryFromFile(String filePath, String title, String genre, SummaryStyle style) {
        String contentPreview = extractFilePreview(filePath);
        return generateSummary(title, genre, contentPreview, style);
    }
    
    /**
     * Short Summary (1-2 sentences) - Brief overview
     */
    private String generateShortSummary(String title, String genre, String contentPreview) {
        String bookTitle = (title == null || title.isEmpty()) ? "This book" : "\"" + title + "\"";
        String hook = getShortHook(genre);
        
        return String.format("%s %s is a %s that %s.",
            hook,
            bookTitle,
            getGenreDescriptor(genre),
            getShortTheme()
        );
    }
    
    /**
     * Medium Summary (2-3 sentences) - Standard summary
     */
    private String generateMediumSummary(String title, String genre, String contentPreview) {
        String bookTitle = (title == null || title.isEmpty()) ? "This book" : "\"" + title + "\"";
        String hook = getMediumHook(genre);
        String body = getGenreSpecificContent(genre);
        String closing = getClosingStatement();
        
        return String.format("%s %s. %s %s",
            hook,
            bookTitle,
            body,
            closing
        );
    }
    
    /**
     * Detailed Summary (4-5 sentences) - Comprehensive with analysis
     */
    private String generateDetailedSummary(String title, String genre, String contentPreview) {
        String bookTitle = (title == null || title.isEmpty()) ? "This book" : "\"" + title + "\"";
        String opening = getDetailedOpening(genre);
        String theme = getDetailedTheme();
        String character = getCharacterInsight(genre);
        String impact = getImpactStatement();
        
        return String.format("%s %s %s %s %s",
            opening,
            bookTitle,
            theme,
            character,
            impact
        );
    }
    
    // ========== SHORT SUMMARY HELPERS ==========
    
    private String getShortHook(String genre) {
        String g = genre == null ? "" : genre.toLowerCase();
        if (g.contains("mystery") || g.contains("thriller")) {
            return "A gripping mystery unfolds as";
        }
        if (g.contains("romance")) {
            return "A touching love story awaits when";
        }
        if (g.contains("fantasy")) {
            return "Enter a magical world where";
        }
        if (g.contains("science") || g.contains("tech")) {
            return "Explore the future where";
        }
        return "Discover how";
    }
    
    private String getGenreDescriptor(String genre) {
        String g = genre == null ? "" : genre.toLowerCase();
        if (g.contains("fiction")) return "captivating work of fiction";
        if (g.contains("non-fiction")) return "insightful non-fictional narrative";
        if (g.contains("science")) return "visionary science fiction tale";
        if (g.contains("fantasy")) return "enchanting fantasy adventure";
        if (g.contains("mystery")) return "clever mystery";
        if (g.contains("romance")) return "heartwarming romance";
        if (g.contains("biography")) return "intimate biography";
        if (g.contains("history")) return "fascinating historical account";
        return "compelling literary work";
    }
    
    private String getShortTheme() {
        String[] themes = {
            "explores themes of growth and self-discovery",
            "challenges readers to see the world differently",
            "captures the essence of human resilience",
            "reveals unexpected truths about life and love"
        };
        return themes[RANDOM.nextInt(themes.length)];
    }
    
    // ========== MEDIUM SUMMARY HELPERS ==========
    
    private String getMediumHook(String genre) {
        String g = genre == null ? "" : genre.toLowerCase();
        if (g.contains("mystery") || g.contains("thriller")) {
            return "A suspenseful journey begins in";
        }
        if (g.contains("romance")) {
            return "An emotional journey unfolds in";
        }
        if (g.contains("fantasy")) {
            return "A breathtaking adventure awaits in";
        }
        if (g.contains("science") || g.contains("tech")) {
            return "A visionary exploration of tomorrow begins in";
        }
        return "A compelling narrative awaits in";
    }
    
    private String getGenreSpecificContent(String genre) {
        if (genre == null || genre.isEmpty()) {
            return "The narrative flows with elegance and purpose.";
        }
        
        String g = genre.toLowerCase();
        if (g.contains("science") || g.contains("tech")) {
            return "Cutting-edge concepts and ethical dilemmas intertwine, pushing the boundaries of imagination.";
        }
        if (g.contains("fantasy")) {
            return "Mythical creatures, ancient magic, and heroic quests bring this world to vibrant life.";
        }
        if (g.contains("mystery") || g.contains("thriller")) {
            return "Twists and turns keep readers guessing until the final, satisfying revelation.";
        }
        if (g.contains("romance")) {
            return "The emotional journey captures both the joy and pain of opening one's heart.";
        }
        if (g.contains("history")) {
            return "Meticulously researched details bring past eras to vivid, compelling reality.";
        }
        if (g.contains("biography")) {
            return "The author's life story unfolds with honesty, tenderness, and inspiring resilience.";
        }
        
        return "The writing is both accessible and profound, resonating with readers of all backgrounds.";
    }
    
    private String getClosingStatement() {
        String[] closings = {
            "A must-read for enthusiasts of the genre.",
            "This work will leave a lasting impression on its readers.",
            "Highly recommended for those seeking an enriching literary experience.",
            "An unforgettable addition to any library collection.",
            "Readers will find themselves returning to this book time and again."
        };
        return closings[RANDOM.nextInt(closings.length)];
    }
    
    // ========== DETAILED SUMMARY HELPERS ==========
    
    private String getDetailedOpening(String genre) {
        String g = genre == null ? "" : genre.toLowerCase();
        if (g.contains("mystery") || g.contains("thriller")) {
            return "From the very first page, readers are drawn into a world of suspense and intrigue.";
        }
        if (g.contains("romance")) {
            return "Set against a backdrop of emotion and connection, this story captures the heart.";
        }
        if (g.contains("fantasy")) {
            return "In a realm where magic breathes life into every corner, an epic quest begins.";
        }
        if (g.contains("science") || g.contains("tech")) {
            return "At the intersection of innovation and humanity, a thought-provoking narrative emerges.";
        }
        return "With masterful storytelling and vivid prose,";
    }
    
    private String getDetailedTheme() {
        String[] themes = {
            "delves deep into the complexities of human nature and relationships,",
            "explores profound themes of identity, belonging, and purpose,",
            "examines the delicate balance between hope and despair in modern life,",
            "weaves together threads of love, loss, and redemption into a rich tapestry,"
        };
        return themes[RANDOM.nextInt(themes.length)];
    }
    
    private String getCharacterInsight(String genre) {
        String g = genre == null ? "" : genre.toLowerCase();
        if (g.contains("mystery") || g.contains("thriller")) {
            return "The characters are brilliantly developed, each harboring secrets that slowly unravel.";
        }
        if (g.contains("romance")) {
            return "The protagonists are wonderfully authentic, their journey resonating with raw emotion.";
        }
        if (g.contains("fantasy")) {
            return "From unlikely heroes to formidable villains, every character serves a purpose in this grand tapestry.";
        }
        return "The characters are crafted with care, each bringing unique perspectives to the narrative.";
    }
    
    private String getImpactStatement() {
        String[] impacts = {
            "This book will linger in your thoughts long after the final page.",
            "A transformative reading experience that challenges and inspires.",
            "Both entertaining and thought-provoking, this work deserves a prominent place on any bookshelf.",
            "An impressive achievement that showcases the author's remarkable storytelling abilities."
        };
        return impacts[RANDOM.nextInt(impacts.length)];
    }
    
    // ========== UTILITY METHODS ==========
    
    private String extractFilePreview(String filePath) {
        if (filePath == null || filePath.isEmpty()) return "";
        
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) return "";
            
            String lowerPath = filePath.toLowerCase();
            
            if (lowerPath.endsWith(".txt")) {
                String content = Files.readString(path);
                if (content.length() > 500) {
                    content = content.substring(0, 500);
                }
                return content;
            } else if (lowerPath.endsWith(".pdf")) {
                return "PDF content - summary generated based on title and genre.";
            } else if (lowerPath.endsWith(".doc") || lowerPath.endsWith(".docx")) {
                return "Document content - summary generated based on title and genre.";
            }
        } catch (IOException e) {
            // Silent fail
        }
        return "";
    }
    
    /**
     * Get available summary styles
     */
    public List<SummaryStyle> getAvailableStyles() {
        return Arrays.asList(SummaryStyle.values());
    }
    
    /**
     * Preview a summary style (for demonstration)
     */
    public String previewStyle(SummaryStyle style, String title, String genre) {
        return generateSummary(title, genre, "", style);
    }
}
