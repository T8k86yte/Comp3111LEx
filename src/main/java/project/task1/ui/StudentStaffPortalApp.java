package project.task1.ui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import project.task1.model.Book;
import project.task1.model.StudentStaffAccount;
import project.task1.repo.InMemoryBookRepository;
import project.task1.repo.StudentStaffRepository;
import project.task1.service.StudentStaffPortalService;

public class StudentStaffPortalApp extends Application {
    private final StudentStaffPortalService portalService = new StudentStaffPortalService(
            new StudentStaffRepository(),
            new InMemoryBookRepository()
    );

    private StudentStaffAccount currentUser;

    private Label statusLabel;
    private Label currentUserLabel;
    private TableView<Book> bookTable;
    private TextField borrowBookIdField;

    private TextField registerUsernameField;
    private TextField registerFullNameField;
    private PasswordField registerPasswordField;
    private ComboBox<String> registerRoleBox;

    private TextField loginUsernameField;
    private PasswordField loginPasswordField;

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(buildHeader());
        root.setCenter(buildBookCenterPanel());
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1060, 700);
        scene.getStylesheets().add(
                getClass().getResource("/project/task1/ui/light-theme.css").toExternalForm()
        );

        stage.setTitle("Task 1 - Student/Staff Portal");
        stage.setScene(scene);
        stage.show();

        refreshBooks();
        setStatus("Ready.");
    }

    private VBox buildHeader() {
        VBox wrapper = new VBox(14);
        wrapper.setPadding(new Insets(18, 18, 8, 18));

        Label title = new Label("Student/Staff Portal");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Task 1 light UI - Register, login, browse available books, and borrow.");
        subtitle.getStyleClass().add("page-subtitle");
        currentUserLabel = new Label("Current user: (none)");
        currentUserLabel.getStyleClass().add("current-user");

        HBox cards = new HBox(16, buildRegisterCard(), buildLoginCard(), buildBorrowCard());
        cards.setAlignment(Pos.TOP_LEFT);

        wrapper.getChildren().addAll(title, subtitle, currentUserLabel, cards);
        return wrapper;
    }

    private VBox buildRegisterCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(330);

        Label heading = new Label("Register");
        heading.getStyleClass().add("card-title");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);

        registerUsernameField = new TextField();
        registerFullNameField = new TextField();
        registerPasswordField = new PasswordField();
        registerRoleBox = new ComboBox<>(FXCollections.observableArrayList("Student", "Staff"));
        registerRoleBox.setValue("Student");
        registerRoleBox.setMaxWidth(Double.MAX_VALUE);

        grid.add(new Label("Username"), 0, 0);
        grid.add(registerUsernameField, 1, 0);
        grid.add(new Label("Full name"), 0, 1);
        grid.add(registerFullNameField, 1, 1);
        grid.add(new Label("Password"), 0, 2);
        grid.add(registerPasswordField, 1, 2);
        grid.add(new Label("Role"), 0, 3);
        grid.add(registerRoleBox, 1, 3);

        Button registerBtn = new Button("Create Account");
        registerBtn.getStyleClass().add("primary-btn");
        registerBtn.setOnAction(event -> handleRegister());

        card.getChildren().addAll(heading, grid, registerBtn);
        return card;
    }

    private VBox buildLoginCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(290);

        Label heading = new Label("Login");
        heading.getStyleClass().add("card-title");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);

        loginUsernameField = new TextField();
        loginPasswordField = new PasswordField();

        grid.add(new Label("Username"), 0, 0);
        grid.add(loginUsernameField, 1, 0);
        grid.add(new Label("Password"), 0, 1);
        grid.add(loginPasswordField, 1, 1);

        HBox actions = new HBox(8);
        Button loginBtn = new Button("Login");
        loginBtn.getStyleClass().add("primary-btn");
        loginBtn.setOnAction(event -> handleLogin());

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("secondary-btn");
        logoutBtn.setOnAction(event -> handleLogout());

        actions.getChildren().addAll(loginBtn, logoutBtn);
        card.getChildren().addAll(heading, grid, actions);
        return card;
    }

    private VBox buildBorrowCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(320);

        Label heading = new Label("Borrow");
        heading.getStyleClass().add("card-title");
        Label hint = new Label("Select a book in the table, or type ID below.");
        hint.getStyleClass().add("muted");

        borrowBookIdField = new TextField();
        borrowBookIdField.setPromptText("Book ID (e.g. B001)");

        Button borrowBtn = new Button("Borrow Book");
        borrowBtn.getStyleClass().add("primary-btn");
        borrowBtn.setOnAction(event -> handleBorrowBook());

        card.getChildren().addAll(heading, hint, borrowBookIdField, borrowBtn);
        return card;
    }

    private VBox buildBookCenterPanel() {
        VBox wrapper = new VBox(10);
        wrapper.setPadding(new Insets(8, 18, 18, 18));
        Label heading = new Label("Available Books");
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

        TableColumn<Book, String> summaryCol = new TableColumn<>("Summary");
        summaryCol.setCellValueFactory(new PropertyValueFactory<>("summary"));

        bookTable.getColumns().addAll(idCol, titleCol, authorCol, dateCol, summaryCol);
        bookTable.getSelectionModel().selectedItemProperty().addListener((obs, oldBook, newBook) -> {
            if (newBook != null) {
                borrowBookIdField.setText(newBook.getId());
            }
        });

        VBox.setVgrow(bookTable, Priority.ALWAYS);
        wrapper.getChildren().addAll(heading, bookTable);
        return wrapper;
    }

    private HBox buildStatusBar() {
        HBox statusBar = new HBox();
        statusBar.setPadding(new Insets(10, 18, 12, 18));
        statusLabel = new Label();
        statusLabel.getStyleClass().add("status");
        statusBar.getChildren().add(statusLabel);
        return statusBar;
    }

    private void handleRegister() {
        String username = registerUsernameField.getText();
        String fullName = registerFullNameField.getText();
        String password = registerPasswordField.getText();
        String role = registerRoleBox.getValue();

        StudentStaffPortalService.OperationResult result =
                portalService.registerStaffStudent(username, fullName, password, role);
        setStatus(result.message());

        if (result.success()) {
            registerUsernameField.clear();
            registerFullNameField.clear();
            registerPasswordField.clear();
        }
    }

    private void handleLogin() {
        StudentStaffPortalService.LoginResult result =
                portalService.login(loginUsernameField.getText(), loginPasswordField.getText());
        setStatus(result.message());
        if (result.success()) {
            currentUser = result.user();
            currentUserLabel.setText("Current user: " + currentUser.getUsername() + " (" + currentUser.getRole() + ")");
            loginPasswordField.clear();
        }
    }

    private void handleLogout() {
        if (currentUser == null) {
            setStatus("No user is currently logged in.");
            return;
        }
        String username = currentUser.getUsername();
        currentUser = null;
        currentUserLabel.setText("Current user: (none)");
        setStatus("Logged out: " + username);
    }

    private void handleBorrowBook() {
        if (currentUser == null) {
            setStatus("Borrow failed: please login first.");
            return;
        }

        String bookId = borrowBookIdField.getText();
        StudentStaffPortalService.OperationResult result =
                portalService.borrowBook(currentUser.getUsername(), bookId);
        setStatus(result.message());
        if (result.success()) {
            refreshBooks();
            borrowBookIdField.clear();
        }
    }

    private void refreshBooks() {
        bookTable.setItems(FXCollections.observableArrayList(portalService.getBookScreenData()));
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}
