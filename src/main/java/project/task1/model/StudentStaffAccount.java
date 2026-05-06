package project.task1.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StudentStaffAccount extends UserAccount {
    public StudentStaffAccount(
            String username,
            String fullName,
            String passwordSaltBase64,
            String passwordHashBase64,
            UserRole role,
            boolean disabled,
            LocalDate lastLogin,
            String profilePicturePath
    ) {
        super(username, fullName, passwordSaltBase64, passwordHashBase64, role, disabled, lastLogin, profilePicturePath);
    }

    @Override
    public String toString() {
        return String.join("|",
                getUsername(),
                getFullName(),
                getPasswordSaltBase64(),
                getPasswordHashBase64(),
                roleString(getRole()),
                isDisabled() ? "1" : "0",
                getLastLoginString(),
                getProfilePicturePath()
        );
    }

    public static StudentStaffAccount fromString(String data) {
        String[] parts = data.split("\\|", -1);
        if (parts.length >= 8) {
            return new StudentStaffAccount(
                    parts[0],
                    parts[1],
                    parts[2],
                    parts[3],
                    parts[4].equals("STUDENT") ? UserRole.STUDENT : UserRole.STAFF,
                    parts[5].equals("1"),
                    parts[6].isEmpty() ? null : LocalDate.parse(parts[6]),
                    parts[7]
            );
        }
        else if (parts.length == 7) {
            return new StudentStaffAccount(
                    parts[0],
                    parts[1],
                    parts[2],
                    parts[3],
                    parts[4].equals("STUDENT") ? UserRole.STUDENT : UserRole.STAFF,
                    parts[5].equals("1"),
                    parts[6].isEmpty() ? null : LocalDate.parse(parts[6]),
                    ""
            );
        }
        else if (parts.length == 6) {
            return new StudentStaffAccount(
                    parts[0],
                    parts[1],
                    parts[2],
                    parts[3],
                    parts[4].equals("STUDENT") ? UserRole.STUDENT : UserRole.STAFF,
                    parts[5].equals("1"),
                    null,
                    ""
            );
        }
        return null;
    }

    private static String roleString(UserRole role) {
        switch (role) {
            case STUDENT: return "STUDENT";
            case STAFF: return "STAFF";
            case AUTHOR: return "AUTHOR";
            case LIBRARIAN: return "LIBRARIAN";
        }
        return "";
    }
}
