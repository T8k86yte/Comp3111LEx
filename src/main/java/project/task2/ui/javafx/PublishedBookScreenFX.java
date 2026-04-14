package project.task2.ui.javafx;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import project.task2.model.AuthorAccount;
import project.task2.model.BookSubmission;
import project.task2.service.AuthorPortalService;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Consumer;

public class PublishedBookScreenFX {
    private AuthorPortalService authorService;
    private AuthorAccount currentAuthor;
    private Stage stage;
    private TableView<BookSubmission> bookTable;
    private ObservableList<BookSubmission> masterData;
    private FilteredList<BookSubmission> filteredData;
    private SortedList<BookSubmission> sortedData;
    private Consumer<Void> onDataChanged;
    
    private TextField searchField;
    private ComboBox<String> statusFilterCombo;
    private ComboBox<String> sortCombo;

    public PublishedBookScreenFX(AuthorAccount author, Consumer<Void> onDataChanged) {
        this.currentAuthor = author;
        this.authorService = new AuthorPortalService();
        this.stage = new Stage();
        this.masterData = FXCollections.observableArrayList();
        this.onDataChanged = onDataChanged;
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
        
        Label titleLabel = new Label("📚 My Published Books");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        
        Label subtitleLabel = new Label("View, edit, or delete your book submissions");
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
        
        titleSection.getChildren().addAll(titleLabel, subtitleLabel);

        HBox filterBar = createFilterBar();
        
        bookTable = createBookTable();
        VBox.setVgrow(bookTable, Priority.ALWAYS);
        bookTable.setPrefHeight(400);
        bookTable.setMinHeight(200);

        HBox actionBox = createActionButtons();

        centerContent.getChildren().addAll(titleSection, filterBar, bookTable, actionBox);
        root.setCenter(centerContent);

        loadBooks();
        setupFilterAndSort();

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(getClass().getResource("/project/task2/css/author-portal.css").toExternalForm());
        
        stage.setTitle("My Published Books - " + currentAuthor.getFullName());
        stage.setScene(scene);
        stage.show();
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

    private HBox createFilterBar() {
        HBox filterBar = new HBox(15);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(10, 0, 15, 0));
        
        Label searchLabel = new Label("🔍 Search:");
        searchLabel.getStyleClass().add("muted");
        searchField = new TextField();
        searchField.setPromptText("Search by title...");
        searchField.setPrefWidth(250);
        searchField.getStyleClass().add("text-field");
        
        Label statusLabel = new Label("Status:");
        statusLabel.getStyleClass().add("muted");
        statusFilterCombo = new ComboBox<>();
        statusFilterCombo.getItems().addAll("All", "PENDING", "APPROVED", "REJECTED");
        statusFilterCombo.setValue("All");
        statusFilterCombo.setPrefWidth(120);
        statusFilterCombo.getStyleClass().add("combo-box");
        
        Label sortLabel = new Label("Sort by:");
        sortLabel.getStyleClass().add("muted");
        sortCombo = new ComboBox<>();
        sortCombo.getItems().addAll("Newest First", "Oldest First", "Title A-Z", "Title Z-A");
        sortCombo.setValue("Newest First");
        sortCombo.setPrefWidth(120);
        sortCombo.getStyleClass().add("combo-box");
        
        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().addAll("button", "secondary-btn");
        refreshBtn.setOnAction(e -> loadBooks());
        
        filterBar.getChildren().addAll(searchLabel, searchField, statusLabel, statusFilterCombo, 
                                        sortLabel, sortCombo, refreshBtn);
        
        return filterBar;
    }

    private void setupFilterAndSort() {
        filteredData = new FilteredList<>(masterData, book -> true);
        sortedData = new SortedList<>(filteredData);
        bookTable.setItems(sortedData);
        updateSort();
        
        searchField.textProperty().addListener((obs, old, newVal) -> updateFilter());
        statusFilterCombo.valueProperty().addListener((obs, old, newVal) -> updateFilter());
        sortCombo.valueProperty().addListener((obs, old, newVal) -> updateSort());
    }

    private void updateFilter() {
        if (filteredData == null) return;
        
        String searchText = searchField.getText().toLowerCase();
        String statusFilter = statusFilterCombo.getValue();
        
        filteredData.setPredicate(book -> {
            if (searchText != null && !searchText.isEmpty()) {
                if (!book.getTitle().toLowerCase().contains(searchText)) {
                    return false;
                }
            }
            if (statusFilter != null && !statusFilter.equals("All")) {
                if (!book.getStatus().equals(statusFilter)) {
                    return false;
                }
            }
            return true;
        });
    }

    private void updateSort() {
        if (sortedData == null) return;
        
        String sortOption = sortCombo.getValue();
        Comparator<BookSubmission> comparator = null;
        
        switch (sortOption) {
            case "Newest First":
                comparator = Comparator.comparing(BookSubmission::getSubmissionDate).reversed();
                break;
            case "Oldest First":
                comparator = Comparator.comparing(BookSubmission::getSubmissionDate);
                break;
            case "Title A-Z":
                comparator = Comparator.comparing(BookSubmission::getTitle, String.CASE_INSENSITIVE_ORDER);
                break;
            case "Title Z-A":
                comparator = Comparator.comparing(BookSubmission::getTitle, String.CASE_INSENSITIVE_ORDER).reversed();
                break;
            default:
                comparator = Comparator.comparing(BookSubmission::getSubmissionDate).reversed();
        }
        
        sortedData.setComparator(comparator);
    }

    private TableView<BookSubmission> createBookTable() {
        TableView<BookSubmission> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<BookSubmission, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("submissionId"));
        idCol.setPrefWidth(100);

        TableColumn<BookSubmission, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(250);

        TableColumn<BookSubmission, String> genreCol = new TableColumn<>("Genres");
        genreCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getGenresAsString()));
        genreCol.setPrefWidth(150);

        TableColumn<BookSubmission, String> dateCol = new TableColumn<>("Submitted");
        dateCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getSubmissionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        dateCol.setPrefWidth(150);

        TableColumn<BookSubmission, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(column -> new TableCell<BookSubmission, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label statusLabel = new Label(status);
                    statusLabel.getStyleClass().addAll("status", 
                        status.equals("APPROVED") ? "status-approved" :
                        status.equals("PENDING") ? "status-pending" : "status-rejected");
                    setGraphic(statusLabel);
                }
            }
        });
        statusCol.setPrefWidth(100);
        
        TableColumn<BookSubmission, Integer> borrowedCol = new TableColumn<>("Currently Borrowed");
        borrowedCol.setCellValueFactory(new PropertyValueFactory<>("currentlyBorrowedCount"));
        borrowedCol.setCellFactory(column -> new TableCell<BookSubmission, Integer>() {
            @Override
            protected void updateItem(Integer count, boolean empty) {
                super.updateItem(count, empty);
                if (empty || count == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(count));
                    if (count > 0) {
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                        setTooltip(new Tooltip("This book is currently borrowed and cannot be deleted"));
                    } else {
                        setStyle("-fx-text-fill: #22c55e;");
                    }
                }
            }
        });
        borrowedCol.setPrefWidth(130);
        
        TableColumn<BookSubmission, Integer> totalCol = new TableColumn<>("Total Borrows");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalBorrowedCount"));
        totalCol.setPrefWidth(100);

        table.getColumns().addAll(idCol, titleCol, genreCol, dateCol, statusCol, borrowedCol, totalCol);
        
        return table;
    }

    private HBox createActionButtons() {
        HBox actionBox = new HBox(15);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setPadding(new Insets(20, 0, 0, 0));

        Button editBtn = new Button("✏️ Edit Selected");
        editBtn.getStyleClass().addAll("button", "primary-btn");
        editBtn.setPrefWidth(150);
        editBtn.setOnAction(e -> editSelectedBook());

        Button deleteBtn = new Button("🗑️ Delete Selected");
        deleteBtn.getStyleClass().addAll("button", "danger-btn");
        deleteBtn.setPrefWidth(150);
        deleteBtn.setOnAction(e -> deleteSelectedBook());

        actionBox.getChildren().addAll(editBtn, deleteBtn);
        return actionBox;
    }

    private void loadBooks() {
        masterData.clear();
        var submissions = authorService.getAuthorSubmissions(currentAuthor.getUsername());
        
        System.out.println("📚 Loading " + submissions.size() + " total submissions for " + currentAuthor.getUsername());
        
        submissions.stream()
            .filter(sub -> !sub.isDraft())
            .forEach(sub -> {
                System.out.println("   • " + sub.getTitle() + " - Status: " + sub.getStatus() + 
                                   " | Currently borrowed: " + sub.getCurrentlyBorrowedCount());
                masterData.add(sub);
            });
        
        if (filteredData != null) {
            updateFilter();
        }
        
        System.out.println("📚 Displaying " + masterData.size() + " books in table");
        
        if (masterData.isEmpty()) {
            System.out.println("📭 No published books found");
        }
    }

    private void editSelectedBook() {
        BookSubmission selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a book to edit.", Alert.AlertType.WARNING);
            return;
        }

        if (!selected.getStatus().equals("PENDING")) {
            showAlert("Cannot Edit", 
                "This book is " + selected.getStatus() + ". Only pending books can be edited.", 
                Alert.AlertType.WARNING);
            return;
        }

        EditBookDialogFX editDialog = new EditBookDialogFX(selected);
        Optional<BookSubmission> result = editDialog.showAndWait();
        
        if (result.isPresent()) {
            BookSubmission updated = result.get();
            boolean success = authorService.updateSubmission(updated);
            if (success) {
                loadBooks();
                if (onDataChanged != null) {
                    onDataChanged.accept(null);
                }
                showAlert("Success", "Book updated successfully!", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Error", "Failed to update book.", Alert.AlertType.ERROR);
            }
        }
    }

    private void deleteSelectedBook() {
        BookSubmission selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No Selection", "Please select a book to delete.", Alert.AlertType.WARNING);
            return;
        }
        
        if (!selected.canBeDeleted()) {
            if (selected.isApproved() && selected.getCurrentlyBorrowedCount() > 0) {
                showAlert("Cannot Delete",
                        "❌ This book is currently borrowed by " + selected.getCurrentlyBorrowedCount()
                                + " reader(s) and has not been returned.\n\n"
                                + "Books that are currently borrowed cannot be deleted.\n"
                                + "Please wait until all copies are returned before deleting.\n\n"
                                + "Currently borrowed: " + selected.getCurrentlyBorrowedCount() + " copy/copies",
                        Alert.AlertType.WARNING);
            } else if (selected.isRejected()) {
                showAlert("Cannot Delete",
                        "Rejected books cannot be deleted. They are kept for record purposes.",
                        Alert.AlertType.WARNING);
            } else {
                showAlert("Cannot Delete",
                        "This book cannot be deleted because it is in " + selected.getStatus() + " status.",
                        Alert.AlertType.WARNING);
            }
            return;
        }
        confirmDelete(selected);
    }
    
    private void confirmDelete(BookSubmission selected) {
        String message = "";
        String additionalInfo = "";
        
        if (selected.isPending()) {
            message = "This book is currently pending review.";
            additionalInfo = "\n\n⚠️ This book has not been reviewed yet.\n" +
                            "It can be deleted safely as no readers have access to it.";
        } else if (selected.isApproved() && selected.getCurrentlyBorrowedCount() == 0) {
            message = "This book is APPROVED and currently not borrowed by anyone.";
            additionalInfo = "\n\n📌 Since no readers are currently borrowing this book, you can delete it.\n" +
                            "Historical borrow records (" + selected.getTotalBorrowedCount() + 
                            " total borrows) will be kept for reference.\n" +
                            "The book will be permanently removed from the library.";
        } else if (selected.isRejected()) {
            message = "This book is REJECTED.";
            additionalInfo = "\n\nIt will be removed from your submissions list.";
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Book: " + selected.getTitle());
        confirm.setContentText(
            message + "\n\n" +
            additionalInfo +
            "\n\nAre you sure you want to delete this book?\n" +
            "This action cannot be undone."
        );

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean deleted = authorService.deleteSubmission(selected.getSubmissionId());
            if (deleted) {
                loadBooks();
                if (onDataChanged != null) {
                    onDataChanged.accept(null);
                }
                showAlert("Success", "Book deleted successfully!", Alert.AlertType.INFORMATION);
            } else {
                showAlert("Error", "Failed to delete book.", Alert.AlertType.ERROR);
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
}
