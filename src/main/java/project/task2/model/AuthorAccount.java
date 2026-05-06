package project.task2.model;

import project.task1.model.UserAccount;
import project.task1.model.UserRole;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuthorAccount extends UserAccount {
    private final String bio;

    public AuthorAccount(
            String username,
            String fullName,
            String passwordSaltBase64,
            String passwordHashBase64,
            boolean disabled,
            LocalDate lastLogin,
            String profilePicturePath,
            String bio
    ) {
        super(username, fullName, passwordSaltBase64, passwordHashBase64, UserRole.AUTHOR, disabled, lastLogin, profilePicturePath);
        this.bio = bio != null ? bio : "";
    }

    public String getBio() {
        return bio;
    }

    public String getPasswordSalt() {
        return getPasswordSaltBase64();
    }

    public String getPasswordHash() {
        return getPasswordHashBase64();
    }

    @Override
    public String toString() {
        // Always include bio, even if empty
        return String.join("|",
            getUsername(),
            getFullName(),
            getPasswordSaltBase64(),
            getPasswordHashBase64(),
            "AUTHOR",
            isDisabled() ? "1" : "0",
            getLastLoginString(),
            getProfilePicturePath(),
            bio  // This will be empty string if no bio
        );
    }

    public static AuthorAccount fromString(String data) {
        if (data == null || data.trim().isEmpty()) {
            return null;
        }
        
        String[] parts = data.split("\\|", -1); // -1 keeps empty trailing fields
        
        if (parts.length >= 9) {
            return new AuthorAccount(
                parts[0].trim(),  // username
                parts[1].trim(),  // fullName
                parts[2].trim(),  // passwordSaltBase64
                parts[3].trim(),  // passwordHashBase64
                parts[5].trim().equals("1"),//disabled
                parts[6].isEmpty() ? null : LocalDate.parse(parts[6]),  //lastLogin
                parts[7].trim(),  // profilePicturePath (may be empty)
                parts[8].trim()   // bio (may be empty)
            );
        }
        else if (parts.length == 8) {
            return new AuthorAccount(
                    parts[0].trim(),  // username
                    parts[1].trim(),  // fullName
                    parts[2].trim(),  // passwordSaltBase64
                    parts[3].trim(),  // passwordHashBase64
                    parts[5].trim().equals("1"),//disabled
                    parts[6].isEmpty() ? null : LocalDate.parse(parts[6]),
                    "",
                    parts[7].trim()   // bio (may be empty)
            );
        }
        else if (parts.length == 7) {
            return new AuthorAccount(
                    parts[0].trim(),  // username
                    parts[1].trim(),  // fullName
                    parts[2].trim(),  // passwordSaltBase64
                    parts[3].trim(),  // passwordHashBase64
                    parts[5].trim().equals("1"),//disabled
                    null,
                    "",
                    parts[6].trim()   // bio (may be empty)
            );
        }
        return null;
    }
}
