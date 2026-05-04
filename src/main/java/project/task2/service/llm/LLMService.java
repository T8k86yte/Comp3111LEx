package project.task2.service.llm;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class LLMService {
    
    private static final String DEEPSEEK_API_KEY = System.getenv("DEEPSEEK_API_KEY");
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
    private static final Gson gson = new Gson();
    
    public enum SummaryStyle {
        SHORT(
            "Short (1 sentence)", 
            "One-line summary - quick preview", 
            "Write EXACTLY ONE sentence (max 25 words) that captures the core essence of this book. Be extremely concise. No extra text, just one sentence."
        ),
        MEDIUM(
            "Medium (2-3 paragraphs)", 
            "Standard summary with key details and themes",
            "Write a medium-length summary of 2-3 paragraphs. Include:\n" +
            "- Paragraph 1: Main plot or core idea (2-3 sentences)\n" +
            "- Paragraph 2: Key themes and what readers will learn (2-3 sentences)\n" +
            "- Paragraph 3 (optional): Why this book matters (1-2 sentences)\n" +
            "Be informative but not overly detailed."
        ),
        DETAILED(
            "Detailed (Full analysis)", 
            "Comprehensive summary with themes, characters, and critical analysis",
            "Write a DETAILED, comprehensive summary (8-12 sentences / 150-250 words) with:\n\n" +
            "1. Opening: Hook and context (2 sentences)\n" +
            "2. Core content: Main arguments or narrative arc (3-4 sentences)\n" +
            "3. Themes analysis: Deeper meaning and key takeaways (2-3 sentences)\n" +
            "4. Character/Subject analysis (if applicable) (2 sentences)\n" +
            "5. Closing: Overall significance and recommendation (1-2 sentences)\n\n" +
            "Be thorough, insightful, and provide genuine depth."
        );
        
        private final String displayName;
        private final String description;
        private final String promptSuffix;
        
        SummaryStyle(String displayName, String description, String promptSuffix) {
            this.displayName = displayName;
            this.description = description;
            this.promptSuffix = promptSuffix;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public String getPromptSuffix() { return promptSuffix; }
    }
    
    public String generateSummary(String title, String genre, String contentPreview, SummaryStyle style) {
        // Try DeepSeek API first
        if (DEEPSEEK_API_KEY != null && !DEEPSEEK_API_KEY.isEmpty()) {
            String result = generateWithDeepSeek(title, genre, contentPreview, style);
            if (result != null && !result.isEmpty()) {
                return result;
            }
        }
        
        // Fallback to enhanced mock generation
        System.out.println("⚠️ DeepSeek API not available. Using fallback mock generation.");
        return generateMockSummary(title, genre, contentPreview, style);
    }
    
    public String generateSummaryFromFile(String filePath, String title, String genre, SummaryStyle style) {
        String contentPreview = extractFilePreview(filePath);
        return generateSummary(title, genre, contentPreview, style);
    }
    
    private String generateWithDeepSeek(String title, String genre, String contentPreview, SummaryStyle style) {
        try {
            String prompt = buildPrompt(title, genre, contentPreview, style);
            
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", "deepseek-chat");
            requestBody.addProperty("temperature", 0.7);
            
            // Adjust max_tokens based on style
            int maxTokens = style == SummaryStyle.SHORT ? 60 : (style == SummaryStyle.MEDIUM ? 300 : 600);
            requestBody.addProperty("max_tokens", maxTokens);
            
            JsonArray messages = new JsonArray();
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", 
                "You are a professional book summarizer. Follow the instructions exactly.\n" +
                "For SHORT: ONE sentence only, max 25 words.\n" +
                "For MEDIUM: 2-3 paragraphs.\n" +
                "For DETAILED: 8-12 sentences with analysis.\n" +
                "Return ONLY the summary, no introductory phrases like 'Here is a summary'."
            );
            messages.add(systemMessage);
            
            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", prompt);
            messages.add(userMessage);
            
            requestBody.add("messages", messages);
            
            System.out.println("📡 Calling DeepSeek API for " + style.getDisplayName() + " summary...");
            
            Request request = new Request.Builder()
                    .url("https://api.deepseek.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + DEEPSEEK_API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(gson.toJson(requestBody), MediaType.parse("application/json")))
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "";
                
                if (response.isSuccessful()) {
                    JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                    String summary = json.getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString()
                            .trim();
                    System.out.println("✅ DeepSeek generated " + style.getDisplayName() + " summary");
                    return summary;
                } else {
                    System.err.println("DeepSeek API error: " + response.code() + " - " + responseBody);
                    return null;
                }
            }
        } catch (Exception e) {
            System.err.println("DeepSeek API error: " + e.getMessage());
            return null;
        }
    }
    
    private String buildPrompt(String title, String genre, String contentPreview, SummaryStyle style) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Book Title: \"").append(title).append("\"\n");
        prompt.append("Genre: ").append(genre).append("\n");
        
        if (contentPreview != null && !contentPreview.isEmpty() && contentPreview.length() > 50) {
            String preview = contentPreview.length() > 800 ? contentPreview.substring(0, 800) : contentPreview;
            prompt.append("\nBook Content Preview:\n").append(preview).append("\n");
        }
        
        prompt.append("\n").append(style.getPromptSuffix());
        prompt.append("\n\nGenerate the summary now:");
        
        return prompt.toString();
    }
    
    private String generateMockSummary(String title, String genre, String contentPreview, SummaryStyle style) {
        switch (style) {
            case SHORT:
                return generateMockShort(title, genre);
            case DETAILED:
                return generateMockDetailed(title, genre);
            default:
                return generateMockMedium(title, genre);
        }
    }
    
    private String generateMockShort(String title, String genre) {
        String[] openers = {
            "A powerful exploration of", "An eye-opening journey through",
            "A compelling look at", "A masterful examination of"
        };
        String[] endings = {
            "that will change how you think about the genre.",
            "that delivers a memorable reading experience.",
            "that stands out in its field.",
            "that offers fresh perspectives."
        };
        
        return openers[new Random().nextInt(openers.length)] + " \"" + title + "\" " + endings[new Random().nextInt(endings.length)];
    }
    
    private String generateMockMedium(String title, String genre) {
        return "\"" + title + "\" offers a " + (genre.isEmpty() ? "compelling" : genre.toLowerCase()) + 
               " narrative that engages readers from the first page. The author skillfully develops the central themes, " +
               "creating a cohesive exploration of the subject matter. " +
               "This work successfully balances depth with accessibility, making it valuable for both newcomers and " +
               "experienced readers in the genre.";
    }
    
    private String generateMockDetailed(String title, String genre) {
        return "\"" + title + "\" is a significant contribution to the " + (genre.isEmpty() ? "literary" : genre.toLowerCase()) + 
               " genre. The author demonstrates strong command of the subject matter, weaving together narrative threads " +
               "that resonate throughout the work.\n\n" +
               "The book's greatest strength lies in its ability to connect thematic elements with real-world implications. " +
               "Key themes include personal growth, societal impact, and the universal human experience. " +
               "Each chapter builds naturally upon the last, creating a satisfying narrative arc.\n\n" +
               "Character/Subject development is handled with care and attention to detail. The author's voice remains " +
               "consistent and engaging throughout. This book is highly recommended for readers seeking both entertainment " +
               "and intellectual stimulation. It will likely become a reference point for future works in this category.";
    }
    
    private String extractFilePreview(String filePath) {
        if (filePath == null || filePath.isEmpty()) return "";
        
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) return "";
            
            String lowerPath = filePath.toLowerCase();
            
            if (lowerPath.endsWith(".txt")) {
                String content = Files.readString(path);
                if (content.length() > 800) {
                    content = content.substring(0, 800);
                }
                return content;
            } else if (lowerPath.endsWith(".pdf")) {
                return "PDF content detected. Using title and genre for summary generation.";
            } else if (lowerPath.endsWith(".doc") || lowerPath.endsWith(".docx")) {
                return "Word document detected. Using title and genre for summary generation.";
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        return "";
    }
    
    public List<SummaryStyle> getAvailableStyles() {
        return Arrays.asList(SummaryStyle.values());
    }
    
    public static String getAPIStatus() {
        if (System.getenv("DEEPSEEK_API_KEY") != null && !System.getenv("DEEPSEEK_API_KEY").isEmpty()) {
            return "🤖 DeepSeek API Connected - 3 Distinct Styles Available";
        } else {
            return "⚠️ Mock Mode - Set DEEPSEEK_API_KEY for real AI";
        }
    }
}
