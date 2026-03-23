package project.task2.ui.javafx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import project.task2.model.AuthorAccount;
import project.task2.model.Notification;
import project.task2.service.AuthorPortalService;

import java.util.List;

public class NotificationBoardFX {
    private AuthorPortalService authorService;
    private AuthorAccount currentAuthor;
    private Stage stage;
    private VBox notificationsContainer;
    private Label unreadCountLabel;
    private List<Notification> notifications;

    public NotificationBoardFX(AuthorAccount author) {
        this.currentAuthor = author;
        this.authorService = new AuthorPortalService();
        this.stage = new Stage();
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
        
        Label titleLabel = new Label("🔔 Notification Board");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        
        Label subtitleLabel = new Label("Stay updated on your book submissions and announcements");
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
        
        titleSection.getChildren().addAll(titleLabel, subtitleLabel);

        // Unread count badge
        HBox badgeBox = new HBox(15);
        badgeBox.setAlignment(Pos.CENTER_RIGHT);
        badgeBox.setPadding(new Insets(0, 0, 10, 0));
        
        unreadCountLabel = new Label();
        unreadCountLabel.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; " +
                                  "-fx-padding: 4px 12px; -fx-background-radius: 20px; -fx-font-size: 12px;");
        
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-weight: 500; " +
                           "-fx-background-radius: 8px; -fx-padding: 6px 16px; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> loadNotifications());
        
        Button markAllReadBtn = new Button("✓ Mark All as Read");
        markAllReadBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-font-weight: 500; " +
                                "-fx-background-radius: 8px; -fx-padding: 6px 16px; -fx-cursor: hand;");
        markAllReadBtn.setOnAction(e -> markAllAsRead());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        badgeBox.getChildren().addAll(spacer, unreadCountLabel, refreshBtn, markAllReadBtn);
        
        // Notifications container
        notificationsContainer = new VBox(10);
        notificationsContainer.setPadding(new Insets(10));
        
        ScrollPane scrollPane = new ScrollPane(notificationsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(450);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        
        centerContent.getChildren().addAll(titleSection, badgeBox, scrollPane);
        root.setCenter(centerContent);

        Scene scene = new Scene(root, 550, 600);
        scene.getStylesheets().add(getClass().getResource("/project/task2/css/author-portal.css").toExternalForm());
        
        stage.setTitle("Notification Board - " + currentAuthor.getUsername());
        stage.setScene(scene);
        stage.show();
        
        loadNotifications();
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

    private void loadNotifications() {
        notifications = authorService.getNotifications(currentAuthor.getUsername());
        int unreadCount = authorService.getUnreadNotificationCount(currentAuthor.getUsername());
        
        updateUnreadCount(unreadCount);
        
        notificationsContainer.getChildren().clear();
        
        if (notifications.isEmpty()) {
            VBox emptyBox = new VBox(15);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(50));
            
            Label emptyIcon = new Label("🔔");
            emptyIcon.setStyle("-fx-font-size: 48px;");
            Label emptyLabel = new Label("No notifications yet");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b;");
            Label emptyHint = new Label("When your books are reviewed, notifications will appear here");
            emptyHint.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
            
            emptyBox.getChildren().addAll(emptyIcon, emptyLabel, emptyHint);
            notificationsContainer.getChildren().add(emptyBox);
        } else {
            for (Notification notification : notifications) {
                VBox card = createNotificationCard(notification);
                notificationsContainer.getChildren().add(card);
            }
        }
    }

    private VBox createNotificationCard(Notification notification) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12px; " +
                     "-fx-border-color: #e2e8f0; -fx-border-radius: 12px; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.03), 4, 0, 0, 1);");
        
        if (!notification.isRead()) {
            card.setStyle(card.getStyle() + "-fx-border-color: #3b82f6; -fx-border-width: 2px;");
        }
        
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label typeIcon = new Label(notification.getTypeIcon());
        typeIcon.setStyle("-fx-font-size: 20px;");
        
        Label titleLabel = new Label(notification.getTitle());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0f172a;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label dateLabel = new Label(notification.getFormattedDate());
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        
        header.getChildren().addAll(typeIcon, titleLabel, spacer, dateLabel);
        
        Label messageLabel = new Label(notification.getMessage());
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
        
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        
        if (!notification.isRead()) {
            Button markReadBtn = new Button("Mark as read");
            markReadBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2563eb; " +
                                "-fx-font-size: 11px; -fx-cursor: hand; -fx-underline: true;");
            markReadBtn.setOnAction(e -> {
                authorService.markNotificationAsRead(notification.getNotificationId());
                loadNotifications();
            });
            footer.getChildren().add(markReadBtn);
        }
        
        card.getChildren().addAll(header, messageLabel, footer);
        return card;
    }

    private void updateUnreadCount(int count) {
        if (count > 0) {
            unreadCountLabel.setText("🔔 " + count + " unread");
            unreadCountLabel.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; " +
                                      "-fx-padding: 4px 12px; -fx-background-radius: 20px; -fx-font-size: 12px;");
            unreadCountLabel.setVisible(true);
        } else {
            unreadCountLabel.setText("✓ All read");
            unreadCountLabel.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; " +
                                      "-fx-padding: 4px 12px; -fx-background-radius: 20px; -fx-font-size: 12px;");
            unreadCountLabel.setVisible(true);
        }
    }

    private void markAllAsRead() {
        authorService.markAllNotificationsAsRead(currentAuthor.getUsername());
        loadNotifications();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notifications");
        alert.setHeaderText(null);
        alert.setContentText("All notifications marked as read");
        alert.showAndWait();
    }
}
