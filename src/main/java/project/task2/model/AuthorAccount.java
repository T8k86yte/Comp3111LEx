package project.task2.model;

import project.task1.model.UserAccount;
import project.task1.model.UserRole;

public class AuthorAccount extends UserAccount {
    private final String bio;

    public AuthorAccount(
            String username,
            String fullName,
            String passwordSaltBase64,
            String passwordHashBase64,
            String bio
    ) {
        super(username, fullName, passwordSaltBase64, passwordHashBase64, UserRole.AUTHOR);
        this.bio = bio != null ? bio : "";
    }

    public String getBio() {
        return bio;
    }

    @Override
    public String toString() {
        return String.join("|",
            getUsername(),
            getFullName(),
            getPasswordSaltBase64(),
            getPasswordHashBase64(),
            "AUTHOR",
            bio
        );
    }

    public static AuthorAccount fromString(String data) {
        String[] parts = data.split("\\|");
        if (parts.length >= 6) {
            return new AuthorAccount(
                parts[0],
                parts[1],
                parts[2],
                parts[3],
                parts[5]
            );
        }
        return null;
    }
}