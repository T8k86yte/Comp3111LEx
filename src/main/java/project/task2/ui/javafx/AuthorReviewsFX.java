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
import project.task2.model.BookSubmission;
import project.task2.model.Review;
import project.task2.service.AuthorPortalService;
import project.task2.service.SentimentAnalysisService;
import project.task2.service.ReplyTemplateService;
import project.task2.service.FeedbackAnalyticsService;
import project.task2.utils.ProfilePictureManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class AuthorReviewsFX {
    private AuthorPortalService authorService;
    private SentimentAnalysisService sentimentService;
    private ReplyTemplateService templateService;
    private FeedbackAnalyticsService analyticsService;
    private AuthorAccount currentAuthor;
    private Stage stage;
    private VBox reviewsContainer;
    private Label statusLabel;
    
    private TabPane tabPane;
    private Tab reviewsTab;
    private Tab analyticsTab;
    
    private ComboBox<String> bookFilterCombo;
    private ComboBox<Integer> ratingFilterCombo;
    private ComboBox<String> dateFilterCombo;
    private ComboBox<String> sentimentFilterCombo;
    private TextField searchField;
    private CheckBox flaggedOnlyCheckBox;
    private Label filterCountLabel;
    
    private List<Review> allReviews;
    private List<BookSubmission> authorBooks;

    public AuthorReviewsFX(AuthorAccount author) {
        this.currentAuthor = author;
        this.authorService = new AuthorPortalService();
        this.sentimentService = new SentimentAnalysisService();
        this.templateService = new ReplyTemplateService();
        this.analyticsService = new FeedbackAnalyticsService();
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

        VBox centerContent = new VBox(20);
        centerContent.setAlignment(Pos.TOP_CENTER);
        centerContent.setPadding(new Insets(20, 30, 30, 30));

        VBox titleSection = new VBox(8);
        titleSection.setAlignment(Pos.CENTER);
        
        Label titleLabel = new Label("📝 Reviews & Feedback");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        
        Label subtitleLabel = new Label("View, analyze, and respond to reader reviews");
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
        
        titleSection.getChildren().addAll(titleLabel, subtitleLabel);
        
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        reviewsTab = new Tab("📋 Reviews");
        analyticsTab = new Tab("📊 Analytics");
        reviewsTab.setClosable(false);
        analyticsTab.setClosable(false);
        
        reviewsTab.setContent(createReviewsContent());
        analyticsTab.setContent(createAnalyticsContent());
        
        tabPane.getTabs().addAll(reviewsTab, analyticsTab);

        centerContent.getChildren().addAll(titleSection, tabPane);
        root.setCenter(centerContent);

        Scene scene = new Scene(root, 1000, 750);
        scene.getStylesheets().add(getClass().getResource("/project/task2/css/author-portal.css").toExternalForm());
        
        stage.setTitle("Reviews & Feedback - " + currentAuthor.getUsername());
        stage.setScene(scene);
        stage.show();
        
        loadData();
    }
    
    private VBox createReviewsContent() {
        VBox content = new VBox(15);
        
        VBox filterSection = createFilterSection();
        HBox controlBar = createControlBar();
        
        reviewsContainer = new VBox(15);
        reviewsContainer.setPadding(new Insets(10));
        
        ScrollPane scrollPane = new ScrollPane(reviewsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(500);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        statusLabel.setVisible(false);
        
        content.getChildren().addAll(filterSection, controlBar, scrollPane, statusLabel);
        return content;
    }
    
    private VBox createAnalyticsContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        
        Label sentimentLabel = new Label("📊 Sentiment Distribution");
        sentimentLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        
        PieChart sentimentChart = new PieChart();
        sentimentChart.setPrefHeight(300);
        sentimentChart.setTitle("Review Sentiment");
        
        Label ratingLabel = new Label("⭐ Rating Distribution");
        ratingLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Rating");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Number of Reviews");
        
        BarChart<String, Number> ratingChart = new BarChart<>(xAxis, yAxis);
        ratingChart.setPrefHeight(300);
        
        Label summaryLabel = new Label("📈 Summary Metrics");
        summaryLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        
        HBox summaryCards = new HBox(15);
        summaryCards.setAlignment(Pos.CENTER);
        summaryCards.setPadding(new Insets(10));
        
        VBox statsBox = new VBox(10);
        statsBox.getChildren().addAll(sentimentLabel, sentimentChart, ratingLabel, ratingChart, summaryLabel, summaryCards);
        
        loadAnalytics(sentimentChart, ratingChart, summaryCards);
        
        return statsBox;
    }
    
    private void loadAnalytics(PieChart sentimentChart, BarChart<String, Number> ratingChart, HBox summaryCards) {
        FeedbackAnalyticsService.FeedbackStats stats = analyticsService.getStatsForAuthor(currentAuthor.getUsername());
        
        sentimentChart.getData().clear();
        sentimentChart.getData().add(new PieChart.Data("Positive (" + stats.sentimentCounts.get("positive") + ")", stats.sentimentCounts.get("positive")));
        sentimentChart.getData().add(new PieChart.Data("Neutral (" + stats.sentimentCounts.get("neutral") + ")", stats.sentimentCounts.get("neutral")));
        sentimentChart.getData().add(new PieChart.Data("Negative (" + stats.sentimentCounts.get("negative") + ")", stats.sentimentCounts.get("negative")));
        
        ratingChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Number of Reviews");
        for (int i = 1; i <= 5; i++) {
            series.getData().add(new XYChart.Data<>(i + " Stars", stats.ratingDistribution.get(i)));
        }
        ratingChart.getData().add(series);
        
        summaryCards.getChildren().clear();
        summaryCards.getChildren().addAll(
            createAnalyticsCard("📝 Total Reviews", String.valueOf(stats.totalReviews)),
            createAnalyticsCard("⭐ Average Rating", String.format("%.1f / 5", stats.averageRating)),
            createAnalyticsCard("😊 Positive", String.format("%.1f%%", stats.positivePercentage)),
            createAnalyticsCard("😞 Negative", String.format("%.1f%%", stats.negativePercentage))
        );
    }
    
    private VBox createAnalyticsCard(String title, String value) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12px; -fx-border-color: #e2e8f0; -fx-border-radius: 12px;");
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(150);
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2563eb;");
        
        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
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
        closeBtn.setOnAction(e -> stage.close());

        HBox userInfo = new HBox(12);
        userInfo.setAlignment(Pos.CENTER);
        userInfo.getChildren().addAll(avatarContainer, usernameLabel);
        
        topBar.getChildren().addAll(userInfo, spacer, closeBtn);
        return topBar;
    }

    private VBox createFilterSection() {
        VBox filterBox = new VBox(10);
        filterBox.setStyle("-fx-background-color: white; -fx-background-radius: 12px; " +
                          "-fx-border-color: #e2e8f0; -fx-border-radius: 12px;");
        filterBox.setPadding(new Insets(15));
        
        Label filterTitle = new Label("🔍 Filter Reviews");
        filterTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
        
        GridPane filterGrid = new GridPane();
        filterGrid.setHgap(15);
        filterGrid.setVgap(10);
        filterGrid.setPadding(new Insets(10, 0, 0, 0));
        
        Label bookLabel = new Label("Book:");
        bookLabel.getStyleClass().add("muted");
        bookFilterCombo = new ComboBox<>();
        bookFilterCombo.setPromptText("All Books");
        bookFilterCombo.setPrefWidth(200);
        bookFilterCombo.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        
        Label ratingLabel = new Label("Rating:");
        ratingLabel.getStyleClass().add("muted");
        ratingFilterCombo = new ComboBox<>();
        ratingFilterCombo.getItems().addAll(0, 1, 2, 3, 4, 5);
        ratingFilterCombo.setValue(0);
        ratingFilterCombo.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        
        Label dateLabel = new Label("Date:");
        dateLabel.getStyleClass().add("muted");
        dateFilterCombo = new ComboBox<>();
        dateFilterCombo.getItems().addAll("All Time", "Last 7 Days", "Last 30 Days", "This Year");
        dateFilterCombo.setValue("All Time");
        dateFilterCombo.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        
        Label sentimentFilterLabel = new Label("Sentiment:");
        sentimentFilterLabel.getStyleClass().add("muted");
        sentimentFilterCombo = new ComboBox<>();
        sentimentFilterCombo.getItems().addAll("All", "Positive", "Neutral", "Negative");
        sentimentFilterCombo.setValue("All");
        sentimentFilterCombo.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        
        Label searchLabel = new Label("Search:");
        searchLabel.getStyleClass().add("muted");
        searchField = new TextField();
        searchField.setPromptText("Search in reviews...");
        searchField.setPrefWidth(200);
        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());
        
        flaggedOnlyCheckBox = new CheckBox("⚠️ Show flagged reviews only");
        flaggedOnlyCheckBox.getStyleClass().add("muted");
        flaggedOnlyCheckBox.selectedProperty().addListener((obs, old, newVal) -> applyFilters());
        
        Button clearFiltersBtn = new Button("Clear Filters");
        clearFiltersBtn.getStyleClass().addAll("button", "secondary-btn");
        clearFiltersBtn.setPrefWidth(100);
        clearFiltersBtn.setOnAction(e -> clearFilters());
        
        filterGrid.add(bookLabel, 0, 0);
        filterGrid.add(bookFilterCombo, 1, 0);
        filterGrid.add(ratingLabel, 2, 0);
        filterGrid.add(ratingFilterCombo, 3, 0);
        filterGrid.add(dateLabel, 4, 0);
        filterGrid.add(dateFilterCombo, 5, 0);
        
        filterGrid.add(sentimentFilterLabel, 0, 1);
        filterGrid.add(sentimentFilterCombo, 1, 1);
        filterGrid.add(searchLabel, 2, 1);
        filterGrid.add(searchField, 3, 1);
        filterGrid.add(flaggedOnlyCheckBox, 4, 1);
        
        filterBox.getChildren().addAll(filterTitle, filterGrid);
        
        HBox clearRow = new HBox();
        clearRow.setAlignment(Pos.CENTER_RIGHT);
        clearRow.getChildren().add(clearFiltersBtn);
        filterBox.getChildren().add(clearRow);
        
        return filterBox;
    }
    
    private HBox createControlBar() {
        HBox controlBar = new HBox(15);
        controlBar.setAlignment(Pos.CENTER_RIGHT);
        controlBar.setPadding(new Insets(0, 0, 10, 0));
        
        filterCountLabel = new Label();
        filterCountLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().addAll("button", "primary-btn");
        refreshBtn.setOnAction(e -> loadData());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        controlBar.getChildren().addAll(filterCountLabel, spacer, refreshBtn);
        return controlBar;
    }
    
    private void clearFilters() {
        bookFilterCombo.setValue(null);
        ratingFilterCombo.setValue(0);
        dateFilterCombo.setValue("All Time");
        sentimentFilterCombo.setValue("All");
        searchField.clear();
        flaggedOnlyCheckBox.setSelected(false);
        applyFilters();
    }
    
    private void loadData() {
        authorBooks = authorService.getAuthorSubmissions(currentAuthor.getUsername())
                .stream()
                .filter(b -> b.isApproved() || b.isPending())
                .collect(Collectors.toList());
        
        allReviews = authorService.getReviewsForAuthorBooks(currentAuthor.getUsername());
        
        bookFilterCombo.getItems().clear();
        bookFilterCombo.getItems().add("All Books");
        for (BookSubmission book : authorBooks) {
            bookFilterCombo.getItems().add(book.getTitle());
        }
        bookFilterCombo.setValue("All Books");
        
        applyFilters();
        
        if (analyticsTab.isSelected()) {
            refreshAnalytics();
        }
    }
    
    private void refreshAnalytics() {
        VBox analyticsContent = createAnalyticsContent();
        analyticsTab.setContent(analyticsContent);
    }
    
    private void applyFilters() {
        if (allReviews == null) return;
        
        String selectedBook = bookFilterCombo.getValue();
        int minRating = ratingFilterCombo.getValue() != null ? ratingFilterCombo.getValue() : 0;
        String dateRange = dateFilterCombo.getValue();
        String sentimentFilter = sentimentFilterCombo.getValue();
        String searchText = searchField.getText().toLowerCase();
        boolean flaggedOnly = flaggedOnlyCheckBox.isSelected();
        
        List<Review> filtered = allReviews.stream()
            .filter(review -> {
                if (selectedBook != null && !selectedBook.equals("All Books")) {
                    if (!review.getBookTitle().equals(selectedBook)) {
                        return false;
                    }
                }
                if (minRating > 0 && review.getRating() < minRating) {
                    return false;
                }
                if (dateRange != null && !dateRange.equals("All Time")) {
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime cutoff = switch (dateRange) {
                        case "Last 7 Days" -> now.minusDays(7);
                        case "Last 30 Days" -> now.minusDays(30);
                        case "This Year" -> now.minusDays(365);
                        default -> null;
                    };
                    if (cutoff != null && review.getCreatedAt().isBefore(cutoff)) {
                        return false;
                    }
                }
                if (!sentimentFilter.equals("All")) {
                    var sentiment = sentimentService.analyzeSentiment(review.getComment());
                    if (!sentiment.getDisplayName().contains(sentimentFilter)) {
                        return false;
                    }
                }
                if (!searchText.isEmpty()) {
                    if (!review.getComment().toLowerCase().contains(searchText) &&
                        !review.getReviewerFullName().toLowerCase().contains(searchText)) {
                        return false;
                    }
                }
                if (flaggedOnly && !review.isFlagged()) {
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());
        
        filterCountLabel.setText("Showing " + filtered.size() + " of " + allReviews.size() + " reviews");
        displayReviews(filtered);
    }
    
    private void displayReviews(List<Review> reviews) {
        reviewsContainer.getChildren().clear();
        
        if (reviews.isEmpty()) {
            Label emptyLabel = new Label("📭 No reviews match your filters");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b;");
            emptyLabel.setAlignment(Pos.CENTER);
            emptyLabel.setMaxWidth(Double.MAX_VALUE);
            reviewsContainer.getChildren().add(emptyLabel);
            return;
        }

        for (Review review : reviews) {
            VBox card = createReviewCard(review);
            reviewsContainer.getChildren().add(card);
        }
    }

    private VBox createReviewCard(Review review) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12px; " +
                     "-fx-border-color: #e2e8f0; -fx-border-radius: 12px; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.03), 4, 0, 0, 1);");
        
        if (review.isFlagged()) {
            card.setStyle(card.getStyle() + "-fx-border-color: #ef4444; -fx-border-width: 2px;");
        }
        
        var sentiment = sentimentService.analyzeSentiment(review.getComment());
        String sentimentColor = sentiment.getColor();
        
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label bookTitle = new Label("📖 " + review.getBookTitle());
        bookTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0f172a;");
        
        String stars = getStarRating(review.getRating());
        Label ratingLabel = new Label(stars);
        ratingLabel.setStyle("-fx-font-size: 12px;");
        
        Label sentimentLabel = new Label(sentiment.getDisplayName());
        sentimentLabel.setStyle("-fx-background-color: " + sentimentColor + "20; -fx-text-fill: " + sentimentColor + "; " +
                               "-fx-padding: 2px 8px; -fx-background-radius: 12px; -fx-font-size: 11px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label dateLabel = new Label(review.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        
        header.getChildren().addAll(bookTitle, ratingLabel, sentimentLabel, spacer, dateLabel);
        
        Label reviewerLabel = new Label("⭐ " + review.getReviewerFullName() + " (" + review.getReviewerUsername() + ") rated " + review.getRating() + "/5");
        reviewerLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        
        TextArea commentArea = new TextArea(review.getComment());
        commentArea.setEditable(false);
        commentArea.setWrapText(true);
        commentArea.setPrefRowCount(3);
        commentArea.setStyle("-fx-background-color: #f8fafc; -fx-font-size: 13px;");
        
        VBox replySection = new VBox(8);
        Label replyLabel = new Label("📝 Your Reply:");
        replyLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #0f172a;");
        
        ComboBox<String> templateCombo = new ComboBox<>();
        templateCombo.getItems().addAll(templateService.getAllTemplates().keySet());
        templateCombo.setPromptText("Quick reply templates...");
        templateCombo.setStyle("-fx-font-size: 11px;");
        
        TextArea replyArea = new TextArea();
        replyArea.setPromptText("Write your reply to this review...");
        replyArea.setWrapText(true);
        replyArea.setPrefRowCount(2);
        
        if (review.getAuthorReply() != null && !review.getAuthorReply().isEmpty()) {
            replyArea.setText(review.getAuthorReply());
            replyArea.setEditable(false);
            templateCombo.setDisable(true);
        }
        
        // Template selection action
        templateCombo.setOnAction(e -> {
            String selected = templateCombo.getValue();
            if (selected != null && replyArea.isEditable()) {
                replyArea.setText(templateService.getTemplate(selected));
            }
        });
        
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_LEFT);
        
        if (review.getAuthorReply() == null || review.getAuthorReply().isEmpty()) {
            Button replyBtn = new Button("💬 Send Reply");
            replyBtn.getStyleClass().addAll("button", "primary-btn");
            replyBtn.setOnAction(ev -> {
                String replyText = replyArea.getText().trim();
                if (replyText.isEmpty()) {
                    showStatus("Please enter a reply message", "error");
                    return;
                }
                boolean success = authorService.replyToReview(review.getReviewId(), replyText);
                if (success) {
                    showStatus("Reply sent successfully!", "success");
                    loadData();
                } else {
                    showStatus("Failed to send reply", "error");
                }
            });
            actionBox.getChildren().addAll(replyBtn, templateCombo);
        } else {
            Label repliedLabel = new Label("✓ Replied on: " + review.getReplyDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            repliedLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981;");
            actionBox.getChildren().add(repliedLabel);
        }
        
        if (!review.isFlagged()) {
            Button flagBtn = new Button("🚩 Flag as Inappropriate");
            flagBtn.getStyleClass().addAll("button", "danger-btn");
            flagBtn.setOnAction(ev -> {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Flag Review");
                dialog.setHeaderText("Flag this review as inappropriate");
                dialog.setContentText("Please provide a reason:");
                dialog.showAndWait().ifPresent(reason -> {
                    if (!reason.trim().isEmpty()) {
                        boolean success = authorService.flagReview(review.getReviewId(), reason);
                        if (success) {
                            showStatus("Review flagged successfully", "success");
                            loadData();
                        } else {
                            showStatus("Failed to flag review", "error");
                        }
                    }
                });
            });
            actionBox.getChildren().add(flagBtn);
        } else {
            Label flaggedLabel = new Label("⚠️ Flagged as inappropriate");
            flaggedLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ef4444;");
            actionBox.getChildren().add(flaggedLabel);
        }
        
        replySection.getChildren().addAll(replyLabel, replyArea, actionBox);
        
        card.getChildren().addAll(header, reviewerLabel, commentArea, replySection);
        return card;
    }

    private String getStarRating(int rating) {
        String stars = "";
        for (int i = 0; i < rating; i++) {
            stars += "★";
        }
        for (int i = rating; i < 5; i++) {
            stars += "☆";
        }
        return stars;
    }

    private void showStatus(String message, String type) {
        statusLabel.setText(message);
        if (type.equals("success")) {
            statusLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 12px;");
        } else {
            statusLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
        }
        statusLabel.setVisible(true);
        
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                javafx.application.Platform.runLater(() -> statusLabel.setVisible(false));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
