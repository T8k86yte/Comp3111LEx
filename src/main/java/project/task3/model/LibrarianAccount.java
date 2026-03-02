package project.task3.model;

import project.task1.model.UserAccount;
import project.task1.model.UserRole;
import project.task2.model.AuthorAccount;

public class LibrarianAccount extends UserAccount {
    private final int employeeID;

    public LibrarianAccount(
            String username,
            String fullName,
            String passwordSaltBase64,
            String passwordHashBase64,
            int employeeID
    ) {
        super(username, fullName, passwordSaltBase64, passwordHashBase64, UserRole.LIBRARIAN);
        this.employeeID = employeeID;
    }

    public int getEmployeeID() {
        return employeeID;
    }

    @Override
    public String toString() {
        return String.join("|",
                getUsername(),
                getFullName(),
                getPasswordSaltBase64(),
                getPasswordHashBase64(),
                "LIBRARIAN",
                Integer.toString(employeeID)
        );
    }

    public static LibrarianAccount fromString(String data) {
        String[] parts = data.split("\\|");
        if (parts.length >= 6) {
            return new LibrarianAccount(
                    parts[0],
                    parts[1],
                    parts[2],
                    parts[3],
                    Integer.parseInt(parts[5])
            );
        }
        return null;
    }
}
