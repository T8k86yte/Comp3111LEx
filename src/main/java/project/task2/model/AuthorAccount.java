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

    // Author-specific getter
    public String getBio() {
        return bio;
    }

    // Get the password (salt) of the author
    public String getPasswordSalt() {
        return getPasswordSaltBase64();
    }
    // get the password (hash) of author
    public String getPasswordHash() {
        return getPasswordHashBase64();
    }

    // tostring function that collect all user data, for repository usage
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
    // fromstring function that decode user data from repository
    public static AuthorAccount fromString(String data) {
        String[] parts = data.split("\\|");
        if (parts.length >= 6) {
            return new AuthorAccount(
                parts[0],  // username
                parts[1],  // fullName
                parts[2],  // passwordSaltBase64
                parts[3],  // passwordHashBase64
                parts[5]   // bio
            );
        }
        return null;
    }
}
