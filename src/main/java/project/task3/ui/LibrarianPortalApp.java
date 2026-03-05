package project.task3.ui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import project.task1.model.Book;
import project.task2.model.BookSubmission;
import project.task3.model.LibrarianAccount;
import project.task1.repo.InMemoryBookRepository;
import project.task3.repo.BookSubmissionRepository;
import project.task3.repo.LibrarianRepository;
import project.task3.service.LibrarianPortalService;

public class LibrarianPortalApp extends Application {
    private final LibrarianPortalService portalService;

    private LibrarianAccount currentUser;

    private Label statusLabel;
    private Label currentUserLabel;
    private TableView<BookSubmission> bookSubmissionTable;
    private TextField approveSubmissionIdField;

    private TextField registerUsernameField;
    private TextField registerFullNameField;
    private PasswordField registerPasswordField;
    private TextField registerStaffIDField;

    private TextField loginUsernameField;
    private PasswordField loginPasswordField;

    public LibrarianPortalApp() {
        portalService = new LibrarianPortalService(
                new LibrarianRepository(),
                new InMemoryBookRepository(),
                new BookSubmissionRepository()
        );
    }

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

        stage.setTitle("Task 3 - Librarian Portal");
        stage.setScene(scene);
        stage.show();

        refreshSubmissions();
        setStatus("Ready.");
    }

    private VBox buildHeader() {
        VBox wrapper = new VBox(14);
        wrapper.setPadding(new Insets(18, 18, 8, 18));

        Label title = new Label("Librarian Portal");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Task 3 light UI - Register, login, browse pending book submissions, and respond to them.");
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
        registerStaffIDField = new TextField();

        grid.add(new Label("Username"), 0, 0);
        grid.add(registerUsernameField, 1, 0);
        grid.add(new Label("Full name"), 0, 1);
        grid.add(registerFullNameField, 1, 1);
        grid.add(new Label("Password"), 0, 2);
        grid.add(registerPasswordField, 1, 2);
        grid.add(new Label("Role"), 0, 3);
        grid.add(registerStaffIDField, 1, 3);

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
        Label hint = new Label("Select a book submission in the table, or type ID below.");
        hint.getStyleClass().add("muted");

        approveSubmissionIdField = new TextField();
        approveSubmissionIdField.setPromptText("Submission ID");

        Button acceptBtn = new Button("Approve Submission");
        Button rejectBtn = new Button("Reject Submission");
        acceptBtn.getStyleClass().add("primary-btn");
        acceptBtn.setOnAction(event -> handleApprove());
        rejectBtn.getStyleClass().add("primary-btn");
        rejectBtn.setOnAction(event -> handleReject());

        card.getChildren().addAll(heading, hint, approveSubmissionIdField, acceptBtn, rejectBtn);
        return card;
    }

    private VBox buildBookCenterPanel() {
        VBox wrapper = new VBox(10);
        wrapper.setPadding(new Insets(8, 18, 18, 18));
        Label heading = new Label("Pending New Book Submissions");
        heading.getStyleClass().add("section-title");

        bookSubmissionTable = new TableView<>();
        bookSubmissionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<BookSubmission, String> idCol = new TableColumn<>("Book Submission ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("submissionId"));

        TableColumn<BookSubmission, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<BookSubmission, String> authorUsernameCol = new TableColumn<>("Author Username");
        authorUsernameCol.setCellValueFactory(new PropertyValueFactory<>("authorUsername"));

        TableColumn<BookSubmission, String> authorFullNameCol = new TableColumn<>("Author Full Name");
        authorFullNameCol.setCellValueFactory(new PropertyValueFactory<>("authorFullName"));

        TableColumn<BookSubmission, String> genreCol = new TableColumn<>("Genre");
        genreCol.setCellValueFactory(new PropertyValueFactory<>("genre"));

        TableColumn<BookSubmission, Object> dateCol = new TableColumn<>("Submitted Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("submitDate"));

        TableColumn<BookSubmission, String> summaryCol = new TableColumn<>("Summary");
        summaryCol.setCellValueFactory(new PropertyValueFactory<>("summary"));

        TableColumn<BookSubmission, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        if (bookSubmissionTable.getColumns().addAll(idCol, titleCol, authorUsernameCol, authorFullNameCol, genreCol, dateCol, summaryCol, statusCol)) {
            return null;
        }
        bookSubmissionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSubmission, newSubmission) -> {
            if (newSubmission != null) {
                approveSubmissionIdField.setText(newSubmission.getSubmissionId());
            }
        });

        VBox.setVgrow(bookSubmissionTable, Priority.ALWAYS);
        wrapper.getChildren().addAll(heading, bookSubmissionTable);
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
        String eId = registerStaffIDField.getText();

        LibrarianPortalService.OperationResult result =
                portalService.registerLibrarian(username, fullName, password, eId);
        setStatus(result.message());

        if (result.success()) {
            registerUsernameField.clear();
            registerFullNameField.clear();
            registerPasswordField.clear();
            registerStaffIDField.clear();
        }
    }

    private void handleLogin() {
        LibrarianPortalService.LoginResult result =
                portalService.login(loginUsernameField.getText(), loginPasswordField.getText());
        setStatus(result.message());
        if (result.success()) {
            currentUser = result.user();
            currentUserLabel.setText("Current user: " + currentUser.getUsername());
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

    private void handleApprove() {
        if (currentUser == null) {
            setStatus("Action failed: please login first.");
            return;
        }

        String bookId = approveSubmissionIdField.getText();
        LibrarianPortalService.OperationResult result =
                portalService.approveBookSubmission(bookId, currentUser);
        setStatus(result.message());
        if (result.success()) {
            refreshSubmissions();
            approveSubmissionIdField.clear();
        }
    }

    private void handleReject() {
        if (currentUser == null) {
            setStatus("Action failed: please login first.");
            return;
        }

        String bookId = approveSubmissionIdField.getText();
        LibrarianPortalService.OperationResult result =
                portalService.rejectBookSubmission(bookId, currentUser, "");
        setStatus(result.message());
        if (result.success()) {
            refreshSubmissions();
            approveSubmissionIdField.clear();
        }
    }

    private void refreshSubmissions() {
        bookSubmissionTable.setItems(FXCollections.observableArrayList(portalService.getBookSubmissionScreenData()));
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}