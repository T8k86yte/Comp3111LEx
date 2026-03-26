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
            boolean disabled,
            int employeeID
    ) {
        super(username, fullName, passwordSaltBase64, passwordHashBase64, UserRole.LIBRARIAN, disabled);
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
                isDisabled() ? "1" : "0",
                Integer.toString(employeeID)
        );
    }

    public static LibrarianAccount fromString(String data) {
        String[] parts = data.split("\\|");
        if (parts.length >= 7) {
            return new LibrarianAccount(
                    parts[0],
                    parts[1],
                    parts[2],
                    parts[3],
                    parts[5].equals("1"),
                    Integer.parseInt(parts[6])
            );
        }
        return null;
    }
}
