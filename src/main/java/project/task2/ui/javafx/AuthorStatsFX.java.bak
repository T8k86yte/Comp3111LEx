package project.task2.ui.javafx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import project.task2.model.AuthorAccount;
import project.task2.model.BookStats;
import project.task2.service.AuthorPortalService;
import project.task2.service.RealTrendDataService;
import project.task2.utils.ProfilePictureManager;
import project.task2.utils.HTMLReportExporter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class AuthorStatsFX {
    private AuthorPortalService authorService;
    private RealTrendDataService trendService;
    private AuthorAccount currentAuthor;
    private Stage stage;
    
    private VBox cardsContainer;
    private BarChart<String, Number> barChart;
    private LineChart<String, Number> trendChart;
    private VBox barChartSection;
    private VBox trendSection;
    
    private CheckBox showBarChartCheck;
    private CheckBox showTrendChartCheck;
    private CheckBox showTotalBooksCheck;
    private CheckBox showTotalBorrowsCheck;
    private CheckBox showAvgRatingCheck;
    private CheckBox showTotalReviewsCheck;
    private CheckBox showTopBooksCheck;
    
    private ComboBox<String> trendPeriodCombo;
    private Label lastUpdatedLabel;
    private Timer refreshTimer;
    
    private List<BookStats> currentStats;
    private Map<LocalDate, Integer> currentTrendData;
    
    private static final int REFRESH_INTERVAL_SECONDS = 30;

    public AuthorStatsFX(AuthorAccount author) {
        this.currentAuthor = author;
        this.authorService = new AuthorPortalService();
        this.trendService = new RealTrendDataService();
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

        VBox customizationPanel = createCustomizationPanel();
        
        cardsContainer = new VBox(10);
        cardsContainer.setAlignment(Pos.CENTER);
        cardsContainer.setPadding(new Insets(10));
        
        HBox controlBar = createControlBar();

        barChartSection = createBarChartSection();
        barChartSection.setVisible(true);
        barChartSection.setManaged(true);
        
        trendSection = createTrendSection();
        trendSection.setVisible(true);
        trendSection.setManaged(true);

        content.getChildren().addAll(titleLabel, subtitleLabel, customizationPanel, cardsContainer, controlBar, barChartSection, trendSection);
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

        Scene scene = new Scene(root, 1100, 950);
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

    private VBox createCustomizationPanel() {
        VBox panel = new VBox(15);
        panel.setStyle("-fx-background-color: white; -fx-background-radius: 12px; " +
                      "-fx-border-color: #e2e8f0; -fx-border-radius: 12px;");
        panel.setPadding(new Insets(15));
        
        Label panelTitle = new Label("🎨 Customize Dashboard");
        panelTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
        
        Label metricsLabel = new Label("Displayed Metrics:");
        metricsLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #334155; -fx-font-size: 13px;");
        
        HBox metricsBox = new HBox(20);
        metricsBox.setAlignment(Pos.CENTER_LEFT);
        
        showTotalBooksCheck = new CheckBox("📚 Total Books");
        showTotalBooksCheck.setSelected(true);
        showTotalBooksCheck.selectedProperty().addListener((obs, old, newVal) -> refreshStats());
        
        showTotalBorrowsCheck = new CheckBox("📖 Total Borrows");
        showTotalBorrowsCheck.setSelected(true);
        showTotalBorrowsCheck.selectedProperty().addListener((obs, old, newVal) -> refreshStats());
        
        showAvgRatingCheck = new CheckBox("⭐ Average Rating");
        showAvgRatingCheck.setSelected(true);
        showAvgRatingCheck.selectedProperty().addListener((obs, old, newVal) -> refreshStats());
        
        showTotalReviewsCheck = new CheckBox("💬 Total Reviews");
        showTotalReviewsCheck.setSelected(true);
        showTotalReviewsCheck.selectedProperty().addListener((obs, old, newVal) -> refreshStats());
        
        showTopBooksCheck = new CheckBox("🏆 Top Books");
        showTopBooksCheck.setSelected(true);
        showTopBooksCheck.selectedProperty().addListener((obs, old, newVal) -> refreshStats());
        
        metricsBox.getChildren().addAll(showTotalBooksCheck, showTotalBorrowsCheck, showAvgRatingCheck, showTotalReviewsCheck, showTopBooksCheck);
        
        Label chartsLabel = new Label("Displayed Charts:");
        chartsLabel.setStyle("-fx-font-weight: 600; -fx-text-fill: #334155; -fx-font-size: 13px;");
        chartsLabel.setPadding(new Insets(10, 0, 0, 0));
        
        HBox chartsBox = new HBox(20);
        chartsBox.setAlignment(Pos.CENTER_LEFT);
        
        showBarChartCheck = new CheckBox("📊 Borrow Counts Chart");
        showBarChartCheck.setSelected(true);
        showBarChartCheck.selectedProperty().addListener((obs, old, newVal) -> {
            barChartSection.setVisible(showBarChartCheck.isSelected());
            barChartSection.setManaged(showBarChartCheck.isSelected());
        });
        
        showTrendChartCheck = new CheckBox("📈 Trend Analysis Chart");
        showTrendChartCheck.setSelected(true);
        showTrendChartCheck.selectedProperty().addListener((obs, old, newVal) -> {
            trendSection.setVisible(showTrendChartCheck.isSelected());
            trendSection.setManaged(showTrendChartCheck.isSelected());
        });
        
        chartsBox.getChildren().addAll(showBarChartCheck, showTrendChartCheck);
        
        HBox periodBox = new HBox(10);
        periodBox.setAlignment(Pos.CENTER_LEFT);
        periodBox.setPadding(new Insets(10, 0, 0, 0));
        
        Label periodLabel = new Label("Trend Period:");
        periodLabel.setStyle("-fx-font-weight: 500; -fx-text-fill: #475569;");
        
        trendPeriodCombo = new ComboBox<>();
        trendPeriodCombo.getItems().addAll("Last 7 Days", "Last 30 Days", "Last 3 Months", "Last Year");
        trendPeriodCombo.setValue("Last 30 Days");
        trendPeriodCombo.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            if (showTrendChartCheck.isSelected()) {
                updateTrendChart();
            }
        });
        
        periodBox.getChildren().addAll(periodLabel, trendPeriodCombo);
        
        panel.getChildren().addAll(panelTitle, metricsLabel, metricsBox, chartsLabel, chartsBox, periodBox);
        return panel;
    }

    private HBox createControlBar() {
        HBox controlBar = new HBox(15);
        controlBar.setAlignment(Pos.CENTER);
        controlBar.setPadding(new Insets(10));
        
        lastUpdatedLabel = new Label("Last updated: Just now");
        lastUpdatedLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        
        Label autoRefreshLabel = new Label("🔄 Auto-refreshes every " + REFRESH_INTERVAL_SECONDS + " seconds");
        autoRefreshLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981;");
        
        Button refreshBtn = new Button("🔄 Refresh Now");
        refreshBtn.getStyleClass().addAll("button", "secondary-btn");
        refreshBtn.setOnAction(e -> refreshStats());
        
        Button exportHTMLBtn = new Button("📄 Export to HTML");
        exportHTMLBtn.getStyleClass().addAll("button", "primary-btn");
        exportHTMLBtn.setOnAction(e -> exportToHTML());
        
        Button exportExcelBtn = new Button("📊 Export to Excel");
        exportExcelBtn.getStyleClass().addAll("button", "primary-btn");
        exportExcelBtn.setOnAction(e -> exportToExcel());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        HBox leftBox = new HBox(10);
        leftBox.getChildren().addAll(lastUpdatedLabel, autoRefreshLabel);
        
        HBox rightBox = new HBox(10);
        rightBox.getChildren().addAll(refreshBtn, exportHTMLBtn, exportExcelBtn);
        
        controlBar.getChildren().addAll(leftBox, spacer, rightBox);
        return controlBar;
    }

    private VBox createBarChartSection() {
        VBox section = new VBox(10);
        
        Label sectionLabel = new Label("📊 Book Borrow Counts");
        sectionLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Books");
        
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Number of Borrows");
        
        barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Book Borrow Counts");
        barChart.setPrefHeight(400);
        barChart.setAnimated(true);
        
        VBox chartBox = new VBox(10);
        chartBox.setPadding(new Insets(20));
        chartBox.setStyle("-fx-background-color: white; -fx-background-radius: 16px; " +
                         "-fx-border-color: #e2e8f0; -fx-border-radius: 16px;");
        chartBox.getChildren().add(barChart);
        
        section.getChildren().addAll(sectionLabel, chartBox);
        return section;
    }

    private VBox createTrendSection() {
        VBox section = new VBox(10);
        
        Label sectionLabel = new Label("📈 Borrowing Trends");
        sectionLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date");
        
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Number of Borrows");
        
        trendChart = new LineChart<>(xAxis, yAxis);
        trendChart.setTitle("Borrowing Trends Over Time");
        trendChart.setPrefHeight(350);
        trendChart.setAnimated(true);
        
        VBox chartBox = new VBox(10);
        chartBox.setPadding(new Insets(20));
        chartBox.setStyle("-fx-background-color: white; -fx-background-radius: 16px; " +
                         "-fx-border-color: #e2e8f0; -fx-border-radius: 16px;");
        chartBox.getChildren().add(trendChart);
        
        section.getChildren().addAll(sectionLabel, chartBox);
        return section;
    }

    private void refreshStats() {
        currentStats = authorService.getAuthorBookStats(currentAuthor.getUsername());
        
        int totalBooks = currentStats.size();
        int totalBorrows = authorService.getTotalBorrowsForAuthor(currentAuthor.getUsername());
        double avgRating = authorService.getAverageRatingForAuthor(currentAuthor.getUsername());
        int totalReviews = authorService.getTotalReviewsForAuthor(currentAuthor.getUsername());
        List<BookStats> topBooks = trendService.getTopBooksByRealBorrows(currentAuthor.getUsername(), 5);
        
        cardsContainer.getChildren().clear();
        HBox cardsRow = new HBox(20);
        cardsRow.setAlignment(Pos.CENTER);
        
        if (showTotalBooksCheck.isSelected()) {
            cardsRow.getChildren().add(createStatCard("📚 Total Books", String.valueOf(totalBooks), "Published books"));
        }
        if (showTotalBorrowsCheck.isSelected()) {
            cardsRow.getChildren().add(createStatCard("📖 Total Borrows", String.valueOf(totalBorrows), "All time reads"));
        }
        if (showAvgRatingCheck.isSelected() && avgRating > 0) {
            cardsRow.getChildren().add(createStatCard("⭐ Average Rating", String.format("%.1f", avgRating) + " / 5", "From user reviews"));
        }
        if (showTotalReviewsCheck.isSelected()) {
            cardsRow.getChildren().add(createStatCard("💬 Total Reviews", String.valueOf(totalReviews), "User feedback"));
        }
        
        if (cardsRow.getChildren().isEmpty()) {
            Label emptyLabel = new Label("No metrics selected");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
            cardsRow.getChildren().add(emptyLabel);
        }
        
        cardsContainer.getChildren().add(cardsRow);
        
        if (showTopBooksCheck.isSelected() && !topBooks.isEmpty()) {
            VBox topBooksBox = new VBox(10);
            topBooksBox.setAlignment(Pos.CENTER);
            topBooksBox.setPadding(new Insets(15, 0, 0, 0));
            
            Label topBooksLabel = new Label("🏆 Top 5 Most Popular Books");
            topBooksLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
            
            VBox booksList = new VBox(5);
            for (int i = 0; i < topBooks.size(); i++) {
                BookStats book = topBooks.get(i);
                Label bookLabel = new Label((i + 1) + ". " + book.getTitle() + " - " + book.getBorrowCount() + " borrows");
                bookLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
                booksList.getChildren().add(bookLabel);
            }
            
            topBooksBox.getChildren().addAll(topBooksLabel, booksList);
            cardsContainer.getChildren().add(topBooksBox);
        }
        
        updateBarChart(currentStats);
        updateTrendChart();
        
        lastUpdatedLabel.setText("Last updated: " + java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    private void updateBarChart(List<BookStats> stats) {
        barChart.getData().clear();
        
        if (stats.isEmpty()) {
            XYChart.Series<String, Number> emptySeries = new XYChart.Series<>();
            emptySeries.setName("No Data");
            emptySeries.getData().add(new XYChart.Data<>("No books", 0));
            barChart.getData().add(emptySeries);
            return;
        }
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Borrow Count");
        
        int maxBooks = Math.min(10, stats.size());
        for (int i = 0; i < maxBooks; i++) {
            BookStats stat = stats.get(i);
            String title = stat.getTitle().length() > 20 ? stat.getTitle().substring(0, 20) + "..." : stat.getTitle();
            series.getData().add(new XYChart.Data<>(title, stat.getBorrowCount()));
        }
        barChart.getData().add(series);
    }

    private void updateTrendChart() {
        String period = trendPeriodCombo.getValue();
        int days = 30;
        if (period.equals("Last 7 Days")) days = 7;
        else if (period.equals("Last 30 Days")) days = 30;
        else if (period.equals("Last 3 Months")) days = 90;
        else if (period.equals("Last Year")) days = 365;
        
        currentTrendData = trendService.getRealBorrowTrends(currentAuthor.getUsername(), days);
        
        trendChart.getData().clear();
        
        if (currentTrendData.isEmpty()) {
            XYChart.Series<String, Number> emptySeries = new XYChart.Series<>();
            emptySeries.setName("No Data");
            emptySeries.getData().add(new XYChart.Data<>("No data", 0));
            trendChart.getData().add(emptySeries);
            return;
        }
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Daily Borrows");
        
        int step = days > 90 ? 7 : 1;
        for (Map.Entry<LocalDate, Integer> entry : currentTrendData.entrySet()) {
            LocalDate date = entry.getKey();
            if (step > 1 && date.getDayOfMonth() % step != 0 && !date.equals(LocalDate.now())) {
                continue;
            }
            String dateStr = date.format(DateTimeFormatter.ofPattern("MM/dd"));
            series.getData().add(new XYChart.Data<>(dateStr, entry.getValue()));
        }
        
        trendChart.getData().add(series);
    }

    private VBox createStatCard(String title, String value, String subtitle) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16px; " +
                     "-fx-border-color: #e2e8f0; -fx-border-radius: 16px;");
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

    private void exportToHTML() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Statistics to HTML");
        fileChooser.setInitialFileName("author_stats_" + currentAuthor.getUsername() + "_" + 
                                       LocalDate.now() + ".html");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("HTML Files", "*.html")
        );
        
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                HTMLReportExporter.exportToHTML(
                    file.getAbsolutePath(),
                    currentAuthor.getFullName(),
                    currentAuthor.getUsername(),
                    currentStats,
                    authorService.getTotalBorrowsForAuthor(currentAuthor.getUsername()),
                    authorService.getAverageRatingForAuthor(currentAuthor.getUsername()),
                    authorService.getTotalReviewsForAuthor(currentAuthor.getUsername()),
                    currentTrendData
                );
                showAlert("Export Successful", "HTML report generated at:\n" + file.getAbsolutePath() + 
                         "\n\nOpen in browser and press Cmd+P to save as PDF", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Export Failed", "Error generating report: " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        }
    }

    private void exportToExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Statistics to Excel");
        fileChooser.setInitialFileName("author_stats_" + currentAuthor.getUsername() + "_" + 
                                       LocalDate.now() + ".xls");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Excel Files", "*.xls", "*.xlsx")
        );
        
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println("AUTHOR STATISTICS REPORT");
                writer.println("Generated:\t" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                writer.println("Author:\t" + currentAuthor.getUsername() + "\t(" + currentAuthor.getFullName() + ")");
                writer.println();
                writer.println("SUMMARY METRICS");
                writer.println("Total Books:\t" + currentStats.size());
                writer.println("Total Borrows:\t" + authorService.getTotalBorrowsForAuthor(currentAuthor.getUsername()));
                writer.println("Average Rating:\t" + authorService.getAverageRatingForAuthor(currentAuthor.getUsername()));
                writer.println("Total Reviews:\t" + authorService.getTotalReviewsForAuthor(currentAuthor.getUsername()));
                writer.println();
                writer.println("BOOK DETAILS");
                writer.println("Book Title\tBorrow Count\tAverage Rating\tReview Count\tStatus");
                
                for (BookStats stat : currentStats) {
                    writer.printf("%s\t%d\t%.1f\t%d\t%s%n",
                        stat.getTitle(),
                        stat.getBorrowCount(),
                        stat.getAverageRating(),
                        stat.getReviewCount(),
                        stat.getStatus()
                    );
                }
                
                showAlert("Export Successful", "Excel report exported to:\n" + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (IOException e) {
                showAlert("Export Failed", "Error exporting data: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void startAutoRefresh() {
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                javafx.application.Platform.runLater(() -> refreshStats());
            }
        }, REFRESH_INTERVAL_SECONDS * 1000, REFRESH_INTERVAL_SECONDS * 1000);
        
        System.out.println("🔄 Statistics auto-refresh started");
    }

    private void stopAutoRefresh() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
            System.out.println("🛑 Statistics auto-refresh stopped");
        }
    }
}
