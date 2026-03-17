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
            bio  // This will be empty string if no bio
        );
    }

    public static AuthorAccount fromString(String data) {
        if (data == null || data.trim().isEmpty()) {
            return null;
        }
        
        String[] parts = data.split("\\|", -1); // -1 keeps empty trailing fields
        
        if (parts.length >= 6) {
            return new AuthorAccount(
                parts[0].trim(),  // username
                parts[1].trim(),  // fullName
                parts[2].trim(),  // passwordSaltBase64
                parts[3].trim(),  // passwordHashBase64
                parts[5].trim()   // bio (may be empty)
            );
        }
        return null;
    }
}
