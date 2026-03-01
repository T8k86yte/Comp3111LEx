package project.task1.ui;

import project.task1.model.Book;
import project.task1.model.UserAccount;
import project.task1.repo.InMemoryBookRepository;
import project.task1.repo.InMemoryUserRepository;
import project.task1.service.StudentStaffPortalService;
import project.task1.service.StudentStaffPortalService.LoginResult;
import project.task1.service.StudentStaffPortalService.OperationResult;

import java.util.List;
import java.util.Scanner;

public class StudentStaffPortalConsole {
    private final StudentStaffPortalService portalService;
    private final Scanner scanner;
    private UserAccount currentUser;

    public StudentStaffPortalConsole() {
        this.portalService = new StudentStaffPortalService(
                new InMemoryUserRepository(),
                new InMemoryBookRepository()
        );
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("=== Student/Staff Portal (Task 1) ===");
        boolean running = true;
        while (running) {
            printMenu();
            String option = scanner.nextLine().trim();
            switch (option) {
                case "1" -> handleRegistration();
                case "2" -> handleLogin();
                case "3" -> showAvailableBookScreen();
                case "4" -> handleBorrowBook();
                case "5" -> handleLogout();
                case "0" -> {
                    running = false;
                    System.out.println("Goodbye.");
                }
                default -> System.out.println("Invalid option. Please choose from the menu.");
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("1) Register Student/Staff");
        System.out.println("2) Login Student/Staff");
        System.out.println("3) Available Book Screen");
        System.out.println("4) Borrow Book");
        System.out.println("5) Logout");
        System.out.println("0) Exit");
        System.out.print("Select option: ");
    }

    private void handleRegistration() {
        System.out.println("\n--- Register Student/Staff ---");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Full Name: ");
        String fullName = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        System.out.print("Role (Student or Staff): ");
        String role = scanner.nextLine();

        OperationResult result = portalService.register(username, fullName, password, role);
        System.out.println(result.message());
    }

    private void handleLogin() {
        System.out.println("\n--- Student/Staff Login ---");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        LoginResult result = portalService.login(username, password);
        if (result.success()) {
            currentUser = result.user();
        }
        System.out.println(result.message());
    }

    private void showAvailableBookScreen() {
        System.out.println("\n--- Available Book Screen ---");
        List<Book> books = portalService.getBookScreenData();
        if (books.isEmpty()) {
            System.out.println("No books are currently available.");
            return;
        }

        for (Book book : books) {
            System.out.println("----------------------------------------");
            System.out.println("Book ID: " + book.getId());
            System.out.println("Title: " + book.getTitle());
            System.out.println("Author: " + book.getAuthor());
            System.out.println("Publish Date: " + book.getPublishDate());
            System.out.println("Availability Status: Available");
            System.out.println("Book Abstract/Summary: " + book.getSummary());
        }
        System.out.println("----------------------------------------");
    }

    private void handleBorrowBook() {
        System.out.println("\n--- Borrow Book ---");
        if (currentUser == null) {
            System.out.println("Borrow failed: please login first.");
            return;
        }

        System.out.print("Enter Book ID to borrow: ");
        String bookId = scanner.nextLine();

        OperationResult result = portalService.borrowBook(currentUser.getUsername(), bookId);
        System.out.println(result.message());
    }

    private void handleLogout() {
        if (currentUser == null) {
            System.out.println("No user is currently logged in.");
            return;
        }
        System.out.println("Logged out: " + currentUser.getUsername());
        currentUser = null;
    }
}
