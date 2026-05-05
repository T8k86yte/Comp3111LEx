package project.task2.service;

import project.task2.model.BookSubmission;
import project.task2.model.BookStats;
import project.task2.repo.SubmissionRepository;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that reads real borrow records from Task 1's data files
 */
public class RealTrendDataService {
    
    private static final String BORROW_RECORDS_FILE = "data/task1/borrow_records.txt";
    private final SubmissionRepository submissionRepository;
    
    public RealTrendDataService() {
        this.submissionRepository = new SubmissionRepository();
    }
    
    /**
     * Get real borrow trend data for an author's books
     */
    public Map<LocalDate, Integer> getRealBorrowTrends(String authorUsername, int days) {
        Map<LocalDate, Integer> borrowCounts = new HashMap<>();
        
        // Initialize all dates in range with 0
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            borrowCounts.put(date, 0);
        }
        
        // Get all approved books by this author
        List<BookSubmission> authorBooks = submissionRepository.findByAuthor(authorUsername);
        Set<String> authorBookTitles = authorBooks.stream()
                .filter(BookSubmission::isApproved)
                .map(BookSubmission::getTitle)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        
        if (authorBookTitles.isEmpty()) {
            System.out.println("No approved books found for author: " + authorUsername);
            return borrowCounts;
        }
        
        // Read real borrow records from Task 1's file
        Path path = Paths.get(BORROW_RECORDS_FILE);
        if (!Files.exists(path)) {
            System.out.println("Borrow records file not found: " + BORROW_RECORDS_FILE);
            return borrowCounts;
        }
        
        try {
            List<String> lines = Files.readAllLines(path);
            System.out.println("📖 Reading " + lines.size() + " borrow records from Task 1");
            
            for (String line : lines) {
                if (line == null || line.trim().isEmpty()) continue;
                
                String[] parts = line.split("\\|", -1);
                if (parts.length < 7) continue;
                
                // Decode the fields
                String username = decode(parts[0]);
                String bookId = decode(parts[1]);
                String bookTitle = decode(parts[2]);
                LocalDate borrowDate = LocalDate.parse(parts[3]);
                
                // Check if this borrow is within our date range
                if (borrowDate.isBefore(startDate) || borrowDate.isAfter(endDate)) {
                    continue;
                }
                
                // Check if this book belongs to our author
                if (authorBookTitles.contains(bookTitle.toLowerCase())) {
                    borrowCounts.put(borrowDate, borrowCounts.getOrDefault(borrowDate, 0) + 1);
                }
            }
            
            int totalBorrows = borrowCounts.values().stream().mapToInt(Integer::intValue).sum();
            System.out.println("✅ Found " + totalBorrows + " real borrows for author's books in the last " + days + " days");
            
        } catch (IOException e) {
            System.err.println("Error reading borrow records: " + e.getMessage());
        }
        
        return borrowCounts;
    }
    
    /**
     * Get top books by real borrow count
     */
    public List<BookStats> getTopBooksByRealBorrows(String authorUsername, int limit) {
        List<BookSubmission> submissions = submissionRepository.findByAuthor(authorUsername);
        Map<String, Integer> borrowCounts = new HashMap<>();
        
        // Initialize borrow counts for each book
        for (BookSubmission sub : submissions) {
            if (sub.isApproved()) {
                borrowCounts.put(sub.getTitle(), 0);
            }
        }
        
        // Read real borrow records
        Path path = Paths.get(BORROW_RECORDS_FILE);
        if (Files.exists(path)) {
            try {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    if (line == null || line.trim().isEmpty()) continue;
                    String[] parts = line.split("\\|", -1);
                    if (parts.length < 3) continue;
                    
                    String bookTitle = decode(parts[2]);
                    if (borrowCounts.containsKey(bookTitle)) {
                        borrowCounts.put(bookTitle, borrowCounts.get(bookTitle) + 1);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error reading borrow records: " + e.getMessage());
            }
        }
        
        // Convert to BookStats list and sort
        List<BookStats> result = new ArrayList<>();
        for (BookSubmission sub : submissions) {
            if (sub.isApproved()) {
                result.add(new BookStats(
                    sub.getSubmissionId(),
                    sub.getTitle(),
                    borrowCounts.getOrDefault(sub.getTitle(), 0),
                    0, 0, 0, sub.getStatus()
                ));
            }
        }
        
        result.sort((a, b) -> Integer.compare(b.getBorrowCount(), a.getBorrowCount()));
        return result.stream().limit(limit).collect(Collectors.toList());
    }
    
    private String decode(String value) {
        try {
            return new String(Base64.getDecoder().decode(value), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
