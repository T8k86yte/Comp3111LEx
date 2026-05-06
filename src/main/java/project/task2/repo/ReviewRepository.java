package project.task2.repo;

import project.task2.model.Review;
import project.task2.model.BookSubmission;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ReviewRepository {
    private static final String TASK1_REVIEWS_FILE = "data/task1/book_reviews.txt";
    private final Map<String, Review> reviewsById = new ConcurrentHashMap<>();

    public ReviewRepository() {
        createDataDirectory();
        loadFromFile();
    }

    private void createDataDirectory() {
        try {
            Files.createDirectories(Paths.get("data"));
            Files.createDirectories(Paths.get("data/task1"));
        } catch (IOException e) {
            System.err.println("Error creating data directory: " + e.getMessage());
        }
    }

    private void loadFromFile() {
        reviewsById.clear();
        loadFromTask1File();
        System.out.println("📖 从 Task 1 加载了 " + reviewsById.size() + " 条评论");
    }

    private void loadFromTask1File() {
        Path path = Paths.get(TASK1_REVIEWS_FILE);
        if (!Files.exists(path)) {
            System.out.println("⚠️ Task 1 评论文件不存在: " + TASK1_REVIEWS_FILE);
            return;
        }

        try {
            List<String> lines = Files.readAllLines(path);
            System.out.println("📄 读取到 " + lines.size() + " 行评论数据");
            
            for (int lineNum = 0; lineNum < lines.size(); lineNum++) {
                String line = lines.get(lineNum);
                if (line == null || line.trim().isEmpty()) continue;
                
                String[] parts = line.split("\\|", -1);
                System.out.println("行 " + (lineNum+1) + " 有 " + parts.length + " 个字段");
                
                // Task 1 的格式: username|bookId|bookTitle|bookAuthor|bookGenre|rating|reviewText|createdAt|anonymous
                if (parts.length >= 9) {
                    try {
                        String reviewerUsername = decode(parts[0]);
                        String bookId = decode(parts[1]);
                        String bookTitle = decode(parts[2]);
                        String bookAuthor = decode(parts[3]);
                        String bookGenre = decode(parts[4]);
                        int rating = Integer.parseInt(parts[5]);
                        String reviewText = decode(parts[6]);
                        LocalDateTime createdAt = LocalDateTime.parse(parts[7], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        boolean anonymous = Boolean.parseBoolean(parts[8]);
                        
                        System.out.println("  ✅ 解析: " + bookTitle + " 评分: " + rating);
                        
                        String reviewId = "REV_" + bookId + "_" + Math.abs(reviewerUsername.hashCode());
                        String displayName = anonymous ? "匿名用户" : reviewerUsername;
                        
                        // 查找这本书的 submission ID
                        String submissionId = findSubmissionIdByBookDetails(bookTitle, bookAuthor);
                        
                        Review review = new Review(
                            reviewId,
                            submissionId != null ? submissionId : bookId,
                            bookTitle,
                            reviewerUsername,
                            displayName,
                            rating,
                            reviewText,
                            createdAt
                        );
                        
                        reviewsById.put(reviewId, review);
                        
                    } catch (Exception e) {
                        System.err.println("  ❌ 解析失败: " + e.getMessage());
                    }
                } 
                // 兼容旧格式 (8个字段)
                else if (parts.length >= 8) {
                    try {
                        String reviewerUsername = decode(parts[0]);
                        String bookId = decode(parts[1]);
                        String bookTitle = decode(parts[2]);
                        String bookAuthor = decode(parts[3]);
                        int rating = Integer.parseInt(parts[4]);
                        String reviewText = decode(parts[5]);
                        LocalDateTime createdAt = LocalDateTime.parse(parts[6], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        boolean anonymous = Boolean.parseBoolean(parts[7]);
                        
                        System.out.println("  ✅ 解析(旧格式): " + bookTitle + " 评分: " + rating);
                        
                        String reviewId = "REV_" + bookId + "_" + Math.abs(reviewerUsername.hashCode());
                        String displayName = anonymous ? "匿名用户" : reviewerUsername;
                        
                        String submissionId = findSubmissionIdByBookDetails(bookTitle, bookAuthor);
                        
                        Review review = new Review(
                            reviewId,
                            submissionId != null ? submissionId : bookId,
                            bookTitle,
                            reviewerUsername,
                            displayName,
                            rating,
                            reviewText,
                            createdAt
                        );
                        
                        reviewsById.put(reviewId, review);
                        
                    } catch (Exception e) {
                        System.err.println("  ❌ 解析失败(旧格式): " + e.getMessage());
                    }
                }
                else {
                    System.out.println("  ❌ 字段数量不足: " + parts.length);
                }
            }
        } catch (IOException e) {
            System.err.println("读取 Task 1 评论文件失败: " + e.getMessage());
        }
    }

    private String findSubmissionIdByBookDetails(String bookTitle, String bookAuthor) {
        SubmissionRepository submissionRepo = new SubmissionRepository();
        submissionRepo.refreshFromFile();
        
        return submissionRepo.findAll().stream()
            .filter(sub -> sub.isApproved())
            .filter(sub -> sub.getTitle().equalsIgnoreCase(bookTitle))
            .filter(sub -> sub.getAuthorFullName().equalsIgnoreCase(bookAuthor))
            .map(BookSubmission::getSubmissionId)
            .findFirst()
            .orElse(null);
    }

    public List<Review> findByAuthorBooks(List<String> bookIds) {
        List<Review> result = reviewsById.values().stream()
                .filter(r -> bookIds.contains(r.getBookId()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
        
        System.out.println("为作者找到 " + result.size() + " 条评论");
        return result;
    }

    public List<Review> findByBookId(String bookId) {
        return reviewsById.values().stream()
                .filter(r -> r.getBookId().equals(bookId))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public Optional<Review> findById(String reviewId) {
        return Optional.ofNullable(reviewsById.get(reviewId));
    }

    public double getAverageRatingForBook(String bookId) {
        return reviewsById.values().stream()
                .filter(r -> r.getBookId().equals(bookId))
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }

    public int getReviewCountForBook(String bookId) {
        return (int) reviewsById.values().stream()
                .filter(r -> r.getBookId().equals(bookId))
                .count();
    }

    public void refresh() {
        loadFromFile();
    }

    public void save(Review review) {
        reviewsById.put(review.getReviewId(), review);
    }

    public void update(Review review) {
        reviewsById.put(review.getReviewId(), review);
    }

    private String decode(String value) {
        try {
            return new String(Base64.getDecoder().decode(value), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
