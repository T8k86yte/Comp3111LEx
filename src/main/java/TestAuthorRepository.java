import project.task2.model.AuthorAccount;
import project.task2.model.UserRole;
import project.task2.repo.AuthorRepository;

public class TestAuthorRepository {
    public static void main(String[] args) {
        System.out.println("=== Testing AuthorRepository ===\n");
        
        // Create repository
        AuthorRepository repo = new AuthorRepository();
        
        // Create some test authors
        AuthorAccount author1 = new AuthorAccount(
            "john_doe",
            "John Doe",
            "salt123",
            "hash456",
            "Science fiction writer"
        );
        
        AuthorAccount author2 = new AuthorAccount(
            "jane_smith",
            "Jane Smith",
            "salt789",
            "hash012",
            "Fantasy novelist"
        );
        
        AuthorAccount author3 = new AuthorAccount(
            "bob_writer",
            "Bob Johnson",
            "salt345",
            "hash678",
            ""
        );
        
        // Save authors to file
        System.out.println("Saving authors...");
        repo.save(author1);
        repo.save(author2);
        repo.save(author3);
        
        // Check if usernames exist
        System.out.println("\n=== Checking Usernames ===");
        System.out.println("john_doe exists? " + repo.existsByUsername("john_doe"));
        System.out.println("jane_smith exists? " + repo.existsByUsername("jane_smith"));
        System.out.println("unknown_user exists? " + repo.existsByUsername("unknown_user"));
        
        // Get all authors
        System.out.println("\n=== All Authors (Total: " + repo.getCount() + ") ===");
        for (AuthorAccount author : repo.findAll()) {
            System.out.println("  - " + author.getUsername() + " | " + 
                             author.getFullName() + " | Bio: " + 
                             (author.getBio().isEmpty() ? "No bio" : author.getBio()));
        }
        
        // Find specific author
        System.out.println("\n=== Finding john_doe ===");
        repo.findByUsername("john_doe").ifPresentOrElse(
            author -> System.out.println("Found: " + author.getFullName()),
            () -> System.out.println("Author not found")
        );
        
        System.out.println("\n✅ Test completed! Check the 'data/authors.txt' file");
    }
}
