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

public class AuthorRegistrationFX extends Application {
    private AuthorPortalService authorService;
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.authorService = new AuthorPortalService();
        this.primaryStage = primaryStage;
        
        // Handle window close event
        primaryStage.setOnCloseRequest(this::handleWindowClose);
        
        showRegistrationScreen();
    }

    private void handleWindowClose(WindowEvent event) {
        System.out.println("🚪 Closing Author Registration...");
        Platform.exit();
        System.exit(0);
    }

    private void showRegistrationScreen() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        VBox centerContent = new VBox(20);
        centerContent.setAlignment(Pos.TOP_CENTER);
        centerContent.setPadding(new Insets(30));

        Label titleLabel = new Label("📝 Author Registration");
        titleLabel.getStyleClass().add("page-title");

        Label subtitleLabel = new Label("Join our community of authors");
        subtitleLabel.getStyleClass().add("page-subtitle");

        VBox regCard = new VBox(20);
        regCard.getStyleClass().add("card");
        regCard.setMaxWidth(500);
        regCard.setPadding(new Insets(30));

        VBox usernameBox = createInputField("Username", "Choose a username (3-20 chars, letters/numbers/underscore)");
        TextField usernameField = (TextField) usernameBox.getChildren().get(1);

        VBox fullNameBox = createInputField("Full Name", "Enter your full name");
        TextField fullNameField = (TextField) fullNameBox.getChildren().get(1);

        VBox passwordBox = createPasswordField("Password", "Create a strong password");
        PasswordField passwordField = (PasswordField) passwordBox.getChildren().get(1);

        VBox confirmBox = createPasswordField("Confirm Password", "Re-enter your password");
        PasswordField confirmField = (PasswordField) confirmBox.getChildren().get(1);

        VBox bioBox = new VBox(5);
        Label bioLabel = new Label("Bio (Optional)");
        bioLabel.getStyleClass().add("muted");
        TextArea bioArea = new TextArea();
        bioArea.setPromptText("Tell us about yourself and your writing");
        bioArea.setPrefRowCount(4);
        bioArea.getStyleClass().add("text-area");
        bioBox.getChildren().addAll(bioLabel, bioArea);

        VBox reqBox = new VBox(5);
        reqBox.setPadding(new Insets(10));
        reqBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8px;");
        
        Label reqTitle = new Label("Password Requirements:");
        reqTitle.setStyle("-fx-font-weight: bold;");
        
        Label req1 = new Label("• At least 8 characters long");
        Label req2 = new Label("• At least one letter");
        Label req3 = new Label("• At least one number");
        Label req4 = new Label("• At least one uppercase letter");
        
        req1.getStyleClass().add("muted");
        req2.getStyleClass().add("muted");
        req3.getStyleClass().add("muted");
        req4.getStyleClass().add("muted");
        
        reqBox.getChildren().addAll(reqTitle, req1, req2, req3, req4);

        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button registerBtn = new Button("Create Account");
        registerBtn.getStyleClass().addAll("button", "primary-btn");
        registerBtn.setPrefWidth(180);

        Button backBtn = new Button("Back to Login");
        backBtn.getStyleClass().addAll("button", "secondary-btn");
        backBtn.setPrefWidth(180);

        buttonBox.getChildren().addAll(registerBtn, backBtn);

        Label messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setVisible(false);

        regCard.getChildren().addAll(usernameBox, fullNameBox, passwordBox, confirmBox, 
                                     reqBox, bioBox, buttonBox, messageLabel);
        
        centerContent.getChildren().addAll(titleLabel, subtitleLabel, regCard);
        root.setCenter(centerContent);

        registerBtn.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String fullName = fullNameField.getText().trim();
            String password = passwordField.getText();
            String confirm = confirmField.getText();
            String bio = bioArea.getText().trim();

            AuthorPortalService.RegistrationResult result = authorService.registerAuthor(
                username, fullName, password, confirm, bio
            );

            if (result.isSuccess()) {
                showMessage(messageLabel, result.getMessage(), "status-approved");
                usernameField.clear();
                fullNameField.clear();
                passwordField.clear();
                confirmField.clear();
                bioArea.clear();
            } else {
                showMessage(messageLabel, result.getMessage(), "status-rejected");
            }
        });

        backBtn.setOnAction(e -> {
            AuthorLoginFX loginUI = new AuthorLoginFX();
            try {
                loginUI.start(new Stage());
                primaryStage.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Scene scene = new Scene(scrollPane, 900, 700);
        scene.getStylesheets().add(getClass().getResource("/project/task2/css/author-portal.css").toExternalForm());
        
        primaryStage.setTitle("Author Portal - Registration");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createInputField(String label, String prompt) {
        VBox box = new VBox(5);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("muted");
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("text-field");
        box.getChildren().addAll(lbl, field);
        return box;
    }

    private VBox createPasswordField(String label, String prompt) {
        VBox box = new VBox(5);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("muted");
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.getStyleClass().add("password-field");
        box.getChildren().addAll(lbl, field);
        return box;
    }

    private void showMessage(Label label, String message, String styleClass) {
        label.setText(message);
        label.getStyleClass().setAll("status", styleClass);
        label.setVisible(true);
    }

    @Override
    public void stop() {
        System.out.println("🛑 Author Registration stopped");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
