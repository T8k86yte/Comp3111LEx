package project.task2.service;

import java.util.*;

public class ReplyTemplateService {
    
    private static final Map<String, String> TEMPLATES = new LinkedHashMap<>();
    
    static {
        // Positive review templates
        TEMPLATES.put("😊 Thank you for your kind review!", 
            "Thank you so much for your wonderful feedback! I'm thrilled that you enjoyed the book. Your support means a lot to me!");
        
        TEMPLATES.put("⭐ Appreciate your positive feedback", 
            "Thank you for your thoughtful review! I'm glad the book resonated with you. I hope my future works will continue to meet your expectations.");
        
        // Neutral review templates
        TEMPLATES.put("📖 Thank you for your honest feedback", 
            "Thank you for taking the time to share your thoughts. I appreciate your honest feedback and will consider your points for future improvements.");
        
        TEMPLATES.put("🤔 Thanks for your balanced perspective", 
            "Thank you for your balanced review. I value all feedback and will use your insights to grow as an author.");
        
        // Negative review templates
        TEMPLATES.put("🫂 Sorry you didn't enjoy it", 
            "I'm sorry to hear that this book wasn't to your taste. Thank you for giving it a chance and sharing your honest feedback. I'll keep your comments in mind.");
        
        TEMPLATES.put("📝 Thank you for your constructive criticism", 
            "Thank you for your detailed feedback. I take all criticism seriously and will use it to improve my writing. I hope you'll give my future work another chance.");
        
        // General templates
        TEMPLATES.put("💬 General appreciation reply", 
            "Thank you for your review! Reader feedback is invaluable to me as an author. I appreciate you taking the time to share your thoughts.");
        
        TEMPLATES.put("✨ Encouragement reply", 
            "Thank you for your review! Your encouragement motivates me to keep writing and improving. I hope you'll enjoy my future books as well!");
    }
    
    public Map<String, String> getAllTemplates() {
        return TEMPLATES;
    }
    
    public String getTemplate(String key) {
        return TEMPLATES.getOrDefault(key, TEMPLATES.values().iterator().next());
    }
    
    public void addTemplate(String name, String content) {
        TEMPLATES.put(name, content);
    }
}
