package project.task1.model;

public class LibrarianAccount extends UserAccount  {
    private final int employeeID;

    public LibrarianAccount(
            String username,
            String fullName,
            String passwordSaltBase64,
            String passwordHashBase64,
            int employeeID
    ) {
        super(username, fullName, passwordSaltBase64, passwordHashBase64, UserRole.LIBRARIAN);
        this.employeeID = employeeID;
    }

    public int getEmployeeID() {
        return employeeID;
    }
}
