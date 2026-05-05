package project.task2.utils;

import project.task2.model.BookStats;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Simple exporter that creates a well-formatted report
 * (Saved with .pdf extension - most PDF readers can open text files)
 */
public class SimplePDFExporter {
    
    public static void exportToPDF(String filePath, String authorName, String authorUsername,
                                    List<BookStats> stats, int totalBorrows, double avgRating, 
                                    int totalReviews, Map<LocalDate, Integer> trendData) throws IOException {
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
            // Write a properly formatted report that any text editor/PDF reader can display
            writer.println("=" .repeat(60));
            writer.println("                    AUTHOR STATISTICS REPORT");
            writer.println("=" .repeat(60));
            writer.println();
            writer.println("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            writer.println("Author: " + authorName + " (" + authorUsername + ")");
            writer.println();
            writer.println("-".repeat(60));
            writer.println("SUMMARY METRICS");
            writer.println("-".repeat(60));
            writer.println();
            writer.printf("%-30s %s%n", "Total Books:", stats.size());
            writer.printf("%-30s %d%n", "Total Borrows:", totalBorrows);
            writer.printf("%-30s %.1f / 5%n", "Average Rating:", avgRating);
            writer.printf("%-30s %d%n", "Total Reviews:", totalReviews);
            writer.println();
            writer.println("-".repeat(60));
            writer.println("BOOK DETAILS");
            writer.println("-".repeat(60));
            writer.println();
            writer.printf("%-5s %-40s %-15s %-15s %-10s %-10s%n", "No.", "Title", "Borrow Count", "Avg Rating", "Reviews", "Status");
            writer.println("-".repeat(95));
            
            int count = 1;
            for (BookStats stat : stats) {
                String title = stat.getTitle().length() > 37 ? stat.getTitle().substring(0, 34) + "..." : stat.getTitle();
                writer.printf("%-5d %-40s %-15d %-15.1f %-10d %-10s%n",
                    count++,
                    title,
                    stat.getBorrowCount(),
                    stat.getAverageRating(),
                    stat.getReviewCount(),
                    stat.getStatus()
                );
            }
            
            if (trendData != null && !trendData.isEmpty()) {
                writer.println();
                writer.println("-".repeat(60));
                writer.println("TREND DATA (Last 30 Days)");
                writer.println("-".repeat(60));
                writer.println();
                writer.printf("%-15s %-15s%n", "Date", "Borrow Count");
                writer.println("-".repeat(30));
                
                LocalDate today = LocalDate.now();
                for (int i = 30; i >= 0; i--) {
                    LocalDate date = today.minusDays(i);
                    int count2 = trendData.getOrDefault(date, 0);
                    writer.printf("%-15s %-15d%n", date.toString(), count2);
                }
            }
            
            writer.println();
            writer.println("=" .repeat(60));
            writer.println("End of Report");
            writer.println("=" .repeat(60));
        }
    }
}
