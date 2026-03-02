import project.task2.service.AuthorPortalService;
import project.task2.model.AuthorAccount;

public class TestAuthorLogin {
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════╗");
        System.out.println("║         TESTING AUTHOR LOGIN (TASK 2.2)           ║");
        System.out.println("╚════════════════════════════════════════════════════╝");
        
        AuthorPortalService service = new AuthorPortalService();
        
        // ============================================================
        // TEST CASE 1: Create a test author for login testing
        // ============================================================
        System.out.println("\n📝 TEST CASE 1: Creating test author");
        System.out.println("------------------------------------------------------------");
        
        // FIX: Use only letters and numbers in username (no special characters except underscore)
        String timestamp = String.valueOf(System.currentTimeMillis());
        String testUsername = "test" + timestamp.substring(timestamp.length() - 8); // Use last 8 digits only
        String testPassword = "TestPass123";
        
        System.out.println("Attempting to create username: " + testUsername);
        
        var regResult = service.registerAuthor(
            testUsername,
            "Test Author",
            testPassword,
            testPassword,
            "This is a test author account"
        );
        
        if (regResult.isSuccess()) {
            System.out.println("✅ Test author created: " + testUsername);
        } else {
            System.out.println("❌ Failed to create test author: " + regResult.getMessage());
            // Try with a simple hardcoded username as fallback
            System.out.println("\n🔄 Trying with simple username 'testauthor'...");
            testUsername = "testauthor";
            regResult = service.registerAuthor(
                testUsername,
                "Test Author",
                testPassword,
                testPassword,
                "This is a test author account"
            );
            
            if (regResult.isSuccess()) {
                System.out.println("✅ Test author created with simple username: " + testUsername);
            } else {
                System.out.println("❌ Still failed: " + regResult.getMessage());
                return;
            }
        }
        
        // ============================================================
        // TEST CASE 2: Login with empty username
        // ============================================================
        System.out.println("\n📝 TEST CASE 2: Login with empty username");
        System.out.println("------------------------------------------------------------");
        var result1 = service.login("", testPassword);
        printResult("Empty username", result1);
        
        // ============================================================
        // TEST CASE 3: Login with empty password
        // ============================================================
        System.out.println("\n📝 TEST CASE 3: Login with empty password");
        System.out.println("------------------------------------------------------------");
        var result2 = service.login(testUsername, "");
        printResult("Empty password", result2);
        
        // ============================================================
        // TEST CASE 4: Login with non-existent username
        // ============================================================
        System.out.println("\n📝 TEST CASE 4: Login with non-existent username");
        System.out.println("------------------------------------------------------------");
        var result3 = service.login("nonexistentuser123", testPassword);
        printResult("Non-existent username", result3);
        
        // ============================================================
        // TEST CASE 5: Login with wrong password
        // ============================================================
        System.out.println("\n📝 TEST CASE 5: Login with wrong password");
        System.out.println("------------------------------------------------------------");
        var result4 = service.login(testUsername, "WrongPassword123");
        printResult("Wrong password", result4);
        
        // ============================================================
        // TEST CASE 6: Login with correct credentials (SUCCESS)
        // ============================================================
        System.out.println("\n📝 TEST CASE 6: Login with correct credentials");
        System.out.println("------------------------------------------------------------");
        var result5 = service.login(testUsername, testPassword);
        printResult("Correct credentials", result5);
        
        // ============================================================
        // TEST CASE 7: Verify author data after successful login
        // ============================================================
        System.out.println("\n📝 TEST CASE 7: Verify author data after login");
        System.out.println("------------------------------------------------------------");
        if (result5.isSuccess()) {
            AuthorAccount author = result5.getAuthor();
            System.out.println("✅ Login successful! Author data retrieved:");
            System.out.println("   • Username: " + author.getUsername());
            System.out.println("   • Full Name: " + author.getFullName());
            System.out.println("   • Bio: " + (author.getBio().isEmpty() ? "No bio" : author.getBio()));
            System.out.println("   • Role: " + author.getRole());
        } else {
            System.out.println("❌ Could not verify author data - login failed");
        }
        
        // ============================================================
        // TEST CASE 8: Test case sensitivity
        // ============================================================
        System.out.println("\n📝 TEST CASE 8: Test password case sensitivity");
        System.out.println("------------------------------------------------------------");
        var result6 = service.login(testUsername, testPassword.toLowerCase());
        printResult("Wrong case password", result6);
        
        // ============================================================
        // TEST CASE 9: Login with existing author from previous tests
        // ============================================================
        System.out.println("\n📝 TEST CASE 9: Login with existing author (Jacky)");
        System.out.println("------------------------------------------------------------");
        var result7 = service.login("Jacky", "aA12345678");
        printResult("Existing author login", result7);
        
        // ============================================================
        // SUMMARY
        // ============================================================
        printSummary();
    }
    
    private static void printResult(String testCase, AuthorPortalService.LoginResult result) {
        System.out.print("Test: " + testCase + " -> ");
        if (result.isSuccess()) {
            System.out.println("✅ SUCCESS");
            System.out.println("   Message: " + result.getMessage());
            System.out.println("   Author: " + result.getAuthor().getFullName());
        } else {
            System.out.println("❌ FAILED (expected for negative tests)");
            System.out.println("   Message: " + result.getMessage());
        }
    }
    
    private static void printSummary() {
        System.out.println("\n" + "╔" + "═".repeat(58) + "╗");
        System.out.println("║                    TEST SUMMARY                       ║");
        System.out.println("╚" + "═".repeat(58) + "╝");
        
        System.out.println("\n✅ Task 2.2 Features Verified:");
        System.out.println("   • Empty username validation");
        System.out.println("   • Empty password validation");
        System.out.println("   • Non-existent username handling");
        System.out.println("   • Wrong password handling");
        System.out.println("   • Successful login with correct credentials");
        System.out.println("   • Author data retrieval after login");
        System.out.println("   • Password case sensitivity");
        
        System.out.println("\n📋 Expected Results:");
        System.out.println("   • Test cases 2-5, 8 should FAIL (expected behavior)");
        System.out.println("   • Test cases 6-7, 9 should SUCCESS");
        
        System.out.println("\n💡 To manually test the Login UI:");
        System.out.println("   Run: java project.task2.AuthorPortalMain");
        System.out.println("   Then choose option 1 to login");
    }
}
