package project.task3.ui;

import javafx.application.Application;
import javafx.application.Platform;
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
import javafx.stage.FileChooser;
import project.shared.SharedAuthFacade;
import project.shared.CrashSimulationManager;
import project.task1.repo.StudentStaffRepository;
import project.task1.model.UserAccount;
import project.task2.model.BookSubmission;
import project.task2.repo.AuthorRepository;
import project.task2.repo.SubmissionRepository;
import project.task2.utils.SessionManager;
import project.task3.model.LibrarianAccount;
import project.task1.repo.InMemoryBookRepository;
import project.task3.repo.LibrarianRepository;
import project.task3.service.LibrarianPortalService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class LibrarianPortalApp extends Application {
    private final LibrarianPortalService portalService;
    private final SharedAuthFacade authFacade;

    private SharedAuthFacade.UserPrincipal currentUser;

    private Stage stage;
    private Scene loginRegisterScene;
    private Scene registerScene;
    private Scene acceptRejectScene;
    private Scene profileScene;
    private Scene notificationScene;
    private Scene manageUsersScene;
    private Scene borrowedBooksScene;

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
    private Label approveStatusLabel;

    private TextField registerUsernameField;
    private TextField registerFullNameField;
    private PasswordField registerPasswordField;
    private PasswordField registerConfirmPasswordField;
    private Label registerPasswordHintLabel;
    private TextField registerStaffIDField;
    private Label registerStatusLabel;

    private TextField loginUsernameField;
    private PasswordField loginPasswordField;
    private Label loginStatusLabel;

    private TextField profileFullNameField;
    private PasswordField profileCurrentPasswordField;
    private TextField profilePasswordField;
    private TextField profileConfirmPasswordField;
    private TextField profileStaffIDField;
    private Label profilePasswordHintLabel;
    private Label profileStatusLabel;

    private ComboBox<String> manageUsersType;
    private TextField manageUsersSelectedName;
    private TextField manageUsersNewFullName;
    private PasswordField manageUsersNewPassword;
    private PasswordField manageUsersNewPasswordConfirm;
    private TableView<UserAccount> allUsersTable;
    private Label manageUsersStatusLabel;

    private ListView<String> notificationList;
    private Label notificationStatusLabel;
    private TextField notificationSearchField;
    private ComboBox<String> notificationTypeFilter;
    private CheckBox notificationUrgentOnly;

    private TableView<LibrarianPortalService.BorrowedBookRecordView> borrowedBooksTable;
    private TextField bookTableTitleFilter;
    private TextField bookTableBorrowedByFilter;
    private ComboBox<String> bookTableStatusFilter;
    private Label bookTableStatusLabel;
    private Timer autoSaveTimer;
    private boolean randomCrashEnabled;

    public LibrarianPortalApp() {
        LibrarianRepository librarianRepository = new LibrarianRepository();
        StudentStaffRepository studentStaffRepository = new StudentStaffRepository();
        AuthorRepository authorRepository = new AuthorRepository();
        portalService = new LibrarianPortalService(
                librarianRepository,
                studentStaffRepository,
                authorRepository,
                new InMemoryBookRepository(),
                new SubmissionRepository()
        );
        authFacade = new SharedAuthFacade(
                studentStaffRepository,
                authorRepository,
                librarianRepository
        );
    }

    @Override
    public void start(Stage stage) {
        SessionManager.loadSession();
        loginRegisterScene = buildLoginRegisterScene();
        registerScene = buildRegisterScene();
        acceptRejectScene = buildAcceptRejectScene();
        profileScene = buildProfileScene();
        notificationScene = buildNotificattionScene();
        manageUsersScene = buildManageUsersScene();
        borrowedBooksScene = buildBorrowedBooksScene();

        stage.setTitle("Task 3 - Librarian Portal");
        stage.setScene(loginRegisterScene);
        stage.show();
        this.stage = stage;

        if (SessionManager.hasSavedSession()) {
            showInfoPopup("Session Restored", "Previous session detected", "Please log in to continue from recovered state.");
        }

        setStatus("Ready.");
    }



    private Scene buildLoginRegisterScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setCenter(buildLoginHeader());
        loginStatusLabel = new Label();
        root.setBottom(buildStatusBar(loginStatusLabel));

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
        registerStatusLabel = new Label();
        root.setBottom(buildStatusBar(registerStatusLabel));

        Scene scene = new Scene(root, 1060, 700);
        scene.getStylesheets().add(
                getClass().getResource("/project/task1/ui/light-theme.css").toExternalForm()
        );
        return scene;
    }

    private Scene buildAcceptRejectScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(new HBox(18, buildSceneSelector(0)));
        root.setCenter(buildBookCenterPanel());
        approveStatusLabel = new Label();
        root.setBottom(buildStatusBar(approveStatusLabel));

        Scene scene = new Scene(root, 1060, 700);
        scene.getStylesheets().add(
                getClass().getResource("/project/task1/ui/light-theme.css").toExternalForm()
        );

        refreshSubmissions();

        return scene;
    }

    private Scene buildProfileScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(new HBox(18, buildSceneSelector(1)));
        root.setCenter(buildProfileCard());
        profileStatusLabel = new Label();
        root.setBottom(buildStatusBar(profileStatusLabel));

        Scene scene = new Scene(root, 1060, 700);
        scene.getStylesheets().add(
                getClass().getResource("/project/task1/ui/light-theme.css").toExternalForm()
        );

        return scene;
    }

    private Scene buildNotificattionScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(new HBox(18, buildSceneSelector(2)));
        root.setCenter(buildNotificationCard());
        notificationStatusLabel = new Label();
        root.setBottom(buildStatusBar(notificationStatusLabel));

        Scene scene = new Scene(root, 1060, 700);
        scene.getStylesheets().add(
                getClass().getResource("/project/task1/ui/light-theme.css").toExternalForm()
        );

        return scene;
    }

    private Scene buildManageUsersScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(new HBox(18, buildSceneSelector(3)));
        root.setCenter(buildManageUsersView());
        manageUsersStatusLabel = new Label();
        root.setBottom(buildStatusBar(manageUsersStatusLabel));

        Scene scene = new Scene(root, 1060, 700);
        scene.getStylesheets().add(
                getClass().getResource("/project/task1/ui/light-theme.css").toExternalForm()
        );

        return scene;
    }

    private Scene buildBorrowedBooksScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(new HBox(18, buildSceneSelector(4)));
        root.setCenter(buildBorrowedBooksView());
        bookTableStatusLabel = new Label();
        root.setBottom(buildStatusBar(bookTableStatusLabel));

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

    private VBox buildProfileCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        Label heading = new Label("Manage Profile");
        heading.getStyleClass().add("card-title");
        Label hint = new Label("Update full name and password.");
        hint.getStyleClass().add("muted");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        profileFullNameField = new TextField();
        profileCurrentPasswordField = new PasswordField();
        profilePasswordField = new PasswordField();
        profileConfirmPasswordField = new PasswordField();
        profileStaffIDField = new TextField();
        profilePasswordHintLabel = new Label();
        profilePasswordHintLabel.getStyleClass().add("muted");
        profilePasswordField.textProperty().addListener((obs, oldValue, newValue) -> updateProfilePasswordHint());
        profileConfirmPasswordField.textProperty().addListener((obs, oldValue, newValue) -> updateProfilePasswordHint());

        grid.add(new Label("Full Name"), 0, 0);
        grid.add(profileFullNameField, 1, 0);
        grid.add(new Label("Current Password"), 0, 1);
        grid.add(profileCurrentPasswordField, 1, 1);
        grid.add(new Label("New Password"), 0, 2);
        grid.add(profilePasswordField, 1, 2);
        grid.add(new Label("Confirm Password"), 0, 3);
        grid.add(profileConfirmPasswordField, 1, 3);

        if (currentUser != null) {
            profileFullNameField.setText(currentUser.fullName());
        }
        updateProfilePasswordHint();

        Button updateBtn = new Button("Update Profile");
        updateBtn.getStyleClass().add("primary-btn");
        updateBtn.setOnAction(e -> handleProfileUpdate());
        card.getChildren().addAll(heading, hint, grid, profilePasswordHintLabel, updateBtn);
        return card;
    }

    private VBox buildNotificationCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        Label heading = new Label("Notification Board");
        heading.getStyleClass().add("card-title");
        Label hint = new Label("Timestamped and categorized notifications.");
        hint.getStyleClass().add("muted");
        HBox filters = new HBox(8);
        notificationSearchField = new TextField();
        notificationSearchField.setPromptText("Search notifications");
        notificationTypeFilter = new ComboBox<>(FXCollections.observableArrayList("ALL", "ANNOUNCEMENT", "SUBMISSION", "URGENT"));
        notificationTypeFilter.setValue("ALL");
        notificationUrgentOnly = new CheckBox("Urgent only");
        Button applyFilter = new Button("Apply");
        applyFilter.getStyleClass().add("secondary-btn");
        applyFilter.setOnAction(e -> refreshNotifications());
        filters.getChildren().addAll(notificationSearchField, notificationTypeFilter, notificationUrgentOnly, applyFilter);
        notificationList = new ListView<>();
        VBox.setVgrow(notificationList, Priority.ALWAYS);
        card.getChildren().addAll(heading, hint, filters, notificationList);
        return card;
    }

    private VBox buildAcceptRejectHeader() {
        VBox wrapper = new VBox(14);
        wrapper.setPadding(new Insets(0, 18, 8, 18));

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
        registerPasswordHintLabel = new Label();
        registerPasswordHintLabel.getStyleClass().add("muted");
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
        registerPasswordField.textProperty().addListener((obs, oldValue, newValue) -> updateRegisterPasswordHint());
        registerConfirmPasswordField.textProperty().addListener((obs, oldValue, newValue) -> updateRegisterPasswordHint());
        updateRegisterPasswordHint();

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

        card.getChildren().addAll(heading, grid, registerPasswordHintLabel, reqBox, actions);
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
        if (SessionManager.hasSavedSession() && SessionManager.getCurrentUsername() != null) {
            loginUsernameField.setText(SessionManager.getCurrentUsername());
        }
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
        Button previewFileBtn = new Button("Preview File");
        previewFileBtn.getStyleClass().add("secondary-btn");
        previewFileBtn.setOnAction(event -> handlePreviewSubmissionFile());
        Button bulkApproveBtn = new Button("Bulk Approve Selected");
        bulkApproveBtn.getStyleClass().add("secondary-btn");
        bulkApproveBtn.setOnAction(event -> handleBulkSubmissionAction(true));
        Button bulkRejectBtn = new Button("Bulk Reject Selected");
        bulkRejectBtn.getStyleClass().add("secondary-btn");
        bulkRejectBtn.setOnAction(event -> handleBulkSubmissionAction(false));

        actions.getChildren().addAll(acceptRejectBtn, previewFileBtn, bulkApproveBtn, bulkRejectBtn);

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
        bookSubmissionTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<BookSubmission, String> idCol = new TableColumn<>("Book Submission ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("submissionId"));

        TableColumn<BookSubmission, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<BookSubmission, String> authorUsernameCol = new TableColumn<>("Author Username");
        authorUsernameCol.setCellValueFactory(new PropertyValueFactory<>("authorUsername"));

        TableColumn<BookSubmission, String> authorFullNameCol = new TableColumn<>("Author Full Name");
        authorFullNameCol.setCellValueFactory(new PropertyValueFactory<>("authorFullName"));

        TableColumn<BookSubmission, String> genreCol = new TableColumn<>("Genre");
        genreCol.setCellValueFactory(new PropertyValueFactory<>("genresAsString"));

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
        wrapper.getChildren().addAll(buildAcceptRejectHeader(), new Label("Filters: "), filters1, filters2, heading, bookSubmissionTable);
        return wrapper;
    }

    private VBox buildManageUsersView() {
        VBox wrapper = new VBox(10);
        wrapper.setPadding(new Insets(8, 18, 18, 18));
        Label heading = new Label("Users");
        heading.getStyleClass().add("section-title");

        allUsersTable = new TableView<>();
        allUsersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        allUsersTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<UserAccount, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<UserAccount, String> fullNameCol = new TableColumn<>("Full Name");
        fullNameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));

        TableColumn<UserAccount, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));

        TableColumn<UserAccount, String> disabledCol = new TableColumn<>("is Disabled");
        disabledCol.setCellValueFactory(new PropertyValueFactory<>("DisabledString"));

        allUsersTable.getColumns().addAll(usernameCol, fullNameCol, roleCol, disabledCol);

        allUsersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldUser, newUser) -> {
            if (newUser != null) manageUsersSelectedName.setText(newUser.getUsername());
        });


        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(320);

        Label cardHeading = new Label("Manage Users");
        cardHeading.getStyleClass().add("card-title");
        Label hint = new Label("Select a user in the table,\n or type the username below.");
        hint.getStyleClass().add("muted");

        HBox fields = new HBox(10);
        manageUsersType = new ComboBox<>(FXCollections.observableArrayList("Student/Staff", "Author", "Librarian", "All"));
        manageUsersType.setValue("All");
        manageUsersType.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                refreshEditUsers();
            }
        });
        manageUsersSelectedName = new TextField();
        manageUsersSelectedName.setPromptText("Username");
        manageUsersNewFullName = new TextField();
        manageUsersNewFullName.setPromptText("New Full Name");
        manageUsersNewPassword = new PasswordField();
        manageUsersNewPassword.setPromptText("New Password (leave blanked if unchanged)");
        manageUsersNewPasswordConfirm = new PasswordField();
        manageUsersNewPasswordConfirm.setPromptText("Confirm New Password");
        fields.getChildren().addAll(manageUsersType, manageUsersSelectedName, manageUsersNewFullName, manageUsersNewPassword, manageUsersNewPasswordConfirm);

        HBox actions = new HBox(10);
        Button applyBtn = new Button("Apply Changes");
        applyBtn.getStyleClass().add("primary-btn");
        applyBtn.setOnAction(event -> handleUserEdit());
        Button disableBtn = new Button("Disable User");
        disableBtn.getStyleClass().add("primary-btn");
        disableBtn.setOnAction(event -> handleDisableUser());
        Button disableSelectedBtn = new Button("Disable Selected");
        disableSelectedBtn.getStyleClass().add("secondary-btn");
        disableSelectedBtn.setOnAction(event -> handleDisableSelectedUsers());
        Button addUserBtn = new Button("Add New User");
        addUserBtn.getStyleClass().add("secondary-btn");
        addUserBtn.setOnAction(event -> handleAddUserDialog());
        actions.getChildren().addAll(applyBtn, disableBtn, disableSelectedBtn, addUserBtn);

        card.getChildren().addAll(heading, hint, fields, actions);

        VBox.setVgrow(bookSubmissionTable, Priority.ALWAYS);
        wrapper.getChildren().addAll(card, heading, allUsersTable);

        return wrapper;
    }

    private VBox buildBorrowedBooksView() {
        VBox wrapper = new VBox(10);
        wrapper.setPadding(new Insets(8, 18, 18, 18));
        Label heading = new Label("Borrowed Books Records");
        heading.getStyleClass().add("section-title");

        borrowedBooksTable = new TableView<>();
        borrowedBooksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<LibrarianPortalService.BorrowedBookRecordView, String> idCol = new TableColumn<>("Book ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("bookId"));
        TableColumn<LibrarianPortalService.BorrowedBookRecordView, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        TableColumn<LibrarianPortalService.BorrowedBookRecordView, String> borrowerCol = new TableColumn<>("Borrower");
        borrowerCol.setCellValueFactory(new PropertyValueFactory<>("borrowerUsername"));
        TableColumn<LibrarianPortalService.BorrowedBookRecordView, Object> borrowDateCol = new TableColumn<>("Borrow Date");
        borrowDateCol.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));
        TableColumn<LibrarianPortalService.BorrowedBookRecordView, Object> returnDateCol = new TableColumn<>("Return Date");
        returnDateCol.setCellValueFactory(new PropertyValueFactory<>("returnDate"));
        TableColumn<LibrarianPortalService.BorrowedBookRecordView, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                if ("OVERDUE".equalsIgnoreCase(item)) {
                    setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                } else {
                    setStyle("");
                }
            }
        });
        borrowedBooksTable.getColumns().addAll(idCol, titleCol, borrowerCol, borrowDateCol, returnDateCol, statusCol);

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(320);

        Label hint = new Label("Search/filter records and export to CSV.");
        hint.getStyleClass().add("muted");

        bookTableTitleFilter = new TextField();
        bookTableTitleFilter.setPromptText("Title");
        bookTableBorrowedByFilter = new TextField();
        bookTableBorrowedByFilter.setPromptText("Borrower username");
        bookTableStatusFilter = new ComboBox<>(FXCollections.observableArrayList("ALL", "BORROWED", "RETURNED", "OVERDUE"));
        bookTableStatusFilter.setValue("ALL");

        Button refreshBtn = new Button("Refresh Table");
        refreshBtn.getStyleClass().add("primary-btn");
        refreshBtn.setOnAction(event -> refreshBorrowedBooks());
        Button exportBtn = new Button("Export CSV");
        exportBtn.getStyleClass().add("secondary-btn");
        exportBtn.setOnAction(event -> handleExportBorrowedRecords());

        HBox filters = new HBox(6,
                new Label("Title:"), bookTableTitleFilter,
                new Label("Borrowed By:"), bookTableBorrowedByFilter,
                new Label("Status:"), bookTableStatusFilter,
                refreshBtn, exportBtn
        );
        card.getChildren().addAll(heading, hint, filters);

        VBox.setVgrow(borrowedBooksTable, Priority.ALWAYS);
        wrapper.getChildren().addAll(card, borrowedBooksTable);
        return wrapper;
    }

    private HBox buildStatusBar(Label s) {
        HBox statusBar = new HBox();
        statusBar.setPadding(new Insets(10, 18, 12, 18));
        s.getStyleClass().add("status");
        statusBar.getChildren().add(s);
        return statusBar;
    }

    private HBox buildSceneSelector(int disable) {
        HBox selector = new HBox();
        selector.setPadding(new Insets(10, 18, 12, 18));

        Button acceptReject = new Button("Manage Book Submission");
        Button profile = new Button("Manage Personal Profile");
        Button notification = new Button("Notifications");
        Button manageUsers = new Button("Manage Users");
        Button borrowedBooks = new Button("Borrowed Books");
        Button crashBtn = new Button("Crash Test");
        Button randomCrashBtn = new Button("Random Crash: OFF");
        Button logoutBtn = new Button("Logout");

        acceptReject.getStyleClass().add("primary-btn");
        acceptReject.setOnAction(event -> { stage.setScene(acceptRejectScene); SessionManager.setCurrentScreen("TASK3_SUBMISSIONS", null); });
        acceptReject.setPrefWidth(200);
        profile.getStyleClass().add("primary-btn");
        profile.setOnAction(event -> { stage.setScene(profileScene); SessionManager.setCurrentScreen("TASK3_PROFILE", null); });
        profile.setPrefWidth(200);
        notification.getStyleClass().add("primary-btn");
        notification.setOnAction(event -> { stage.setScene(notificationScene); SessionManager.setCurrentScreen("TASK3_NOTIFICATIONS", null); refreshNotifications(); });
        notification.setPrefWidth(160);
        manageUsers.getStyleClass().add("primary-btn");
        manageUsers.setOnAction(event -> { stage.setScene(manageUsersScene); SessionManager.setCurrentScreen("TASK3_USERS", null); refreshEditUsers(); });
        manageUsers.setPrefWidth(160);
        borrowedBooks.getStyleClass().add("primary-btn");
        borrowedBooks.setOnAction(event -> { stage.setScene(borrowedBooksScene); SessionManager.setCurrentScreen("TASK3_BORROWED", null); refreshBorrowedBooks(); });
        borrowedBooks.setPrefWidth(160);
        crashBtn.getStyleClass().add("secondary-btn");
        crashBtn.setOnAction(event -> handleCrashTest());
        randomCrashBtn.getStyleClass().add("secondary-btn");
        randomCrashBtn.setOnAction(event -> {
            randomCrashEnabled = !randomCrashEnabled;
            if (randomCrashEnabled) {
                CrashSimulationManager.enableRandomCrash(30, 120, this::persistSessionSnapshot);
                randomCrashBtn.setText("Random Crash: ON");
                showInfoPopup("Random Crash", "Enabled", "Random crash simulation enabled (30-120s window).");
            } else {
                CrashSimulationManager.disableRandomCrash();
                randomCrashBtn.setText("Random Crash: OFF");
                showInfoPopup("Random Crash", "Disabled", "Random crash simulation disabled.");
            }
        });
        logoutBtn.getStyleClass().add("secondary-btn");
        logoutBtn.setOnAction(event -> handleLogout());

        switch (disable) {
            case 0: acceptReject.setDisable(true);
            break;
            case 1: profile.setDisable(true);
            break;
            case 2: notification.setDisable(true);
            break;
            case 3: manageUsers.setDisable(true);
            break;
            case 4: borrowedBooks.setDisable(true);
        }

        selector.getChildren().addAll(acceptReject, profile, notification, manageUsers, borrowedBooks, crashBtn, randomCrashBtn, logoutBtn);

        return selector;
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
        if (!result.success()) {
            showErrorPopup("Login Failed", "Invalid credentials.", result.message());
            return;
        }
        currentUser = result.principal();
        if (!"LIBRARIAN".equalsIgnoreCase(currentUser.role())) {
            currentUser = null;
            showErrorPopup("Login Failed", "Unsupported role.", "This dashboard is only for librarian users.");
            return;
        }
        currentUserLabel.setText("Current user: " + currentUser.username() + " (" + currentUser.role() + ")");
        String restoredScreen = SessionManager.getCurrentScreen();
        SessionManager.setCurrentUser(currentUser.username(), currentUser.fullName());
        if (restoredScreen == null || restoredScreen.isBlank()) {
            SessionManager.setCurrentScreen("TASK3_SUBMISSIONS", null);
        }
        loginPasswordField.clear();
        stage.setScene(acceptRejectScene);
        boolean restored = restoreLastScreen(restoredScreen);
        if (restored) {
            showInfoPopup("Session Restore", "Restored successfully", "Recovered your last screen and saved inputs.");
        } else {
            showErrorPopup("Session Restore", "Restore failed", "Could not restore previous screen. Returned to submissions.");
        }
        startAutoSave();
        showInfoPopup("Login", "Welcome", result.message());
    }

    private void handleLogout() {
        if (currentUser == null) {
            showErrorPopup("Logout", "No logged in users found", "No user is logged in currently.");
            return;
        }
        String username = currentUser.username();
        currentUser = null;
        currentUserLabel.setText("Current user: (none)");
        setStatus("Logged out: " + username);
        stopAutoSave();
        CrashSimulationManager.disableRandomCrash();
        randomCrashEnabled = false;
        SessionManager.clearSession();

        stage.setScene(loginRegisterScene);
    }

    private void handleApproveReject() {
        if (actionBox.getValue().equals("Approve")) handleApprove();
        else handleReject();
    }

    private void handleApprove() {
        if (currentUser == null) {
            showErrorPopup("Approve Submission", "User not logged in", "Please log in first.");
            return;
        }

        String subId = approveSubmissionIdField.getText();
        LibrarianPortalService.OperationResult result = portalService.validateBookSubmissionId(subId);
        if (!result.success()) {
            showErrorPopup("Approve Submission", "Action failed", result.message());
            return;
        }

        if (!showConfirmPopup(
                "Confirm Approval",
                "Approve this book submission?",
                portalService.getConfirmDetail(subId)
        )) return;

        result = portalService.approveBookSubmission(subId, currentUser.username());
        setStatus(result.message());
        if (result.success()) {
            refreshSubmissions();
            approveSubmissionIdField.clear();
        }
    }

    private void handleReject() {
        if (currentUser == null) {
            showErrorPopup("Reject Submission", "User not logged in", "Please log in first.");
            return;
        }

        String subId = approveSubmissionIdField.getText();
        LibrarianPortalService.OperationResult result = portalService.validateBookSubmissionId(subId);
        if (!result.success()) {
            showErrorPopup("Reject Submission", "Action failed", result.message());
            return;
        }

        String rReason = rejectReasonField.getText();

        if (!showConfirmPopup(
                "Confirm Rejection",
                "Reject this book submission?",
                portalService.getConfirmDetail(subId) + "Rejection reason: " + (rReason.isEmpty() ? "Empty" : rReason) + "\n"
        )) return;

        result = portalService.rejectBookSubmission(subId, currentUser.username(), rReason);
        setStatus(result.message());
        if (result.success()) {
            refreshSubmissions();
            approveSubmissionIdField.clear();
            rejectReasonField.clear();
        }
    }

    private void handlePreviewSubmissionFile() {
        BookSubmission selected = bookSubmissionTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorPopup("Preview File", "No submission selected", "Select one submission first.");
            return;
        }
        try {
            java.io.File file = new java.io.File(selected.getFilePath());
            if (!file.exists()) {
                showErrorPopup("Preview File", "File not found", selected.getFilePath());
                return;
            }
            if (!java.awt.Desktop.isDesktopSupported()) {
                showErrorPopup("Preview File", "Unsupported", "Desktop open is not supported on this machine.");
                return;
            }
            java.awt.Desktop.getDesktop().open(file);
        } catch (Exception e) {
            showErrorPopup("Preview File", "Open failed", e.getMessage());
        }
    }

    private void handleBulkSubmissionAction(boolean approve) {
        if (currentUser == null) {
            showErrorPopup("Bulk Action", "User not logged in", "Please log in first.");
            return;
        }
        List<BookSubmission> selected = bookSubmissionTable.getSelectionModel().getSelectedItems();
        if (selected == null || selected.isEmpty()) {
            showErrorPopup("Bulk Action", "No submissions selected", "Select one or more rows first.");
            return;
        }
        String reason = "";
        if (!approve) {
            reason = rejectReasonField == null ? "" : rejectReasonField.getText();
        }
        List<String> ids = selected.stream().map(BookSubmission::getSubmissionId).toList();
        LibrarianPortalService.OperationResult result = portalService.bulkApproveReject(ids, currentUser.username(), approve, reason);
        setStatus(result.message());
        if (!result.success()) {
            showErrorPopup("Bulk Action", "Partial/Failed", result.message());
        } else {
            showInfoPopup("Bulk Action", "Success", result.message());
        }
        refreshSubmissions();
    }

    private void handleProfileUpdate() {
        if (currentUser == null) {
            showErrorPopup("Profile Update", "User not logged in.", "Please log in first.");
            return;
        }
        LibrarianPortalService.ProfileUpdateResult result = portalService.updateProfile(
                currentUser.username(),
                profileCurrentPasswordField == null ? "" : profileCurrentPasswordField.getText(),
                profileFullNameField == null ? "" : profileFullNameField.getText(),
                profilePasswordField == null ? "" : profilePasswordField.getText(),
                profileConfirmPasswordField == null ? "" : profileConfirmPasswordField.getText(),
                profileStaffIDField == null ? "" : profileStaffIDField.getText()
        );
        setStatus(result.message());
        if (!result.success()) {
            showErrorPopup("Profile Update Failed", "Unable to update profile.", result.message());
            return;
        }
        currentUser = new SharedAuthFacade.UserPrincipal(
                currentUser.username(),
                profileFullNameField.getText().trim(),
                currentUser.role()
        );
        currentUserLabel.setText("Current user: " + currentUser.username() + " (" + currentUser.role() + ")");
        if (profilePasswordField != null) {
            profilePasswordField.clear();
        }
        if (profileCurrentPasswordField != null) {
            profileCurrentPasswordField.clear();
        }
        if (profileConfirmPasswordField != null) {
            profileConfirmPasswordField.clear();
        }
        if (profileStaffIDField != null) {
            profileStaffIDField.clear();
        }
        if (result.passwordChanged()) {
            showInfoPopup("Profile Update", "Password changed", "Password changed successfully. You will be logged out now.");
            handleLogout();
            return;
        }
        showInfoPopup("Profile Update", "Success", result.message());
    }

    private void handleUserEdit() {
        if (currentUser == null) {
            showErrorPopup("Edit User", "No user logged in.", "Please log in first.");
            return;
        }

        String name = manageUsersSelectedName.getText();
        LibrarianPortalService.OperationResult result = portalService.validateUsername(name);
        if (!result.success()) {
            showErrorPopup("Edit User", "Action failed", result.message());
            return;
        }

        if (!showConfirmPopup(
                "Edit User",
                "Edit this user?",
                portalService.getUserEditConfirmDetail(name, manageUsersNewFullName.getText())
        )) return;

        result = portalService.editUserAccount(name, manageUsersNewFullName.getText(), manageUsersNewPassword.getText(), manageUsersNewPasswordConfirm.getText());
        if (!result.success()) {
            showErrorPopup("Edit User", "Action failed", result.message());
            return;
        }
        else {
            setStatus(result.message());
            refreshEditUsers();
            manageUsersNewFullName.clear();
            manageUsersNewPassword.clear();
            manageUsersNewPasswordConfirm.clear();
        }
    }

    private void handleDisableUser() {
        if (currentUser == null) {
            showErrorPopup("Disable User", "No user logged in.", "Please log in first.");
            return;
        }

        String name = manageUsersSelectedName.getText();
        if (currentUser.username().equals(name)) {
            showErrorPopup("Disable User", "Unsupported action.", "You cannot disable yourself.");
            return;
        }

        LibrarianPortalService.OperationResult result = portalService.validateDisabledUsername(name);
        if (!result.success()) {
            showErrorPopup("Disable User", "Action failed", result.message());
            return;
        }

        if (!showConfirmPopup(
                "Disable User",
                "Disable this user?",
                portalService.getUserEditConfirmDetail(name, manageUsersNewFullName.getText())
        )) return;

        result = portalService.disableUser(name);
        if (!result.success()) showErrorPopup("Edit User", "Action failed", result.message());
        else {
            setStatus(result.message());
            refreshEditUsers();
            manageUsersNewFullName.clear();
            manageUsersNewPassword.clear();
            manageUsersNewPasswordConfirm.clear();
        }
    }

    private void handleDisableSelectedUsers() {
        List<UserAccount> selected = allUsersTable.getSelectionModel().getSelectedItems();
        if (selected == null || selected.isEmpty()) {
            showErrorPopup("Disable Selected", "No users selected", "Select one or more users first.");
            return;
        }
        List<String> usernames = selected.stream()
                .map(UserAccount::getUsername)
                .filter(u -> currentUser == null || !u.equals(currentUser.username()))
                .toList();
        LibrarianPortalService.OperationResult result = portalService.disableUsers(usernames);
        setStatus(result.message());
        refreshEditUsers();
    }

    private void handleAddUserDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add New User");
        dialog.setHeaderText("Create a new account");
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        ComboBox<String> roleBox = new ComboBox<>(FXCollections.observableArrayList("Student", "Staff", "Author", "Librarian"));
        roleBox.setValue("Student");
        TextField usernameField = new TextField();
        TextField fullNameField = new TextField();
        PasswordField passwordField = new PasswordField();
        TextField employeeIdField = new TextField();
        TextField bioField = new TextField();
        grid.add(new Label("Role"), 0, 0); grid.add(roleBox, 1, 0);
        grid.add(new Label("Username"), 0, 1); grid.add(usernameField, 1, 1);
        grid.add(new Label("Full Name"), 0, 2); grid.add(fullNameField, 1, 2);
        grid.add(new Label("Password"), 0, 3); grid.add(passwordField, 1, 3);
        grid.add(new Label("Employee ID"), 0, 4); grid.add(employeeIdField, 1, 4);
        grid.add(new Label("Bio"), 0, 5); grid.add(bioField, 1, 5);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Optional<ButtonType> choice = dialog.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) {
            return;
        }
        LibrarianPortalService.OperationResult result = portalService.addNewUser(
                roleBox.getValue(),
                usernameField.getText(),
                fullNameField.getText(),
                passwordField.getText(),
                employeeIdField.getText(),
                bioField.getText()
        );
        setStatus(result.message());
        if (!result.success()) {
            showErrorPopup("Add User", "Failed", result.message());
            return;
        }
        refreshEditUsers();
        showInfoPopup("Add User", "Success", result.message());
    }


    private void refreshBorrowedBooks() {
        borrowedBooksTable.setItems(FXCollections.observableArrayList(
                portalService.getBorrowedBookRecords(
                        bookTableTitleFilter.getText(),
                        bookTableBorrowedByFilter.getText(),
                        bookTableStatusFilter == null ? "ALL" : bookTableStatusFilter.getValue())));
    }

    private void refreshEditUsers() {
        allUsersTable.setItems(FXCollections.observableArrayList(portalService.getUsersScreenData(manageUsersType.getValue())));
    }

    private void refreshSubmissions() {
        LocalDate mind = tableSubmissionMin.getValue();
        LocalDate maxd = tableSubmissionMax.getValue();
        List<BookSubmission> l = portalService.getBookSubmissionScreenData(tableTitleFilter.getText(), tableAuthorUsernameFilter.getText(), tableGenreFilter.getText(), mind != null ? mind.atStartOfDay() : null, maxd != null ? tableSubmissionMax.getValue().atTime(23, 59) : null, tableStatusFilter.getValue());
        bookSubmissionTable.setItems(FXCollections.observableArrayList(l));
    }

    private void refreshNotifications() {
        if (notificationList == null || currentUser == null) return;
        List<String> rows = portalService.getNotificationBoard(
                        currentUser.username(),
                        notificationSearchField == null ? "" : notificationSearchField.getText(),
                        notificationTypeFilter == null ? "ALL" : notificationTypeFilter.getValue(),
                        notificationUrgentOnly != null && notificationUrgentOnly.isSelected()
                )
                .stream()
                .map(n -> formatNotificationRow(n.timestamp(), n.category(), n.message()))
                .collect(java.util.stream.Collectors.toList());
        if (rows.isEmpty()) {
            notificationList.setItems(FXCollections.observableArrayList("No notifications."));
            return;
        }
        notificationList.setItems(FXCollections.observableArrayList(rows));
    }

    private String formatNotificationRow(LocalDateTime timestamp, String category, String message) {
        return "[" + timestamp.toLocalDate() + " " + timestamp.toLocalTime().withNano(0) + "] "
                + "[" + category + "] " + message;
    }

    private boolean restoreLastScreen(String screen) {
        if (screen == null || screen.isBlank()) {
            return true;
        }
        switch (screen) {
            case "TASK3_PROFILE" -> stage.setScene(profileScene);
            case "TASK3_NOTIFICATIONS" -> {
                stage.setScene(notificationScene);
                refreshNotifications();
            }
            case "TASK3_USERS" -> {
                stage.setScene(manageUsersScene);
                refreshEditUsers();
            }
            case "TASK3_BORROWED" -> {
                stage.setScene(borrowedBooksScene);
                refreshBorrowedBooks();
            }
            case "TASK3_SUBMISSIONS" -> stage.setScene(acceptRejectScene);
            default -> {
                stage.setScene(acceptRejectScene);
                return false;
            }
        }
        applyScreenState(screen);
        return true;
    }

    private void handleExportBorrowedRecords() {
        List<LibrarianPortalService.BorrowedBookRecordView> rows = borrowedBooksTable.getItems();
        if (rows == null || rows.isEmpty()) {
            showErrorPopup("Export", "No records", "There are no rows to export.");
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Borrowed Records");
        chooser.setInitialFileName("borrowed_records.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV file", "*.csv"));
        java.io.File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        LibrarianPortalService.OperationResult result = portalService.exportBorrowedRecordsCsv(file.toPath(), rows);
        if (result.success()) {
            showInfoPopup("Export", "Success", result.message());
        } else {
            showErrorPopup("Export", "Failed", result.message());
        }
    }

    private void handleCrashTest() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Crash Test");
        confirm.setHeaderText("Simulate crash?");
        confirm.setContentText("The app will close immediately. Reopen to verify restoration flow.");
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) {
            return;
        }
        persistSessionSnapshot();
        CrashSimulationManager.triggerCrashNow(this::persistSessionSnapshot);
    }

    private void startAutoSave() {
        stopAutoSave();
        autoSaveTimer = new Timer(true);
        autoSaveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> persistSessionSnapshot());
            }
        }, 1000, 1000);
    }

    private void stopAutoSave() {
        if (autoSaveTimer != null) {
            autoSaveTimer.cancel();
            autoSaveTimer = null;
        }
    }

    private void persistSessionSnapshot() {
        saveScreenState();
        SessionManager.autoSave();
    }

    private void saveScreenState() {
        String screen = SessionManager.getCurrentScreen();
        if (screen == null || screen.isBlank()) {
            return;
        }
        String state = switch (screen) {
            case "TASK3_SUBMISSIONS" -> safe(tableTitleFilter) + "|" + safe(tableAuthorUsernameFilter) + "|" + safe(tableGenreFilter)
                    + "|" + (tableSubmissionMin != null && tableSubmissionMin.getValue() != null ? tableSubmissionMin.getValue().toString() : "")
                    + "|" + (tableSubmissionMax != null && tableSubmissionMax.getValue() != null ? tableSubmissionMax.getValue().toString() : "")
                    + "|" + (tableStatusFilter == null ? "PENDING" : tableStatusFilter.getValue());
            case "TASK3_PROFILE" -> safe(profileFullNameField);
            case "TASK3_NOTIFICATIONS" -> safe(notificationSearchField)
                    + "|" + (notificationTypeFilter == null ? "ALL" : notificationTypeFilter.getValue())
                    + "|" + (notificationUrgentOnly != null && notificationUrgentOnly.isSelected());
            case "TASK3_USERS" -> safe(manageUsersSelectedName) + "|" + safe(manageUsersNewFullName)
                    + "|" + (manageUsersType == null ? "All" : manageUsersType.getValue());
            case "TASK3_BORROWED" -> safe(bookTableTitleFilter) + "|" + safe(bookTableBorrowedByFilter)
                    + "|" + (bookTableStatusFilter == null ? "ALL" : bookTableStatusFilter.getValue());
            default -> "";
        };
        SessionManager.saveScreenState(screen, state);
    }

    private void applyScreenState(String screen) {
        String state = SessionManager.getScreenState(screen);
        if (state == null || state.isBlank()) {
            return;
        }
        String[] p = state.split("\\|", -1);
        switch (screen) {
            case "TASK3_SUBMISSIONS" -> {
                if (p.length > 0 && tableTitleFilter != null) tableTitleFilter.setText(p[0]);
                if (p.length > 1 && tableAuthorUsernameFilter != null) tableAuthorUsernameFilter.setText(p[1]);
                if (p.length > 2 && tableGenreFilter != null) tableGenreFilter.setText(p[2]);
                if (p.length > 3 && tableSubmissionMin != null && !p[3].isBlank()) tableSubmissionMin.setValue(LocalDate.parse(p[3]));
                if (p.length > 4 && tableSubmissionMax != null && !p[4].isBlank()) tableSubmissionMax.setValue(LocalDate.parse(p[4]));
                if (p.length > 5 && tableStatusFilter != null && !p[5].isBlank()) tableStatusFilter.setValue(p[5]);
                refreshSubmissions();
            }
            case "TASK3_PROFILE" -> {
                if (p.length > 0 && profileFullNameField != null) profileFullNameField.setText(p[0]);
            }
            case "TASK3_NOTIFICATIONS" -> {
                if (p.length > 0 && notificationSearchField != null) notificationSearchField.setText(p[0]);
                if (p.length > 1 && notificationTypeFilter != null && !p[1].isBlank()) notificationTypeFilter.setValue(p[1]);
                if (p.length > 2 && notificationUrgentOnly != null) notificationUrgentOnly.setSelected(Boolean.parseBoolean(p[2]));
                refreshNotifications();
            }
            case "TASK3_USERS" -> {
                if (p.length > 0 && manageUsersSelectedName != null) manageUsersSelectedName.setText(p[0]);
                if (p.length > 1 && manageUsersNewFullName != null) manageUsersNewFullName.setText(p[1]);
                if (p.length > 2 && manageUsersType != null && !p[2].isBlank()) manageUsersType.setValue(p[2]);
                refreshEditUsers();
            }
            case "TASK3_BORROWED" -> {
                if (p.length > 0 && bookTableTitleFilter != null) bookTableTitleFilter.setText(p[0]);
                if (p.length > 1 && bookTableBorrowedByFilter != null) bookTableBorrowedByFilter.setText(p[1]);
                if (p.length > 2 && bookTableStatusFilter != null && !p[2].isBlank()) bookTableStatusFilter.setValue(p[2]);
                refreshBorrowedBooks();
            }
            default -> {
            }
        }
    }

    private String safe(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText();
    }

    private void setStatus(String message) {
        registerStatusLabel.setText(message);
        loginStatusLabel.setText(message);
        approveStatusLabel.setText(message);
        profileStatusLabel.setText(message);
        notificationStatusLabel.setText(message);
        manageUsersStatusLabel.setText(message);
        bookTableStatusLabel.setText(message);
    }

    private void updateRegisterPasswordHint() {
        updatePasswordHint(registerPasswordField, registerConfirmPasswordField, registerPasswordHintLabel);
    }

    private void updateProfilePasswordHint() {
        updatePasswordHint(profilePasswordField, profileConfirmPasswordField, profilePasswordHintLabel);
    }

    private void readBookSummary() {
        LibrarianPortalService.BorrowedBookRecordView selected = borrowedBooksTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorPopup("Borrowed Record", "Action failed.", "No row selected.");
            return;
        }
        showInfoPopup("Borrowed Record", selected.bookTitle(), "Borrower: " + selected.borrowerUsername() + "\nStatus: " + selected.status());
    }

    static private void updatePasswordHint(TextField passwordField, TextField confirmPasswordField, Label hint) {
        if (hint == null) return;
        String password = passwordField == null ? "" : passwordField.getText();
        String confirm = confirmPasswordField == null ? "" : confirmPasswordField.getText();
        if (password == null || password.isEmpty()) {
            hint.setText("Leave password fields blank to keep current password.");
            hint.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
            return;
        }
        boolean strong = password.length() >= 8
                && password.matches(".*[A-Za-z].*")
                && password.matches(".*\\d.*")
                && password.matches(".*[A-Z].*");
        if (!strong) {
            hint.setText("Weak password: use at least 8 chars with letter, number, and uppercase.");
            hint.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11px;");
            return;
        }
        if (!confirm.isEmpty() && !password.equals(confirm)) {
            hint.setText("Passwords do not match.");
            hint.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11px;");
            return;
        }
        hint.setText("Strong password.");
        hint.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 11px;");
    }

    private void showInfoPopup(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showErrorPopup(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private boolean showConfirmPopup(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        return alert.showAndWait().get() == ButtonType.OK;
    }
}