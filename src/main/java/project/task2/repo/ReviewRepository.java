package project.task2.repo;

import project.task2.model.Review;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ReviewRepository {
    private static final String REVIEWS_FILE = "data/reviews.txt";
    private final Map<String, Review> reviewsById = new ConcurrentHashMap<>();

    public ReviewRepository() {
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
            Path path = Paths.get(REVIEWS_FILE);
            if (Files.exists(path)) {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        Review review = fromString(line);
                        if (review != null) {
                            reviewsById.put(review.getReviewId(), review);
                        }
                    }
                }
                System.out.println("Loaded " + reviewsById.size() + " reviews");
            }
        } catch (IOException e) {
            System.err.println("Error loading reviews: " + e.getMessage());
        }
    }

    private void saveToFile() {
        try {
            List<String> lines = new ArrayList<>();
            for (Review review : reviewsById.values()) {
                lines.add(toString(review));
            }
            Path path = Paths.get(REVIEWS_FILE);
            Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Error saving reviews: " + e.getMessage());
        }
    }

    public void save(Review review) {
        reviewsById.put(review.getReviewId(), review);
        saveToFile();
    }

    public void update(Review review) {
        reviewsById.put(review.getReviewId(), review);
        saveToFile();
    }

    public List<Review> findByBookId(String bookId) {
        return reviewsById.values().stream()
                .filter(r -> r.getBookId().equals(bookId))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public List<Review> findByAuthorBooks(List<String> bookIds) {
        return reviewsById.values().stream()
                .filter(r -> bookIds.contains(r.getBookId()))
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

    private String toString(Review r) {
        return String.join("|",
            r.getReviewId(),
            r.getBookId(),
            r.getBookTitle(),
            r.getReviewerUsername(),
            r.getReviewerFullName(),
            String.valueOf(r.getRating()),
            encode(r.getComment()),
            r.getCreatedAt().toString(),
            r.getAuthorReply() != null ? encode(r.getAuthorReply()) : "",
            r.getReplyDate() != null ? r.getReplyDate().toString() : "",
            String.valueOf(r.isFlagged()),
            r.getFlagReason() != null ? encode(r.getFlagReason()) : ""
        );
    }

    private Review fromString(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length >= 8) {
            Review r = new Review(
                parts[0], parts[1], parts[2], parts[3], parts[4],
                Integer.parseInt(parts[5]),
                decode(parts[6]),
                LocalDateTime.parse(parts[7])
            );
            if (parts.length > 8 && !parts[8].isEmpty()) {
                r.setAuthorReply(decode(parts[8]));
            }
            if (parts.length > 10) {
                r.setFlagged(Boolean.parseBoolean(parts[10]), 
                    parts.length > 11 ? decode(parts[11]) : null);
            }
            return r;
        }
        return null;
    }

    private String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private String decode(String value) {
        return new String(Base64.getDecoder().decode(value), java.nio.charset.StandardCharsets.UTF_8);
    }
}
