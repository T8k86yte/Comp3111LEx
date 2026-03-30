package project.shared;

import project.task1.model.StudentStaffAccount;
import project.task1.model.UserRole;
import project.task1.repo.StudentStaffRepository;
import project.task1.security.PasswordSecurity;
import project.task2.model.AuthorAccount;
import project.task2.repo.AuthorRepository;
import project.task2.utils.PasswordUtils;
import project.task3.model.LibrarianAccount;
import project.task3.repo.LibrarianRepository;

import java.util.Optional;

public class SharedAuthFacade {
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final StudentStaffRepository studentStaffRepository;
    private final AuthorRepository authorRepository;
    private final LibrarianRepository librarianRepository;
    private final CrossRoleAccountService crossRoleAccountService;

    public SharedAuthFacade(
            StudentStaffRepository studentStaffRepository,
            AuthorRepository authorRepository,
            LibrarianRepository librarianRepository
    ) {
        this.studentStaffRepository = studentStaffRepository;
        this.authorRepository = authorRepository;
        this.librarianRepository = librarianRepository;
        this.crossRoleAccountService = new CrossRoleAccountService(
                studentStaffRepository,
                authorRepository,
                librarianRepository
        );
    }

    public AuthResult register(
            String username,
            String fullName,
            String password,
            String confirmPassword,
            String roleText,
            String bio,
            String employeeIdText
    ) {
        String normalizedUsername = safeTrim(username);
        String normalizedFullName = safeTrim(fullName);
        String normalizedRole = safeTrim(roleText).toUpperCase();

        if (normalizedUsername.isEmpty()) {
            return AuthResult.failure("Registration failed: username is required.");
        }
        if (normalizedFullName.isEmpty()) {
            return AuthResult.failure("Registration failed: full name is required.");
        }
        if (password == null || password.isBlank()) {
            return AuthResult.failure("Registration failed: password is required.");
        }
        if (confirmPassword != null && !password.equals(confirmPassword)) {
            return AuthResult.failure("Registration failed: passwords do not match.");
        }
        if (!isStrongPassword(password)) {
            return AuthResult.failure(
                    "Registration failed: weak password. Use at least "
                            + MIN_PASSWORD_LENGTH + " chars with letter, number, and uppercase."
            );
        }
        if (crossRoleAccountService.usernameExistsAcrossAllRoles(normalizedUsername)) {
            return AuthResult.failure("Registration failed: username already exists across user types.");
        }

        switch (normalizedRole) {
            case "STUDENT", "STAFF" -> {
                UserRole role = "STUDENT".equals(normalizedRole) ? UserRole.STUDENT : UserRole.STAFF;
                String salt = PasswordSecurity.generateSaltBase64();
                String hash = PasswordSecurity.hashPasswordBase64(password, salt);
                StudentStaffAccount account = new StudentStaffAccount(
                        normalizedUsername,
                        normalizedFullName,
                        salt,
                        hash,
                        role,
                        false
                );
                studentStaffRepository.save(account);
                return AuthResult.success("Registration successful for " + normalizedUsername + ".", new UserPrincipal(
                        normalizedUsername, normalizedFullName, role.name()
                ));
            }
            case "AUTHOR" -> {
                String salt = PasswordUtils.generateSalt();
                String hash = PasswordUtils.hashPassword(password, salt);
                AuthorAccount account = new AuthorAccount(
                        normalizedUsername,
                        normalizedFullName,
                        salt,
                        hash,
                        false,
                        bio == null ? "" : bio.trim()
                );
                authorRepository.save(account);
                return AuthResult.success("Registration successful for " + normalizedUsername + ".", new UserPrincipal(
                        normalizedUsername, normalizedFullName, "AUTHOR"
                ));
            }
            case "LIBRARIAN" -> {
                int employeeId = 0;
                if (employeeIdText != null && !employeeIdText.isBlank()) {
                    try {
                        employeeId = Integer.parseInt(employeeIdText.trim());
                    } catch (NumberFormatException ex) {
                        return AuthResult.failure("Registration failed: employee ID must be a number.");
                    }
                }
                String salt = PasswordSecurity.generateSaltBase64();
                String hash = PasswordSecurity.hashPasswordBase64(password, salt);
                LibrarianAccount account = new LibrarianAccount(
                        normalizedUsername,
                        normalizedFullName,
                        salt,
                        hash,
                        false,
                        employeeId
                );
                librarianRepository.save(account);
                return AuthResult.success("Registration successful for " + normalizedUsername + ".", new UserPrincipal(
                        normalizedUsername, normalizedFullName, "LIBRARIAN"
                ));
            }
            default -> {
                return AuthResult.failure("Registration failed: role must be Student, Staff, Author, or Librarian.");
            }
        }
    }

    public AuthResult login(String username, String password, String roleText) {
        String normalizedUsername = safeTrim(username);
        String normalizedRole = safeTrim(roleText).toUpperCase();
        if (normalizedUsername.isEmpty() || password == null || password.isBlank()) {
            return AuthResult.failure("Login failed: username and password are required.");
        }

        Optional<String> ownerRole = crossRoleAccountService.findRoleByUsername(normalizedUsername);
        if (ownerRole.isEmpty()) {
            return AuthResult.failure("Login failed: invalid username or password.");
        }
        if (!ownerRole.get().equalsIgnoreCase(normalizedRole)) {
            return AuthResult.failure("Login failed: username belongs to " + ownerRole.get() + " account.");
        }

        switch (normalizedRole) {
            case "STUDENT", "STAFF" -> {
                Optional<StudentStaffAccount> account = studentStaffRepository.findByUsername(normalizedUsername);
                if (account.isEmpty()) {
                    return AuthResult.failure("Login failed: invalid username or password.");
                }
                boolean ok = PasswordSecurity.verifyPassword(
                        password,
                        account.get().getPasswordSaltBase64(),
                        account.get().getPasswordHashBase64()
                );
                if (!ok) {
                    ok = PasswordUtils.verifyPassword(
                            password,
                            account.get().getPasswordSaltBase64(),
                            account.get().getPasswordHashBase64()
                    );
                }
                if (!ok) {
                    return AuthResult.failure("Login failed: invalid username or password.");
                }
                return AuthResult.success(
                        "Login successful. Welcome, " + account.get().getFullName() + ".",
                        new UserPrincipal(account.get().getUsername(), account.get().getFullName(), account.get().getRole().name())
                );
            }
            case "AUTHOR" -> {
                Optional<AuthorAccount> account = authorRepository.findByUsername(normalizedUsername);
                if (account.isEmpty()) {
                    return AuthResult.failure("Login failed: invalid username or password.");
                }
                boolean ok = PasswordSecurity.verifyPassword(
                        password,
                        account.get().getPasswordSaltBase64(),
                        account.get().getPasswordHashBase64()
                );
                if (!ok) {
                    return AuthResult.failure("Login failed: invalid username or password.");
                }
                return AuthResult.success(
                        "Login successful. Welcome, " + account.get().getFullName() + ".",
                        new UserPrincipal(account.get().getUsername(), account.get().getFullName(), "AUTHOR")
                );
            }
            case "LIBRARIAN" -> {
                Optional<LibrarianAccount> account = librarianRepository.findByUsername(normalizedUsername);
                if (account.isEmpty()) {
                    return AuthResult.failure("Login failed: invalid username or password.");
                }
                boolean ok = PasswordSecurity.verifyPassword(
                        password,
                        account.get().getPasswordSaltBase64(),
                        account.get().getPasswordHashBase64()
                );
                // Backward compatibility for old librarian accounts hashed with task2 utility.
                if (!ok) {
                    ok = project.task2.utils.PasswordUtils.verifyPassword(
                            password,
                            account.get().getPasswordSaltBase64(),
                            account.get().getPasswordHashBase64()
                    );
                }
                if (!ok) {
                    return AuthResult.failure("Login failed: invalid username or password.");
                }
                return AuthResult.success(
                        "Login successful. Welcome, " + account.get().getFullName() + ".",
                        new UserPrincipal(account.get().getUsername(), account.get().getFullName(), "LIBRARIAN")
                );
            }
            default -> {
                return AuthResult.failure("Login failed: role must be Student, Staff, Author, or Librarian.");
            }
        }
    }

    private static boolean isStrongPassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return false;
        }
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasNumber = password.matches(".*\\d.*");
        boolean hasUpper = password.matches(".*[A-Z].*");
        return hasLetter && hasNumber && hasUpper;
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    public record UserPrincipal(String username, String fullName, String role) {}

    public record AuthResult(boolean success, String message, UserPrincipal principal) {
        public static AuthResult success(String message, UserPrincipal principal) {
            return new AuthResult(true, message, principal);
        }

        public static AuthResult failure(String message) {
            return new AuthResult(false, message, null);
        }
    }
}
