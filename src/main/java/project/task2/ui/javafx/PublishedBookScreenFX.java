package project.task2.ui.javafx;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import java.util.Optional;

public class PublishedBookScreenFX {
    private AuthorPortalService authorService;
    private AuthorAccount currentAuthor;
    private Stage stage;
    private TableView<BookSubmission> bookTable;
    private ObservableList<BookSubmission> bookData;

    public PublishedBookScreenFX(AuthorAccount author) {
        this.currentAuthor = author;
        this.authorService = new AuthorPortalService();
        this.stage = new Stage();
        this.bookData = FXCollections.observableArrayList();
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        // Top bar
        HBox topBar = createTopBar();
        root.setTop(topBar);

        // Center - Table (with proper constraints)
        VBox centerContent = new VBox(20);
        centerContent.setAlignment(Pos.TOP_CENTER);
        centerContent.setPadding(new Insets(30));

        Label titleLabel = new Label("📚 My Published Books");
        titleLabel.getStyleClass().add("page-title");

        Label subtitleLabel = new Label("View, edit, or delete your book submissions");
        subtitleLabel.getStyleClass().add("page-subtitle");

        // Create table with proper sizing
        bookTable = createBookTable();
        
        // Make table fill available space
        VBox.setVgrow(bookTable, Priority.ALWAYS);
        bookTable.setPrefHeight(400);
        bookTable.setMinHeight(200);

        // Action buttons
        HBox actionBox = createActionButtons();

        centerContent.getChildren().addAll(titleLabel, subtitleLabel, bookTable, actionBox);
        root.setCenter(centerContent);

        // Load data
        loadBooks();

        Scene scene = new Scene(root, 1000, 600);
        scene.getStylesheets().add(getClass().getResource("/project/task2/css/author-portal.css").toExternalForm());
        
        stage.setTitle("My Published Books - " + currentAuthor.getFullName());
        stage.setScene(scene);
        
        // Handle window resize
        stage.widthProperty().addListener((obs, oldVal, newVal) -> adjustTableColumns());
        stage.heightProperty().addListener((obs, oldVal, newVal) -> adjustTableColumns());
        
        stage.show();
    }

    private void adjustTableColumns() {
        if (bookTable != null && bookTable.getScene() != null) {
            double width = bookTable.getScene().getWidth();
            if (width > 0) {
                bookTable.setPrefWidth(width - 40);
            }
        }
    }

    private HBox createTopBar() {
        HBox topBar = new HBox();
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setAlignment(Pos.CENTER_RIGHT);
        topBar.setStyle("-fx-background-color: white; -fx-border-color: #dbe6f2; -fx-border-width: 0 0 1 0;");

        Label welcomeLabel = new Label("Welcome, " + currentAuthor.getFullName());
        welcomeLabel.getStyleClass().add("current-user");

        Button refreshBtn = new Button("🔄 Refresh");
        refreshBtn.getStyleClass().addAll("button", "secondary-btn");
        refreshBtn.setOnAction(e -> loadBooks());

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().addAll("button", "secondary-btn");
        closeBtn.setOnAction(e -> stage.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        topBar.getChildren().addAll(welcomeLabel, spacer, refreshBtn, closeBtn);
        return topBar;
    }

    private TableView<BookSubmission> createBookTable() {
        TableView<BookSubmission> table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        // Enable column reordering
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Columns with percentage widths
        TableColumn<BookSubmission, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("submissionId"));
        idCol.prefWidthProperty().bind(table.widthProperty().multiply(0.12));

        TableColumn<BookSubmission, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.prefWidthProperty().bind(table.widthProperty().multiply(0.28));

        TableColumn<BookSubmission, String> genreCol = new TableColumn<>("Genres");
        genreCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(cellData.getValue().getGenresAsString()));
        genreCol.prefWidthProperty().bind(table.widthProperty().multiply(0.20));

        TableColumn<BookSubmission, String> dateCol = new TableColumn<>("Submitted");
        dateCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getSubmissionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
        dateCol.prefWidthProperty().bind(table.widthProperty().multiply(0.20));

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
        statusCol.prefWidthProperty().bind(table.widthProperty().multiply(0.20));

        table.getColumns().addAll(idCol, titleCol, genreCol, dateCol, statusCol);
        table.setItems(bookData);

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
        bookData.clear();
        var submissions = authorService.getAuthorSubmissions(currentAuthor.getUsername());
        
        submissions.stream()
            .filter(sub -> !sub.isDraft())
            .forEach(bookData::add);
        
        if (bookData.isEmpty()) {
            System.out.println("📭 No published books found");
        } else {
            System.out.println("📚 Loaded " + bookData.size() + " books");
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

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Book: " + selected.getTitle());
        confirm.setContentText("Are you sure you want to delete this book? This action cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean deleted = authorService.deleteSubmission(selected.getSubmissionId());
            if (deleted) {
                loadBooks();
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
