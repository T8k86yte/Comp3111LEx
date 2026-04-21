package project.task2.ui.javafx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import project.task2.model.AuthorAccount;
import project.task2.model.Review;
import project.task2.service.AuthorPortalService;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class AuthorReviewsFX {
    private AuthorPortalService authorService;
    private AuthorAccount currentAuthor;
    private Stage stage;
    private VBox reviewsContainer;
    private Label statusLabel;

    public AuthorReviewsFX(AuthorAccount author) {
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

        VBox centerContent = new VBox(20);
        centerContent.setAlignment(Pos.TOP_CENTER);
        centerContent.setPadding(new Insets(20, 30, 30, 30));

        VBox titleSection = new VBox(8);
        titleSection.setAlignment(Pos.CENTER);
        
        Label titleLabel = new Label("📝 Reviews & Feedback");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        
        Label subtitleLabel = new Label("View and respond to reader reviews on your books");
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
        
        titleSection.getChildren().addAll(titleLabel, subtitleLabel);

        // Refresh button
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().addAll("button", "primary-btn");
        refreshBtn.setOnAction(e -> loadReviews());

        HBox controlBar = new HBox();
        controlBar.setAlignment(Pos.CENTER_RIGHT);
        controlBar.getChildren().add(refreshBtn);

        // Reviews container
        reviewsContainer = new VBox(15);
        reviewsContainer.setPadding(new Insets(10));
        
        ScrollPane scrollPane = new ScrollPane(reviewsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(500);
        scrollPane.setStyle("-fx-background-color: transparent;");

        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        statusLabel.setVisible(false);

        centerContent.getChildren().addAll(titleSection, controlBar, scrollPane, statusLabel);
        root.setCenter(centerContent);

        Scene scene = new Scene(root, 800, 650);
        scene.getStylesheets().add(getClass().getResource("/project/task2/css/author-portal.css").toExternalForm());
        
        stage.setTitle("Reviews & Feedback - " + currentAuthor.getUsername());
        stage.setScene(scene);
        stage.show();
        
        loadReviews();
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

    private void loadReviews() {
        List<Review> reviews = authorService.getReviewsForAuthorBooks(currentAuthor.getUsername());
        
        reviewsContainer.getChildren().clear();
        
        if (reviews.isEmpty()) {
            Label emptyLabel = new Label("📭 No reviews yet for your books");
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
        
        showStatus("Loaded " + reviews.size() + " reviews", "success");
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

        // Header: Book title and rating
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label bookTitle = new Label("📖 " + review.getBookTitle());
        bookTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0f172a;");
        
        // Star rating
        String stars = getStarRating(review.getRating());
        Label ratingLabel = new Label(stars);
        ratingLabel.setStyle("-fx-font-size: 12px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label dateLabel = new Label(review.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        
        header.getChildren().addAll(bookTitle, ratingLabel, spacer, dateLabel);
        
        // Reviewer info
        Label reviewerLabel = new Label("⭐ " + review.getReviewerFullName() + " (" + review.getReviewerUsername() + ") rated " + review.getRating() + "/5");
        reviewerLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #475569;");
        
        // Review comment
        TextArea commentArea = new TextArea(review.getComment());
        commentArea.setEditable(false);
        commentArea.setWrapText(true);
        commentArea.setPrefRowCount(3);
        commentArea.setStyle("-fx-background-color: #f8fafc; -fx-font-size: 13px;");
        
        // Author reply section
        VBox replySection = new VBox(8);
        Label replyLabel = new Label("📝 Your Reply:");
        replyLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #0f172a;");
        
        TextArea replyArea = new TextArea();
        replyArea.setPromptText("Write your reply to this review...");
        replyArea.setWrapText(true);
        replyArea.setPrefRowCount(2);
        
        if (review.getAuthorReply() != null && !review.getAuthorReply().isEmpty()) {
            replyArea.setText(review.getAuthorReply());
            replyArea.setEditable(false);
        }
        
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_LEFT);
        
        if (review.getAuthorReply() == null || review.getAuthorReply().isEmpty()) {
            Button replyBtn = new Button("💬 Send Reply");
            replyBtn.getStyleClass().addAll("button", "primary-btn");
            replyBtn.setOnAction(e -> {
                String replyText = replyArea.getText().trim();
                if (replyText.isEmpty()) {
                    showStatus("Please enter a reply message", "error");
                    return;
                }
                boolean success = authorService.replyToReview(review.getReviewId(), replyText);
                if (success) {
                    showStatus("Reply sent successfully!", "success");
                    loadReviews(); // Refresh to show the reply
                } else {
                    showStatus("Failed to send reply", "error");
                }
            });
            actionBox.getChildren().add(replyBtn);
        } else {
            Label repliedLabel = new Label("✓ Replied on: " + review.getReplyDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            repliedLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #10b981;");
            actionBox.getChildren().add(repliedLabel);
        }
        
        // Flag button (if not already flagged)
        if (!review.isFlagged()) {
            Button flagBtn = new Button("🚩 Flag as Inappropriate");
            flagBtn.getStyleClass().addAll("button", "danger-btn");
            flagBtn.setOnAction(e -> {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Flag Review");
                dialog.setHeaderText("Flag this review as inappropriate");
                dialog.setContentText("Please provide a reason:");
                dialog.showAndWait().ifPresent(reason -> {
                    if (!reason.trim().isEmpty()) {
                        boolean success = authorService.flagReview(review.getReviewId(), reason);
                        if (success) {
                            showStatus("Review flagged successfully", "success");
                            loadReviews();
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
