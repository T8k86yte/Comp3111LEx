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
import project.shared.SharedAuthFacade;
import project.task1.repo.StudentStaffRepository;
import project.task2.model.BookSubmission;
import project.task2.repo.AuthorRepository;
import project.task2.repo.SubmissionRepository;
import project.task3.model.LibrarianAccount;
import project.task1.repo.InMemoryBookRepository;
import project.task3.repo.LibrarianRepository;
import project.task3.service.LibrarianPortalService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class LibrarianPortalApp extends Application {
    private final LibrarianPortalService portalService;
    private final LibrarianRepository librarianRepository;
    private final SharedAuthFacade authFacade;

    private LibrarianAccount currentUser;

    private Stage stage;
    private Scene loginRegisterScene;
    private Scene registerScene;
    private Scene acceptRejectScene;
    private HBox statusBar;

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
    private PasswordField registerConfirmPasswordField;
    private TextField registerStaffIDField;

    private TextField loginUsernameField;
    private PasswordField loginPasswordField;

    public LibrarianPortalApp() {
        librarianRepository = new LibrarianRepository();
        portalService = new LibrarianPortalService(
                librarianRepository,
                new InMemoryBookRepository(),
                new SubmissionRepository()
        );
        authFacade = new SharedAuthFacade(
                new StudentStaffRepository(),
                new AuthorRepository(),
                librarianRepository
        );
    }

    @Override
    public void start(Stage stage) {
        statusBar = buildStatusBar();
        loginRegisterScene = buildLoginRegisterScene();
        registerScene = buildRegisterScene();
        acceptRejectScene = buildAcceptRejectScene();

        stage.setTitle("Task 3 - Librarian Portal");
        stage.setScene(loginRegisterScene);
        stage.show();
        this.stage = stage;

        setStatus("Ready.");
    }



    private Scene buildLoginRegisterScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setCenter(buildLoginHeader());
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(
                getClass().getResource("/project/task1/ui/light-theme.css").toExternalForm()
        );

        return scene;
    }

    private Scene buildRegisterScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setCenter(buildRegisterHeader());
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1060, 700);
        scene.getStylesheets().add(
                getClass().getResource("/project/task1/ui/light-theme.css").toExternalForm()
        );
        return scene;
    }

    private VBox buildLoginHeader() {
        VBox wrapper = new VBox(20);
        wrapper.setPadding(new Insets(50));
        wrapper.setAlignment(Pos.CENTER);

        Label title = new Label("🛡 Librarian Portal");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Sign in with your librarian account.");
        subtitle.getStyleClass().add("page-subtitle");

        wrapper.getChildren().addAll(title, subtitle, buildLoginCard());
        return wrapper;
    }

    private VBox buildRegisterHeader() {
        VBox wrapper = new VBox(20);
        wrapper.setPadding(new Insets(30));
        wrapper.setAlignment(Pos.TOP_CENTER);
        Label title = new Label("📝 Librarian Registration");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Create a librarian account.");
        subtitle.getStyleClass().add("page-subtitle");
        wrapper.getChildren().addAll(title, subtitle, buildRegisterCard());
        return wrapper;
    }


    private Scene buildAcceptRejectScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(buildAcceptRejectHeader());
        root.setCenter(buildBookCenterPanel());
        root.setBottom(statusBar);

        Scene scene = new Scene(root, 1060, 700);
        scene.getStylesheets().add(
                getClass().getResource("/project/task1/ui/light-theme.css").toExternalForm()
        );

        refreshSubmissions();

        return scene;
    }

    private VBox buildAcceptRejectHeader() {
        VBox wrapper = new VBox(14);
        wrapper.setPadding(new Insets(18, 18, 8, 18));

        Label title = new Label("Librarian Portal");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Task 3 Register, login");
        subtitle.getStyleClass().add("page-subtitle");

        currentUserLabel = new Label("Current user: (none)");
        currentUserLabel.getStyleClass().add("current-user");

        wrapper.getChildren().addAll(title, subtitle, currentUserLabel, buildApproveRejectCard());
        return wrapper;
    }

    private VBox buildRegisterCard() {
        VBox card = new VBox(20);
        card.getStyleClass().add("card");
        card.setMaxWidth(500);
        card.setPadding(new Insets(30));

        Label heading = new Label("Create Account");
        heading.getStyleClass().add("card-title");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);

        registerUsernameField = new TextField();
        registerUsernameField.setPromptText("Choose username");
        registerFullNameField = new TextField();
        registerFullNameField.setPromptText("Enter full name");
        registerPasswordField = new PasswordField();
        registerPasswordField.setPromptText("Create password");
        registerConfirmPasswordField = new PasswordField();
        registerConfirmPasswordField.setPromptText("Re-enter password");
        registerStaffIDField = new TextField();
        registerStaffIDField.setPromptText("Employee ID (optional)");

        grid.add(new Label("Username"), 0, 0);
        grid.add(registerUsernameField, 1, 0);
        grid.add(new Label("Full name"), 0, 1);
        grid.add(registerFullNameField, 1, 1);
        grid.add(new Label("Password"), 0, 2);
        grid.add(registerPasswordField, 1, 2);
        grid.add(new Label("Confirm password"), 0, 3);
        grid.add(registerConfirmPasswordField, 1, 3);
        grid.add(new Label("Staff ID"), 0, 4);
        grid.add(registerStaffIDField, 1, 4);

        VBox reqBox = new VBox(4);
        reqBox.setPadding(new Insets(10));
        reqBox.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8px;");
        Label reqTitle = new Label("Password Requirements:");
        reqTitle.setStyle("-fx-font-weight: bold;");
        Label req1 = new Label("• At least 8 characters long");
        Label req2 = new Label("• At least one letter");
        Label req3 = new Label("• At least one number");
        Label req4 = new Label("• At least one uppercase letter");
        req1.getStyleClass().add("muted");
        req2.getStyleClass().add("muted");
        req3.getStyleClass().add("muted");
        req4.getStyleClass().add("muted");
        reqBox.getChildren().addAll(reqTitle, req1, req2, req3, req4);

        Button registerBtn = new Button("Create Account");
        registerBtn.getStyleClass().add("primary-btn");
        registerBtn.setOnAction(event -> handleRegister());
        registerBtn.setPrefWidth(180);

        Button backBtn = new Button("Back to Login");
        backBtn.getStyleClass().add("secondary-btn");
        backBtn.setOnAction(event -> stage.setScene(loginRegisterScene));
        backBtn.setPrefWidth(180);

        HBox actions = new HBox(10, registerBtn, backBtn);
        actions.setAlignment(Pos.CENTER);

        card.getChildren().addAll(heading, grid, reqBox, actions);
        return card;
    }

    private VBox buildLoginCard() {
        VBox card = new VBox(20);
        card.getStyleClass().add("card");
        card.setMaxWidth(400);
        card.setPadding(new Insets(30));

        Label heading = new Label("Sign In");
        heading.getStyleClass().add("card-title");
        heading.setAlignment(Pos.CENTER);
        heading.setMaxWidth(Double.MAX_VALUE);

        VBox usernameBox = new VBox(5);
        Label usernameLabel = new Label("Username");
        usernameLabel.getStyleClass().add("muted");

        loginUsernameField = new TextField();
        loginUsernameField.setPromptText("Enter username");
        loginUsernameField.getStyleClass().add("text-field");

        usernameBox.getChildren().addAll(usernameLabel, loginUsernameField);

        VBox passwordBox = new VBox(5);
        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("muted");
        loginPasswordField = new PasswordField();
        loginPasswordField.setPromptText("Enter password");
        loginPasswordField.getStyleClass().add("password-field");

        passwordBox.getChildren().addAll(passwordLabel, loginPasswordField);

        HBox actions = new HBox(10);
        Button loginBtn = new Button("Sign In");
        loginBtn.getStyleClass().add("primary-btn");
        loginBtn.setOnAction(event -> handleLogin());
        loginBtn.setPrefWidth(150);

        Button registerBtn = new Button("Register");
        registerBtn.getStyleClass().add("secondary-btn");
        registerBtn.setOnAction(event -> stage.setScene(registerScene));
        registerBtn.setPrefWidth(150);

        actions.setSpacing(15);
        actions.setAlignment(Pos.CENTER);
        actions.getChildren().addAll(loginBtn, registerBtn);
        card.getChildren().addAll(heading, usernameBox, passwordBox, actions);
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

        HBox actions = new HBox(10);

        Button acceptRejectBtn = new Button("Approve Submission");
        acceptRejectBtn.getStyleClass().add("primary-btn");
        acceptRejectBtn.setOnAction(event -> handleApproveReject());

        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("secondary-btn");
        logoutBtn.setOnAction(event -> handleLogout());

        actions.getChildren().addAll(acceptRejectBtn, logoutBtn);

        HBox fields = new HBox(10);

        approveSubmissionIdField = new TextField();
        approveSubmissionIdField.setPromptText("Submission ID");
        actionBox = new ComboBox<>(FXCollections.observableArrayList("Approve", "Reject"));
        actionBox.setValue("Approve");
        actionBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                rejectReasonField.setDisable(newValue.equals("Approve"));
                acceptRejectBtn.setText(newValue + " Submission");
            }
        });
        rejectReasonField = new TextField();
        rejectReasonField.setPromptText("Rejection Reason (can be empty)");
        rejectReasonField.setDisable(true);

        fields.getChildren().addAll(approveSubmissionIdField, actionBox, rejectReasonField);


        card.getChildren().addAll(heading, hint, fields, actions);
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
        tableStatusFilter = new ComboBox<>(FXCollections.observableArrayList("PENDING", "APPROVED", "REJECTED", "ALL"));
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
        String username = registerUsernameField.getText() == null ? "" : registerUsernameField.getText().trim();
        String fullName = registerFullNameField.getText() == null ? "" : registerFullNameField.getText().trim();
        String password = registerPasswordField.getText() == null ? "" : registerPasswordField.getText();
        String confirmPassword = registerConfirmPasswordField.getText() == null ? "" : registerConfirmPasswordField.getText();
        String eId = registerStaffIDField.getText();

        if (username.isEmpty() || fullName.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            setStatus("Registration failed: username, full name, password, and confirm password are required.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            setStatus("Registration failed: passwords do not match.");
            return;
        }

        SharedAuthFacade.AuthResult result =
                authFacade.register(username, fullName, password, confirmPassword, "Librarian", null, eId);
        setStatus(result.message());

        if (result.success()) {
            registerUsernameField.clear();
            registerFullNameField.clear();
            registerPasswordField.clear();
            registerConfirmPasswordField.clear();
            registerStaffIDField.clear();
            stage.setScene(loginRegisterScene);
        }
    }

    private void handleLogin() {
        SharedAuthFacade.AuthResult result =
                authFacade.login(loginUsernameField.getText(), loginPasswordField.getText(), "Librarian");
        setStatus(result.message());
        if (result.success()) {
            currentUser = librarianRepository.findByUsername(loginUsernameField.getText().trim()).orElse(null);
            if (currentUser == null) {
                setStatus("Login failed: unable to resolve librarian profile.");
                return;
            }
            currentUserLabel.setText("Current user: " + currentUser.getUsername());
            loginPasswordField.clear();
            stage.setScene(acceptRejectScene);
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

        stage.setScene(loginRegisterScene);
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

        String subId = approveSubmissionIdField.getText();
        LibrarianPortalService.OperationResult result = portalService.validateBookSubmissionId(subId);
        if (!result.success()) {
            setStatus(result.message());
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Approval");
        alert.setHeaderText("Approve this book submission?");
        alert.setContentText(portalService.getConfirmDetail(subId));

        Optional<ButtonType> alertr = alert.showAndWait();
        if (alertr.get() != ButtonType.OK) return;

        result = portalService.approveBookSubmission(subId, currentUser);
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

        String subId = approveSubmissionIdField.getText();
        LibrarianPortalService.OperationResult result = portalService.validateBookSubmissionId(subId);
        if (!result.success()) {
            setStatus(result.message());
            return;
        }

        String rReason = rejectReasonField.getText();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Rejection");
        alert.setHeaderText("Reject this book submission?");
        alert.setContentText(portalService.getConfirmDetail(subId) + "Rejection reason: " + (rReason.isEmpty() ? "Empty" : rReason) + "\n");

        Optional<ButtonType> alertr = alert.showAndWait();
        if (alertr.get() != ButtonType.OK) return;

        result = portalService.rejectBookSubmission(subId, currentUser, rReason);
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