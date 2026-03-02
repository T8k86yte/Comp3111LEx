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
        // Super class constructor
        super(username, fullName, passwordSaltBase64, passwordHashBase64, UserRole.AUTHOR);
        if(bio == null){
            this.bio = "";
        }else{
            this.bio = bio;
        }
    }

    public String getBio() {
        return bio;
    }

    // toString, which convert all data to string for repo
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

    
}