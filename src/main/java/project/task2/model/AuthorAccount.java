package project.task2.model;

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

    // Add these getters to access password fields from UserAccount
    public String getPasswordSalt() {
        return getPasswordSaltBase64();
    }

    public String getPasswordHash() {
        return getPasswordHashBase64();
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
