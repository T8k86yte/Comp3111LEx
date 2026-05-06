package project.task2.service;

import project.task2.model.Review;
import project.task2.model.BookSubmission;
import project.task2.repo.ReviewRepository;
import project.task2.repo.SubmissionRepository;

import java.util.*;
import java.util.stream.Collectors;

public class FeedbackAnalyticsService {
    
    private final ReviewRepository reviewRepository;
    private final SubmissionRepository submissionRepository;
    private final SentimentAnalysisService sentimentService;
    
    public FeedbackAnalyticsService() {
        this.reviewRepository = new ReviewRepository();
        this.submissionRepository = new SubmissionRepository();
        this.sentimentService = new SentimentAnalysisService();
    }
    
    public FeedbackStats getStatsForAuthor(String authorUsername) {
        List<BookSubmission> books = submissionRepository.findByAuthor(authorUsername);
        List<String> bookIds = books.stream()
                .map(BookSubmission::getSubmissionId)
                .collect(Collectors.toList());
        
        List<Review> allReviews = new ArrayList<>();
        for (String bookId : bookIds) {
            allReviews.addAll(reviewRepository.findByBookId(bookId));
        }
        
        FeedbackStats stats = new FeedbackStats();
        stats.totalReviews = allReviews.size();
        
        Map<String, Integer> sentimentCounts = new HashMap<>();
        sentimentCounts.put("positive", 0);
        sentimentCounts.put("neutral", 0);
        sentimentCounts.put("negative", 0);
        
        Map<Integer, Integer> ratingDistribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingDistribution.put(i, 0);
        }
        
        double ratingSum = 0;
        
        for (Review review : allReviews) {
            SentimentAnalysisService.Sentiment sentiment = sentimentService.analyzeSentiment(review.getComment());
            switch (sentiment) {
                case POSITIVE -> sentimentCounts.put("positive", sentimentCounts.get("positive") + 1);
                case NEGATIVE -> sentimentCounts.put("negative", sentimentCounts.get("negative") + 1);
                default -> sentimentCounts.put("neutral", sentimentCounts.get("neutral") + 1);
            }
            
            ratingDistribution.put(review.getRating(), ratingDistribution.get(review.getRating()) + 1);
            ratingSum += review.getRating();
        }
        
        stats.sentimentCounts = sentimentCounts;
        stats.ratingDistribution = ratingDistribution;
        stats.averageRating = allReviews.isEmpty() ? 0 : ratingSum / allReviews.size();
        
        // Calculate percentages
        if (stats.totalReviews > 0) {
            stats.positivePercentage = (sentimentCounts.get("positive") * 100.0) / stats.totalReviews;
            stats.neutralPercentage = (sentimentCounts.get("neutral") * 100.0) / stats.totalReviews;
            stats.negativePercentage = (sentimentCounts.get("negative") * 100.0) / stats.totalReviews;
        }
        
        return stats;
    }
    
    public static class FeedbackStats {
        public int totalReviews;
        public double averageRating;
        public Map<String, Integer> sentimentCounts;
        public Map<Integer, Integer> ratingDistribution;
        public double positivePercentage;
        public double neutralPercentage;
        public double negativePercentage;
    }
}
