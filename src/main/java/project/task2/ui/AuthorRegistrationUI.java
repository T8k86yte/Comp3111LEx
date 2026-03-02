package project.task2.ui;

import project.task2.service.AuthorPortalService;

import java.util.Scanner;

public class AuthorRegistrationUI {
    private final AuthorPortalService authorService;
    private final Scanner scanner;

    public AuthorRegistrationUI() {
        this.authorService = new AuthorPortalService();
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        printWelcomeBanner();
        
        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    registerNewAuthor();
                    break;
                case "2":
                    // Go to login
                    AuthorLoginUI loginUI = new AuthorLoginUI();
                    loginUI.start();
                    break;
                case "3":
                    viewRegistrationGuidelines();
                    break;
                case "0":
                    running = false;
                    System.out.println("\nThank you for using Author Portal. Goodbye!");
                    break;
                default:
                    System.out.println("❌ Invalid option. Please choose again.");
            }
        }
        scanner.close();
    }

    private void printWelcomeBanner() {
        System.out.println("\n" + "╔" + "═".repeat(58) + "╗");
        System.out.println("║            AUTHOR REGISTRATION PORTAL            ║");
        System.out.println("║                Task 2.1 - COMP3111               ║");
        System.out.println("╚" + "═".repeat(58) + "╝");
    }

    private void printMainMenu() {
        System.out.println("\n" + "─".repeat(60));
        System.out.println("MAIN MENU:");
        System.out.println("  1) Register as New Author");
        System.out.println("  2) Login to Existing Account");
        System.out.println("  3) View Registration Guidelines");
        System.out.println("  0) Exit");
        System.out.print("Enter your choice: ");
    }

    private void registerNewAuthor() {
        System.out.println("\n" + "┌" + "─".repeat(58) + "┐");
        System.out.println("│               NEW AUTHOR REGISTRATION               │");
        System.out.println("└" + "─".repeat(58) + "┘");
        
        // Get username
        System.out.print("\n📝 Username: ");
        String username = scanner.nextLine().trim();
        
        // Get full name
        System.out.print("👤 Full Name: ");
        String fullName = scanner.nextLine().trim();
        
        // Get password
        System.out.print("🔑 Password: ");
        String password = scanner.nextLine();
        
        // Confirm password
        System.out.print("🔑 Confirm Password: ");
        String confirmPassword = scanner.nextLine();
        
        // Get bio
        System.out.print("📖 Bio (optional): ");
        String bio = scanner.nextLine().trim();

        // Show summary
        System.out.println("\n📋 Registration Summary:");
        System.out.println("  Username: " + username);
        System.out.println("  Full Name: " + fullName);
        System.out.println("  Bio: " + (bio.isEmpty() ? "[Not provided]" : 
            (bio.length() > 50 ? bio.substring(0, 50) + "..." : bio)));
        
        System.out.print("\n❓ Confirm registration? (y/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        
        if (confirm.equals("y")) {
            System.out.print("\n⏳ Processing registration");
            animateDots(3);
            
            AuthorPortalService.RegistrationResult result = authorService.registerAuthor(
                username, fullName, password, confirmPassword, bio
            );
            
            System.out.println("\n" + "─".repeat(60));
            if (result.isSuccess()) {
                System.out.println("✅ " + result.getMessage());
                System.out.println("\n🔐 You can now login with your credentials!");
            } else {
                System.out.println("❌ " + result.getMessage());
            }
            System.out.println("─".repeat(60));
        } else {
            System.out.println("\n❌ Registration cancelled.");
        }
    }

    private void viewRegistrationGuidelines() {
        System.out.println("\n" + "┌" + "─".repeat(58) + "┐");
        System.out.println("│              REGISTRATION GUIDELINES                │");
        System.out.println("└" + "─".repeat(58) + "┘");
        
        System.out.println("\n📋 REQUIREMENTS:");
        System.out.println("  • Username:");
        System.out.println("    - 3 to 20 characters");
        System.out.println("    - Letters, numbers, underscores only");
        System.out.println("  • Password:");
        System.out.println("    - At least 8 characters");
        System.out.println("    - At least one letter");
        System.out.println("    - At least one number");
        System.out.println("  • Bio:");
        System.out.println("    - Maximum 500 characters");
        System.out.println("    - Optional");
        
        System.out.print("\nPress Enter to continue...");
        scanner.nextLine();
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
        AuthorRegistrationUI ui = new AuthorRegistrationUI();
        ui.start();
    }
}
