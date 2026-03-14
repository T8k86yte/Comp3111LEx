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

public class AuthorLoginFX extends Application {
    private AuthorPortalService authorService;
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.authorService = new AuthorPortalService();
        this.primaryStage = primaryStage;
        
        // FIX: When X is clicked, only close this window
        // If this is the last window, the app will exit automatically
        primaryStage.setOnCloseRequest(this::handleWindowClose);
        
        showLoginScreen();
    }

    private void handleWindowClose(WindowEvent event) {
        System.out.println("🚪 Closing Author Login window...");
        // Let the window close naturally
        // Platform.exit() is NOT called here
    }

    private void showLoginScreen() {
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
                openDashboard(result.getAuthor());
            } else {
                showMessage(messageLabel, result.getMessage(), "status-rejected");
            }
        });

        registerBtn.setOnAction(e -> {
            AuthorRegistrationFX regUI = new AuthorRegistrationFX();
            try {
                regUI.start(new Stage());
                primaryStage.close(); // Close login window when opening registration
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

    private void openDashboard(AuthorAccount author) {
        AuthorDashboardFX dashboard = new AuthorDashboardFX(author);
        try {
            dashboard.start(new Stage());
            primaryStage.close(); // Close login window when opening dashboard
        } catch (Exception e) {
            e.printStackTrace();
        }
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
