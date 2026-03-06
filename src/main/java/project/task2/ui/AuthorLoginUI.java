package project.task2.ui;

import project.task2.service.AuthorPortalService;
import project.task2.model.AuthorAccount;
import project.task2.model.BookSubmission;

import java.util.List;
import java.util.Scanner;

public class AuthorLoginUI {
    private final AuthorPortalService authorService;
    private final Scanner scanner;
    private AuthorAccount loggedInAuthor;

    public AuthorLoginUI() {
        this.authorService = new AuthorPortalService();
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        printLoginBanner();
        
        boolean running = true;
        while (running) {
            if (loggedInAuthor == null) {
                printLoginMenu();
                String choice = scanner.nextLine().trim();
                
                switch (choice) {
                    case "1":
                        handleLogin();
                        break;
                    case "2":
                        System.out.println("\nOpening Registration Portal...");
                        AuthorRegistrationUI regUI = new AuthorRegistrationUI();
                        regUI.start();
                        break;
                    case "0":
                        running = false;
                        System.out.println("\nThank you for using Author Portal. Goodbye!");
                        break;
                    default:
                        System.out.println("❌ Invalid option. Please try again.");
                }
            } else {
                showAuthorDashboard();
            }
        }
        scanner.close();
    }

    private void printLoginBanner() {
        System.out.println("\n" + "╔" + "═".repeat(58) + "╗");
        System.out.println("║                    AUTHOR LOGIN                      ║");
        System.out.println("║                   Task 2.2 - COMP3111                ║");
        System.out.println("╚" + "═".repeat(58) + "╝");
    }

    private void printLoginMenu() {
        System.out.println("\n" + "─".repeat(60));
        System.out.println("LOGIN MENU:");
        System.out.println("  1) Login to Existing Account");
        System.out.println("  2) Register New Account");
        System.out.println("  0) Exit");
        System.out.print("Enter your choice: ");
    }

    private void handleLogin() {
        System.out.println("\n" + "┌" + "─".repeat(58) + "┐");
        System.out.println("│                  AUTHOR LOGIN                        │");
        System.out.println("└" + "─".repeat(58) + "┘");
        
        System.out.print("\n📝 Username: ");
        String username = scanner.nextLine().trim();
        
        System.out.print("🔑 Password: ");
        String password = scanner.nextLine();

        System.out.print("\n⏳ Verifying credentials");
        animateDots(3);

        AuthorPortalService.LoginResult result = authorService.login(username, password);

        System.out.println("\n" + "─".repeat(60));
        if (result.isSuccess()) {
            loggedInAuthor = result.getAuthor();
            System.out.println("✅ " + result.getMessage());
            showAuthorDashboard();
        } else {
            System.out.println("❌ " + result.getMessage());
            System.out.println("\n💡 Tips:");
            System.out.println("   • Check your username and password");
            System.out.println("   • Passwords are case-sensitive");
            System.out.println("   • Not registered? Choose option 2");
        }
        System.out.println("─".repeat(60));
    }

    private void showAuthorDashboard() {
        while (loggedInAuthor != null) {
            System.out.println("\n" + "┌" + "─".repeat(58) + "┐");
            System.out.println("│                 AUTHOR DASHBOARD                     │");
            System.out.println("└" + "─".repeat(58) + "┘");
            
            System.out.println("\n👋 Welcome, " + loggedInAuthor.getFullName() + "!");
            
            System.out.println("\n" + "─".repeat(60));
            System.out.println("DASHBOARD MENU:");
            System.out.println("  1) View Profile");
            System.out.println("  2) Publish New Book");
            System.out.println("  3) View My Submissions");
            System.out.println("  4) Logout");
            System.out.print("Enter your choice: ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    viewProfile();
                    break;
                case "2":
                    // Launch Publish Book UI
                    PublishBookUI publishUI = new PublishBookUI(loggedInAuthor);
                    publishUI.start();
                    break;
                case "3":
                    viewMySubmissions();
                    break;
                case "4":
                    logout();
                    break;
                default:
                    System.out.println("❌ Invalid option.");
            }
        }
    }

    private void viewProfile() {
        System.out.println("\n" + "┌" + "─".repeat(58) + "┐");
        System.out.println("│                  AUTHOR PROFILE                      │");
        System.out.println("└" + "─".repeat(58) + "┘");
        
        System.out.println("\n📋 Profile Details:");
        System.out.println("   • Username: " + loggedInAuthor.getUsername());
        System.out.println("   • Full Name: " + loggedInAuthor.getFullName());
        System.out.println("   • Bio: " + (loggedInAuthor.getBio().isEmpty() ? 
            "Not provided" : loggedInAuthor.getBio()));
        
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    // ========== TASK 2.3: VIEW SUBMISSIONS METHOD ==========
    private void viewMySubmissions() {
        System.out.println("\n" + "┌" + "─".repeat(58) + "┐");
        System.out.println("│              MY BOOK SUBMISSIONS                   │");
        System.out.println("└" + "─".repeat(58) + "┘");
        
        List<BookSubmission> submissions = authorService.getAuthorSubmissions(loggedInAuthor.getUsername());
        
        if (submissions.isEmpty()) {
            System.out.println("\n📭 You haven't submitted any books yet.");
            System.out.println("\n💡 Use option 2 to publish your first book!");
        } else {
            System.out.println("\n📚 Your Submissions (Total: " + submissions.size() + "):");
            System.out.println("─".repeat(60));
            
            for (int i = 0; i < submissions.size(); i++) {
                BookSubmission sub = submissions.get(i);
                System.out.println("\n📖 Book #" + (i+1));
                System.out.println("   ID: " + sub.getSubmissionId());
                System.out.println("   Title: " + sub.getTitle());
                System.out.println("   Genre: " + sub.getGenre());
                System.out.println("   Submitted: " + sub.getSubmissionDate());
                System.out.println("   Status: " + getStatusEmoji(sub.getStatus()) + " " + sub.getStatus());
                
                if (sub.isRejected() && sub.getRejectionReason() != null) {
                    System.out.println("   ❌ Rejection Reason: " + sub.getRejectionReason());
                }
                System.out.println("   ────────────────────────────────────────────");
            }
        }
        
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
    }

    private String getStatusEmoji(String status) {
        switch (status) {
            case "PENDING": return "⏳";
            case "APPROVED": return "✅";
            case "REJECTED": return "❌";
            default: return "📌";
        }
    }

    private void logout() {
        System.out.println("\n👋 Goodbye, " + loggedInAuthor.getFullName() + "! You have been logged out.");
        loggedInAuthor = null;
    }

    private void animateDots(int count) {
        for (int i = 0; i < count; i++) {
            try {
                Thread.sleep(300);
                System.out.print(".");
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public static void main(String[] args) {
        AuthorLoginUI loginUI = new AuthorLoginUI();
        loginUI.start();
    }
}
