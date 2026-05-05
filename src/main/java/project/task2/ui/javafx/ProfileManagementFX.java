package project.task2.ui.javafx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import project.task2.model.AuthorAccount;
import project.task2.service.AuthorPortalService;
import project.task2.utils.PasswordUtils;
import project.task2.utils.ProfilePictureManager;

import java.io.File;
import java.util.function.Consumer;

public class ProfileManagementFX {
    private AuthorPortalService authorService;
    private AuthorAccount currentAuthor;
    private Stage stage;
    private Consumer<AuthorAccount> onProfileUpdated;
    private Runnable onPictureUpdated;
    
    private String currentFullName;
    private String currentBio;
    private Label infoNameLabel;
    private Label infoBioLabel;
    private ImageView profileImageView;
    private String currentProfilePicturePath;
    private Label profilePictureStatus;
    
    private VBox profileContent;
    private VBox passwordContent;
    private Button profileTabBtn;
    private Button passwordTabBtn;
    
    private ProgressBar strengthMeter;
    private Label strengthLabel;
    
    private String activeButtonStyle = "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: 600; " +
                                       "-fx-background-radius: 10px; -fx-padding: 10px 24px; -fx-cursor: hand; -fx-font-size: 13px;";
    private String inactiveButtonStyle = "-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-weight: 600; " +
                                         "-fx-background-radius: 10px; -fx-padding: 10px 24px; -fx-cursor: hand; -fx-font-size: 13px;";
    
    private static final long MAX_FILE_SIZE_BYTES = 3 * 1024 * 1024;
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png"};

    public ProfileManagementFX(AuthorAccount author, Consumer<AuthorAccount> onProfileUpdated) {
        this(author, onProfileUpdated, null);
    }
    
    public ProfileManagementFX(AuthorAccount author, Consumer<AuthorAccount> onProfileUpdated, Runnable onPictureUpdated) {
        this.currentAuthor = author;
        this.currentFullName = author.getFullName();
        this.currentBio = author.getBio();
        this.onProfileUpdated = onProfileUpdated;
        this.onPictureUpdated = onPictureUpdated;
        this.authorService = new AuthorPortalService();
        this.stage = new Stage();
        this.currentProfilePicturePath = ProfilePictureManager.getProfilePicturePath(author.getUsername());
    }

    public Stage getStage() {
        return stage;
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

        Label titleLabel = new Label("👤 Profile Management");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        Label subtitleLabel = new Label("Manage your personal information and security settings");
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
        
        VBox titleSection = new VBox(8);
        titleSection.setAlignment(Pos.CENTER);
        titleSection.getChildren().addAll(titleLabel, subtitleLabel);

        VBox infoCard = createInfoCard();
        VBox tabContainer = createTabContainer();
        
        mainContent.getChildren().addAll(titleSection, infoCard, tabContainer);
        
        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        root.setCenter(scrollPane);

        Scene scene = new Scene(root, 600, 850);
        scene.getStylesheets().add(getClass().getResource("/project/task2/css/author-portal.css").toExternalForm());
        
        stage.setTitle("Profile Management - " + currentAuthor.getUsername());
        stage.setScene(scene);
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
        card.setMaxWidth(500);

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label headerIcon = new Label("📋");
        headerIcon.setStyle("-fx-font-size: 18px");
        Label headerTitle = new Label("Current Information");
        headerTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #1e293b;");
        header.getChildren().addAll(headerIcon, headerTitle);
        
        HBox profileSection = new HBox(15);
        profileSection.setAlignment(Pos.CENTER_LEFT);
        
        profileImageView = new ImageView();
        profileImageView.setFitWidth(60);
        profileImageView.setFitHeight(60);
        profileImageView.setPreserveRatio(true);
        profileImageView.setStyle("-fx-border-color: #e2e8f0; -fx-border-radius: 30px; -fx-background-radius: 30px;");
        
        loadProfilePicture();
        
        VBox profileText = new VBox(4);
        Label profileLabel = new Label("Profile Picture");
        profileLabel.setStyle("-fx-font-weight: 500; -fx-text-fill: #334155;");
        profilePictureStatus = new Label(currentProfilePicturePath != null ? "✓ Picture uploaded" : "No picture uploaded");
        profilePictureStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        profileText.getChildren().addAll(profileLabel, profilePictureStatus);
        
        profileSection.getChildren().addAll(profileImageView, profileText);

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

        card.getChildren().addAll(header, profileSection, infoGrid);
        return card;
    }
    
    private void loadProfilePicture() {
        Image image = ProfilePictureManager.loadProfilePicture(currentAuthor.getUsername(), 60, 60);
        if (image != null) {
            profileImageView.setImage(image);
        } else {
            profileImageView.setImage(null);
        }
    }
    
    private void refreshProfilePicture() {
        loadProfilePicture();
        if (onPictureUpdated != null) {
            onPictureUpdated.run();
        }
    }
    
    private boolean isValidImageFile(File file) {
        String fileName = file.getName().toLowerCase();
        boolean validExtension = false;
        for (String ext : ALLOWED_EXTENSIONS) {
            if (fileName.endsWith(ext)) {
                validExtension = true;
                break;
            }
        }
        
        if (!validExtension) {
            showLocalMessage(profilePictureStatus, "❌ Invalid format! Use JPG or PNG", "status-rejected");
            return false;
        }
        
        if (file.length() > MAX_FILE_SIZE_BYTES) {
            double sizeMB = file.length() / (1024.0 * 1024.0);
            showLocalMessage(profilePictureStatus, String.format("❌ File too large! %.2fMB (max 3MB)", sizeMB), "status-rejected");
            return false;
        }
        
        return true;
    }
    
    private void saveProfilePicture(File sourceFile) {
        boolean success = ProfilePictureManager.saveProfilePicture(currentAuthor.getUsername(), sourceFile);
        if (success) {
            currentProfilePicturePath = ProfilePictureManager.getProfilePicturePath(currentAuthor.getUsername());
            profilePictureStatus.setText("✓ Picture uploaded");
            profilePictureStatus.setStyle("-fx-text-fill: #10b981; -fx-font-size: 11px;");
            refreshProfilePicture();
            showLocalMessage(profilePictureStatus, "✅ Profile picture uploaded!", "status-approved");
        } else {
            showLocalMessage(profilePictureStatus, "❌ Failed to save picture", "status-rejected");
        }
    }
    
    private void removeProfilePicture() {
        boolean success = ProfilePictureManager.deleteProfilePicture(currentAuthor.getUsername());
        if (success) {
            currentProfilePicturePath = null;
            refreshProfilePicture();
            profilePictureStatus.setText("No picture uploaded");
            profilePictureStatus.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
            showLocalMessage(profilePictureStatus, "✅ Profile picture removed", "status-approved");
        }
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

    private VBox createTabContainer() {
        VBox container = new VBox(20);
        container.setMaxWidth(500);
        
        HBox tabButtons = new HBox(15);
        tabButtons.setAlignment(Pos.CENTER);
        tabButtons.setPadding(new Insets(0, 0, 10, 0));
        
        profileTabBtn = new Button("✏️ Edit Profile");
        passwordTabBtn = new Button("🔐 Change Password");
        
        profileTabBtn.setStyle(activeButtonStyle);
        passwordTabBtn.setStyle(inactiveButtonStyle);
        
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
        
        VBox pictureBox = new VBox(10);
        Label pictureLabel = new Label("Profile Picture");
        pictureLabel.setStyle("-fx-font-weight: 500; -fx-text-fill: #334155;");
        
        HBox pictureButtons = new HBox(10);
        Button uploadBtn = new Button("📷 Upload Picture");
        uploadBtn.getStyleClass().addAll("button", "secondary-btn");
        uploadBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Profile Picture");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png")
            );
            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null && isValidImageFile(selectedFile)) {
                saveProfilePicture(selectedFile);
            }
        });
        
        Button removePictureBtn = new Button("🗑️ Remove Picture");
        removePictureBtn.getStyleClass().addAll("button", "danger-btn");
        removePictureBtn.setOnAction(e -> removeProfilePicture());
        
        pictureButtons.getChildren().addAll(uploadBtn, removePictureBtn);
        
        Label pictureHint = new Label("Formats: JPG, PNG | Max size: 3MB");
        pictureHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        
        pictureBox.getChildren().addAll(pictureLabel, pictureButtons, pictureHint);
        
        Separator separator1 = new Separator();
        separator1.setStyle("-fx-background-color: #e2e8f0;");
        
        VBox nameBox = new VBox(6);
        Label nameLabel = new Label("Full Name");
        nameLabel.setStyle("-fx-font-weight: 500; -fx-text-fill: #334155;");
        TextField fullNameField = new TextField(currentFullName);
        fullNameField.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 10px; " +
                              "-fx-background-radius: 10px; -fx-padding: 10px 12px;");
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
        bioBox.getChildren().addAll(bioLabel, bioArea);
        
        Separator separator2 = new Separator();
        separator2.setStyle("-fx-background-color: #e2e8f0;");
        
        VBox verifyBox = new VBox(6);
        Label verifyLabel = new Label("🔐 Verification Required");
        verifyLabel.setStyle("-fx-font-weight: 500; -fx-text-fill: #475569;");
        PasswordField verifyPasswordField = new PasswordField();
        verifyPasswordField.setPromptText("Enter your current password to save changes");
        verifyPasswordField.setStyle("-fx-background-color: white; -fx-border-color: #cbd5e1; -fx-border-radius: 10px; " +
                                     "-fx-background-radius: 10px; -fx-padding: 10px 12px;");
        verifyBox.getChildren().addAll(verifyLabel, verifyPasswordField);
        
        Label localMessage = new Label();
        localMessage.setWrapText(true);
        localMessage.setVisible(false);
        
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button saveBtn = new Button("💾 Save Changes");
        Button resetBtn = new Button("↺ Reset");
        
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
                    currentAuthor.isDisabled(),
                    currentAuthor.getLastLogin(),
                    currentAuthor.getProfilePicturePath(),
                    newBio
                );
                
                boolean success = authorService.updateProfile(updatedAuthor);
                
                if (success) {
                    currentFullName = newFullName;
                    currentBio = newBio;
                    updateInfoDisplay();
                    
                    if (onProfileUpdated != null) {
                        onProfileUpdated.accept(updatedAuthor);
                    }
                    
                    showLocalMessage(localMessage, "✅ Profile updated successfully!", "status-approved");
                    
                    new Thread(() -> {
                        try { Thread.sleep(1500); } catch (InterruptedException ex) {}
                        javafx.application.Platform.runLater(() -> stage.close());
                    }).start();
                } else {
                    showLocalMessage(localMessage, "❌ Failed to update profile", "status-rejected");
                }
            } catch (Exception ex) {
                showLocalMessage(localMessage, "❌ Error: " + ex.getMessage(), "status-rejected");
            }
        });
        
        pane.getChildren().addAll(pictureBox, separator1, nameBox, bioBox, separator2, verifyBox, localMessage, buttonBox);
        return pane;
    }

    private VBox createPasswordEditPane() {
        VBox pane = new VBox(18);
        pane.setPadding(new Insets(10, 0, 10, 0));
        
        VBox currentBox = new VBox(6);
        Label currentLabel = new Label("Current Password");
        PasswordField currentPasswordField = new PasswordField();
        currentBox.getChildren().addAll(currentLabel, currentPasswordField);
        
        VBox newBox = new VBox(6);
        Label newLabel = new Label("New Password");
        PasswordField newPasswordField = new PasswordField();
        
        HBox strengthBox = new HBox(10);
        strengthBox.setAlignment(Pos.CENTER_LEFT);
        strengthMeter = new ProgressBar(0);
        strengthMeter.setPrefWidth(200);
        strengthLabel = new Label("Very Weak");
        strengthBox.getChildren().addAll(strengthMeter, strengthLabel);
        
        newPasswordField.textProperty().addListener((obs, old, newVal) -> updatePasswordStrength(newVal));
        newBox.getChildren().addAll(newLabel, newPasswordField, strengthBox);
        
        VBox confirmBox = new VBox(6);
        Label confirmLabel = new Label("Confirm New Password");
        PasswordField confirmPasswordField = new PasswordField();
        
        Label matchLabel = new Label();
        matchLabel.setVisible(false);
        confirmPasswordField.textProperty().addListener((obs, old, newVal) -> {
            String newPass = newPasswordField.getText();
            if (!newVal.isEmpty()) {
                matchLabel.setVisible(true);
                if (newVal.equals(newPass)) {
                    matchLabel.setText("✓ Passwords match");
                    matchLabel.setStyle("-fx-text-fill: #10b981");
                } else {
                    matchLabel.setText("✗ Passwords do not match");
                    matchLabel.setStyle("-fx-text-fill: #ef4444");
                }
            } else {
                matchLabel.setVisible(false);
            }
        });
        confirmBox.getChildren().addAll(confirmLabel, confirmPasswordField, matchLabel);
        
        VBox reqBox = new VBox(8);
        reqBox.setStyle("-fx-background-color: #fef9e7; -fx-background-radius: 12px; -fx-padding: 12px; -fx-border-color: #fed7aa");
        Label reqTitle = new Label("📋 Password Requirements");
        reqTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #b45309");
        VBox reqList = new VBox(4);
        reqList.setPadding(new Insets(5, 0, 0, 15));
        reqList.getChildren().addAll(
            new Label("✓ At least 8 characters"),
            new Label("✓ At least one letter"),
            new Label("✓ At least one number"),
            new Label("✓ At least one uppercase letter")
        );
        reqBox.getChildren().addAll(reqTitle, reqList);
        
        Label localMessage = new Label();
        localMessage.setVisible(false);
        
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button changeBtn = new Button("🔐 Change Password");
        Button clearBtn = new Button("Clear");
        buttonBox.getChildren().addAll(changeBtn, clearBtn);
        
        clearBtn.setOnAction(e -> {
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
            matchLabel.setVisible(false);
            localMessage.setVisible(false);
            updatePasswordStrength("");
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
                    currentAuthor.isDisabled(),
                    currentAuthor.getLastLogin(),
                    currentAuthor.getProfilePicturePath(),
                    currentBio
                );
                
                boolean success = authorService.updateProfile(updatedAuthor);
                
                if (success) {
                    showLocalMessage(localMessage, "✅ Password changed successfully! You will be logged out.", "status-approved");
                    
                    new Thread(() -> {
                        try { Thread.sleep(2000); } catch (InterruptedException ex) {}
                        javafx.application.Platform.runLater(() -> {
                            stage.close();
                            if (onProfileUpdated != null) {
                                onProfileUpdated.accept(null);
                            }
                        });
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
    
    private void updatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            strengthMeter.setProgress(0);
            strengthLabel.setText("Very Weak");
            strengthLabel.setStyle("-fx-text-fill: #ef4444");
            strengthMeter.setStyle("-fx-accent: #ef4444");
            return;
        }
        
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*\\d.*")) score++;
        if (password.matches(".*[!@#$%^&*].*")) score++;
        
        double strength = Math.min(score / 7.0, 1.0);
        strengthMeter.setProgress(strength);
        
        if (strength < 0.3) {
            strengthMeter.setStyle("-fx-accent: #ef4444");
            strengthLabel.setText("Very Weak");
            strengthLabel.setStyle("-fx-text-fill: #ef4444");
        } else if (strength < 0.5) {
            strengthMeter.setStyle("-fx-accent: #f97316");
            strengthLabel.setText("Weak");
            strengthLabel.setStyle("-fx-text-fill: #f97316");
        } else if (strength < 0.7) {
            strengthMeter.setStyle("-fx-accent: #eab308");
            strengthLabel.setText("Fair");
            strengthLabel.setStyle("-fx-text-fill: #eab308");
        } else if (strength < 0.85) {
            strengthMeter.setStyle("-fx-accent: #22c55e");
            strengthLabel.setText("Good");
            strengthLabel.setStyle("-fx-text-fill: #22c55e");
        } else {
            strengthMeter.setStyle("-fx-accent: #10b981");
            strengthLabel.setText("Strong");
            strengthLabel.setStyle("-fx-text-fill: #10b981");
        }
    }

    private void showLocalMessage(Label label, String message, String styleClass) {
        label.setText(message);
        label.getStyleClass().setAll("status", styleClass);
        label.setVisible(true);
        
        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException e) {}
            javafx.application.Platform.runLater(() -> label.setVisible(false));
        }).start();
    }
}
