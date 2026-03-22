package project.task2.ui.javafx;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import project.task2.model.AuthorAccount;
import project.task2.service.AuthorPortalService;
import project.task2.utils.PasswordUtils;

import java.util.function.Consumer;

public class ProfileManagementFX {
    private AuthorPortalService authorService;
    private AuthorAccount currentAuthor;
    private Stage stage;
    private Consumer<AuthorAccount> onProfileUpdated;
    
    private String currentFullName;
    private String currentBio;
    private Label infoNameLabel;
    private Label infoBioLabel;
    
    private VBox profileContent;
    private VBox passwordContent;
    private Button profileTabBtn;
    private Button passwordTabBtn;
    private String activeButtonStyle = "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: 600; " +
                                       "-fx-background-radius: 10px; -fx-padding: 10px 24px; -fx-cursor: hand; -fx-font-size: 13px;";
    private String inactiveButtonStyle = "-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-weight: 600; " +
                                         "-fx-background-radius: 10px; -fx-padding: 10px 24px; -fx-cursor: hand; -fx-font-size: 13px;";

    public ProfileManagementFX(AuthorAccount author, Consumer<AuthorAccount> onProfileUpdated) {
        this.currentAuthor = author;
        this.currentFullName = author.getFullName();
        this.currentBio = author.getBio();
        this.onProfileUpdated = onProfileUpdated;
        this.authorService = new AuthorPortalService();
        this.stage = new Stage();
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #f8fafc, #eef2f7);");

        HBox topBar = createTopBar();
        root.setTop(topBar);

        VBox mainContent = new VBox(25);
        mainContent.setAlignment(Pos.TOP_CENTER);
        mainContent.setPadding(new Insets(20, 30, 30, 30));

        VBox titleSection = new VBox(8);
        titleSection.setAlignment(Pos.CENTER);
        
        Label titleLabel = new Label("👤 Profile Management");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        
        Label subtitleLabel = new Label("Manage your personal information and security settings");
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
        
        titleSection.getChildren().addAll(titleLabel, subtitleLabel);

        VBox infoCard = createInfoCard();
        
        VBox tabContainer = createStyledTabContainer();
        
        mainContent.getChildren().addAll(titleSection, infoCard, tabContainer);
        
        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.setCenter(scrollPane);

        Scene scene = new Scene(root, 520, 650);
        scene.getStylesheets().add(getClass().getResource("/project/task2/css/author-portal.css").toExternalForm());
        
        stage.setTitle("Profile Management - " + currentAuthor.getUsername());
        stage.setScene(scene);
        
        // FIX: Proper close handling - just close the window, don't do anything else
        stage.setOnCloseRequest(e -> {
            stage.close();
        });
        
        stage.show();
    }

    private HBox createTopBar() {
        HBox topBar = new HBox();
        topBar.setPadding(new Insets(12, 24, 12, 24));
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");

        String initial = currentAuthor.getUsername().substring(0, 1).toUpperCase();
        Label avatar = new Label(initial);
        avatar.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; " +
                       "-fx-padding: 8px; -fx-background-radius: 20px; -fx-font-size: 14px;");
        avatar.setPrefSize(36, 36);
        avatar.setAlignment(Pos.CENTER);

        Label usernameLabel = new Label(currentAuthor.getUsername());
        usernameLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #1e293b;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-size: 18px; -fx-cursor: hand;");
        // FIX: Simple close - just close the stage
        closeBtn.setOnAction(e -> stage.close());

        HBox userInfo = new HBox(12);
        userInfo.setAlignment(Pos.CENTER);
        userInfo.getChildren().addAll(avatar, usernameLabel);
        
        topBar.getChildren().addAll(userInfo, spacer, closeBtn);
        return topBar;
    }

    private VBox createInfoCard() {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16px; " +
                     "-fx-border-color: #e2e8f0; -fx-border-radius: 16px; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);");
        card.setPadding(new Insets(20));
        card.setMaxWidth(460);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label headerIcon = new Label("📋");
        headerIcon.setStyle("-fx-font-size: 18px;");
        Label headerTitle = new Label("Current Information");
        headerTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");
        header.getChildren().addAll(headerIcon, headerTitle);

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(15);
        infoGrid.setVgap(12);
        infoGrid.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12px; -fx-padding: 15px;");

        Label usernameIcon = new Label("👤");
        usernameIcon.setStyle("-fx-font-size: 14px;");
        Label usernameValue = new Label(currentAuthor.getUsername());
        usernameValue.setStyle("-fx-font-family: monospace; -fx-font-weight: 500; -fx-text-fill: #1e293b;");
        
        Label nameIcon = new Label("📝");
        nameIcon.setStyle("-fx-font-size: 14px;");
        infoNameLabel = new Label(currentFullName);
        infoNameLabel.setStyle("-fx-font-weight: 500; -fx-text-fill: #1e293b;");
        
        Label bioIcon = new Label("📖");
        bioIcon.setStyle("-fx-font-size: 14px;");
        String bioText = currentBio.isEmpty() ? "Not provided" : currentBio;
        infoBioLabel = new Label(bioText);
        infoBioLabel.setWrapText(true);
        infoBioLabel.setStyle("-fx-text-fill: #475569;");
        
        infoGrid.add(usernameIcon, 0, 0);
        infoGrid.add(usernameValue, 1, 0);
        infoGrid.add(nameIcon, 0, 1);
        infoGrid.add(infoNameLabel, 1, 1);
        infoGrid.add(bioIcon, 0, 2);
        infoGrid.add(infoBioLabel, 1, 2);
        
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setPrefWidth(40);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        infoGrid.getColumnConstraints().addAll(col1, col2);

        card.getChildren().addAll(header, infoGrid);
        return card;
    }

    private void updateInfoDisplay() {
        if (infoNameLabel != null) {
            infoNameLabel.setText(currentFullName);
        }
        if (infoBioLabel != null) {
            String bioText = currentBio.isEmpty() ? "Not provided" : currentBio;
            infoBioLabel.setText(bioText);
        }
    }

    private VBox createStyledTabContainer() {
        VBox container = new VBox(20);
        container.setMaxWidth(460);
        
        HBox tabButtons = new HBox(15);
        tabButtons.setAlignment(Pos.CENTER);
        tabButtons.setPadding(new Insets(0, 0, 10, 0));
        
        profileTabBtn = new Button("✏️ Edit Profile");
        passwordTabBtn = new Button("🔐 Change Password");
        
        profileTabBtn.setStyle(activeButtonStyle);
        passwordTabBtn.setStyle(inactiveButtonStyle);
        
        profileTabBtn.setOnMouseEntered(e -> {
            if (!profileTabBtn.getStyle().contains("#2563eb")) {
                profileTabBtn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #1e293b; -fx-font-weight: 600; " +
                                      "-fx-background-radius: 10px; -fx-padding: 10px 24px; -fx-cursor: hand; -fx-font-size: 13px;");
            }
        });
        profileTabBtn.setOnMouseExited(e -> {
            if (!profileTabBtn.getStyle().contains("#2563eb")) {
                profileTabBtn.setStyle(inactiveButtonStyle);
            }
        });
        
        passwordTabBtn.setOnMouseEntered(e -> {
            if (!passwordTabBtn.getStyle().contains("#2563eb")) {
                passwordTabBtn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #1e293b; -fx-font-weight: 600; " +
                                       "-fx-background-radius: 10px; -fx-padding: 10px 24px; -fx-cursor: hand; -fx-font-size: 13px;");
            }
        });
        passwordTabBtn.setOnMouseExited(e -> {
            if (!passwordTabBtn.getStyle().contains("#2563eb")) {
                passwordTabBtn.setStyle(inactiveButtonStyle);
            }
        });
        
        profileContent = createProfileEditPane();
        passwordContent = createPasswordEditPane();
        
        passwordContent.setVisible(false);
        passwordContent.setManaged(false);
        
        profileTabBtn.setOnAction(e -> {
            profileTabBtn.setStyle(activeButtonStyle);
            passwordTabBtn.setStyle(inactiveButtonStyle);
            profileContent.setVisible(true);
            profileContent.setManaged(true);
            passwordContent.setVisible(false);
            passwordContent.setManaged(false);
        });
        
        passwordTabBtn.setOnAction(e -> {
            passwordTabBtn.setStyle(activeButtonStyle);
            profileTabBtn.setStyle(inactiveButtonStyle);
            passwordContent.setVisible(true);
            passwordContent.setManaged(true);
            profileContent.setVisible(false);
            profileContent.setManaged(false);
        });
        
        tabButtons.getChildren().addAll(profileTabBtn, passwordTabBtn);
        
        container.getChildren().addAll(tabButtons, profileContent, passwordContent);
        return container;
    }

    private VBox createProfileEditPane() {
        VBox pane = new VBox(20);
        pane.setPadding(new Insets(10, 0, 10, 0));
        
        VBox nameBox = new VBox(6);
        Label nameLabel = new Label("Full Name");
        nameLabel.setStyle("-fx-font-weight: 500; -fx-text-fill: #334155;");
        TextField fullNameField = new TextField(currentFullName);
        fullNameField.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 10px; " +
                              "-fx-background-radius: 10px; -fx-padding: 10px 12px;");
        fullNameField.setPromptText("Enter your full name");
        fullNameField.setMaxWidth(Double.MAX_VALUE);
        nameBox.getChildren().addAll(nameLabel, fullNameField);
        
        VBox bioBox = new VBox(6);
        Label bioLabel = new Label("Bio");
        bioLabel.setStyle("-fx-font-weight: 500; -fx-text-fill: #334155;");
        TextArea bioArea = new TextArea(currentBio);
        bioArea.setPromptText("Tell us about yourself...");
        bioArea.setPrefRowCount(4);
        bioArea.setWrapText(true);
        bioArea.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 10px; " +
                        "-fx-background-radius: 10px; -fx-padding: 10px 12px;");
        bioArea.setMaxWidth(Double.MAX_VALUE);
        bioBox.getChildren().addAll(bioLabel, bioArea);
        
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #e2e8f0;");
        
        VBox verifyBox = new VBox(6);
        Label verifyLabel = new Label("🔐 Verification Required");
        verifyLabel.setStyle("-fx-font-weight: 500; -fx-text-fill: #475569;");
        PasswordField verifyPasswordField = new PasswordField();
        verifyPasswordField.setPromptText("Enter your current password to save changes");
        verifyPasswordField.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 10px; " +
                                     "-fx-background-radius: 10px; -fx-padding: 10px 12px;");
        verifyPasswordField.setMaxWidth(Double.MAX_VALUE);
        verifyBox.getChildren().addAll(verifyLabel, verifyPasswordField);
        
        Label localMessage = new Label();
        localMessage.setWrapText(true);
        localMessage.setVisible(false);
        
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button saveBtn = new Button("💾 Save Changes");
        saveBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-background-radius: 10px; -fx-padding: 10px 24px; -fx-cursor: hand;");
        saveBtn.setOnMouseEntered(e -> saveBtn.setStyle("-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-font-weight: bold; " +
                                                       "-fx-background-radius: 10px; -fx-padding: 10px 24px;"));
        saveBtn.setOnMouseExited(e -> saveBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; " +
                                                      "-fx-background-radius: 10px; -fx-padding: 10px 24px;"));
        
        Button resetBtn = new Button("↺ Reset");
        resetBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-weight: 500; " +
                         "-fx-background-radius: 10px; -fx-padding: 10px 20px; -fx-cursor: hand;");
        resetBtn.setOnMouseEntered(e -> resetBtn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-font-weight: 500; " +
                                                         "-fx-background-radius: 10px; -fx-padding: 10px 20px;"));
        resetBtn.setOnMouseExited(e -> resetBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-weight: 500; " +
                                                        "-fx-background-radius: 10px; -fx-padding: 10px 20px;"));
        
        buttonBox.getChildren().addAll(saveBtn, resetBtn);
        
        resetBtn.setOnAction(e -> {
            fullNameField.setText(currentFullName);
            bioArea.setText(currentBio);
            verifyPasswordField.clear();
            localMessage.setVisible(false);
        });
        
        saveBtn.setOnAction(e -> {
            String newFullName = fullNameField.getText().trim();
            String newBio = bioArea.getText().trim();
            String currentPassword = verifyPasswordField.getText();
            
            if (newFullName.isEmpty()) {
                showLocalMessage(localMessage, "❌ Full name cannot be empty", "status-rejected");
                return;
            }
            
            if (currentPassword.isEmpty()) {
                showLocalMessage(localMessage, "❌ Current password is required", "status-rejected");
                return;
            }
            
            AuthorPortalService.LoginResult verifyResult = authorService.login(currentAuthor.getUsername(), currentPassword);
            if (!verifyResult.isSuccess()) {
                showLocalMessage(localMessage, "❌ Current password is incorrect", "status-rejected");
                return;
            }
            
            if (newFullName.equals(currentFullName) && newBio.equals(currentBio)) {
                showLocalMessage(localMessage, "ℹ️ No changes to save", "status-pending");
                return;
            }
            
            try {
                AuthorAccount updatedAuthor = new AuthorAccount(
                    currentAuthor.getUsername(),
                    newFullName,
                    currentAuthor.getPasswordSalt(),
                    currentAuthor.getPasswordHash(),
                    newBio
                );
                
                AuthorAccount result = authorService.updateProfile(updatedAuthor);
                
                if (result != null) {
                    currentFullName = newFullName;
                    currentBio = newBio;
                    updateInfoDisplay();
                    
                    if (onProfileUpdated != null) {
                        onProfileUpdated.accept(result);
                    }
                    
                    showLocalMessage(localMessage, "✅ Profile updated successfully!", "status-approved");
                    new Thread(() -> {
                        try { Thread.sleep(1500); } catch (InterruptedException ex) {}
                        Platform.runLater(() -> stage.close());
                    }).start();
                } else {
                    showLocalMessage(localMessage, "❌ Failed to update profile", "status-rejected");
                }
            } catch (Exception ex) {
                showLocalMessage(localMessage, "❌ Error: " + ex.getMessage(), "status-rejected");
            }
        });
        
        pane.getChildren().addAll(nameBox, bioBox, separator, verifyBox, localMessage, buttonBox);
        return pane;
    }

    private VBox createPasswordEditPane() {
        VBox pane = new VBox(18);
        pane.setPadding(new Insets(10, 0, 10, 0));
        
        VBox currentBox = new VBox(6);
        Label currentLabel = new Label("Current Password");
        currentLabel.setStyle("-fx-font-weight: 500; -fx-text-fill: #334155;");
        PasswordField currentPasswordField = new PasswordField();
        currentPasswordField.setPromptText("Enter your current password");
        currentPasswordField.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 10px; " +
                                      "-fx-background-radius: 10px; -fx-padding: 10px 12px;");
        currentPasswordField.setMaxWidth(Double.MAX_VALUE);
        currentBox.getChildren().addAll(currentLabel, currentPasswordField);
        
        VBox newBox = new VBox(6);
        Label newLabel = new Label("New Password");
        newLabel.setStyle("-fx-font-weight: 500; -fx-text-fill: #334155;");
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Create a strong password");
        newPasswordField.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 10px; " +
                                  "-fx-background-radius: 10px; -fx-padding: 10px 12px;");
        newPasswordField.setMaxWidth(Double.MAX_VALUE);
        newBox.getChildren().addAll(newLabel, newPasswordField);
        
        VBox confirmBox = new VBox(6);
        Label confirmLabel = new Label("Confirm New Password");
        confirmLabel.setStyle("-fx-font-weight: 500; -fx-text-fill: #334155;");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Re-enter your new password");
        confirmPasswordField.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 10px; " +
                                      "-fx-background-radius: 10px; -fx-padding: 10px 12px;");
        confirmPasswordField.setMaxWidth(Double.MAX_VALUE);
        confirmBox.getChildren().addAll(confirmLabel, confirmPasswordField);
        
        VBox reqBox = new VBox(8);
        reqBox.setStyle("-fx-background-color: #fef9e7; -fx-background-radius: 12px; -fx-padding: 12px; -fx-border-color: #fed7aa; -fx-border-radius: 12px;");
        
        Label reqTitle = new Label("📋 Password Requirements");
        reqTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #b45309;");
        
        VBox reqList = new VBox(4);
        reqList.setPadding(new Insets(5, 0, 0, 15));
        
        Label req1 = new Label("✓ At least 8 characters");
        Label req2 = new Label("✓ At least one letter");
        Label req3 = new Label("✓ At least one number");
        Label req4 = new Label("✓ At least one uppercase letter");
        
        req1.setStyle("-fx-font-size: 11px; -fx-text-fill: #92400e;");
        req2.setStyle("-fx-font-size: 11px; -fx-text-fill: #92400e;");
        req3.setStyle("-fx-font-size: 11px; -fx-text-fill: #92400e;");
        req4.setStyle("-fx-font-size: 11px; -fx-text-fill: #92400e;");
        
        reqList.getChildren().addAll(req1, req2, req3, req4);
        reqBox.getChildren().addAll(reqTitle, reqList);
        
        Label localMessage = new Label();
        localMessage.setWrapText(true);
        localMessage.setVisible(false);
        
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button changeBtn = new Button("🔐 Change Password");
        changeBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; " +
                          "-fx-background-radius: 10px; -fx-padding: 10px 24px; -fx-cursor: hand;");
        changeBtn.setOnMouseEntered(e -> changeBtn.setStyle("-fx-background-color: #1d4ed8; -fx-text-fill: white; -fx-font-weight: bold; " +
                                                           "-fx-background-radius: 10px; -fx-padding: 10px 24px;"));
        changeBtn.setOnMouseExited(e -> changeBtn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; " +
                                                          "-fx-background-radius: 10px; -fx-padding: 10px 24px;"));
        
        Button clearBtn = new Button("Clear");
        clearBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-weight: 500; " +
                         "-fx-background-radius: 10px; -fx-padding: 10px 20px; -fx-cursor: hand;");
        clearBtn.setOnMouseEntered(e -> clearBtn.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-font-weight: 500; " +
                                                         "-fx-background-radius: 10px; -fx-padding: 10px 20px;"));
        clearBtn.setOnMouseExited(e -> clearBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-weight: 500; " +
                                                        "-fx-background-radius: 10px; -fx-padding: 10px 20px;"));
        
        buttonBox.getChildren().addAll(changeBtn, clearBtn);
        
        clearBtn.setOnAction(e -> {
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            localMessage.setVisible(false);
        });
        
        changeBtn.setOnAction(e -> {
            String currentPassword = currentPasswordField.getText();
            String newPassword = newPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText();
            
            if (currentPassword.isEmpty()) {
                showLocalMessage(localMessage, "❌ Current password is required", "status-rejected");
                return;
            }
            
            if (newPassword.isEmpty()) {
                showLocalMessage(localMessage, "❌ New password is required", "status-rejected");
                return;
            }
            
            if (!newPassword.equals(confirmPassword)) {
                showLocalMessage(localMessage, "❌ New passwords do not match", "status-rejected");
                return;
            }
            
            if (newPassword.equals(currentPassword)) {
                showLocalMessage(localMessage, "❌ New password must be different from current password", "status-rejected");
                return;
            }
            
            if (!PasswordUtils.isStrongPassword(newPassword)) {
                showLocalMessage(localMessage, "❌ " + PasswordUtils.getPasswordRequirements().replace("\n", " "), "status-rejected");
                return;
            }
            
            AuthorPortalService.LoginResult verifyResult = authorService.login(currentAuthor.getUsername(), currentPassword);
            if (!verifyResult.isSuccess()) {
                showLocalMessage(localMessage, "❌ Current password is incorrect", "status-rejected");
                return;
            }
            
            try {
                String salt = PasswordUtils.generateSalt();
                String hash = PasswordUtils.hashPassword(newPassword, salt);
                
                AuthorAccount updatedAuthor = new AuthorAccount(
                    currentAuthor.getUsername(),
                    currentFullName,
                    salt,
                    hash,
                    currentBio
                );
                
                AuthorAccount result = authorService.updateProfile(updatedAuthor);
                
                if (result != null) {
                    if (onProfileUpdated != null) {
                        onProfileUpdated.accept(result);
                    }
                    showLocalMessage(localMessage, "✅ Password changed successfully!", "status-approved");
                    new Thread(() -> {
                        try { Thread.sleep(1500); } catch (InterruptedException ex) {}
                        Platform.runLater(() -> stage.close());
                    }).start();
                } else {
                    showLocalMessage(localMessage, "❌ Failed to change password", "status-rejected");
                }
            } catch (Exception ex) {
                showLocalMessage(localMessage, "❌ Error: " + ex.getMessage(), "status-rejected");
            }
        });
        
        pane.getChildren().addAll(currentBox, newBox, confirmBox, reqBox, localMessage, buttonBox);
        return pane;
    }

    private void showLocalMessage(Label label, String message, String styleClass) {
        label.setText(message);
        label.getStyleClass().setAll("status", styleClass);
        label.setVisible(true);
    }
}
