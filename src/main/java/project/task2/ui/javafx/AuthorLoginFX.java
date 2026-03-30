package project.task2.ui.javafx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import project.task2.service.AuthorPortalService;
import project.task2.model.AuthorAccount;
import project.task2.utils.SessionManager;

public class AuthorLoginFX extends Application {
    private AuthorPortalService authorService;
    private Stage primaryStage;
    private String restoredScreen = null;

    @Override
    public void start(Stage primaryStage) {
        this.authorService = new AuthorPortalService();
        this.primaryStage = primaryStage;
        
        // Handle window close event
        primaryStage.setOnCloseRequest(this::handleWindowClose);
        
        // Load any saved session
        SessionManager.loadSession();
        
        // Check for saved session and auto-restore
        if (SessionManager.hasSavedSession()) {
            String username = SessionManager.getCurrentUsername();
            restoredScreen = SessionManager.getCurrentScreen();
            System.out.println("🔄 Auto-restoring session for: " + username + " to screen: " + restoredScreen);
            
            // Only show restoration notification if there's a screen to restore (not just dashboard)
            if (restoredScreen != null && !restoredScreen.equals("DASHBOARD")) {
                Alert notification = new Alert(Alert.AlertType.INFORMATION);
                notification.setTitle("Session Restored");
                notification.setHeaderText("✅ Previous Session Detected");
                notification.setContentText(
                    "Welcome back!\n\n" +
                    "Your previous session has been detected.\n" +
                    "Please log in again to continue.\n" +
                    "You were previously on: " + getScreenDisplayName(restoredScreen)
                );
                notification.showAndWait();
            } else {
                Alert notification = new Alert(Alert.AlertType.INFORMATION);
                notification.setTitle("Session Restored");
                notification.setHeaderText("✅ Previous Session Detected");
                notification.setContentText("Session data found. Please log in to restore your last state.");
                notification.showAndWait();
            }
            
            // Show login screen with username pre-filled
            showLoginScreenWithUsername(username);
        } else {
            restoredScreen = null;
            showLoginScreenWithUsername(null);
        }
    }
    
    private String getScreenDisplayName(String screen) {
        switch (screen) {
            case "PUBLISH_BOOK": return "Publish Book";
            case "MY_BOOKS": return "My Books";
            case "MY_SUBMISSIONS": return "My Submissions";
            case "PROFILE": return "Profile Management";
            case "NOTIFICATIONS": return "Notification Board";
            default: return "Dashboard";
        }
    }
    
    private void showLoginScreenWithUsername(String prefilledUsername) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        VBox centerContent = new VBox(20);
        centerContent.setAlignment(Pos.CENTER);
        centerContent.setPadding(new Insets(50));

        Label titleLabel = new Label("📚 Author Portal");
        titleLabel.getStyleClass().add("page-title");

        Label subtitleLabel = new Label("Sign in to your author account");
        subtitleLabel.getStyleClass().add("page-subtitle");

        VBox loginCard = new VBox(20);
        loginCard.getStyleClass().add("card");
        loginCard.setMaxWidth(400);
        loginCard.setPadding(new Insets(30));

        Label cardTitle = new Label("Author Login");
        cardTitle.getStyleClass().add("card-title");

        VBox usernameBox = new VBox(5);
        Label usernameLabel = new Label("Username");
        usernameLabel.getStyleClass().add("muted");
        TextField usernameField = new TextField();
        if (prefilledUsername != null) {
            usernameField.setText(prefilledUsername);
        }
        usernameField.setPromptText("Enter your username");
        usernameField.getStyleClass().add("text-field");
        usernameBox.getChildren().addAll(usernameLabel, usernameField);

        VBox passwordBox = new VBox(5);
        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("muted");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.getStyleClass().add("password-field");
        passwordBox.getChildren().addAll(passwordLabel, passwordField);

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button loginBtn = new Button("Sign In");
        loginBtn.getStyleClass().addAll("button", "primary-btn");
        loginBtn.setPrefWidth(150);

        Button registerBtn = new Button("Register");
        registerBtn.getStyleClass().addAll("button", "secondary-btn");
        registerBtn.setPrefWidth(150);

        buttonBox.getChildren().addAll(loginBtn, registerBtn);

        Label messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setVisible(false);

        loginCard.getChildren().addAll(cardTitle, usernameBox, passwordBox, buttonBox, messageLabel);
        centerContent.getChildren().addAll(titleLabel, subtitleLabel, loginCard);
        root.setCenter(centerContent);

        loginBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText();

            if (username.isEmpty() || password.isEmpty()) {
                showMessage(messageLabel, "Username and password are required", "status-rejected");
                return;
            }

            AuthorPortalService.LoginResult result = authorService.login(username, password);
            
            if (result.isSuccess()) {
                showMessage(messageLabel, "Login successful!", "status-approved");
                // Save session after successful login
                SessionManager.setCurrentUser(username, result.getAuthor().getFullName());
                SessionManager.setCurrentScreen("DASHBOARD", null);
                
                openDashboard(result.getAuthor());
            } else {
                showMessage(messageLabel, result.getMessage(), "status-rejected");
            }
        });

        registerBtn.setOnAction(e -> {
            AuthorRegistrationFX regUI = new AuthorRegistrationFX();
            try {
                regUI.start(new Stage());
                primaryStage.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(getClass().getResource("/project/task2/css/author-portal.css").toExternalForm());
        
        primaryStage.setTitle("Author Portal - Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void handleWindowClose(WindowEvent event) {
        System.out.println("🚪 Closing Author Login...");
        Platform.exit();
        System.exit(0);
    }

    private void openDashboard(AuthorAccount author) {
        AuthorDashboardFX dashboard = new AuthorDashboardFX(author);
        
        final String targetScreen = restoredScreen;
        
        try {
            Stage dashboardStage = new Stage();
            dashboard.start(dashboardStage);
            
            // Only navigate if there's a saved screen and it's not the dashboard
            if (targetScreen != null && !targetScreen.equals("DASHBOARD")) {
                // Use a slight delay to ensure dashboard is fully loaded
                new Thread(() -> {
                    try {
                        Thread.sleep(800);
                        Platform.runLater(() -> {
                            System.out.println("🔄 Navigating to restored screen: " + targetScreen);
                            if (isKnownScreen(targetScreen)) {
                                dashboard.navigateToScreen(targetScreen);
                                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                                ok.setTitle("Restore Complete");
                                ok.setHeaderText("✅ Restored successfully");
                                ok.setContentText("Returned to: " + getScreenDisplayName(targetScreen));
                                ok.showAndWait();
                            } else {
                                Alert fail = new Alert(Alert.AlertType.ERROR);
                                fail.setTitle("Restore Failed");
                                fail.setHeaderText("Could not restore last screen");
                                fail.setContentText("Fallback to dashboard.");
                                fail.showAndWait();
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
            
            primaryStage.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isKnownScreen(String screen) {
        return "DASHBOARD".equals(screen)
                || "PUBLISH_BOOK".equals(screen)
                || "MY_BOOKS".equals(screen)
                || "MY_SUBMISSIONS".equals(screen)
                || "PROFILE".equals(screen)
                || "NOTIFICATIONS".equals(screen);
    }

    private void showMessage(Label label, String message, String styleClass) {
        label.setText(message);
        label.getStyleClass().setAll("status", styleClass);
        label.setVisible(true);
    }

    @Override
    public void stop() {
        System.out.println("🛑 Author Login stopped");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
