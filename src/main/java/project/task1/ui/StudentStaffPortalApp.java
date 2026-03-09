package project.task1.ui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import project.task1.model.Book;
import project.task1.model.UserAccount;
import project.task1.repo.InMemoryBookRepository;
import project.task1.repo.StudentStaffRepository;
import project.task1.service.StudentStaffPortalService;

import java.util.List;
import java.util.Optional;

public class StudentStaffPortalApp extends Application {
    private final StudentStaffPortalService portalService = new StudentStaffPortalService(
            new StudentStaffRepository(),
            new InMemoryBookRepository()
    );

    private UserAccount currentUser;

    private BorderPane root;
    private Label statusLabel;
    private Label currentUserLabel;
    private VBox authPage;
    private BorderPane studentDashboard;
    private StackPane contentPane;

    private TextField authUsernameField;
    private TextField authFullNameField;
    private PasswordField authPasswordField;
    private ComboBox<String> authRoleBox;
    private RadioButton loginModeButton;
    private RadioButton registerModeButton;
    private Label fullNameLabel;

    private TableView<Book> bookTable;
    private TextField borrowBookIdField;
    private TextField returnBookIdField;
    private VBox recommendationBox;

    @Override
    public void start(Stage stage) {
        root = new BorderPane();
        root.getStyleClass().add("root-pane");
        authPage = buildAuthPage();
        studentDashboard = buildStudentDashboard();
        root.setCenter(authPage);
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1060, 700);
        scene.getStylesheets().add(
                getClass().getResource("/project/task1/ui/light-theme.css").toExternalForm()
        );

        stage.setTitle("Task 1 - Student/Staff Portal");
        stage.setScene(scene);
        stage.show();

        setStatus("Please login or register.");
    }

    private VBox buildAuthPage() {
        VBox page = new VBox(14);
        page.setPadding(new Insets(26));
        page.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("Student/Staff Portal");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Login/Register to enter the student dashboard.");
        subtitle.getStyleClass().add("page-subtitle");
        page.getChildren().addAll(title, subtitle, buildUnifiedAuthCard());
        return page;
    }

    private VBox buildUnifiedAuthCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(430);

        Label heading = new Label("Account");
        heading.getStyleClass().add("card-title");
        Label hint = new Label("Choose mode and role first, then submit.");
        hint.getStyleClass().add("muted");

        ToggleGroup authModeGroup = new ToggleGroup();
        loginModeButton = new RadioButton("Login");
        registerModeButton = new RadioButton("Register");
        loginModeButton.setToggleGroup(authModeGroup);
        registerModeButton.setToggleGroup(authModeGroup);
        registerModeButton.setSelected(true);

        HBox modeBox = new HBox(10, loginModeButton, registerModeButton);
        modeBox.setAlignment(Pos.CENTER_LEFT);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);

        authUsernameField = new TextField();
        authFullNameField = new TextField();
        authPasswordField = new PasswordField();
        authRoleBox = new ComboBox<>(FXCollections.observableArrayList("Student", "Staff", "Author", "Librarian"));
        authRoleBox.setValue("Student");
        authRoleBox.setMaxWidth(Double.MAX_VALUE);

        grid.add(new Label("Username"), 0, 0);
        grid.add(authUsernameField, 1, 0);
        fullNameLabel = new Label("Full name");
        grid.add(fullNameLabel, 0, 1);
        grid.add(authFullNameField, 1, 1);
        grid.add(new Label("Password"), 0, 2);
        grid.add(authPasswordField, 1, 2);
        grid.add(new Label("Role"), 0, 3);
        grid.add(authRoleBox, 1, 3);

        Button submitBtn = new Button("Submit");
        submitBtn.getStyleClass().add("primary-btn");
        submitBtn.setOnAction(event -> handleAuthSubmit());

        Button readSummaryBtn = new Button("Read Summary");
        readSummaryBtn.getStyleClass().add("secondary-btn");
        readSummaryBtn.setOnAction(event -> handleReadSummary());

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("secondary-btn");
        logoutBtn.setOnAction(event -> handleLogout());

        HBox actions = new HBox(8, submitBtn, readSummaryBtn, logoutBtn);
        authModeGroup.selectedToggleProperty().addListener((obs, oldValue, newValue) -> updateAuthFormVisibility());
        updateAuthFormVisibility();

        card.getChildren().addAll(heading, hint, modeBox, grid, actions);
        return card;
    }

    private BorderPane buildStudentDashboard() {
        BorderPane dashboard = new BorderPane();
        dashboard.setPadding(new Insets(16));
        dashboard.setTop(buildDashboardHeader());

        VBox nav = new VBox(8);
        nav.setPrefWidth(170);
        Button booksBtn = new Button("Book List");
        Button borrowBtn = new Button("Borrow");
        Button returnBtn = new Button("Return");
        Button recBtn = new Button("Recommendations");
        booksBtn.getStyleClass().add("secondary-btn");
        borrowBtn.getStyleClass().add("secondary-btn");
        returnBtn.getStyleClass().add("secondary-btn");
        recBtn.getStyleClass().add("secondary-btn");
        booksBtn.setMaxWidth(Double.MAX_VALUE);
        borrowBtn.setMaxWidth(Double.MAX_VALUE);
        returnBtn.setMaxWidth(Double.MAX_VALUE);
        recBtn.setMaxWidth(Double.MAX_VALUE);
        nav.getChildren().addAll(booksBtn, borrowBtn, returnBtn, recBtn);

        contentPane = new StackPane();
        contentPane.setPadding(new Insets(0, 0, 0, 12));

        booksBtn.setOnAction(e -> showBooksView());
        borrowBtn.setOnAction(e -> showBorrowView());
        returnBtn.setOnAction(e -> showReturnView());
        recBtn.setOnAction(e -> showRecommendationView());

        HBox center = new HBox(nav, contentPane);
        HBox.setHgrow(contentPane, Priority.ALWAYS);
        dashboard.setCenter(center);
        return dashboard;
    }

    private HBox buildDashboardHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 14, 0));
        Label title = new Label("Student Dashboard");
        title.getStyleClass().add("section-title");
        currentUserLabel = new Label("Current user: (none)");
        currentUserLabel.getStyleClass().add("current-user");
        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("secondary-btn");
        logoutBtn.setOnAction(event -> handleLogout());
        header.getChildren().addAll(title, currentUserLabel, logoutBtn);
        return header;
    }

    private VBox buildBookListView() {
        VBox wrapper = new VBox(10);
        wrapper.getStyleClass().add("card");
        Label heading = new Label("Book List");
        heading.getStyleClass().add("section-title");

        bookTable = new TableView<>();
        bookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Book, String> idCol = new TableColumn<>("Book ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Book, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Book, String> authorCol = new TableColumn<>("Author");
        authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));

        TableColumn<Book, Object> dateCol = new TableColumn<>("Publish Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("publishDate"));

        TableColumn<Book, String> availabilityCol = new TableColumn<>("Availability");
        availabilityCol.setCellValueFactory(cell -> {
            String text = cell.getValue().isAvailable() ? "Available" : "Unavailable";
            return new javafx.beans.property.SimpleStringProperty(text);
        });

        TableColumn<Book, String> summaryCol = new TableColumn<>("Summary");
        summaryCol.setCellValueFactory(new PropertyValueFactory<>("summary"));

        titleCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                Book rowBook = getTableRow() == null ? null : (Book) getTableRow().getItem();
                setText(item);
                if (rowBook != null && !rowBook.isAvailable()) {
                    setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 600;");
                } else {
                    setStyle("-fx-text-fill: #111827;");
                }
            }
        });

        Button readSummaryBtn = new Button("Read Summary");
        readSummaryBtn.getStyleClass().add("secondary-btn");
        readSummaryBtn.setOnAction(event -> handleReadSummary());

        bookTable.getColumns().addAll(idCol, titleCol, authorCol, dateCol, availabilityCol, summaryCol);
        bookTable.getSelectionModel().selectedItemProperty().addListener((obs, oldBook, newBook) -> {
            if (newBook != null) {
                if (borrowBookIdField != null) {
                    borrowBookIdField.setText(newBook.getId());
                }
                if (returnBookIdField != null) {
                    returnBookIdField.setText(newBook.getId());
                }
            }
        });

        VBox.setVgrow(bookTable, Priority.ALWAYS);
        wrapper.getChildren().addAll(heading, bookTable, readSummaryBtn);
        return wrapper;
    }

    private VBox buildBorrowView() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        Label heading = new Label("Borrow Book");
        heading.getStyleClass().add("card-title");
        Label hint = new Label("Go to Book List first, select an available book, then confirm borrow.");
        hint.getStyleClass().add("muted");

        borrowBookIdField = new TextField();
        borrowBookIdField.setPromptText("Book ID (e.g. B001)");
        Button borrowBtn = new Button("Borrow");
        borrowBtn.getStyleClass().add("primary-btn");
        borrowBtn.setOnAction(event -> handleBorrowBook());
        card.getChildren().addAll(heading, hint, borrowBookIdField, borrowBtn);
        return card;
    }

    private VBox buildReturnView() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        Label heading = new Label("Return Book");
        heading.getStyleClass().add("card-title");
        Label hint = new Label("Return a book currently borrowed by you.");
        hint.getStyleClass().add("muted");

        returnBookIdField = new TextField();
        returnBookIdField.setPromptText("Book ID (e.g. B001)");
        Button returnBtn = new Button("Return");
        returnBtn.getStyleClass().add("primary-btn");
        returnBtn.setOnAction(event -> handleReturnBook());
        card.getChildren().addAll(heading, hint, returnBookIdField, returnBtn);
        return card;
    }

    private VBox buildRecommendationView() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        Label heading = new Label("Recommendations");
        heading.getStyleClass().add("card-title");
        Label hint = new Label("Popular titles based on borrow history.");
        hint.getStyleClass().add("muted");
        recommendationBox = new VBox(6);
        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("secondary-btn");
        refreshBtn.setOnAction(event -> refreshRecommendations());
        card.getChildren().addAll(heading, hint, recommendationBox, refreshBtn);
        return card;
    }

    private HBox buildStatusBar() {
        HBox statusBar = new HBox();
        statusBar.setPadding(new Insets(10, 18, 12, 18));
        statusLabel = new Label();
        statusLabel.getStyleClass().add("status");
        statusBar.getChildren().add(statusLabel);
        return statusBar;
    }

    private void showBooksView() {
        contentPane.getChildren().setAll(buildBookListView());
        refreshBooks();
    }

    private void showBorrowView() {
        contentPane.getChildren().setAll(buildBorrowView());
    }

    private void showReturnView() {
        contentPane.getChildren().setAll(buildReturnView());
    }

    private void showRecommendationView() {
        refreshRecommendations();
        contentPane.getChildren().setAll(buildRecommendationView());
        refreshRecommendations();
    }

    private void updateAuthFormVisibility() {
        boolean registerMode = registerModeButton.isSelected();
        authFullNameField.setManaged(registerMode);
        authFullNameField.setVisible(registerMode);
        fullNameLabel.setManaged(registerMode);
        fullNameLabel.setVisible(registerMode);
    }

    private void handleAuthSubmit() {
        if (registerModeButton.isSelected()) {
            StudentStaffPortalService.OperationResult result = portalService.registerWithRoleSelection(
                    authUsernameField.getText(),
                    authFullNameField.getText(),
                    authPasswordField.getText(),
                    authRoleBox.getValue()
            );
            setStatus(result.message());
            if (result.success()) {
                authFullNameField.clear();
                authPasswordField.clear();
            }
            return;
        }
        StudentStaffPortalService.LoginResult result =
                portalService.login(authUsernameField.getText(), authPasswordField.getText(), authRoleBox.getValue());
        setStatus(result.message());
        if (result.success()) {
            currentUser = result.user();
            if (!"STUDENT".equalsIgnoreCase(currentUser.getRole().name())) {
                currentUser = null;
                setStatus("This dashboard is for student users. Please login as Student.");
                return;
            }
            currentUserLabel.setText("Current user: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
            authPasswordField.clear();
            root.setCenter(studentDashboard);
            showBooksView();
        }
    }

    private void handleReadSummary() {
        if (bookTable == null) {
            setStatus("Open Book List first.");
            return;
        }
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            setStatus("Select a book first.");
            return;
        }
        String summary = selected.getSummary();
        if (summary.length() <= 180) {
            setStatus("Summary: " + summary);
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Quick Summary");
        alert.setHeaderText(selected.getTitle() + " - " + selected.getAuthor());
        TextArea area = new TextArea(summary);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefColumnCount(48);
        area.setPrefRowCount(12);
        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }

    private void handleLogout() {
        if (currentUser == null) {
            setStatus("No user is currently logged in.");
            return;
        }
        String username = currentUser.getUsername();
        currentUser = null;
        currentUserLabel.setText("Current user: (none)");
        root.setCenter(authPage);
        setStatus("Logged out: " + username);
    }

    private void handleBorrowBook() {
        if (currentUser == null) {
            setStatus("Borrow failed: please login first.");
            return;
        }

        String bookId = borrowBookIdField.getText() == null ? "" : borrowBookIdField.getText().trim();
        if (bookId.isEmpty()) {
            setStatus("Borrow failed: please provide a book ID.");
            return;
        }

        String confirmationDetails = portalService.buildBorrowConfirmation(currentUser.getUsername(), bookId);
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Borrow");
        confirmation.setHeaderText("Borrow request for " + bookId.toUpperCase());
        confirmation.setContentText(confirmationDetails);
        Optional<ButtonType> choice = confirmation.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) {
            setStatus("Borrow cancelled.");
            return;
        }

        StudentStaffPortalService.OperationResult result =
                portalService.borrowBook(currentUser.getUsername(), bookId);
        setStatus(result.message());
        if (result.success()) {
            refreshBooks();
            refreshRecommendations();
            borrowBookIdField.clear();
        }
    }

    private void handleReturnBook() {
        if (currentUser == null) {
            setStatus("Return failed: please login first.");
            return;
        }
        String bookId = returnBookIdField.getText() == null ? "" : returnBookIdField.getText().trim();
        if (bookId.isEmpty()) {
            setStatus("Return failed: please provide a book ID.");
            return;
        }

        StudentStaffPortalService.OperationResult result =
                portalService.returnBook(currentUser.getUsername(), bookId);
        setStatus(result.message());
        if (result.success()) {
            refreshBooks();
            refreshRecommendations();
            returnBookIdField.clear();
        }
    }

    private void refreshBooks() {
        if (bookTable == null) {
            if (contentPane != null) {
                contentPane.getChildren().setAll(buildBookListView());
            } else {
                return;
            }
        }
        bookTable.setItems(FXCollections.observableArrayList(portalService.getBookScreenData()));
    }

    private void refreshRecommendations() {
        if (recommendationBox == null) {
            return;
        }
        recommendationBox.getChildren().clear();
        List<Book> recommended = portalService.getRecommendedBooks(3);
        if (recommended.isEmpty()) {
            recommendationBox.getChildren().add(new Label("No recommendations yet."));
            return;
        }
        for (Book book : recommended) {
            Label item = new Label(
                    book.getTitle() + " (" + book.getAuthor() + ") - borrowed " + book.getBorrowCount() + " time(s)"
            );
            item.getStyleClass().add("muted");
            recommendationBox.getChildren().add(item);
        }
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}
