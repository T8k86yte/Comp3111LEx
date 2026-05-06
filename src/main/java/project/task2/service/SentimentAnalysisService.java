package project.task2.service;

import java.util.*;

public class SentimentAnalysisService {
    
    // Positive keywords
    private static final Set<String> POSITIVE_WORDS = new HashSet<>(Arrays.asList(
        "amazing", "awesome", "brilliant", "captivating", "compelling", "creative", "delightful",
        "engaging", "enjoyable", "entertaining", "excellent", "exceptional", "fantastic", "fascinating",
        "great", "helpful", "impressive", "incredible", "informative", "inspiring", "interesting",
        "love", "lovely", "magnificent", "masterpiece", "must-read", "outstanding", "perfect",
        "powerful", "refreshing", "remarkable", "satisfying", "superb", "terrific", "thorough",
        "thought-provoking", "unique", "wonderful", "worth", "recommend", "highly recommend"
    ));
    
    // Negative keywords
    private static final Set<String> NEGATIVE_WORDS = new HashSet<>(Arrays.asList(
        "awful", "bad", "boring", "confusing", "disappointing", "dull", "failed", "frustrating",
        "hate", "horrible", "lack", "lacking", "mediocre", "poor", "predictable", "slow",
        "tedious", "terrible", "unappealing", "unconvincing", "uninteresting", "unreadable",
        "waste", "weak", "worst", "not recommend", "wouldn't recommend"
    ));
    
    public enum Sentiment {
        POSITIVE("😊 Positive", "#10b981"),
        NEUTRAL("😐 Neutral", "#f59e0b"),
        NEGATIVE("😞 Negative", "#ef4444");
        
        private final String displayName;
        private final String color;
        
        Sentiment(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }
        
        public String getDisplayName() { return displayName; }
        public String getColor() { return color; }
    }
    
    /**
     * Analyze sentiment of a review comment
     */
    public Sentiment analyzeSentiment(String comment) {
        if (comment == null || comment.isEmpty()) {
            return Sentiment.NEUTRAL;
        }
        
        String lowerComment = comment.toLowerCase();
        int positiveCount = 0;
        int negativeCount = 0;
        
        for (String word : POSITIVE_WORDS) {
            if (lowerComment.contains(word)) {
                positiveCount++;
            }
        }
        
        for (String word : NEGATIVE_WORDS) {
            if (lowerComment.contains(word)) {
                negativeCount++;
            }
        }
        
        // Check rating context (if available in comment)
        if (lowerComment.matches(".*\\b[1-2]\\b.*")) {
            negativeCount += 1;
        }
        if (lowerComment.matches(".*\\b[4-5]\\b.*")) {
            positiveCount += 1;
        }
        
        if (positiveCount > negativeCount) {
            return Sentiment.POSITIVE;
        } else if (negativeCount > positiveCount) {
            return Sentiment.NEGATIVE;
        } else {
            // Check for emojis or strong indicators
            if (lowerComment.contains("👍") || lowerComment.contains("❤️") || lowerComment.contains("⭐")) {
                return Sentiment.POSITIVE;
            }
            if (lowerComment.contains("👎") || lowerComment.contains("💔")) {
                return Sentiment.NEGATIVE;
            }
            return Sentiment.NEUTRAL;
        }
    }
    
    /**
     * Get sentiment score (-100 to +100)
     */
    public int getSentimentScore(String comment) {
        Sentiment sentiment = analyzeSentiment(comment);
        switch (sentiment) {
            case POSITIVE: return 50 + new Random().nextInt(50);
            case NEGATIVE: return -50 - new Random().nextInt(50);
            default: return -10 + new Random().nextInt(20);
        }
    }
}
