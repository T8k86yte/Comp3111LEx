package project.task3.ui;

import project.task1.repo.InMemoryBookRepository;
import project.task1.service.StudentStaffPortalService;
import project.task3.repo.LibrarianRepository;
import project.task3.service.LibrarianPortalService;
import project.task3.model.LibrarianAccount;

import java.util.Scanner;

public class LibrarianPortalConsole {
    public LibrarianPortalConsole() {
        this.portalService = new LibrarianPortalService(
                new LibrarianRepository(),
                new InMemoryBookRepository()
        );
        this.scanner = new Scanner(System.in);
    }

    private final LibrarianPortalService portalService;
    private final Scanner scanner;
    private LibrarianAccount currentUser;

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
}
