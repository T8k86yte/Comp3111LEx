package project.task3.ui;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import project.task2.model.BookSubmission;
import project.task2.repo.SubmissionRepository;
import project.task3.model.LibrarianAccount;
import project.task1.repo.InMemoryBookRepository;
import project.task3.repo.LibrarianRepository;
import project.task3.service.LibrarianPortalService;

import java.time.LocalDate;
import java.util.List;

public class LibrarianPortalApp extends Application {
    private final LibrarianPortalService portalService;

    private LibrarianAccount currentUser;

    private Label statusLabel;
    private Label currentUserLabel;
    private TableView<BookSubmission> bookSubmissionTable;

    private TextField tableTitleFilter;
    private TextField tableAuthorUsernameFilter;
    private TextField tableGenreFilter;
    private DatePicker tableSubmissionMin;
    private DatePicker tableSubmissionMax;
    private ComboBox<String> tableStatusFilter;

    private TextField approveSubmissionIdField;
    private ComboBox<String> actionBox;
    private TextField rejectReasonField;

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
                new SubmissionRepository()
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

        HBox cards = new HBox(16, buildRegisterCard(), buildLoginCard(), buildApproveRejectCard());
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
        grid.add(new Label("Staff ID"), 0, 3);
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

    private VBox buildApproveRejectCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(320);

        Label heading = new Label("Approve or Reject");
        heading.getStyleClass().add("card-title");
        Label hint = new Label("Select a book submission in the table,\n or type ID below.");
        hint.getStyleClass().add("muted");

        approveSubmissionIdField = new TextField();
        approveSubmissionIdField.setPromptText("Submission ID");
        rejectReasonField = new TextField();
        rejectReasonField.setPromptText("Rejection Reason (can be empty)");
        rejectReasonField.setDisable(true);
        Button acceptRejectBtn = new Button("Approve Submission");
        acceptRejectBtn.getStyleClass().add("primary-btn");
        acceptRejectBtn.setOnAction(event -> handleApproveReject());
        actionBox = new ComboBox<>(FXCollections.observableArrayList("Approve", "Reject"));
        actionBox.setValue("Approve");
        actionBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                rejectReasonField.setDisable(newValue.equals("Approve"));
                acceptRejectBtn.setText(newValue + " Submission");
            }
        });

        card.getChildren().addAll(heading, hint, approveSubmissionIdField, actionBox, rejectReasonField, acceptRejectBtn);
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
        dateCol.setCellValueFactory(new PropertyValueFactory<>("submissionDate"));

        TableColumn<BookSubmission, String> summaryCol = new TableColumn<>("Summary");
        summaryCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<BookSubmission, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        bookSubmissionTable.getColumns().addAll(idCol, titleCol, authorUsernameCol, authorFullNameCol, genreCol, dateCol, summaryCol, statusCol);

        bookSubmissionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSubmission, newSubmission) -> {
            if (newSubmission != null) {
                approveSubmissionIdField.setText(newSubmission.getSubmissionId());
            }
        });

        tableTitleFilter = new TextField();
        tableAuthorUsernameFilter = new TextField();
        tableGenreFilter = new TextField();
        tableSubmissionMin = new DatePicker();
        tableSubmissionMax = new DatePicker();
        tableStatusFilter = new ComboBox<>(FXCollections.observableArrayList("PENDING", "APPROVED", "REJECTED"));
        tableStatusFilter.setValue("PENDING");

        Button refreshBtn = new Button("Refresh Table");
        refreshBtn.getStyleClass().add("primary-btn");
        refreshBtn.setOnAction(event -> refreshSubmissions());

        HBox filters1 = new HBox(5);
        HBox filters2 = new HBox(5);
        filters1.getChildren().add(new Label("Title: "));
        filters1.getChildren().add(tableTitleFilter);
        filters1.getChildren().add(new Label("Author Username: "));
        filters1.getChildren().add(tableAuthorUsernameFilter);
        filters1.getChildren().add(new Label("Genre: "));
        filters1.getChildren().add(tableGenreFilter);
        filters2.getChildren().add(new Label("Submission min: "));
        filters2.getChildren().add(tableSubmissionMin);
        filters2.getChildren().add(new Label("Submission max: "));
        filters2.getChildren().add(tableSubmissionMax);
        filters2.getChildren().add(new Label("Status: "));
        filters2.getChildren().add(tableStatusFilter);
        filters2.getChildren().add(refreshBtn);

        VBox.setVgrow(bookSubmissionTable, Priority.ALWAYS);
        wrapper.getChildren().addAll(new Label("Filters: "), filters1, filters2, heading, bookSubmissionTable);
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

    private void handleApproveReject() {
        if (actionBox.getValue().equals("Approve")) handleApprove();
        else handleReject();
    }

    private void handleApprove() {
        if (currentUser == null) {
            setStatus("Action failed: please login first.");
            return;
        }

        LibrarianPortalService.OperationResult result =
                portalService.approveBookSubmission(approveSubmissionIdField.getText(), currentUser);
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

        LibrarianPortalService.OperationResult result =
                portalService.rejectBookSubmission(approveSubmissionIdField.getText(), currentUser, rejectReasonField.getText());
        setStatus(result.message());
        if (result.success()) {
            refreshSubmissions();
            approveSubmissionIdField.clear();
            rejectReasonField.clear();
        }
    }

    private void refreshSubmissions() {
        LocalDate mind = tableSubmissionMin.getValue();
        LocalDate maxd = tableSubmissionMax.getValue();
        List<BookSubmission> l = portalService.getBookSubmissionScreenData(tableTitleFilter.getText(), tableAuthorUsernameFilter.getText(), tableGenreFilter.getText(), mind != null ? mind.atStartOfDay() : null, maxd != null ? tableSubmissionMax.getValue().atTime(23, 59) : null, tableStatusFilter.getValue());
        bookSubmissionTable.setItems(FXCollections.observableArrayList(l));
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}