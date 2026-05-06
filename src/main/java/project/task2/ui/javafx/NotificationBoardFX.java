package project.task2.ui.javafx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import project.task2.model.AuthorAccount;
import project.task2.model.Notification;
import project.task2.model.ArchivedNotification;
import project.task2.service.AuthorPortalService;
import project.task2.utils.ProfilePictureManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationBoardFX {
    private AuthorPortalService authorService;
    private AuthorAccount currentAuthor;
    private Stage stage;
    private VBox notificationsContainer;
    private VBox archivedContainer;
    private Label unreadCountLabel;
    private Label archivedCountLabel;
    
    private TextField searchField;
    private ComboBox<String> typeFilterCombo;
    private DatePicker dateFilterPicker;
    private CheckBox urgentOnlyCheckBox;
    private ComboBox<String> sortCombo;
    private ToggleGroup viewToggle;
    
    private List<Notification> allNotifications;
    private List<ArchivedNotification> allArchived;

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

        // View toggle
        HBox toggleBar = createToggleBar();
        VBox filterBox = createFilterBox();
        HBox controlBar = createControlBar();
        
        // Active notifications container
        notificationsContainer = new VBox(10);
        notificationsContainer.setPadding(new Insets(10));
        
        // Archived notifications container
        archivedContainer = new VBox(10);
        archivedContainer.setPadding(new Insets(10));
        archivedContainer.setVisible(false);
        archivedContainer.setManaged(false);
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(450);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        
        // Stack both containers
        StackPane contentStack = new StackPane();
        contentStack.getChildren().addAll(notificationsContainer, archivedContainer);
        scrollPane.setContent(contentStack);
        
        centerContent.getChildren().addAll(titleSection, toggleBar, filterBox, controlBar, scrollPane);
        root.setCenter(centerContent);

        Scene scene = new Scene(root, 750, 750);
        scene.getStylesheets().add(getClass().getResource("/project/task2/css/author-portal.css").toExternalForm());
        
        stage.setTitle("Notification Board - " + currentAuthor.getUsername());
        stage.setScene(scene);
        stage.show();
        
        loadData();
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
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-size: 18px; -fx-cursor: hand");
        closeBtn.setOnAction(e -> stage.close());

        HBox userInfo = new HBox(12);
        userInfo.setAlignment(Pos.CENTER);
        userInfo.getChildren().addAll(avatarContainer, usernameLabel);
        
        topBar.getChildren().addAll(userInfo, spacer, closeBtn);
        return topBar;
    }

    private HBox createToggleBar() {
        HBox toggleBar = new HBox(20);
        toggleBar.setAlignment(Pos.CENTER);
        toggleBar.setPadding(new Insets(10));
        
        viewToggle = new ToggleGroup();
        
        RadioButton activeBtn = new RadioButton("📬 Active Notifications");
        activeBtn.setToggleGroup(viewToggle);
        activeBtn.setSelected(true);
        activeBtn.setOnAction(e -> {
            notificationsContainer.setVisible(true);
            notificationsContainer.setManaged(true);
            archivedContainer.setVisible(false);
            archivedContainer.setManaged(false);
            loadData();
        });
        
        RadioButton archivedBtn = new RadioButton("📦 Archived Notifications");
        archivedBtn.setToggleGroup(viewToggle);
        archivedBtn.setOnAction(e -> {
            notificationsContainer.setVisible(false);
            notificationsContainer.setManaged(false);
            archivedContainer.setVisible(true);
            archivedContainer.setManaged(true);
            loadArchivedData();
        });
        
        toggleBar.getChildren().addAll(activeBtn, archivedBtn);
        return toggleBar;
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
        
        Label searchLabel = new Label("Search:");
        searchLabel.getStyleClass().add("muted");
        searchField = new TextField();
        searchField.setPromptText("Search by title or message...");
        searchField.setPrefWidth(200);
        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());
        
        Label typeLabel = new Label("Type:");
        typeLabel.getStyleClass().add("muted");
        typeFilterCombo = new ComboBox<>();
        typeFilterCombo.getItems().addAll("All", "Book Submitted", "Book Approved", "Book Rejected", "Book Deleted", "Urgent");
        typeFilterCombo.setValue("All");
        typeFilterCombo.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        
        Label dateLabel = new Label("Date:");
        dateLabel.getStyleClass().add("muted");
        dateFilterPicker = new DatePicker();
        dateFilterPicker.setPromptText("Filter by date");
        dateFilterPicker.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        
        urgentOnlyCheckBox = new CheckBox("⚠️ Show urgent only");
        urgentOnlyCheckBox.getStyleClass().add("muted");
        urgentOnlyCheckBox.selectedProperty().addListener((obs, old, newVal) -> applyFilters());
        
        Label sortLabel = new Label("Sort:");
        sortLabel.getStyleClass().add("muted");
        sortCombo = new ComboBox<>();
        sortCombo.getItems().addAll("Newest First", "Oldest First", "Priority First");
        sortCombo.setValue("Priority First");
        sortCombo.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        
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
                if (!searchText.isEmpty()) {
                    if (!n.getTitle().toLowerCase().contains(searchText) &&
                        !n.getMessage().toLowerCase().contains(searchText)) {
                        return false;
                    }
                }
                if (!typeFilter.equals("All")) {
                    String notificationType = getTypeDisplayName(n.getType());
                    if (!notificationType.equals(typeFilter)) {
                        return false;
                    }
                }
                if (dateFilter != null) {
                    if (!n.getCreatedAt().toLocalDate().equals(dateFilter)) {
                        return false;
                    }
                }
                if (urgentOnly && !n.isPriority()) {
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());
        
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
        
        archivedCountLabel = new Label();
        archivedCountLabel.setStyle("-fx-background-color: #64748b; -fx-text-fill: white; -fx-font-weight: bold; " +
                                    "-fx-padding: 4px 12px; -fx-background-radius: 20px; -fx-font-size: 12px;");
        
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().addAll("button", "secondary-btn");
        refreshBtn.setOnAction(e -> loadData());
        
        Button markAllReadBtn = new Button("✓ Mark All as Read");
        markAllReadBtn.getStyleClass().addAll("button", "primary-btn");
        markAllReadBtn.setOnAction(e -> markAllAsRead());
        
        Button archiveAllReadBtn = new Button("📦 Archive All Read");
        archiveAllReadBtn.getStyleClass().addAll("button", "primary-btn");
        archiveAllReadBtn.setOnAction(e -> archiveAllReadNotifications());
        
        Button deleteAllArchivedBtn = new Button("🗑️ Delete Archived");
        deleteAllArchivedBtn.getStyleClass().addAll("button", "danger-btn");
        deleteAllArchivedBtn.setOnAction(e -> deleteAllArchivedNotifications());
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        controlBar.getChildren().addAll(spacer, unreadCountLabel, refreshBtn, markAllReadBtn, archiveAllReadBtn);
        return controlBar;
    }

    private void loadData() {
        allNotifications = authorService.getNotifications(currentAuthor.getUsername());
        int unreadCount = authorService.getUnreadNotificationCount(currentAuthor.getUsername());
        
        updateUnreadCount(unreadCount);
        applyFilters();
    }
    
    private void loadArchivedData() {
        allArchived = authorService.getArchivedNotifications(currentAuthor.getUsername());
        archivedCountLabel.setText("📦 " + allArchived.size() + " archived");
        displayArchivedNotifications(allArchived);
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
            
            emptyBox.getChildren().addAll(emptyIcon, emptyLabel);
            notificationsContainer.getChildren().add(emptyBox);
        } else {
            for (Notification notification : notifications) {
                VBox card = createNotificationCard(notification);
                notificationsContainer.getChildren().add(card);
            }
        }
    }
    
    private void displayArchivedNotifications(List<ArchivedNotification> archived) {
        archivedContainer.getChildren().clear();
        
        if (archived.isEmpty()) {
            VBox emptyBox = new VBox(15);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(50));
            
            Label emptyIcon = new Label("📦");
            emptyIcon.setStyle("-fx-font-size: 48px;");
            Label emptyLabel = new Label("No archived notifications");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b;");
            
            emptyBox.getChildren().addAll(emptyIcon, emptyLabel);
            archivedContainer.getChildren().add(emptyBox);
        } else {
            for (ArchivedNotification archivedNotif : archived) {
                VBox card = createArchivedCard(archivedNotif);
                archivedContainer.getChildren().add(card);
            }
        }
    }

    private VBox createNotificationCard(Notification notification) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12px; " +
                     "-fx-border-color: #e2e8f0; -fx-border-radius: 12px; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.03), 4, 0, 0, 1);");
        
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
                loadData();
            });
            footer.getChildren().add(markReadBtn);
        }
        
        Button archiveBtn = new Button("Archive");
        archiveBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; " +
                           "-fx-font-size: 11px; -fx-cursor: hand; -fx-underline: true;");
        archiveBtn.setOnAction(e -> {
            authorService.archiveNotification(notification.getNotificationId());
            loadData();
            loadArchivedData();
        });
        footer.getChildren().add(archiveBtn);
        
        card.getChildren().addAll(header, messageLabel, footer);
        return card;
    }
    
    private VBox createArchivedCard(ArchivedNotification archived) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 12px; " +
                     "-fx-border-color: #e2e8f0; -fx-border-radius: 12px; " +
                     "-fx-opacity: 0.8;");
        
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label typeIcon = new Label(getArchivedTypeIcon(archived.getType()));
        typeIcon.setStyle("-fx-font-size: 20px;");
        
        Label archivedBadge = new Label("ARCHIVED");
        archivedBadge.setStyle("-fx-background-color: #64748b; -fx-text-fill: white; " +
                               "-fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2px 8px; " +
                               "-fx-background-radius: 12px;");
        
        Label titleLabel = new Label(archived.getTitle());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #0f172a;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label dateLabel = new Label(archived.getArchivedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        
        header.getChildren().addAll(typeIcon, archivedBadge, titleLabel, spacer, dateLabel);
        
        Label messageLabel = new Label(archived.getMessage());
        messageLabel.setWrapText(true);
        messageLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
        
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_RIGHT);
        
        Button unarchiveBtn = new Button("Unarchive");
        unarchiveBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #10b981; " +
                             "-fx-font-size: 11px; -fx-cursor: hand; -fx-underline: true;");
        unarchiveBtn.setOnAction(e -> {
            authorService.unarchiveNotification(archived.getOriginalId());
            loadArchivedData();
            loadData();
        });
        footer.getChildren().add(unarchiveBtn);
        
        Button deleteBtn = new Button("Delete");
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; " +
                          "-fx-font-size: 11px; -fx-cursor: hand; -fx-underline: true;");
        deleteBtn.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Archived Notification");
            confirm.setHeaderText("Delete this archived notification?");
            confirm.setContentText("This action cannot be undone.");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    authorService.deleteArchivedNotification(archived.getOriginalId());
                    loadArchivedData();
                }
            });
        });
        footer.getChildren().add(deleteBtn);
        
        card.getChildren().addAll(header, messageLabel, footer);
        return card;
    }
    
    private String getArchivedTypeIcon(String type) {
        return switch (type) {
            case "BOOK_APPROVED" -> "✅";
            case "BOOK_REJECTED" -> "❌";
            case "BOOK_SUBMITTED" -> "📝";
            case "BOOK_DELETED" -> "🗑️";
            default -> "📌";
        };
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
        loadData();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Notifications");
        alert.setHeaderText(null);
        alert.setContentText("All notifications marked as read");
        alert.showAndWait();
    }
    
    private void archiveAllReadNotifications() {
        authorService.archiveAllNotifications(currentAuthor.getUsername());
        loadData();
        loadArchivedData();
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Archive Notifications");
        alert.setHeaderText(null);
        alert.setContentText("All read notifications have been archived");
        alert.showAndWait();
    }
    
    private void deleteAllArchivedNotifications() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Archived Notifications");
        confirm.setHeaderText("Delete all archived notifications?");
        confirm.setContentText("This action cannot be undone.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                authorService.deleteAllArchivedNotifications(currentAuthor.getUsername());
                loadArchivedData();
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Archived Notifications");
                alert.setHeaderText(null);
                alert.setContentText("All archived notifications have been deleted");
                alert.showAndWait();
            }
        });
    }
}
