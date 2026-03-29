package project.task1.model;

import java.util.Objects;

public class UserAccount {
    private final String username;
    private final String fullName;
    private final String passwordSaltBase64;
    private final String passwordHashBase64;
    private final UserRole role;
    private boolean disabled;

    public UserAccount(
            String username,
            String fullName,
            String passwordSaltBase64,
            String passwordHashBase64,
            UserRole role,
            boolean disabled
    ) {
        this.username = Objects.requireNonNull(username, "username must not be null");
        this.fullName = Objects.requireNonNull(fullName, "fullName must not be null");
        this.passwordSaltBase64 = Objects.requireNonNull(passwordSaltBase64, "passwordSaltBase64 must not be null");
        this.passwordHashBase64 = Objects.requireNonNull(passwordHashBase64, "passwordHashBase64 must not be null");
        this.role = Objects.requireNonNull(role, "role must not be null");
        this.disabled = disabled;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPasswordSaltBase64() {
        return passwordSaltBase64;
    }

    public String getPasswordHashBase64() {
        return passwordHashBase64;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isDisabled() { return disabled; }
    public String getDisabledString() { return disabled ? "Yes" : "No"; }

    public void setDisabled(boolean d) { disabled = d; }
}
