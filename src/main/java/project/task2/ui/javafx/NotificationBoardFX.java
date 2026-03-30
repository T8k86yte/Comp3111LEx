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

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationBoardFX {
    private AuthorPortalService authorService;
    private AuthorAccount currentAuthor;
    private Stage stage;
    private VBox notificationsContainer;
    private Label unreadCountLabel;
    
    // Filter components
    private TextField searchField;
    private ComboBox<String> typeFilterCombo;
    private DatePicker dateFilterPicker;
    private CheckBox urgentOnlyCheckBox;
    private ComboBox<String> sortCombo;
    
    private List<Notification> allNotifications;

    public NotificationBoardFX(AuthorAccount author) {
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
        
        Label titleLabel = new Label("🔔 Notification Board");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        
        Label subtitleLabel = new Label("Stay updated on your book submissions and announcements");
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
        
        titleSection.getChildren().addAll(titleLabel, subtitleLabel);

        // Filter bar
        VBox filterBox = createFilterBox();
        
        // Control bar
        HBox controlBar = createControlBar();
        
        // Notifications container
        notificationsContainer = new VBox(10);
        notificationsContainer.setPadding(new Insets(10));
        
        ScrollPane scrollPane = new ScrollPane(notificationsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(450);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        
        centerContent.getChildren().addAll(titleSection, filterBox, controlBar, scrollPane);
        root.setCenter(centerContent);

        Scene scene = new Scene(root, 650, 700);
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

    private VBox createFilterBox() {
        VBox filterBox = new VBox(10);
        filterBox.setStyle("-fx-background-color: white; -fx-background-radius: 12px; " +
                          "-fx-border-color: #e2e8f0; -fx-border-radius: 12px;");
        filterBox.setPadding(new Insets(15));
        
        Label filterTitle = new Label("🔍 Filter Notifications");
        filterTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
        
        GridPane filterGrid = new GridPane();
        filterGrid.setHgap(15);
        filterGrid.setVgap(10);
        filterGrid.setPadding(new Insets(10, 0, 0, 0));
        
        // Search by title/message
        Label searchLabel = new Label("Search:");
        searchLabel.getStyleClass().add("muted");
        searchField = new TextField();
        searchField.setPromptText("Search by title or message...");
        searchField.setPrefWidth(200);
        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());
        
        // Filter by type
        Label typeLabel = new Label("Type:");
        typeLabel.getStyleClass().add("muted");
        typeFilterCombo = new ComboBox<>();
        typeFilterCombo.getItems().addAll("All", "Book Submitted", "Book Approved", "Book Rejected", "Book Deleted", "Urgent");
        typeFilterCombo.setValue("All");
        typeFilterCombo.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        
        // Filter by date
        Label dateLabel = new Label("Date:");
        dateLabel.getStyleClass().add("muted");
        dateFilterPicker = new DatePicker();
        dateFilterPicker.setPromptText("Filter by date");
        dateFilterPicker.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        
        // Urgent only
        urgentOnlyCheckBox = new CheckBox("⚠️ Show urgent only");
        urgentOnlyCheckBox.getStyleClass().add("muted");
        urgentOnlyCheckBox.selectedProperty().addListener((obs, old, newVal) -> applyFilters());
        
        // Sort options
        Label sortLabel = new Label("Sort:");
        sortLabel.getStyleClass().add("muted");
        sortCombo = new ComboBox<>();
        sortCombo.getItems().addAll("Newest First", "Oldest First", "Priority First");
        sortCombo.setValue("Priority First");
        sortCombo.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        
        // Clear filters button
        Button clearFiltersBtn = new Button("Clear Filters");
        clearFiltersBtn.getStyleClass().addAll("button", "secondary-btn");
        clearFiltersBtn.setPrefWidth(100);
        clearFiltersBtn.setOnAction(e -> clearFilters());
        
        filterGrid.add(searchLabel, 0, 0);
        filterGrid.add(searchField, 1, 0);
        filterGrid.add(typeLabel, 2, 0);
        filterGrid.add(typeFilterCombo, 3, 0);
        
        filterGrid.add(dateLabel, 0, 1);
        filterGrid.add(dateFilterPicker, 1, 1);
        filterGrid.add(urgentOnlyCheckBox, 2, 1);
        filterGrid.add(sortLabel, 3, 1);
        filterGrid.add(sortCombo, 4, 1);
        
        filterBox.getChildren().addAll(filterTitle, filterGrid);
        
        // Add clear filters button row
        HBox clearRow = new HBox();
        clearRow.setAlignment(Pos.CENTER_RIGHT);
        clearRow.getChildren().add(clearFiltersBtn);
        filterBox.getChildren().add(clearRow);
        
        return filterBox;
    }
    
    private void clearFilters() {
        searchField.clear();
        typeFilterCombo.setValue("All");
        dateFilterPicker.setValue(null);
        urgentOnlyCheckBox.setSelected(false);
        sortCombo.setValue("Priority First");
        applyFilters();
    }
    
    private void applyFilters() {
        if (allNotifications == null) return;
        
        String searchText = searchField.getText().toLowerCase();
        String typeFilter = typeFilterCombo.getValue();
        LocalDate dateFilter = dateFilterPicker.getValue();
        boolean urgentOnly = urgentOnlyCheckBox.isSelected();
        String sortOption = sortCombo.getValue();
        
        List<Notification> filtered = allNotifications.stream()
            .filter(n -> {
                // Search filter
                if (!searchText.isEmpty()) {
                    if (!n.getTitle().toLowerCase().contains(searchText) &&
                        !n.getMessage().toLowerCase().contains(searchText)) {
                        return false;
                    }
                }
                // Type filter
                if (!typeFilter.equals("All")) {
                    String notificationType = getTypeDisplayName(n.getType());
                    if (!notificationType.equals(typeFilter)) {
                        return false;
                    }
                }
                // Date filter
                if (dateFilter != null) {
                    if (!n.getCreatedAt().toLocalDate().equals(dateFilter)) {
                        return false;
                    }
                }
                // Urgent only filter
                if (urgentOnly && !n.isPriority()) {
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());
        
        // Sort
        switch (sortOption) {
            case "Newest First":
                filtered.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                break;
            case "Oldest First":
                filtered.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
                break;
            case "Priority First":
                filtered.sort((a, b) -> {
                    if (a.isPriority() && !b.isPriority()) return -1;
                    if (!a.isPriority() && b.isPriority()) return 1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                });
                break;
        }
        
        displayNotifications(filtered);
    }
    
    private String getTypeDisplayName(String type) {
        return switch (type) {
            case "BOOK_SUBMITTED" -> "Book Submitted";
            case "BOOK_APPROVED" -> "Book Approved";
            case "BOOK_REJECTED" -> "Book Rejected";
            case "BOOK_DELETED" -> "Book Deleted";
            case "URGENT_ANNOUNCEMENT" -> "Urgent";
            default -> "Other";
        };
    }

    private HBox createControlBar() {
        HBox controlBar = new HBox(15);
        controlBar.setAlignment(Pos.CENTER_RIGHT);
        controlBar.setPadding(new Insets(0, 0, 10, 0));
        
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
        
        Button deleteReadBtn = new Button("🗑️ Delete Read");
        deleteReadBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #ef4444; -fx-font-weight: 500; " +
                               "-fx-background-radius: 8px; -fx-padding: 6px 16px; -fx-cursor: hand;");
        deleteReadBtn.setOnAction(e -> deleteReadNotifications());
        
        Button deleteAllBtn = new Button("🗑️ Delete All");
        deleteAllBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #dc2626; -fx-font-weight: 500; " +
                              "-fx-background-radius: 8px; -fx-padding: 6px 16px; -fx-cursor: hand;");
        deleteAllBtn.setOnAction(e -> deleteAllNotifications());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        controlBar.getChildren().addAll(spacer, unreadCountLabel, refreshBtn, markAllReadBtn, deleteReadBtn, deleteAllBtn);
        return controlBar;
    }

    private void loadNotifications() {
        allNotifications = authorService.getNotifications(currentAuthor.getUsername());
        int unreadCount = authorService.getUnreadNotificationCount(currentAuthor.getUsername());
        
        updateUnreadCount(unreadCount);
        applyFilters();
    }
    
    private void displayNotifications(List<Notification> notifications) {
        notificationsContainer.getChildren().clear();
        
        if (notifications.isEmpty()) {
            VBox emptyBox = new VBox(15);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(50));
            
            Label emptyIcon = new Label("🔔");
            emptyIcon.setStyle("-fx-font-size: 48px;");
            Label emptyLabel = new Label("No notifications found");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b;");
            Label emptyHint = new Label("Try changing your filters or check back later");
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
        
        // Priority highlight
        if (notification.isPriority()) {
            card.setStyle(card.getStyle() + "-fx-border-color: #f97316; -fx-border-width: 2px; " +
                          "-fx-background-color: #fff7ed;");
        } else if (!notification.isRead()) {
            card.setStyle(card.getStyle() + "-fx-border-color: #3b82f6; -fx-border-width: 1px;");
        }
        
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label typeIcon = new Label(notification.getTypeIcon());
        typeIcon.setStyle("-fx-font-size: 20px;");
        
        // Priority badge
        if (notification.isPriority()) {
            Label priorityBadge = new Label("URGENT");
            priorityBadge.setStyle("-fx-background-color: #f97316; -fx-text-fill: white; " +
                                   "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2px 8px; " +
                                   "-fx-background-radius: 12px;");
            header.getChildren().add(priorityBadge);
        }
        
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
        
        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; " +
                          "-fx-font-size: 11px; -fx-cursor: hand; -fx-underline: true;");
        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Notification");
            confirm.setHeaderText("Delete this notification?");
            confirm.setContentText("This action cannot be undone.");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    authorService.deleteNotification(notification.getNotificationId());
                    loadNotifications();
                }
            });
        });
        footer.getChildren().add(deleteBtn);
        
        card.getChildren().addAll(header, messageLabel, footer);
        return card;
    }

    private void updateUnreadCount(int count) {
        if (count > 0) {
            unreadCountLabel.setText("🔔 " + count + " unread");
            unreadCountLabel.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; " +
                                      "-fx-padding: 4px 12px; -fx-background-radius: 20px; -fx-font-size: 12px;");
        } else {
            unreadCountLabel.setText("✓ All read");
            unreadCountLabel.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; " +
                                      "-fx-padding: 4px 12px; -fx-background-radius: 20px; -fx-font-size: 12px;");
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
    
    private void deleteReadNotifications() {
        int readCount = (int) allNotifications.stream()
            .filter(Notification::isRead)
            .count();
        
        if (readCount == 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Read Notifications");
            alert.setHeaderText(null);
            alert.setContentText("There are no read notifications to delete.");
            alert.showAndWait();
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Read Notifications");
        confirm.setHeaderText("Delete all read notifications?");
        confirm.setContentText("This will delete " + readCount + " read notification(s). This action cannot be undone.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                authorService.deleteReadNotifications(currentAuthor.getUsername());
                loadNotifications();
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Notifications Deleted");
                alert.setHeaderText(null);
                alert.setContentText("Deleted " + readCount + " read notification(s).");
                alert.showAndWait();
            }
        });
    }
    
    private void deleteAllNotifications() {
        int totalCount = allNotifications.size();
        
        if (totalCount == 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Notifications");
            alert.setHeaderText(null);
            alert.setContentText("There are no notifications to delete.");
            alert.showAndWait();
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete All Notifications");
        confirm.setHeaderText("Delete all notifications?");
        confirm.setContentText("This will delete " + totalCount + " notification(s). This action cannot be undone.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                authorService.deleteAllNotifications(currentAuthor.getUsername());
                loadNotifications();
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Notifications Deleted");
                alert.setHeaderText(null);
                alert.setContentText("Deleted all " + totalCount + " notification(s).");
                alert.showAndWait();
            }
        });
    }
}
