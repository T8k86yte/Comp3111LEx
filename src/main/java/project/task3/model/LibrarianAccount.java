package project.task3.model;

import project.task1.model.UserAccount;
import project.task1.model.UserRole;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LibrarianAccount extends UserAccount {
    private final int employeeID;

    public LibrarianAccount(
            String username,
            String fullName,
            String passwordSaltBase64,
            String passwordHashBase64,
            boolean disabled,
            LocalDate lastLogin,
            String profilePicturePath,
            int employeeID
    ) {
        super(username, fullName, passwordSaltBase64, passwordHashBase64, UserRole.LIBRARIAN, disabled, lastLogin, profilePicturePath);
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
                getLastLoginString(),
                getProfilePicturePath(),
                Integer.toString(employeeID)
        );
    }

    public static LibrarianAccount fromString(String data) {
        String[] parts = data.split("\\|", -1);
        if (parts.length >= 9) {
            return new LibrarianAccount(
                    parts[0],
                    parts[1],
                    parts[2],
                    parts[3],
                    parts[5].equals("1"),
                    parts[6].isEmpty() ? null : LocalDate.parse(parts[6]),
                    parts[7],
                    Integer.parseInt(parts[8])
            );
        }
        else if (parts.length == 8) {
            return new LibrarianAccount(
                    parts[0],
                    parts[1],
                    parts[2],
                    parts[3],
                    parts[5].equals("1"),
                    parts[6].isEmpty() ? null : LocalDate.parse(parts[6]),
                    "",
                    Integer.parseInt(parts[7])
            );
        }
        else if (parts.length == 7) {
            return new LibrarianAccount(
                    parts[0],
                    parts[1],
                    parts[2],
                    parts[3],
                    parts[5].equals("1"),
                    null,
                    "",
                    Integer.parseInt(parts[6])
            );
        }
        return null;
    }
}
