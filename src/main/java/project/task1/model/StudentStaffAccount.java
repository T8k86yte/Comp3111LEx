package project.task1.model;

public class StudentStaffAccount extends UserAccount {
    public StudentStaffAccount(
            String username,
            String fullName,
            String passwordSaltBase64,
            String passwordHashBase64,
            UserRole role
    ) {
        super(username, fullName, passwordSaltBase64, passwordHashBase64, role);
    }

    @Override
    public String toString() {
        return String.join("|",
                getUsername(),
                getFullName(),
                getPasswordSaltBase64(),
                getPasswordHashBase64(),
                roleString(getRole())
        );
    }

    public static StudentStaffAccount fromString(String data) {
        String[] parts = data.split("\\|");
        if (parts.length >= 5) {
            return new StudentStaffAccount(
                    parts[0],
                    parts[1],
                    parts[2],
                    parts[3],
                    parts[4].equals("STUDENT") ? UserRole.STUDENT : UserRole.STAFF
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
