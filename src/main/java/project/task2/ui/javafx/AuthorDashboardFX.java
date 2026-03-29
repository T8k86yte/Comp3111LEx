package project.task2.ui.javafx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import project.task2.model.AuthorAccount;
import project.task2.model.BookSubmission;
import project.task2.service.AuthorPortalService;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class AuthorDashboardFX extends Application {
    private AuthorPortalService authorService;
    private AuthorAccount currentAuthor;
    private Stage primaryStage;
    private Timer refreshTimer;
    private Stage submissionsStage;
    private VBox submissionsContainer;
    private Label statusLabel;
    
    private HBox statsBox;

    public AuthorDashboardFX(AuthorAccount author) {
        this.currentAuthor = author;
        this.authorService = new AuthorPortalService();
        System.out.println("📊 AuthorDashboardFX created for: " + author.getUsername());
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        HBox topBar = createTopBar();
        root.setTop(topBar);

        VBox centerContent = new VBox(30);
        centerContent.setAlignment(Pos.TOP_CENTER);
        centerContent.setPadding(new Insets(50));

        Label titleLabel = new Label("📊 Author Dashboard");
        titleLabel.getStyleClass().add("page-title");

        statsBox = createStatsBox();

        GridPane menuGrid = createMenuGrid();

        Button refreshDashboardBtn = new Button("🔄 Refresh Stats");
        refreshDashboardBtn.getStyleClass().addAll("button", "secondary-btn");
        refreshDashboardBtn.setOnAction(e -> refreshDashboardStats());

        centerContent.getChildren().addAll(titleLabel, statsBox, refreshDashboardBtn, menuGrid);
        root.setCenter(centerContent);

        Scene scene = new Scene(root, 1000, 650);
        scene.getStylesheets().add(getClass().getResource("/project/task2/css/author-portal.css").toExternalForm());
        
        primaryStage.setTitle("Author Dashboard");
        primaryStage.setScene(scene);
        
        primaryStage.setOnCloseRequest(this::handleWindowClose);
        
        primaryStage.show();

        startDashboardAutoRefresh();
    }

    private void handleWindowClose(WindowEvent event) {
        System.out.println("🚪 Closing Author Dashboard...");
        stopRefreshTimer();
    }

    private void stopRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer.purge();
            refreshTimer = null;
        }
    }

    private void startDashboardAutoRefresh() {
        stopRefreshTimer(); // Stop any existing timer
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (primaryStage != null && primaryStage.isShowing()) {
                    Platform.runLater(() -> refreshDashboardStats());
                } else {
                    stopRefreshTimer();
                }
            }
        }, 5000, 5000);
    }

    private void refreshDashboardStats() {
        if (primaryStage == null || !primaryStage.isShowing()) return;
        
        List<BookSubmission> submissions = authorService.getAuthorSubmissions(currentAuthor.getUsername());
        
        int pendingCount = (int) submissions.stream().filter(s -> "PENDING".equals(s.getStatus())).count();
        int approvedCount = (int) submissions.stream().filter(s -> "APPROVED".equals(s.getStatus())).count();
        int totalCount = submissions.size();

        Platform.runLater(() -> {
            if (statsBox != null) {
                statsBox.getChildren().clear();
                statsBox.getChildren().addAll(
                    createStatCard("Total", String.valueOf(totalCount)),
                    createStatCard("Pending", String.valueOf(pendingCount)),
                    createStatCard("Approved", String.valueOf(approvedCount))
                );
            }
        });
    }

    private HBox createTopBar() {
        HBox topBar = new HBox();
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setStyle("-fx-background-color: white; -fx-border-color: #dbe6f2; -fx-border-width: 0 0 1 0;");

        Label welcomeLabel = new Label("Welcome, " + currentAuthor.getFullName());
        welcomeLabel.getStyleClass().add("current-user");

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().addAll("button", "secondary-btn");
        logoutBtn.setOnAction(e -> logout());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(welcomeLabel, spacer, logoutBtn);
        return topBar;
    }

    private HBox createStatsBox() {
        HBox statsBox = new HBox(20);
        statsBox.setAlignment(Pos.CENTER);

        List<BookSubmission> submissions = authorService.getAuthorSubmissions(currentAuthor.getUsername());
        
        int pendingCount = (int) submissions.stream().filter(s -> "PENDING".equals(s.getStatus())).count();
        int approvedCount = (int) submissions.stream().filter(s -> "APPROVED".equals(s.getStatus())).count();
        int totalCount = submissions.size();

        statsBox.getChildren().addAll(
            createStatCard("Total", String.valueOf(totalCount)),
            createStatCard("Pending", String.valueOf(pendingCount)),
            createStatCard("Approved", String.valueOf(approvedCount))
        );
        return statsBox;
    }

    private VBox createStatCard(String label, String value) {
        VBox card = new VBox(5);
        card.getStyleClass().add("stats-card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(20));
        card.setPrefWidth(120);

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("stats-number");

        Label descLabel = new Label(label);
        descLabel.getStyleClass().add("stats-label");

        card.getChildren().addAll(valueLabel, descLabel);
        return card;
    }

    private GridPane createMenuGrid() {
        GridPane menuGrid = new GridPane();
        menuGrid.setHgap(20);
        menuGrid.setVgap(20);
        menuGrid.setAlignment(Pos.CENTER);

        Button publishBtn = createMenuButton("📚 Publish Book", "Submit a new book for review");
        Button viewBtn = createMenuButton("📋 My Submissions", "View your book submissions");
        Button profileBtn = createMenuButton("👤 Profile", "Manage your profile");
        Button notificationBtn = createMenuButton("🔔 Notifications", "Review author notifications");

        publishBtn.setOnAction(e -> {
            PublishBookFX publishUI = new PublishBookFX(currentAuthor);
            publishUI.show();
            
            // Schedule a stats refresh after a short delay
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if (primaryStage != null && primaryStage.isShowing()) {
                        Platform.runLater(() -> refreshDashboardStats());
                    }
                }
            }, 1000);
        });

        viewBtn.setOnAction(e -> showSubmissions());
        profileBtn.setOnAction(e -> showProfile());
        notificationBtn.setOnAction(e -> showNotifications());

        menuGrid.add(publishBtn, 0, 0);
        menuGrid.add(viewBtn, 1, 0);
        menuGrid.add(profileBtn, 2, 0);
        menuGrid.add(notificationBtn, 0, 1);

        return menuGrid;
    }

    private Button createMenuButton(String title, String subtitle) {
        Button btn = new Button(title + "\n" + subtitle);
        btn.getStyleClass().addAll("button", "card");
        btn.setPrefSize(220, 100);
        btn.setWrapText(true);
        btn.setAlignment(Pos.CENTER);
        return btn;
    }

    private void showSubmissions() {
        if (submissionsStage != null && submissionsStage.isShowing()) {
            submissionsStage.requestFocus();
            return;
        }

        submissionsStage = new Stage();
        
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: white;");
        root.setPadding(new Insets(20));

        Label titleLabel = new Label("📋 My Book Submissions");
        titleLabel.getStyleClass().add("section-title");
        BorderPane.setAlignment(titleLabel, Pos.CENTER);
        root.setTop(titleLabel);

        submissionsContainer = new VBox(10);
        submissionsContainer.setPadding(new Insets(10));
        
        ScrollPane scrollPane = new ScrollPane(submissionsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        scrollPane.setStyle("-fx-background-color: transparent;");
        root.setCenter(scrollPane);

        VBox bottomBox = new VBox(10);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new Insets(10, 0, 0, 0));

        statusLabel = new Label("Last updated: --:--:--");
        statusLabel.getStyleClass().add("muted");

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button refreshBtn = new Button("🔄 Refresh Now");
        refreshBtn.getStyleClass().addAll("button", "primary-btn");
        refreshBtn.setPrefWidth(150);
        refreshBtn.setOnAction(e -> {
            refreshSubmissions();
            refreshDashboardStats();
        });

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().addAll("button", "secondary-btn");
        closeBtn.setPrefWidth(100);
        closeBtn.setOnAction(e -> submissionsStage.close());

        buttonBox.getChildren().addAll(refreshBtn, closeBtn);
        bottomBox.getChildren().addAll(statusLabel, buttonBox);
        root.setBottom(bottomBox);

        refreshSubmissions();

        submissionsStage.setOnCloseRequest(e -> {
            submissionsStage = null;
            refreshDashboardStats();
        });

        Scene scene = new Scene(root, 600, 500);
        scene.getStylesheets().add(getClass().getResource("/project/task2/css/author-portal.css").toExternalForm());
        
        submissionsStage.setTitle("My Submissions");
        submissionsStage.setScene(scene);
        submissionsStage.show();
    }

    private void refreshSubmissions() {
        if (submissionsContainer == null) return;

        submissionsContainer.getChildren().clear();
        
        List<BookSubmission> submissions = authorService.getAuthorSubmissions(currentAuthor.getUsername());

        if (submissions.isEmpty()) {
            Label emptyLabel = new Label("📭 No submissions yet. Click 'Publish Book' to get started!");
            emptyLabel.getStyleClass().add("muted");
            emptyLabel.setWrapText(true);
            submissionsContainer.getChildren().add(emptyLabel);
        } else {
            for (BookSubmission sub : submissions) {
                VBox card = createSubmissionCard(sub);
                submissionsContainer.getChildren().add(card);
            }
        }

        statusLabel.setText("Last updated: " + 
            java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    private VBox createSubmissionCard(BookSubmission sub) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-background-radius: 8px; " +
                     "-fx-background-color: white; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 2);");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        Label statusLabel = new Label(sub.getStatus());
        statusLabel.getStyleClass().addAll("status", 
            sub.getStatus().equals("PENDING") ? "status-pending" :
            sub.getStatus().equals("APPROVED") ? "status-approved" : "status-rejected");
        statusLabel.setPrefWidth(90);

        Label titleLabel = new Label(sub.getTitle());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        titleLabel.setWrapText(true);
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        header.getChildren().addAll(statusLabel, titleLabel);

        Label genreLabel = new Label("📚 Genres: " + sub.getGenresAsString());
        genreLabel.getStyleClass().add("muted");

        Label dateLabel = new Label("📅 Submitted: " + 
            sub.getSubmissionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        dateLabel.getStyleClass().add("muted");
        Label coverLabel = new Label("🖼️ Cover: " + (sub.getCoverImagePath().isBlank() ? "None" : sub.getCoverImagePath()));
        coverLabel.getStyleClass().add("muted");

        card.getChildren().addAll(header, genreLabel, dateLabel, coverLabel);

        if (sub.getStatus().equals("REJECTED") && sub.getRejectionReason() != null) {
            Label reasonLabel = new Label("❌ Reason: " + sub.getRejectionReason());
            reasonLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
            reasonLabel.setWrapText(true);
            card.getChildren().add(reasonLabel);
        }

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        if (sub.isPending()) {
            Button editBtn = new Button("Edit");
            editBtn.getStyleClass().addAll("button", "secondary-btn");
            editBtn.setOnAction(e -> showEditSubmissionDialog(sub));
            actions.getChildren().add(editBtn);
        }
        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().addAll("button", "secondary-btn");
        deleteBtn.setOnAction(e -> handleDeleteSubmission(sub));
        actions.getChildren().add(deleteBtn);
        card.getChildren().add(actions);

        return card;
    }

    private void showProfile() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Manage Profile");
        dialog.setHeaderText("Update full name, password, and bio");
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        TextField fullNameField = new TextField(currentAuthor.getFullName());
        PasswordField currentPwdField = new PasswordField();
        PasswordField newPwdField = new PasswordField();
        PasswordField confirmPwdField = new PasswordField();
        TextArea bioField = new TextArea(currentAuthor.getBio());
        bioField.setPrefRowCount(3);
        Label meter = new Label("Leave new password blank to keep current one.");
        meter.setTextFill(Color.GRAY);
        newPwdField.textProperty().addListener((obs, oldV, newV) -> updatePasswordMeter(meter, newV, confirmPwdField.getText()));
        confirmPwdField.textProperty().addListener((obs, oldV, newV) -> updatePasswordMeter(meter, newPwdField.getText(), newV));

        grid.add(new Label("Full Name"), 0, 0);
        grid.add(fullNameField, 1, 0);
        grid.add(new Label("Current Password"), 0, 1);
        grid.add(currentPwdField, 1, 1);
        grid.add(new Label("New Password"), 0, 2);
        grid.add(newPwdField, 1, 2);
        grid.add(new Label("Confirm Password"), 0, 3);
        grid.add(confirmPwdField, 1, 3);
        grid.add(new Label("Bio"), 0, 4);
        grid.add(bioField, 1, 4);
        grid.add(meter, 1, 5);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> choice = dialog.showAndWait();
        if (choice.isEmpty() || choice.get() != saveType) {
            return;
        }
        AuthorPortalService.RegistrationResult result = authorService.updateProfile(
                currentAuthor.getUsername(),
                fullNameField.getText(),
                newPwdField.getText(),
                confirmPwdField.getText(),
                bioField.getText(),
                currentPwdField.getText()
        );
        if (!result.isSuccess()) {
            showAlert(Alert.AlertType.ERROR, "Profile Update Failed", result.getMessage());
            return;
        }
        AuthorAccount refreshed = authorService.getAuthorByUsername(currentAuthor.getUsername());
        if (refreshed != null) {
            currentAuthor = refreshed;
        }
        showAlert(Alert.AlertType.INFORMATION, "Profile Updated", result.getMessage());
    }

    private void showNotifications() {
        List<AuthorPortalService.NotificationView> notifications =
                authorService.getNotificationBoard(currentAuthor.getUsername(), "ALL", "", false);
        if (notifications.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Notifications", "No notifications.");
            return;
        }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                notifications.get(0).id(),
                notifications.stream()
                        .map(n -> n.id() + " | " + n.timestamp().toLocalDate() + " [" + n.category() + "] " + n.message())
                        .toList()
        );
        dialog.setTitle("Notifications");
        dialog.setHeaderText("Select one notification");
        dialog.setContentText("Notification:");
        Optional<String> picked = dialog.showAndWait();
        if (picked.isEmpty()) {
            return;
        }
        String id = picked.get().split("\\|")[0].trim();
        Alert action = new Alert(Alert.AlertType.CONFIRMATION);
        action.setTitle("Notification Action");
        action.setHeaderText("Choose action for selected notification");
        ButtonType readBtn = new ButtonType("Mark Read");
        ButtonType deleteBtn = new ButtonType("Delete");
        action.getButtonTypes().setAll(readBtn, deleteBtn, ButtonType.CANCEL);
        Optional<ButtonType> actionPicked = action.showAndWait();
        if (actionPicked.isEmpty() || actionPicked.get() == ButtonType.CANCEL) {
            return;
        }
        AuthorPortalService.SubmissionResult result = actionPicked.get() == deleteBtn
                ? authorService.deleteNotification(currentAuthor.getUsername(), id)
                : authorService.markNotificationRead(currentAuthor.getUsername(), id);
        showAlert(result.isSuccess() ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                "Notification",
                result.getMessage());
    }

    private void handleDeleteSubmission(BookSubmission sub) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Submission");
        confirm.setHeaderText("Delete submission " + sub.getSubmissionId() + "?");
        confirm.setContentText("Pending submissions can be deleted. Approved ones only if not currently borrowed.");
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) {
            return;
        }
        AuthorPortalService.SubmissionResult result =
                authorService.deleteSubmission(currentAuthor.getUsername(), sub.getSubmissionId());
        showAlert(result.isSuccess() ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                "Delete Submission",
                result.getMessage());
        if (result.isSuccess()) {
            refreshSubmissions();
            refreshDashboardStats();
        }
    }

    private void showEditSubmissionDialog(BookSubmission sub) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Submission");
        dialog.setHeaderText("Only pending submissions can be edited");
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        TextField title = new TextField(sub.getTitle());
        TextField genres = new TextField(sub.getGenresAsString());
        TextArea desc = new TextArea(sub.getDescription());
        TextField file = new TextField(sub.getFilePath());
        TextField cover = new TextField(sub.getCoverImagePath());
        grid.add(new Label("Title"), 0, 0);
        grid.add(title, 1, 0);
        grid.add(new Label("Genres"), 0, 1);
        grid.add(genres, 1, 1);
        grid.add(new Label("Description"), 0, 2);
        grid.add(desc, 1, 2);
        grid.add(new Label("File Path"), 0, 3);
        grid.add(file, 1, 3);
        grid.add(new Label("Cover Path"), 0, 4);
        grid.add(cover, 1, 4);
        dialog.getDialogPane().setContent(grid);
        Optional<ButtonType> choice = dialog.showAndWait();
        if (choice.isEmpty() || choice.get() != saveType) {
            return;
        }
        AuthorPortalService.SubmissionResult result = authorService.editPendingSubmission(
                currentAuthor.getUsername(),
                sub.getSubmissionId(),
                title.getText(),
                genres.getText(),
                desc.getText(),
                file.getText(),
                cover.getText()
        );
        showAlert(result.isSuccess() ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                "Edit Submission",
                result.getMessage());
        if (result.isSuccess()) {
            refreshSubmissions();
        }
    }

    private void updatePasswordMeter(Label meter, String password, String confirm) {
        if (password == null || password.isBlank()) {
            meter.setText("Leave new password blank to keep current one.");
            meter.setTextFill(Color.GRAY);
            return;
        }
        boolean strong = password.length() >= 8
                && password.matches(".*[A-Za-z].*")
                && password.matches(".*\\d.*")
                && password.matches(".*[A-Z].*");
        if (!strong) {
            meter.setText("Weak password.");
            meter.setTextFill(Color.CRIMSON);
            return;
        }
        if (confirm != null && !confirm.isBlank() && !password.equals(confirm)) {
            meter.setText("Passwords do not match.");
            meter.setTextFill(Color.CRIMSON);
            return;
        }
        meter.setText("Strong password.");
        meter.setTextFill(Color.FORESTGREEN);
    }

    private void showAlert(Alert.AlertType type, String header, String message) {
        Alert alert = new Alert(type);
        alert.setTitle("Author Portal");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void logout() {
        System.out.println("🚪 Logging out: " + currentAuthor.getUsername());
        
        // Stop the refresh timer
        stopRefreshTimer();
        
        // Close any open submissions window
        if (submissionsStage != null && submissionsStage.isShowing()) {
            submissionsStage.close();
        }
        
        // Close dashboard and open login
        primaryStage.close();
        
        // Open login screen
        AuthorLoginFX loginUI = new AuthorLoginFX();
        try {
            loginUI.start(new Stage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init() {
        // Required for Application class
    }

    @Override
    public void stop() {
        System.out.println("🛑 Author Dashboard stopped");
        stopRefreshTimer();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
