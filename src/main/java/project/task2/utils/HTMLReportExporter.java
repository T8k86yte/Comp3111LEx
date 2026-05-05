package project.task2.utils;

import project.task2.model.BookStats;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class HTMLReportExporter {
    
    public static void exportToHTML(String filePath, String authorName, String authorUsername,
                                     List<BookStats> stats, int totalBorrows, double avgRating, 
                                     int totalReviews, Map<LocalDate, Integer> trendData) throws IOException {
        
        // Change extension to .html
        String htmlPath = filePath;
        if (filePath.endsWith(".pdf")) {
            htmlPath = filePath.substring(0, filePath.length() - 4) + ".html";
        }
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(htmlPath))) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html>");
            writer.println("<head>");
            writer.println("<meta charset=\"UTF-8\">");
            writer.println("<title>Author Statistics Report</title>");
            writer.println("<style>");
            writer.println("* { margin: 0; padding: 0; box-sizing: border-box; }");
            writer.println("body { font-family: 'Segoe UI', Arial, sans-serif; background: #f0f4f8; padding: 40px; }");
            writer.println(".report-container { max-width: 1200px; margin: 0 auto; background: white; border-radius: 16px; box-shadow: 0 20px 40px rgba(0,0,0,0.1); overflow: hidden; }");
            writer.println(".header { background: linear-gradient(135deg, #1e3c72, #2a5298); color: white; padding: 40px; text-align: center; }");
            writer.println(".header h1 { font-size: 32px; margin-bottom: 10px; }");
            writer.println(".header p { font-size: 14px; opacity: 0.9; margin: 5px 0; }");
            writer.println(".content { padding: 40px; }");
            writer.println("h2 { color: #2c3e50; border-left: 4px solid #3498db; padding-left: 15px; margin: 30px 0 20px 0; font-size: 22px; }");
            writer.println(".summary-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; margin: 20px 0; }");
            writer.println(".summary-card { background: linear-gradient(135deg, #667eea, #764ba2); color: white; padding: 20px; border-radius: 12px; text-align: center; }");
            writer.println(".summary-card:nth-child(2) { background: linear-gradient(135deg, #f093fb, #f5576c); }");
            writer.println(".summary-card:nth-child(3) { background: linear-gradient(135deg, #4facfe, #00f2fe); }");
            writer.println(".summary-card:nth-child(4) { background: linear-gradient(135deg, #43e97b, #38f9d7); }");
            writer.println(".summary-card h3 { font-size: 14px; opacity: 0.9; margin-bottom: 10px; }");
            writer.println(".summary-card .value { font-size: 32px; font-weight: bold; }");
            writer.println("table { width: 100%; border-collapse: collapse; margin: 20px 0; }");
            writer.println("th { background: #34495e; color: white; padding: 12px; text-align: left; font-weight: 600; }");
            writer.println("td { padding: 10px 12px; border-bottom: 1px solid #e0e0e0; }");
            writer.println("tr:hover { background: #f5f5f5; }");
            writer.println(".status-approved { color: #27ae60; font-weight: bold; }");
            writer.println(".status-pending { color: #f39c12; font-weight: bold; }");
            writer.println(".status-rejected { color: #e74c3c; font-weight: bold; }");
            writer.println(".trend-chart { background: #f8f9fa; padding: 20px; border-radius: 8px; margin: 20px 0; }");
            writer.println(".trend-bar { display: flex; align-items: center; margin: 5px 0; }");
            writer.println(".trend-date { width: 100px; font-size: 12px; }");
            writer.println(".trend-bar-fill { background: linear-gradient(90deg, #3498db, #2980b9); height: 25px; border-radius: 4px; color: white; line-height: 25px; padding-left: 8px; font-size: 12px; }");
            writer.println(".footer { background: #f8f9fa; padding: 20px; text-align: center; color: #7f8c8d; font-size: 12px; border-top: 1px solid #e0e0e0; }");
            writer.println("@media print { body { background: white; padding: 0; } .no-print { display: none; } .report-container { box-shadow: none; } }");
            writer.println("</style>");
            writer.println("</head>");
            writer.println("<body>");
            writer.println("<div class=\"report-container\">");
            writer.println("<div class=\"header\">");
            writer.println("<h1>📊 AUTHOR STATISTICS REPORT</h1>");
            writer.println("<p>Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "</p>");
            writer.println("<p>Author: " + authorName + " (" + authorUsername + ")</p>");
            writer.println("</div>");
            writer.println("<div class=\"content\">");
            
            // Summary Cards
            writer.println("<div class=\"summary-grid\">");
            writer.println("<div class=\"summary-card\"><h3>📚 Total Books</h3><div class=\"value\">" + stats.size() + "</div></div>");
            writer.println("<div class=\"summary-card\"><h3>📖 Total Borrows</h3><div class=\"value\">" + totalBorrows + "</div></div>");
            writer.println("<div class=\"summary-card\"><h3>⭐ Average Rating</h3><div class=\"value\">" + String.format("%.1f", avgRating) + " / 5</div></div>");
            writer.println("<div class=\"summary-card\"><h3>💬 Total Reviews</h3><div class=\"value\">" + totalReviews + "</div></div>");
            writer.println("</div>");
            
            // Book Details Table
            writer.println("<h2>📖 Book Details</h2>");
            writer.println("</table>");
            writer.println("<tr><th>#</th><th>Title</th><th>Borrows</th><th>Rating</th><th>Reviews</th><th>Status</th></tr>");
            
            int count = 1;
            for (BookStats stat : stats) {
                String statusClass = "";
                if (stat.getStatus().equals("APPROVED")) statusClass = "status-approved";
                else if (stat.getStatus().equals("PENDING")) statusClass = "status-pending";
                else if (stat.getStatus().equals("REJECTED")) statusClass = "status-rejected";
                
                writer.println("<tr>");
                writer.println("<td>" + count++ + "</td>");
                writer.println("<td>" + stat.getTitle() + "</td>");
                writer.println("<td>" + stat.getBorrowCount() + "</td>");
                writer.println("<td>" + String.format("%.1f", stat.getAverageRating()) + "</td>");
                writer.println("<td>" + stat.getReviewCount() + "</td>");
                writer.println("<td class=\"" + statusClass + "\">" + stat.getStatus() + "</td>");
                writer.println("</tr>");
            }
            writer.println("</table>");
            
            // Trend Chart
            if (trendData != null && !trendData.isEmpty()) {
                writer.println("<h2>📈 Borrowing Trends (Last 30 Days)</h2>");
                writer.println("<div class=\"trend-chart\">");
                
                int maxValue = trendData.values().stream().max(Integer::compare).orElse(1);
                LocalDate today = LocalDate.now();
                
                for (int i = 30; i >= 0; i--) {
                    LocalDate date = today.minusDays(i);
                    int value = trendData.getOrDefault(date, 0);
                    int percentage = (int)((value * 100.0) / maxValue);
                    
                    writer.println("<div class=\"trend-bar\">");
                    writer.println("<div class=\"trend-date\">" + date.toString() + "</div>");
                    writer.println("<div style=\"flex: 1; margin-left: 10px;\">");
                    writer.println("<div class=\"trend-bar-fill\" style=\"width: " + percentage + "%;\">" + (value > 0 ? value : "") + "</div>");
                    writer.println("</div>");
                    writer.println("</div>");
                }
                
                writer.println("</div>");
            }
            
            // Status Legend
            writer.println("<div style=\"margin-top: 30px; padding: 15px; background: #f8f9fa; border-radius: 8px;\">");
            writer.println("<strong>📌 Status Legend:</strong><br>");
            writer.println("<span class=\"status-approved\">● APPROVED</span> - Book is available in library<br>");
            writer.println("<span class=\"status-pending\">● PENDING</span> - Under review by librarian<br>");
            writer.println("<span class=\"status-rejected\">● REJECTED</span> - Not approved for publication");
            writer.println("</div>");
            
            writer.println("</div>");
            writer.println("<div class=\"footer\">");
            writer.println("<p>End of Report - Generated by Library Management System</p>");
            writer.println("<p class=\"no-print\" style=\"margin-top: 10px;\">💡 Tip: Press Ctrl+P (Windows) or Cmd+P (Mac) to save as PDF</p>");
            writer.println("</div>");
            writer.println("</div>");
            writer.println("</body>");
            writer.println("</html>");
        }
        
        System.out.println("✅ HTML report generated at: " + htmlPath);
        System.out.println("💡 Open in browser and use Print > Save as PDF to get a PDF file");
    }
}
