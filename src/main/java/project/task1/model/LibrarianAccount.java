package project.task1.model;

import java.util.Objects;
import project.task1.model.UserAccount;

public class LibrarianAccount extends UserAccount  {
    private int employeeID;

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
