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
import project.task2.service.BorrowTrackingService;
import project.task2.utils.ProfilePictureManager;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Consumer;

public class PublishedBookScreenFX {
    private AuthorPortalService authorService;
    private BorrowTrackingService borrowTrackingService;
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
    
    private Button deleteBtn;
    private Button readBtn;
    private Button editBtn;
    private Label selectionLabel;

    public PublishedBookScreenFX(AuthorAccount author, Consumer<Void> onDataChanged) {
        this.currentAuthor = author;
        this.authorService = new AuthorPortalService();
        this.borrowTrackingService = new BorrowTrackingService();
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

        Scene scene = new Scene(root, 1200, 800);
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
        
        bookTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> {
            updateButtonStates();
        });
        
        bookTable.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener.Change<? extends BookSubmission> c) -> {
            updateButtonStates();
            updateSelectionLabel();
        });
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
    
    private void updateButtonStates() {
        int selectedCount = bookTable.getSelectionModel().getSelectedItems().size();
        boolean hasSelection = selectedCount > 0;
        boolean isSingleSelection = selectedCount == 1;
        
        if (editBtn != null) {
            editBtn.setDisable(!isSingleSelection);
        }
        if (readBtn != null) {
            readBtn.setDisable(!hasSelection);
        }
        if (deleteBtn != null) {
            deleteBtn.setDisable(!hasSelection);
        }
    }
    
    private void updateSelectionLabel() {
        int selectedCount = bookTable.getSelectionModel().getSelectedItems().size();
        if (selectionLabel != null) {
            if (selectedCount == 0) {
                selectionLabel.setText("");
            } else {
                selectionLabel.setText("✓ " + selectedCount + " book(s) selected");
                selectionLabel.setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold;");
            }
        }
    }

    private TableView<BookSubmission> createBookTable() {
        TableView<BookSubmission> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<BookSubmission, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("submissionId"));
        idCol.setPrefWidth(100);

        TableColumn<BookSubmission, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(250);
        titleCol.setComparator(String.CASE_INSENSITIVE_ORDER);

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
        
        selectionLabel = new Label();
        selectionLabel.setPrefWidth(150);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        readBtn = new Button("📖 Read Selected");
        readBtn.getStyleClass().addAll("button", "primary-btn");
        readBtn.setPrefWidth(150);
        readBtn.setOnAction(e -> readSelectedBooks());
        readBtn.setDisable(true);

        editBtn = new Button("✏️ Edit Selected");
        editBtn.getStyleClass().addAll("button", "primary-btn");
        editBtn.setPrefWidth(150);
        editBtn.setOnAction(e -> editSelectedBook());
        editBtn.setDisable(true);

        deleteBtn = new Button("🗑️ Delete Selected");
        deleteBtn.getStyleClass().addAll("button", "danger-btn");
        deleteBtn.setPrefWidth(150);
        deleteBtn.setOnAction(e -> deleteSelectedBooks());
        deleteBtn.setDisable(true);

        actionBox.getChildren().addAll(selectionLabel, spacer, readBtn, editBtn, deleteBtn);
        return actionBox;
    }

    private void loadBooks() {
        masterData.clear();
        var submissions = authorService.getAuthorSubmissions(currentAuthor.getUsername());
        
        submissions.stream()
            .filter(sub -> !sub.isDraft())
            .forEach(sub -> {
                masterData.add(sub);
            });
        
        if (filteredData != null) {
            updateFilter();
        }
        
        updateButtonStates();
        updateSelectionLabel();
    }

    private void readSelectedBooks() {
        ObservableList<BookSubmission> selected = bookTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showAlert("No Selection", "Please select at least one book to read.", Alert.AlertType.WARNING);
            return;
        }

        for (BookSubmission book : selected) {
            String filePath = book.getFilePath();
            if (filePath == null || filePath.isEmpty()) {
                showAlert("Cannot Read", "No file available for: " + book.getTitle(), Alert.AlertType.WARNING);
                continue;
            }
            
            File file = new File(filePath);
            if (!file.exists()) {
                showAlert("Cannot Read", "File not found for: " + book.getTitle(), Alert.AlertType.WARNING);
                continue;
            }
            
            try {
                Desktop.getDesktop().open(file);
            } catch (IOException e) {
                showAlert("Error", "Cannot open file for: " + book.getTitle(), Alert.AlertType.ERROR);
            }
        }
        
        showAlert("Reading", "Opened " + selected.size() + " book(s).", Alert.AlertType.INFORMATION);
    }

    private void editSelectedBook() {
        ObservableList<BookSubmission> selected = bookTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showAlert("No Selection", "Please select a book to edit.", Alert.AlertType.WARNING);
            return;
        }
        
        if (selected.size() > 1) {
            showAlert("Cannot Edit", "Please select only one book to edit.", Alert.AlertType.WARNING);
            return;
        }
        
        BookSubmission book = selected.get(0);

        boolean canEdit = false;
        if (book.getStatus().equals("PENDING")) {
            canEdit = true;
        } else if (book.getStatus().equals("APPROVED") && book.getCurrentlyBorrowedCount() == 0) {
            canEdit = true;
        }

        if (!canEdit) {
            String reason = book.getStatus().equals("APPROVED") 
                ? "This book is APPROVED and has been borrowed. Cannot edit."
                : "This book is " + book.getStatus() + ". Only pending or unborrowed approved books can be edited.";
            showAlert("Cannot Edit", reason, Alert.AlertType.WARNING);
            return;
        }

        EditBookDialogFX editDialog = new EditBookDialogFX(book);
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

    private void deleteSelectedBooks() {
        ObservableList<BookSubmission> selected = bookTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) {
            showAlert("No Selection", "Please select at least one book to delete.", Alert.AlertType.WARNING);
            return;
        }

        StringBuilder cannotDeleteList = new StringBuilder();
        StringBuilder canDeleteList = new StringBuilder();
        int canDeleteCount = 0;
        
        for (BookSubmission book : selected) {
            boolean canDelete = borrowTrackingService.canDeleteBook(book.getSubmissionId());
            if (canDelete) {
                canDeleteCount++;
                canDeleteList.append("• ").append(book.getTitle()).append(" (").append(book.getStatus()).append(")\n");
            } else {
                cannotDeleteList.append("• ").append(book.getTitle()).append(" - Currently borrowed by ")
                    .append(book.getCurrentlyBorrowedCount()).append(" reader(s)\n");
            }
        }

        String message = "";
        if (canDeleteCount > 0) {
            message = "Books that can be deleted (" + canDeleteCount + "):\n" + canDeleteList.toString() + "\n";
        }
        if (cannotDeleteList.length() > 0) {
            message += "\n❌ Cannot delete:\n" + cannotDeleteList.toString();
        }

        if (canDeleteCount == 0) {
            showAlert("Cannot Delete", message, Alert.AlertType.WARNING);
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete " + canDeleteCount + " selected book(s)?");
        confirm.setContentText(message + "\n\n⚠️ This action cannot be undone!");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            int deletedCount = 0;
            for (BookSubmission book : selected) {
                if (borrowTrackingService.canDeleteBook(book.getSubmissionId())) {
                    boolean deleted = authorService.deleteSubmission(book.getSubmissionId());
                    if (deleted) deletedCount++;
                }
            }
            
            loadBooks();
            if (onDataChanged != null) {
                onDataChanged.accept(null);
            }
            showAlert("Deletion Complete", "Successfully deleted " + deletedCount + " book(s).", Alert.AlertType.INFORMATION);
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
