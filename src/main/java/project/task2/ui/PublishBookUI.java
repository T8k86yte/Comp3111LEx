package project.task2.ui;

import project.task2.model.AuthorAccount;
import project.task2.service.AuthorPortalService;

import java.util.Scanner;

public class PublishBookUI {
    private final AuthorPortalService authorService;
    private final Scanner scanner;
    private final AuthorAccount loggedInAuthor;

    public PublishBookUI(AuthorAccount author) {
        this.authorService = new AuthorPortalService();
        this.scanner = new Scanner(System.in);
        this.loggedInAuthor = author;
    }

    public void start() {
        printHeader();
        
        boolean publishing = true;
        while (publishing) {
            printMenu();
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    publishNewBook();
                    break;
                case "2":
                    viewMySubmissions();
                    break;
                case "0":
                    publishing = false;
                    break;
                default:
                    System.out.println("❌ Invalid option.");
            }
        }
    }

    private void printHeader() {
        System.out.println("\n" + "╔" + "═".repeat(58) + "╗");
        System.out.println("║              PUBLISH NEW BOOK                      ║");
        System.out.println("╚" + "═".repeat(58) + "╝");
        System.out.println("👋 Welcome, " + loggedInAuthor.getFullName() + "!");
    }

    private void printMenu() {
        System.out.println("\n" + "─".repeat(60));
        System.out.println("PUBLISH MENU:");
        System.out.println("  1) Publish a New Book");
        System.out.println("  2) View My Submissions");
        System.out.println("  0) Back to Dashboard");
        System.out.print("Enter your choice: ");
    }

    private void publishNewBook() {
        System.out.println("\n" + "┌" + "─".repeat(58) + "┐");
        System.out.println("│              NEW BOOK SUBMISSION                    │");
        System.out.println("└" + "─".repeat(58) + "┘");
        
        // Get book details
        System.out.print("\n📚 Book Title: ");
        String title = scanner.nextLine().trim();
        
        // Author Name (pre-filled)
        System.out.println("👤 Author Name: " + loggedInAuthor.getFullName() + " (auto-filled)");
        
        // Genre selection
        String genre = selectGenre();
        
        // Description
        System.out.print("📝 Description/Abstract: ");
        String description = scanner.nextLine().trim();
        
        // File path (simulated - in real app would be file chooser)
        System.out.println("\n📁 Book File Upload:");
        System.out.println("   Supported formats: PDF, TXT, DOC, DOCX");
        System.out.print("   Enter file path: ");
        String filePath = scanner.nextLine().trim();

        // Show summary
        System.out.println("\n" + "─".repeat(60));
        System.out.println("📋 SUBMISSION SUMMARY:");
        System.out.println("   Title: " + title);
        System.out.println("   Author: " + loggedInAuthor.getFullName());
        System.out.println("   Genre: " + genre);
        System.out.println("   Description: " + (description.length() > 50 ? 
            description.substring(0, 50) + "..." : description));
        System.out.println("   File: " + filePath);
        System.out.println("─".repeat(60));

        System.out.print("\n❓ Submit for librarian approval? (y/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();

        if (confirm.equals("y")) {
            // Call service to submit book
            var result = authorService.submitBookForApproval(
                loggedInAuthor.getUsername(),
                loggedInAuthor.getFullName(),
                title,
                genre,
                description,
                filePath
            );

            System.out.println("\n" + "═".repeat(60));
            if (result.isSuccess()) {
                System.out.println("✅ " + result.getMessage());
                System.out.println("📬 Your book has been sent to the librarian for review.");
            } else {
                System.out.println("❌ " + result.getMessage());
            }
            System.out.println("═".repeat(60));
        } else {
            System.out.println("\n❌ Submission cancelled.");
        }
    }

    private String selectGenre() {
        System.out.println("\n🎭 Select Genre:");
        System.out.println("   1) Fiction");
        System.out.println("   2) Non-Fiction");
        System.out.println("   3) Science Fiction");
        System.out.println("   4) Fantasy");
        System.out.println("   5) Mystery");
        System.out.println("   6) Biography");
        System.out.println("   7) History");
        System.out.println("   8) Technology");
        System.out.println("   9) Other");
        
        System.out.print("   Choose (1-9): ");
        String choice = scanner.nextLine().trim();
        
        return switch (choice) {
            case "1" -> "Fiction";
            case "2" -> "Non-Fiction";
            case "3" -> "Science Fiction";
            case "4" -> "Fantasy";
            case "5" -> "Mystery";
            case "6" -> "Biography";
            case "7" -> "History";
            case "8" -> "Technology";
            default -> "Other";
        };
    }

    private void viewMySubmissions() {
        System.out.println("\n" + "┌" + "─".repeat(58) + "┐");
        System.out.println("│              MY BOOK SUBMISSIONS                   │");
        System.out.println("└" + "─".repeat(58) + "┘");
        
        var submissions = authorService.getAuthorSubmissions(loggedInAuthor.getUsername());
        
        if (submissions.isEmpty()) {
            System.out.println("\n📭 You haven't submitted any books yet.");
            return;
        }

        System.out.println("\n📚 Your Submissions:");
        System.out.println("─".repeat(60));
        
        for (var sub : submissions) {
            System.out.println("📖 Title: " + sub.getTitle());
            System.out.println("   Genre: " + sub.getGenresAsString());
            System.out.println("   Submitted: " + sub.getSubmissionDate());
            System.out.println("   Status: " + getStatusEmoji(sub.getStatus()) + " " + sub.getStatus());
            if (sub.isRejected() && sub.getRejectionReason() != null) {
                System.out.println("   Reason: " + sub.getRejectionReason());
            }
            System.out.println("─".repeat(40));
        }
    }

    private String getStatusEmoji(String status) {
        return switch (status) {
            case "PENDING" -> "⏳";
            case "APPROVED" -> "✅";
            case "REJECTED" -> "❌";
            default -> "📌";
        };
    }
}
