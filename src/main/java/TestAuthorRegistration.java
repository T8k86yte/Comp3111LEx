import project.task2.service.AuthorPortalService;

public class TestAuthorRegistration {
    public static void main(String[] args) {
        System.out.println("=== Testing Author Registration Error Handling ===\n");
        
        AuthorPortalService service = new AuthorPortalService();
        
        // Test 1: Empty username
        System.out.println("Test 1: Empty username");
        var result1 = service.registerAuthor("", "John Doe", "Password123", "Password123", "Bio");
        printResult(result1);
        
        // Test 2: Empty full name
        System.out.println("\nTest 2: Empty full name");
        var result2 = service.registerAuthor("john123", "", "Password123", "Password123", "Bio");
        printResult(result2);
        
        // Test 3: Password too short
        System.out.println("\nTest 3: Password too short (6 chars)");
        var result3 = service.registerAuthor("john123", "John Doe", "Pass1", "Pass1", "Bio");
        printResult(result3);
        
        // Test 4: Password with no letters
        System.out.println("\nTest 4: Password with no letters");
        var result4 = service.registerAuthor("john123", "John Doe", "12345678", "12345678", "Bio");
        printResult(result4);
        
        // Test 5: Password with no numbers
        System.out.println("\nTest 5: Password with no numbers");
        var result5 = service.registerAuthor("john123", "John Doe", "Password", "Password", "Bio");
        printResult(result5);
        
        // Test 6: Password with no uppercase
        System.out.println("\nTest 6: Password with no uppercase");
        var result6 = service.registerAuthor("john123", "John Doe", "password123", "password123", "Bio");
        printResult(result6);
        
        // Test 7: Passwords don't match
        System.out.println("\nTest 7: Passwords don't match");
        var result7 = service.registerAuthor("john123", "John Doe", "Password123", "Password456", "Bio");
        printResult(result7);
        
        // Test 8: Invalid username (special characters)
        System.out.println("\nTest 8: Invalid username (special characters)");
        var result8 = service.registerAuthor("john@123", "John Doe", "Password123", "Password123", "Bio");
        printResult(result8);
        
        // Test 9: Username too short
        System.out.println("\nTest 9: Username too short (2 chars)");
        var result9 = service.registerAuthor("jo", "John Doe", "Password123", "Password123", "Bio");
        printResult(result9);
        
        // Test 10: Bio too long
        System.out.println("\nTest 10: Bio too long (501 chars)");
        String longBio = "a".repeat(501);
        var result10 = service.registerAuthor("john123", "John Doe", "Password123", "Password123", longBio);
        printResult(result10);
        
        // Test 11: Successful registration
        System.out.println("\nTest 11: Successful registration");
        var result11 = service.registerAuthor("john_doe", "John Doe", "Password123", "Password123", "I am a writer");
        printResult(result11);
        
        // Test 12: Duplicate username
        System.out.println("\nTest 12: Duplicate username");
        var result12 = service.registerAuthor("john_doe", "John Doe Jr", "Password123", "Password123", "Another writer");
        printResult(result12);
    }
    
    private static void printResult(AuthorPortalService.RegistrationResult result) {
        if (result.isSuccess()) {
            System.out.println("  ✅ SUCCESS: " + result.getMessage());
        } else {
            System.out.println("  ❌ FAILED: " + result.getMessage());
        }
    }
}
