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
import project.task2.model.AuthorAccount;
import project.task2.model.BookSubmission;
import project.task2.service.AuthorPortalService;
import project.task2.utils.SessionManager;
import project.shared.CrashSimulationManager;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;

public class AuthorDashboardFX extends Application {
    private AuthorPortalService authorService;
    private AuthorAccount currentAuthor;
    private Stage primaryStage;
    private Timer refreshTimer;
    private Timer autoSaveTimer;
    private Stage submissionsStage;
    private VBox submissionsContainer;
    private Label statusLabel;
    
    private HBox statsBox;
    private List<Stage> childWindows = new ArrayList<>();
    private Label welcomeLabel;
    private boolean randomCrashEnabled = false;
    
    // Track currently open screen
    private String currentActiveScreen = "DASHBOARD";

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

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(getClass().getResource("/project/task2/css/author-portal.css").toExternalForm());
        
        primaryStage.setTitle("Author Dashboard");
        primaryStage.setScene(scene);
        
        primaryStage.setOnCloseRequest(this::handleWindowClose);
        
        primaryStage.show();

        startDashboardAutoRefresh();
        startAutoSave();
        
        // Save session when dashboard is opened
        SessionManager.setCurrentUser(currentAuthor.getUsername(), currentAuthor.getFullName());
        setActiveScreen("DASHBOARD");
    }

    private void handleWindowClose(WindowEvent event) {
        System.out.println("🚪 Closing Author Dashboard window...");
        
        // Clear session on normal close (not crash)
        SessionManager.clearSession();
        
        for (Stage child : childWindows) {
            if (child != null && child.isShowing()) {
                child.close();
            }
        }
        childWindows.clear();
        
        stopRefreshTimer();
        stopAutoSave();
    }

    private void registerChildWindow(Stage stage, String screenName) {
        childWindows.add(stage);
        // When child window opens, set it as active
        setActiveScreen(screenName);
        stage.setOnCloseRequest(e -> {
            childWindows.remove(stage);
            // When child window closes, revert to dashboard
            setActiveScreen("DASHBOARD");
        });
    }

    private void stopRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer.purge();
            refreshTimer = null;
        }
    }
    
    private void startAutoSave() {
        autoSaveTimer = new Timer(true);
        autoSaveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    SessionManager.autoSave();
                });
            }
        }, 5000, 5000);
        System.out.println("⏰ Auto-save started (every 5 seconds)");
    }
    
    private void stopAutoSave() {
        if (autoSaveTimer != null) {
            autoSaveTimer.cancel();
            autoSaveTimer = null;
        }
    }
    
    private void setActiveScreen(String screenName) {
        currentActiveScreen = screenName;
        SessionManager.setCurrentScreen(screenName, null);
        System.out.println("📱 Active screen set to: " + screenName);
    }

    private void startDashboardAutoRefresh() {
        stopRefreshTimer();
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
        topBar.getStyleClass().add("top-bar");

        welcomeLabel = new Label("Welcome, " + currentAuthor.getFullName());
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

        // Row 1
        Button publishBtn = createMenuButton("📚 Publish Book", "Submit a new book for review");
        Button viewBtn = createMenuButton("📋 My Submissions", "View your book submissions");
        Button booksBtn = createMenuButton("📖 My Books", "View, edit, or delete your books");

        // Row 2
        Button profileBtn = createMenuButton("👤 Profile", "Manage your profile information");
        Button notifBtn = createMenuButton("🔔 Notifications", "View your notifications");

        // Row 3 - Crash Test button
        Button crashBtn = createMenuButton("💥 Crash Test", "Simulate application crash");
        crashBtn.getStyleClass().addAll("button", "danger-btn");
        Button randomCrashBtn = createMenuButton("🎲 Random Crash: OFF", "Toggle random runtime crashes");

        // Publish Book button
        publishBtn.setOnAction(e -> {
            System.out.println("📚 Opening Publish Book window...");
            try {
                PublishBookFX publishUI = new PublishBookFX(currentAuthor, updated -> {
                    refreshDashboardStats();
                    System.out.println("✅ Dashboard stats refreshed after book published");
                });
                publishUI.show();
                registerChildWindow(publishUI.getStage(), "PUBLISH_BOOK");
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Error", "Could not open publish book window: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });

        // My Submissions button
        viewBtn.setOnAction(e -> {
            System.out.println("📋 Opening My Submissions...");
            showSubmissions();
            setActiveScreen("MY_SUBMISSIONS");
        });
        
        // My Books button
        booksBtn.setOnAction(e -> {
            System.out.println("📖 Opening My Books...");
            try {
                PublishedBookScreenFX bookScreen = new PublishedBookScreenFX(currentAuthor, updated -> {
                    refreshDashboardStats();
                    System.out.println("✅ Dashboard stats refreshed after book update");
                });
                bookScreen.show();
                registerChildWindow(bookScreen.getStage(), "MY_BOOKS");
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Error", "Could not open my books window: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });
        
        // Profile button
        profileBtn.setOnAction(e -> {
            System.out.println("👤 Opening Profile Management...");
            try {
                ProfileManagementFX profileScreen = new ProfileManagementFX(currentAuthor, updatedAuthor -> {
                    if (updatedAuthor == null) {
                        // Password was changed - need to logout
                        System.out.println("🔐 Password changed - logging out...");
                        showAlert("Password Changed", 
                            "Your password has been changed. You will be logged out for security reasons.\n\n" +
                            "Please log in again with your new password.", 
                            Alert.AlertType.INFORMATION);
                        logout();
                    } else {
                        // Normal profile update
                        this.currentAuthor = updatedAuthor;
                        if (welcomeLabel != null) {
                            welcomeLabel.setText("Welcome, " + updatedAuthor.getFullName());
                        }
                        refreshDashboardStats();
                        System.out.println("✅ Dashboard updated with new profile info");
                    }
                });
                profileScreen.show();
                registerChildWindow(profileScreen.getStage(), "PROFILE");
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Error", "Could not open profile window: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });
        
        // Notifications button
        notifBtn.setOnAction(e -> {
            System.out.println("🔔 Opening Notifications...");
            try {
                NotificationBoardFX notifBoard = new NotificationBoardFX(currentAuthor);
                notifBoard.show();
                registerChildWindow(notifBoard.getStage(), "NOTIFICATIONS");
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Error", "Could not open notifications: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        });
        
        // Crash Test button action
        crashBtn.setOnAction(e -> simulateCrash());
        randomCrashBtn.setOnAction(e -> {
            randomCrashEnabled = !randomCrashEnabled;
            if (randomCrashEnabled) {
                CrashSimulationManager.enableRandomCrash(30, 120, SessionManager::autoSave);
                randomCrashBtn.setText("🎲 Random Crash: ON\nToggle random runtime crashes");
                showAlert("Random Crash", "Random crash simulation enabled (30-120s window).", Alert.AlertType.INFORMATION);
            } else {
                CrashSimulationManager.disableRandomCrash();
                randomCrashBtn.setText("🎲 Random Crash: OFF\nToggle random runtime crashes");
                showAlert("Random Crash", "Random crash simulation disabled.", Alert.AlertType.INFORMATION);
            }
        });

        // Add buttons to grid
        menuGrid.add(publishBtn, 0, 0);
        menuGrid.add(viewBtn, 1, 0);
        menuGrid.add(booksBtn, 2, 0);
        menuGrid.add(profileBtn, 0, 1);
        menuGrid.add(notifBtn, 1, 1);
        menuGrid.add(crashBtn, 2, 1);
        menuGrid.add(randomCrashBtn, 0, 2);

        return menuGrid;
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
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
        registerChildWindow(submissionsStage, "MY_SUBMISSIONS");
        
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
        closeBtn.setOnAction(e -> {
            submissionsStage.close();
            setActiveScreen("DASHBOARD");
        });

        buttonBox.getChildren().addAll(refreshBtn, closeBtn);
        bottomBox.getChildren().addAll(statusLabel, buttonBox);
        root.setBottom(bottomBox);

        refreshSubmissions();

        submissionsStage.setOnCloseRequest(e -> {
            submissionsStage = null;
            setActiveScreen("DASHBOARD");
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

        card.getChildren().addAll(header, genreLabel, dateLabel);

        if (sub.getStatus().equals("REJECTED") && sub.getRejectionReason() != null) {
            Label reasonLabel = new Label("❌ Reason: " + sub.getRejectionReason());
            reasonLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
            reasonLabel.setWrapText(true);
            card.getChildren().add(reasonLabel);
        }

        return card;
    }

    private void simulateCrash() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Crash Simulation");
        confirm.setHeaderText("⚠️ Crash Simulation");
        confirm.setContentText(
            "This will simulate an application crash.\n\n" +
            "Your current session will be saved.\n" +
            "When you reopen the app, your session will be automatically restored.\n\n" +
            "Do you want to proceed?"
        );
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Save current session before crash
                SessionManager.autoSave();
                
                // Show crash message
                Alert crashAlert = new Alert(Alert.AlertType.WARNING);
                crashAlert.setTitle("Crash Simulation");
                crashAlert.setHeaderText("💥 Application Crashing...");
                crashAlert.setContentText(
                    "The application will now close.\n\n" +
                    "When you restart, your session will be automatically restored."
                );
                crashAlert.showAndWait();
                
                CrashSimulationManager.triggerCrashNow(SessionManager::autoSave);
            }
        });
    }

    private void logout() {
        System.out.println("🚪 Logging out: " + currentAuthor.getUsername());

        for (Stage child : childWindows) {
            if (child != null && child.isShowing()) {
                child.close();
            }
        }
        childWindows.clear();

        stopRefreshTimer();
        stopAutoSave();
        CrashSimulationManager.disableRandomCrash();
        randomCrashEnabled = false;

        if (submissionsStage != null && submissionsStage.isShowing()) {
            submissionsStage.close();
        }

        // Clear session on normal logout
        SessionManager.clearSession();
        
        primaryStage.close();

        AuthorLoginFX loginUI = new AuthorLoginFX();
        try {
            loginUI.start(new Stage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Navigate to a specific screen (used for crash recovery)
     */
    public void navigateToScreen(String screenName) {
        System.out.println("🎯 Navigating to screen: " + screenName);
        
        switch (screenName) {
            case "PUBLISH_BOOK":
                try {
                    PublishBookFX publishUI = new PublishBookFX(currentAuthor, updated -> {
                        refreshDashboardStats();
                    });
                    publishUI.show();
                    registerChildWindow(publishUI.getStage(), "PUBLISH_BOOK");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
                
            case "MY_BOOKS":
                try {
                    PublishedBookScreenFX bookScreen = new PublishedBookScreenFX(currentAuthor, updated -> {
                        refreshDashboardStats();
                    });
                    bookScreen.show();
                    registerChildWindow(bookScreen.getStage(), "MY_BOOKS");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
                
            case "MY_SUBMISSIONS":
                showSubmissions();
                break;
                
            case "PROFILE":
                try {
                    ProfileManagementFX profileScreen = new ProfileManagementFX(currentAuthor, updatedAuthor -> {
                        if (updatedAuthor == null) {
                            logout();
                        } else {
                            this.currentAuthor = updatedAuthor;
                            if (welcomeLabel != null) {
                                welcomeLabel.setText("Welcome, " + updatedAuthor.getFullName());
                            }
                            refreshDashboardStats();
                        }
                    });
                    profileScreen.show();
                    registerChildWindow(profileScreen.getStage(), "PROFILE");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
                
            case "NOTIFICATIONS":
                try {
                    NotificationBoardFX notifBoard = new NotificationBoardFX(currentAuthor);
                    notifBoard.show();
                    registerChildWindow(notifBoard.getStage(), "NOTIFICATIONS");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;
                
            default:
                System.out.println("Staying on dashboard");
                break;
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
        stopAutoSave();
        CrashSimulationManager.disableRandomCrash();
        randomCrashEnabled = false;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
