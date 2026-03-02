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
                    loginAuthor();
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
        System.out.println("  2) Login (Coming Soon)");
        System.out.println("  3) View Registration Guidelines");
        System.out.println("  0) Exit");
        System.out.print("Enter your choice: ");
    }

    private void registerNewAuthor() {
        System.out.println("\n" + "┌" + "─".repeat(58) + "┐");
        System.out.println("│               NEW AUTHOR REGISTRATION               │");
        System.out.println("└" + "─".repeat(58) + "┘");
        
        // Get username with validation
        String username = getValidatedInput(
            "\n📝 Username (3-20 chars, letters/numbers/underscore only): ",
            "Username cannot be empty."
        );
        
        // Get full name
        String fullName = getValidatedInput(
            "👤 Full Name: ",
            "Full name cannot be empty."
        );
        
        // Get password with confirmation
        String password = getPasswordWithConfirmation();
        
        // Get bio (optional)
        System.out.print("📖 Bio (optional, max 500 chars, press Enter to skip): ");
        String bio = scanner.nextLine().trim();
        
        // Show summary
        displayRegistrationSummary(username, fullName, bio);
        
        // Confirm registration
        System.out.print("\n❓ Confirm registration? (y/n): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        
        if (confirm.equals("y")) {
            // Process registration
            System.out.print("\n⏳ Processing registration");
            animateDots(3);
            
            AuthorPortalService.RegistrationResult result = authorService.registerAuthor(
                username, fullName, password, password, bio
            );
            
            // Display result
            System.out.println("\n" + "─".repeat(60));
            if (result.isSuccess()) {
                System.out.println("✅ " + result.getMessage());
                System.out.println("🎉 You can now login to your author account!");
            } else {
                System.out.println("❌ " + result.getMessage());
                System.out.println("💡 Please try again or contact support.");
            }
            System.out.println("─".repeat(60));
        } else {
            System.out.println("\n❌ Registration cancelled.");
        }
    }

    private void loginAuthor() {
        System.out.println("\n" + "┌" + "─".repeat(58) + "┐");
        System.out.println("│                   AUTHOR LOGIN                      │");
        System.out.println("└" + "─".repeat(58) + "┘");
        System.out.println("\n⚠️  Login feature coming soon in Task 2.2!");
        System.out.println("Please register first if you haven't already.\n");
        
        System.out.print("Press Enter to continue...");
        scanner.nextLine();
    }

    private void viewRegistrationGuidelines() {
        System.out.println("\n" + "┌" + "─".repeat(58) + "┐");
        System.out.println("│              REGISTRATION GUIDELINES                │");
        System.out.println("└" + "─".repeat(58) + "┘");
        
        System.out.println("\n📋 REQUIREMENTS:");
        System.out.println("  • Username:");
        System.out.println("    - 3 to 20 characters long");
        System.out.println("    - Can contain letters (a-z, A-Z)");
        System.out.println("    - Can contain numbers (0-9)");
        System.out.println("    - Can contain underscores (_)");
        System.out.println("    - No spaces or special characters");
        
        System.out.println("\n  • Password:");
        System.out.println("    - At least 8 characters long");
        System.out.println("    - Must contain at least one letter");
        System.out.println("    - Must contain at least one number");
        System.out.println("    - Must contain at least one uppercase letter");
        
        System.out.println("\n  • Full Name:");
        System.out.println("    - Your real name or pen name");
        System.out.println("    - Cannot be empty");
        
        System.out.println("\n  • Bio (Optional):");
        System.out.println("    - Maximum 500 characters");
        System.out.println("    - Tell us about yourself and your writing");
        
        System.out.println("\n✅ BENEFITS OF REGISTRATION:");
        System.out.println("  • Publish your books");
        System.out.println("  • Track your submissions");
        System.out.println("  • Connect with readers");
        System.out.println("  • Build your author profile");
        
        System.out.print("\nPress Enter to return to menu...");
        scanner.nextLine();
    }

    // Helper method to get validated input
    private String getValidatedInput(String prompt, String errorMessage) {
        String input;
        do {
            System.out.print(prompt);
            input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println("❌ " + errorMessage);
            }
        } while (input.isEmpty());
        return input;
    }

    // Helper method to get password with confirmation
    private String getPasswordWithConfirmation() {
        String password;
        String confirmPassword;
        
        System.out.println("\n🔐 PASSWORD REQUIREMENTS:");
        System.out.println("   • At least 8 characters");
        System.out.println("   • At least one letter");
        System.out.println("   • At least one number");
        System.out.println("   • At least one uppercase letter");
        
        do {
            System.out.print("\n🔑 Enter password: ");
            password = scanner.nextLine();
            
            System.out.print("🔑 Confirm password: ");
            confirmPassword = scanner.nextLine();
            
            if (!password.equals(confirmPassword)) {
                System.out.println("❌ Passwords do not match. Please try again.");
                continue;
            }
            
            // Let the service validate password strength
            break;
            
        } while (true);
        
        return password;
    }

    // Helper method to display registration summary
    private void displayRegistrationSummary(String username, String fullName, String bio) {
        System.out.println("\n" + "┌" + "─".repeat(58) + "┐");
        System.out.println("│               REGISTRATION SUMMARY                  │");
        System.out.println("└" + "─".repeat(58) + "┘");
        
        System.out.println("\n📋 Please review your information:");
        System.out.println("  Username: " + username);
        System.out.println("  Full Name: " + fullName);
        System.out.println("  Bio: " + (bio.isEmpty() ? "[Not provided]" : 
            (bio.length() > 50 ? bio.substring(0, 50) + "..." : bio)));
        
        System.out.println("\n⚠️  Make sure all information is correct.");
    }

    // Helper method to animate processing dots
    private void animateDots(int count) {
        for (int i = 0; i < count; i++) {
            try {
                Thread.sleep(500);
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