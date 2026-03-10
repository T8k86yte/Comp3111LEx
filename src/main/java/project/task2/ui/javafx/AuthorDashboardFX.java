package project.task2.ui.javafx;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import project.task2.model.AuthorAccount;
import project.task2.model.BookSubmission;
import project.task2.service.AuthorPortalService;

public class AuthorDashboardFX extends Application {
    private AuthorPortalService authorService;
    private AuthorAccount currentAuthor;
    private Stage primaryStage;

    public AuthorDashboardFX(AuthorAccount author) {
        this.currentAuthor = author;
        this.authorService = new AuthorPortalService();
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        // Top bar
        HBox topBar = new HBox();
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setStyle("-fx-background-color: white; -fx-border-color: #dbe6f2; -fx-border-width: 0 0 1 0;");

        Label welcomeLabel = new Label("Welcome, " + currentAuthor.getFullName());
        welcomeLabel.getStyleClass().add("current-user");

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().addAll("button", "secondary-btn");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(welcomeLabel, spacer, logoutBtn);
        root.setTop(topBar);

        // Center content
        VBox centerContent = new VBox(30);
        centerContent.setAlignment(Pos.TOP_CENTER);
        centerContent.setPadding(new Insets(50));

        Label titleLabel = new Label("📊 Author Dashboard");
        titleLabel.getStyleClass().add("page-title");

        // Stats cards
        HBox statsBox = new HBox(20);
        statsBox.setAlignment(Pos.CENTER);

        int pendingCount = (int) authorService.getAuthorSubmissions(currentAuthor.getUsername())
                .stream().filter(s -> "PENDING".equals(s.getStatus())).count();
        int approvedCount = (int) authorService.getAuthorSubmissions(currentAuthor.getUsername())
                .stream().filter(s -> "APPROVED".equals(s.getStatus())).count();
        int totalCount = authorService.getAuthorSubmissions(currentAuthor.getUsername()).size();

        statsBox.getChildren().addAll(
            createStatCard("Total", String.valueOf(totalCount)),
            createStatCard("Pending", String.valueOf(pendingCount)),
            createStatCard("Approved", String.valueOf(approvedCount))
        );

        // Menu buttons
        GridPane menuGrid = new GridPane();
        menuGrid.setHgap(20);
        menuGrid.setVgap(20);
        menuGrid.setAlignment(Pos.CENTER);

        Button publishBtn = createMenuButton("📚 Publish Book", "Submit a new book for review");
        Button viewBtn = createMenuButton("📋 My Submissions", "View your book submissions");
        Button profileBtn = createMenuButton("👤 Profile", "View your profile information");

        publishBtn.setOnAction(e -> {
            PublishBookFX publishUI = new PublishBookFX(currentAuthor);
            publishUI.show();
        });

        viewBtn.setOnAction(e -> showSubmissions());
        profileBtn.setOnAction(e -> showProfile());

        menuGrid.add(publishBtn, 0, 0);
        menuGrid.add(viewBtn, 1, 0);
        menuGrid.add(profileBtn, 2, 0);

        centerContent.getChildren().addAll(titleLabel, statsBox, menuGrid);
        root.setCenter(centerContent);

        logoutBtn.setOnAction(e -> {
            AuthorLoginFX loginUI = new AuthorLoginFX();
            try {
                loginUI.start(new Stage());
                primaryStage.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Scene scene = new Scene(root, 1000, 600);
        scene.getStylesheets().add(getClass().getResource("/project/task2/css/author-portal.css").toExternalForm());
        
        primaryStage.setTitle("Author Dashboard");
        primaryStage.setScene(scene);
        primaryStage.show();
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

    private Button createMenuButton(String title, String subtitle) {
        Button btn = new Button(title + "\n" + subtitle);
        btn.getStyleClass().addAll("button", "card");
        btn.setPrefSize(220, 100);
        btn.setWrapText(true);
        btn.setAlignment(Pos.CENTER);
        return btn;
    }

    private void showSubmissions() {
        Stage submissionsStage = new Stage();
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");

        Label titleLabel = new Label("My Book Submissions");
        titleLabel.getStyleClass().add("section-title");

        var submissions = authorService.getAuthorSubmissions(currentAuthor.getUsername());

        if (submissions.isEmpty()) {
            Label emptyLabel = new Label("No submissions yet. Click 'Publish Book' to get started!");
            emptyLabel.getStyleClass().add("muted");
            content.getChildren().addAll(titleLabel, emptyLabel);
        } else {
            for (BookSubmission sub : submissions) {
                VBox submissionCard = new VBox(5);
                submissionCard.setPadding(new Insets(15));
                submissionCard.setStyle("-fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-background-radius: 8px;");

                HBox header = new HBox(10);
                header.setAlignment(Pos.CENTER_LEFT);

                Label statusLabel = new Label(sub.getStatus());
                statusLabel.getStyleClass().addAll("status", 
                    sub.getStatus().equals("PENDING") ? "status-pending" :
                    sub.getStatus().equals("APPROVED") ? "status-approved" : "status-rejected");

                Label title = new Label(sub.getTitle());
                title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

                Region spacer2 = new Region();
                HBox.setHgrow(spacer2, Priority.ALWAYS);

                header.getChildren().addAll(statusLabel, title);

                Label genreLabel = new Label("Genre: " + sub.getGenre());
                genreLabel.getStyleClass().add("muted");

                Label dateLabel = new Label("Submitted: " + sub.getSubmissionDate());
                dateLabel.getStyleClass().add("muted");

                submissionCard.getChildren().addAll(header, genreLabel, dateLabel);

                if (sub.getStatus().equals("REJECTED") && sub.getRejectionReason() != null) {
                    Label reasonLabel = new Label("Reason: " + sub.getRejectionReason());
                    reasonLabel.setStyle("-fx-text-fill: #dc2626;");
                    submissionCard.getChildren().add(reasonLabel);
                }

                content.getChildren().add(submissionCard);
            }
        }

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().addAll("button", "secondary-btn");
        closeBtn.setMaxWidth(200);
        closeBtn.setOnAction(e -> submissionsStage.close());

        content.getChildren().add(closeBtn);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);

        Scene scene = new Scene(scrollPane, 500, 500);
        scene.getStylesheets().add(getClass().getResource("/project/task2/css/author-portal.css").toExternalForm());
        
        submissionsStage.setTitle("My Submissions");
        submissionsStage.setScene(scene);
        submissionsStage.show();
    }

    private void showProfile() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Profile");
        alert.setHeaderText("Author Profile");
        alert.setContentText(
            "Username: " + currentAuthor.getUsername() + "\n" +
            "Full Name: " + currentAuthor.getFullName() + "\n" +
            "Bio: " + (currentAuthor.getBio().isEmpty() ? "Not provided" : currentAuthor.getBio())
        );
        alert.showAndWait();
    }

    @Override
    public void init() {
        // Required for Application class
    }

    public static void main(String[] args) {
        launch(args);
    }
}
