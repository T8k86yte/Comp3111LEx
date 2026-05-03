package project.task2.ui.javafx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import project.task2.model.AuthorAccount;
import project.task2.model.BookStats;
import project.task2.service.AuthorPortalService;
import project.task2.utils.ProfilePictureManager;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class AuthorStatsFX {
    private AuthorPortalService authorService;
    private AuthorAccount currentAuthor;
    private Stage stage;
    
    private HBox cardsContainer;
    private BarChart<String, Number> barChart;
    private PieChart pieChart;
    private Label lastUpdatedLabel;
    private Timer refreshTimer;
    
    private static final int REFRESH_INTERVAL_SECONDS = 10;

    public AuthorStatsFX(AuthorAccount author) {
        this.currentAuthor = author;
        this.authorService = new AuthorPortalService();
        this.stage = new Stage();
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

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox content = new VBox(25);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(30));

        Label titleLabel = new Label("📊 Author Statistics Dashboard");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        
        Label subtitleLabel = new Label("View performance metrics for your published books");
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");

        lastUpdatedLabel = new Label("Last updated: Just now");
        lastUpdatedLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        
        Label autoRefreshLabel = new Label("🔄 Auto-refreshes every " + REFRESH_INTERVAL_SECONDS + " seconds");
        autoRefreshLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981;");

        cardsContainer = new HBox(20);
        cardsContainer.setAlignment(Pos.CENTER);
        cardsContainer.setPadding(new Insets(20));

        VBox barChartSection = new VBox(10);
        Label barChartLabel = new Label("📊 Book Borrow Counts");
        barChartLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        
        barChart = createBarChart();
        
        VBox barChartBox = new VBox(10);
        barChartBox.setPadding(new Insets(20));
        barChartBox.setStyle("-fx-background-color: white; -fx-background-radius: 16px; " +
                            "-fx-border-color: #e2e8f0; -fx-border-radius: 16px;");
        barChartBox.getChildren().addAll(barChart);
        barChartSection.getChildren().addAll(barChartLabel, barChartBox);

        VBox pieChartSection = new VBox(10);
        Label pieChartLabel = new Label("📈 Book Popularity Distribution");
        pieChartLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        
        pieChart = createPieChart();
        
        VBox pieChartBox = new VBox(10);
        pieChartBox.setPadding(new Insets(20));
        pieChartBox.setStyle("-fx-background-color: white; -fx-background-radius: 16px; " +
                            "-fx-border-color: #e2e8f0; -fx-border-radius: 16px;");
        pieChartBox.getChildren().addAll(pieChart);
        pieChartSection.getChildren().addAll(pieChartLabel, pieChartBox);

        Button refreshBtn = new Button("🔄 Refresh Now");
        refreshBtn.getStyleClass().addAll("button", "primary-btn");
        refreshBtn.setOnAction(e -> refreshStats());

        HBox refreshBox = new HBox();
        refreshBox.setAlignment(Pos.CENTER);
        refreshBox.setPadding(new Insets(10));
        refreshBox.getChildren().add(refreshBtn);

        HBox statusBox = new HBox(20);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.getChildren().addAll(lastUpdatedLabel, autoRefreshLabel);

        content.getChildren().addAll(titleLabel, subtitleLabel, statusBox, cardsContainer, barChartSection, pieChartSection, refreshBox);
        scrollPane.setContent(content);
        root.setCenter(scrollPane);

        HBox bottomBar = new HBox();
        bottomBar.setAlignment(Pos.CENTER);
        bottomBar.setPadding(new Insets(20));
        
        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().addAll("button", "primary-btn");
        closeBtn.setPrefWidth(150);
        closeBtn.setOnAction(e -> {
            stopAutoRefresh();
            stage.close();
        });
        bottomBar.getChildren().add(closeBtn);
        root.setBottom(bottomBar);

        Scene scene = new Scene(root, 1000, 900);
        scene.getStylesheets().add(getClass().getResource("/project/task2/css/author-portal.css").toExternalForm());
        
        stage.setTitle("Statistics Dashboard - " + currentAuthor.getUsername());
        stage.setScene(scene);
        stage.show();
        
        refreshStats();
        startAutoRefresh();
        
        stage.setOnCloseRequest(e -> stopAutoRefresh());
    }

    private HBox createTopBar() {
        HBox topBar = new HBox();
        topBar.setPadding(new Insets(12, 24, 12, 24));
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");

        StackPane avatarContainer = ProfilePictureManager.createAvatar(
            currentAuthor.getUsername(), 
            currentAuthor.getFullName(), 
            40
        );

        Label usernameLabel = new Label(currentAuthor.getUsername());
        usernameLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #1e293b;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-size: 18px; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> {
            stopAutoRefresh();
            stage.close();
        });

        HBox userInfo = new HBox(12);
        userInfo.setAlignment(Pos.CENTER);
        userInfo.getChildren().addAll(avatarContainer, usernameLabel);
        
        topBar.getChildren().addAll(userInfo, spacer, closeBtn);
        return topBar;
    }

    private VBox createStatCard(String title, String value, String subtitle) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16px; " +
                     "-fx-border-color: #e2e8f0; -fx-border-radius: 16px; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 8, 0, 0, 2);");
        card.setPadding(new Insets(20));
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(190);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
        
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: #2563eb;");
        
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        card.getChildren().addAll(titleLabel, valueLabel, subtitleLabel);
        return card;
    }

    private BarChart<String, Number> createBarChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Books");
        yAxis.setLabel("Number of Borrows");
        
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("📊 Book Borrow Counts");
        chart.setPrefHeight(400);
        chart.setAnimated(true);
        
        return chart;
    }

    private PieChart createPieChart() {
        PieChart chart = new PieChart();
        chart.setTitle("📈 Book Popularity Distribution");
        chart.setPrefHeight(400);
        chart.setAnimated(true);
        
        return chart;
    }

    private void refreshStats() {
        List<BookStats> stats = authorService.getAuthorBookStats(currentAuthor.getUsername());
        int totalBooks = stats.size();
        int totalBorrows = authorService.getTotalBorrowsForAuthor(currentAuthor.getUsername());
        double avgRating = authorService.getAverageRatingForAuthor(currentAuthor.getUsername());
        int totalReviews = authorService.getTotalReviewsForAuthor(currentAuthor.getUsername());

        cardsContainer.getChildren().clear();
        cardsContainer.getChildren().addAll(
            createStatCard("📚 Total Books", String.valueOf(totalBooks), "Published books"),
            createStatCard("📖 Total Borrows", String.valueOf(totalBorrows), "All time reads"),
            createStatCard("⭐ Average Rating", String.format("%.1f", avgRating) + " / 5", "From user reviews"),
            createStatCard("💬 Total Reviews", String.valueOf(totalReviews), "User feedback")
        );

        barChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Borrow Count");
        
        for (BookStats stat : stats) {
            String title = stat.getTitle().length() > 20 ? 
                          stat.getTitle().substring(0, 20) + "..." : stat.getTitle();
            series.getData().add(new XYChart.Data<>(title, stat.getBorrowCount()));
        }
        barChart.getData().add(series);

        pieChart.getData().clear();
        boolean hasData = false;
        for (BookStats stat : stats) {
            if (stat.getBorrowCount() > 0) {
                hasData = true;
                String title = stat.getTitle().length() > 25 ? 
                              stat.getTitle().substring(0, 22) + "..." : stat.getTitle();
                pieChart.getData().add(new PieChart.Data(title + " (" + stat.getBorrowCount() + ")", stat.getBorrowCount()));
            }
        }
        
        if (!hasData) {
            pieChart.setTitle("No borrow data available yet");
        } else {
            pieChart.setTitle("📈 Book Popularity Distribution");
        }

        lastUpdatedLabel.setText("Last updated: " + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    private void startAutoRefresh() {
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                javafx.application.Platform.runLater(() -> refreshStats());
            }
        }, REFRESH_INTERVAL_SECONDS * 1000, REFRESH_INTERVAL_SECONDS * 1000);
        
        System.out.println("🔄 Statistics auto-refresh started (every " + REFRESH_INTERVAL_SECONDS + " seconds)");
    }

    private void stopAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
            System.out.println("🛑 Statistics auto-refresh stopped");
        }
    }
}
