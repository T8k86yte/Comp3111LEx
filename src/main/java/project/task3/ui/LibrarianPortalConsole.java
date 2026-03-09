package project.task3.ui;

import project.task1.model.Book;
import project.task1.repo.InMemoryBookRepository;
import project.task2.model.BookSubmission;
import project.task2.repo.SubmissionRepository;
import project.task3.repo.LibrarianRepository;
import project.task3.service.LibrarianPortalService;
import project.task3.model.LibrarianAccount;

import java.util.List;
import java.util.Scanner;

public class LibrarianPortalConsole {
    private final LibrarianPortalService portalService;
    private final Scanner scanner;
    private LibrarianAccount currentUser;

    public LibrarianPortalConsole() {
        this.portalService = new LibrarianPortalService(
                new LibrarianRepository(),
                new InMemoryBookRepository(),
                new SubmissionRepository()
        );
        this.scanner = new Scanner(System.in);
    }

    public void start() {
        System.out.println("=== Librarian Portal (Task 3) ===");
        boolean running = true;
        while (running) {
            printMenu();
            String option = scanner.nextLine().trim();
            switch (option) {
                case "1" -> handleLibrarianRegistration();
                case "2" -> handleLogin();
                case "3" -> showBookSubmissionScreen();
                case "4" -> handleApproveReject();
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
        System.out.println("1) Register");
        System.out.println("2) Login Librarian");
        System.out.println("3) Book Submission Screen");
        System.out.println("4) Approve/Reject Book Submission");
        System.out.println("5) Logout");
        System.out.println("0) Exit");
        System.out.print("Select option: ");
    }

    private void handleLibrarianRegistration() {
        System.out.println("\n--- Register Librarian ---");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Full Name: ");
        String fullName = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        System.out.print("Employee ID: ");
        String eid = scanner.nextLine();

        LibrarianPortalService.OperationResult result = portalService.registerLibrarian(username, fullName, password, eid);
        System.out.println(result.message());
    }

    private void handleLogin() {
        System.out.println("\n--- Librarian Login ---");
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();

        LibrarianPortalService.LoginResult result = portalService.login(username, password);
        if (result.success()) currentUser = result.user();
        System.out.println(result.message());
    }

    private void showBookSubmissionScreen() {
        System.out.println("\n--- Pending Book Submission Screen ---");
        List<BookSubmission> subs = portalService.getBookSubmissionScreenData();
        if (subs.isEmpty()) {
            System.out.println("There are no pending book submissions currently.");
            return;
        }

        for (BookSubmission sub : subs) {
            System.out.println("----------------------------------------");
            System.out.println("Submission ID: " + sub.getSubmissionId());
            System.out.println("Title: " + sub.getTitle());
            System.out.println("Author Username: " + sub.getAuthorUsername());
            System.out.println("Author Full Name: " + sub.getAuthorFullName());
            System.out.println("Genre: " + sub.getGenre());
            System.out.println("Submit Date: " + sub.getSubmissionDate());
            System.out.println("Book Abstract/Summary: " + sub.getDescription());
            System.out.println("Status: PENDING");
        }
        System.out.println("----------------------------------------");
    }

    private void handleApproveReject() {
        System.out.println("\n--- Approve/Reject Book Submission ---");
        if (currentUser == null) {
            System.out.println("Approve/Reject failed: please login first.");
            return;
        }

        System.out.print("Enter Submission ID to handle: ");
        String subId = scanner.nextLine();
        LibrarianPortalService.OperationResult res = portalService.validateBookSubmissionId(subId);
        if (!res.success()) {
            System.out.println(res.message());
            return;
        }

        System.out.print("Enter the action (Approve/Reject): ");
        String action = scanner.nextLine();
        boolean isApprove;
        if (action.equals("Approve")) isApprove = true;
        else if (action.equals("Reject")) isApprove = false;
        else {
            System.out.println("Invalid choice: action must be Approve or Reject.");
            return;
        }

        if (isApprove) res = portalService.approveBookSubmission(subId, currentUser);
        else {
            System.out.print("Enter the rejection reason: ");
            String reason = scanner.nextLine();
            res = portalService.rejectBookSubmission(subId, currentUser, reason);
        }

        System.out.println(res.message());
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
