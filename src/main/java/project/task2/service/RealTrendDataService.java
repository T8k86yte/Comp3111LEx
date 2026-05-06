package project.task2.service;

import project.task2.model.BookSubmission;
import project.task2.model.BookStats;
import project.task2.repo.SubmissionRepository;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class RealTrendDataService {
    
    private static final String BORROW_RECORDS_FILE = "data/task1/borrow_records.txt";
    private final SubmissionRepository submissionRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    public RealTrendDataService() {
        this.submissionRepository = new SubmissionRepository();
    }
    
    public Map<LocalDate, Integer> getRealBorrowTrends(String authorUsername, int days) {
        Map<LocalDate, Integer> borrowCounts = new HashMap<>();
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            borrowCounts.put(date, 0);
        }
        
        // Get author's approved books
        List<BookSubmission> authorBooks = submissionRepository.findByAuthor(authorUsername);
        Set<String> authorBookTitles = authorBooks.stream()
                .filter(BookSubmission::isApproved)
                .map(BookSubmission::getTitle)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        
        System.out.println("Author's approved books: " + authorBookTitles);
        
        if (authorBookTitles.isEmpty()) {
            System.out.println("No approved books found for author: " + authorUsername);
            return borrowCounts;
        }
        
        Path path = Paths.get(BORROW_RECORDS_FILE);
        if (!Files.exists(path)) {
            System.out.println("Borrow records file not found: " + BORROW_RECORDS_FILE);
            return borrowCounts;
        }
        
        try {
            List<String> lines = Files.readAllLines(path);
            System.out.println("Reading " + lines.size() + " borrow records");
            
            for (String line : lines) {
                if (line == null || line.trim().isEmpty()) continue;
                
                String[] parts = line.split("\\|", -1);
                if (parts.length < 7) continue;
                
                try {
                    // Decode all fields
                    String username = decode(parts[0]);
                    String bookId = decode(parts[1]);
                    String bookTitle = decode(parts[2]);
                    String bookAuthor = decode(parts[3]);
                    String borrowDateStr = parts[5];  // Date is at index 5
                    LocalDate borrowDate = LocalDate.parse(borrowDateStr, DATE_FORMATTER);
                    
                    System.out.println("Borrow record: title='" + bookTitle + "', author='" + bookAuthor + "', date=" + borrowDate);
                    
                    if (borrowDate.isBefore(startDate) || borrowDate.isAfter(endDate)) {
                        continue;
                    }
                    
                    // Match by book title (case-insensitive)
                    if (authorBookTitles.contains(bookTitle.toLowerCase())) {
                        borrowCounts.put(borrowDate, borrowCounts.getOrDefault(borrowDate, 0) + 1);
                        System.out.println("  ✅ Matched: " + bookTitle);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing line: " + e.getMessage());
                }
            }
            
            int totalBorrows = borrowCounts.values().stream().mapToInt(Integer::intValue).sum();
            System.out.println("Found " + totalBorrows + " borrows for author's books");
            
        } catch (IOException e) {
            System.err.println("Error reading borrow records: " + e.getMessage());
        }
        
        return borrowCounts;
    }
    
    public List<BookStats> getTopBooksByRealBorrows(String authorUsername, int limit) {
        List<BookSubmission> submissions = submissionRepository.findByAuthor(authorUsername);
        Map<String, Integer> borrowCounts = new HashMap<>();
        
        for (BookSubmission sub : submissions) {
            if (sub.isApproved()) {
                borrowCounts.put(sub.getTitle(), 0);
            }
        }
        
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
        if (value == null || value.isEmpty()) return "";
        try {
            return new String(Base64.getDecoder().decode(value), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }
}
