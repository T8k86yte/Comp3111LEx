package project.task3.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import project.shared.SharedAuthFacade;
import project.shared.CrashSimulationManager;
import project.task1.model.Book;
import project.task3.model.BookDownloadHelper;
import project.task3.model.SummaryGenerator;
import project.task1.repo.StudentStaffRepository;
import project.task1.model.UserAccount;
import project.task2.model.BookSubmission;
import project.task2.repo.AuthorRepository;
import project.task2.repo.SubmissionRepository;
import project.task3.model.LibrarianAccount;
import project.task3.service.LibrarianPortalService.*;
import project.task1.repo.InMemoryBookRepository;
import project.task3.repo.LibrarianRepository;
import project.task3.service.LibrarianPortalService;

import java.awt.*;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import static javafx.application.Platform.exit;

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
    private Scene publishedBooksScene;

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
    private TextField profilePasswordField;
    private TextField profileConfirmPasswordField;
    private TextField profileEmployeeIDField;
    private TextField profilePicturePathField;
    private ImageView profilePicture;
    private TextField profileOldPasswordField;
    private Label profilePasswordHintLabel;
    private Label profileStatusLabel;

    private ComboBox<String> manageUsersType;
    private TextField manageUsersSelectedName;
    private TextField manageUsersNewFullName;
    private PasswordField manageUsersNewPassword;
    private PasswordField manageUsersNewPasswordConfirm;
    private TableView<UserAccount> allUsersTable;
    private HashMap<String, AtomicInteger> manageUsersBorrowCounts;
    private Label manageUsersStatusLabel;

    private ComboBox<String> createUsersType;
    private TextField createUsersUsername;
    private TextField createUsersFullName;
    private PasswordField createUsersPassword;
    private PasswordField createUsersPasswordConfirm;
    private TextField createUsersBioOrEmployeeId;

    private ListView<LibrarianPortalService.NotificationView> notificationList;
    private Label notificationStatusLabel;
    private ComboBox<String> notificationCategoryFilter;
    private DatePicker notificationDateMin;
    private DatePicker notificationDateMax;
    private ComboBox<String> notificationUrgencyFilter;

    private TableView<BorrowedBookRecordView> borrowedBooksTable;
    private TextField bookTableTitleFilter;
    private TextField bookTableBorrowedByFilter;
    private ComboBox<String> bookTableStatusFilter;
    private TableView<BookRequestView> bookRequestTable;
    private TextField bookRequestKeywordFilter;
    private ComboBox<String> bookRequestStatusFilter;
    private TextField requestActionIdField;
    private ComboBox<String> requestActionTypeBox;
    private TextField requestActionCommentField;
    private TextField requestFilePathField;
    private Label bookTableStatusLabel;

    private TableView<Book> publishedBooksTable;
    private TextField publishedBookSelectedId;
    private TextField publishedBookTitle;
    private TextField publishedBookAuthorName;
    private TextField publishedBookGenre;
    private TextField publishedBookDescription;
    private Button publishedBookGenerateButton;
    private TextField publishedBookFilePath;
    private TextField publishedBookCoverPath;
    private Label publishedBookStatusLabel;

    private Timer autoSaveTimer;


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
        loginRegisterScene = buildLoginRegisterScene();
        registerScene = buildRegisterScene();
        acceptRejectScene = buildAcceptRejectScene();
        profileScene = buildProfileScene();
        notificationScene = buildNotificattionScene();
        manageUsersScene = buildManageUsersScene();
        borrowedBooksScene = buildBorrowedBooksScene();
        publishedBooksScene = buildPublishedBooksScene();

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
        root.setCenter(buildNotificationView());
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

    private Scene buildPublishedBooksScene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(new HBox(18, buildSceneSelector(5)));
        root.setCenter(buildPublishedBooksView());
        publishedBookStatusLabel = new Label();
        root.setBottom(buildStatusBar(publishedBookStatusLabel));

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
        profilePasswordField = new PasswordField();
        profileConfirmPasswordField = new PasswordField();
        profileEmployeeIDField = new TextField();
        profilePicturePathField = new TextField();
        Button selectPictureBtn = new Button("Select Profile Picture");
        selectPictureBtn.getStyleClass().add("primary-btn");
        selectPictureBtn.setOnAction((e) -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Profile Image");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("jpg or png files (*.jpg, *.png)","*.jpg", "*.png"));
            File selectedFile = chooser.showOpenDialog(stage);
            if (selectedFile != null) {
                String filePath = selectedFile.getAbsolutePath();
                profilePicturePathField.setText(filePath);
            }
            else profilePicturePathField.setText("");
            handleProfilePictureUpdate();
        });
        profilePicture = new ImageView();
        profileOldPasswordField = new PasswordField();
        profilePasswordHintLabel = new Label();
        profilePasswordHintLabel.getStyleClass().add("muted");
        profilePasswordField.textProperty().addListener((obs, oldValue, newValue) -> updateProfilePasswordHint());
        profileConfirmPasswordField.textProperty().addListener((obs, oldValue, newValue) -> updateProfilePasswordHint());

        grid.add(new Label("Full Name"), 0, 0);
        grid.add(profileFullNameField, 1, 0);
        grid.add(new Label("Old Password"), 0, 1);
        grid.add(profileOldPasswordField, 1, 1);
        grid.add(new Label("New Password"), 0, 2);
        grid.add(profilePasswordField, 1, 2);
        grid.add(new Label("Confirm Password"), 0, 3);
        grid.add(profileConfirmPasswordField, 1, 3);
        grid.add(new Label("New Employee ID"), 0, 4);
        grid.add(profileEmployeeIDField, 1, 4);
        grid.add(new Label("New Profile Picture Path"), 0, 5);
        grid.add(profilePicturePathField, 1, 5);
        grid.add(selectPictureBtn, 2, 5);

        if (currentUser != null) {
            profileFullNameField.setText(currentUser.fullName());
        }
        updateProfilePasswordHint();

        Button updateBtn = new Button("Update Profile");
        updateBtn.getStyleClass().add("primary-btn");
        updateBtn.setOnAction(e -> handleProfileUpdate());
        card.getChildren().addAll(heading, hint, grid, profilePicture, profilePasswordHintLabel, updateBtn);
        return card;
    }

    private VBox buildNotificationView() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        Label heading = new Label("Notification Board");
        heading.getStyleClass().add("card-title");
        Label hint = new Label("Timestamped and categorized notifications.");
        hint.getStyleClass().add("muted");
        notificationList = new ListView<>();
        notificationList.setCellFactory(list -> new javafx.scene.control.ListCell<LibrarianPortalService.NotificationView>() {
            @Override
            protected void updateItem(LibrarianPortalService.NotificationView item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String c = item.category() == null ? "" : item.category().toUpperCase();
                boolean urgent = c.equals("NEW_BOOK_SUBMISSION")
                        || c.equals("USER_ACCOUNT_UPDATE")
                        || c.equals("BOOK_REJECTED")
                        || c.equals("RESPONSE")
                        || c.contains("URGENT");
                String urgentTag = urgent ? "[URGENT] " : "";
                String readTag = item.read() ? "" : "[NEW] ";
                setText(readTag + urgentTag
                        + "[" + item.timestamp().toLocalDate() + " " + item.timestamp().toLocalTime().withNano(0) + "] "
                        + "[" + item.category() + "] " + item.message());
            }
        });
        Button markReadBtn = new Button("Mark Selected Read");
        markReadBtn.getStyleClass().add("secondary-btn");
        markReadBtn.setOnAction(e -> handleMarkTask3NotificationRead());
        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.getStyleClass().add("secondary-btn");
        deleteBtn.setOnAction(e -> handleDeleteTask3Notification());
        Button deleteReadBtn = new Button("Delete Read");
        deleteReadBtn.getStyleClass().add("secondary-btn");
        deleteReadBtn.setOnAction(e -> handleDeleteTask3ReadNotifications());
        HBox actions = new HBox(8, markReadBtn, deleteBtn, deleteReadBtn);
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(notificationList, Priority.ALWAYS);
        card.getChildren().addAll(heading, hint, buildNotificationFilterCard(), notificationList, actions);
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
        Button previewBtn = new Button("Preview File");
        previewBtn.getStyleClass().add("primary-btn");
        previewBtn.setOnAction(event -> handlePreviewFile());

        actions.getChildren().addAll(acceptRejectBtn, previewBtn);

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

    private VBox buildNotificationFilterCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(320);

        Label heading = new Label("Notification");
        heading.getStyleClass().add("card-title");


        notificationCategoryFilter = new ComboBox<>(FXCollections.observableArrayList(
                "ALL",
                "ANNOUNCEMENT",
                "NEW_BOOK_SUBMISSION",
                "NEW_BOOK_REQUEST",
                "USER_ACCOUNT_UPDATE"
        ));
        notificationCategoryFilter.setValue("ALL");
        notificationDateMin = new DatePicker();
        notificationDateMax = new DatePicker();
        notificationUrgencyFilter = new ComboBox<>(FXCollections.observableArrayList("ALL", "URGENT", "NORMAL"));
        notificationUrgencyFilter.setValue("ALL");

        HBox fields = new HBox(10);
        fields.getChildren().addAll(notificationCategoryFilter, notificationDateMin, notificationDateMax, notificationUrgencyFilter);


        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("primary-btn");
        refreshBtn.setOnAction(event -> refreshNotifications());

        card.getChildren().addAll(heading, fields, refreshBtn);
        return card;
    }

    private VBox buildBookCenterPanel() {
        VBox wrapper = new VBox(10);
        wrapper.setPadding(new Insets(8, 18, 18, 18));
        Label heading = new Label("New Book Submissions");
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
        genreCol.setCellValueFactory(new PropertyValueFactory<>("genresAsString"));

        TableColumn<BookSubmission, Object> dateCol = new TableColumn<>("Submitted Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("submissionDate"));

        TableColumn<BookSubmission, String> summaryCol = new TableColumn<>("Summary");
        summaryCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<BookSubmission, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        bookSubmissionTable.getColumns().addAll(idCol, titleCol, authorUsernameCol, authorFullNameCol, genreCol, dateCol, summaryCol, statusCol);
        bookSubmissionTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);//Allow the users to select multiple items by holding ctrl
        bookSubmissionTable.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<BookSubmission>() {
            @Override
            public void onChanged(Change<? extends BookSubmission> s) {
                approveSubmissionIdField.setText(String.join(", ",
                        bookSubmissionTable.getSelectionModel().getSelectedItems().stream()
                                .map(sub -> sub.getSubmissionId()).toList()));
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

        allUsersTable = new TableView<>();
        allUsersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        manageUsersBorrowCounts = new HashMap<String, AtomicInteger>();

        TableColumn<UserAccount, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<UserAccount, String> fullNameCol = new TableColumn<>("Full Name");
        fullNameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));

        TableColumn<UserAccount, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));

        TableColumn<UserAccount, String> disabledCol = new TableColumn<>("is Disabled");
        disabledCol.setCellValueFactory(new PropertyValueFactory<>("disabled"));

        TableColumn<UserAccount, String> lastLoginCol = new TableColumn<>("Last Login Time");
        lastLoginCol.setCellValueFactory(new PropertyValueFactory<>("LastLoginString"));

        TableColumn<UserAccount, String> borrowCountCol = new TableColumn<>("Borrow Book Count");
        borrowCountCol.setCellValueFactory((val) -> {
            return new ReadOnlyStringProperty() {
                @Override
                public Object getBean() {
                    return null;
                }

                @Override
                public String getName() {
                    return "Borrow Book Count";
                }

                @Override
                public String get() {
                    AtomicInteger v = manageUsersBorrowCounts.get(val.getValue().getUsername());
                    return v == null ? "" : v.toString();
                }

                @Override
                public void addListener(ChangeListener<? super String> changeListener) {}
                @Override
                public void removeListener(ChangeListener<? super String> changeListener) {}
                @Override
                public void addListener(InvalidationListener invalidationListener) {}
                @Override
                public void removeListener(InvalidationListener invalidationListener) {}
            };
        });


        allUsersTable.getColumns().addAll(usernameCol, fullNameCol, roleCol, disabledCol, lastLoginCol, borrowCountCol);
        allUsersTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        allUsersTable.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<UserAccount>() {
            @Override
            public void onChanged(Change<? extends UserAccount> c) {
                manageUsersSelectedName.setText(String.join(", ",
                        allUsersTable.getSelectionModel().getSelectedItems().stream()
                                .map(user -> user.getUsername()).toList()));
            }
        });

        HBox cards = new HBox(10);
        cards.getChildren().addAll(buildManageUserCard(), buildCreateUserCard());

        VBox.setVgrow(bookSubmissionTable, Priority.ALWAYS);
        wrapper.getChildren().addAll(cards, allUsersTable);

        return wrapper;
    }

    private VBox buildManageUserCard() {
        Label heading = new Label("Manage Users");
        heading.getStyleClass().add("section-title");

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(640);

        Label hint = new Label("Select a user in the table, or type the username below.");
        hint.getStyleClass().add("muted");

        HBox fields1 = new HBox(10);
        HBox fields2 = new HBox(10);
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
        manageUsersNewPassword.setPromptText("New Password (leave blank if unchanged)");
        manageUsersNewPassword.setPrefWidth(260);
        manageUsersNewPasswordConfirm = new PasswordField();
        manageUsersNewPasswordConfirm.setPromptText("Confirm New Password");
        fields1.getChildren().addAll(manageUsersType, manageUsersSelectedName, manageUsersNewFullName);
        fields2.getChildren().addAll(manageUsersNewPassword, manageUsersNewPasswordConfirm);

        HBox actions = new HBox(10);
        Button applyBtn = new Button("Apply Changes");
        applyBtn.getStyleClass().add("primary-btn");
        applyBtn.setOnAction(event -> handleUserEdit());
        Button disableBtn = new Button("Disable User");
        disableBtn.getStyleClass().add("primary-btn");
        disableBtn.setOnAction(event -> handleDisableUser());
        Button activateBtn = new Button("Activate User");
        activateBtn.getStyleClass().add("primary-btn");
        activateBtn.setOnAction(event -> handleActivateUser());
        actions.getChildren().addAll(applyBtn, disableBtn, activateBtn);

        card.getChildren().addAll(heading, hint, fields1, fields2, actions);

        return card;
    }

    private VBox buildCreateUserCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(640);

        Label heading = new Label("Create Users");
        heading.getStyleClass().add("section-title");

        Label hint = new Label("Input information and create a new user.");
        hint.getStyleClass().add("muted");

        HBox fields1 = new HBox(10);
        HBox fields2 = new HBox(10);
        createUsersType = new ComboBox<>(FXCollections.observableArrayList("Student", "Staff", "Author", "Librarian"));
        createUsersType.setValue("Student");
        createUsersType.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                if (newValue == null || oldValue.equals(newValue)) return;

                switch (newValue) {
                    case "Student":
                    case "Staff":
                        createUsersBioOrEmployeeId.setDisable(true);
                        createUsersBioOrEmployeeId.setPromptText("");
                        break;
                    case "Author":
                        createUsersBioOrEmployeeId.setDisable(false);
                        createUsersBioOrEmployeeId.setPromptText("Bio");
                        break;
                    case "Librarian":
                        createUsersBioOrEmployeeId.setDisable(false);
                        createUsersBioOrEmployeeId.setPromptText("Employee ID");
                        break;
                }
                createUsersBioOrEmployeeId.clear();
            }
        });
        createUsersUsername = new TextField();
        createUsersUsername.setPromptText("Username");
        createUsersFullName = new TextField();
        createUsersFullName.setPromptText("Full Name");
        createUsersPassword = new PasswordField();
        createUsersPassword.setPromptText("Password");
        createUsersPasswordConfirm = new PasswordField();
        createUsersPasswordConfirm.setPromptText("Confirm Password");
        createUsersBioOrEmployeeId = new TextField();
        createUsersBioOrEmployeeId.setDisable(true);
        fields1.getChildren().addAll(createUsersType, createUsersUsername, createUsersFullName);
        fields2.getChildren().addAll(createUsersPassword, createUsersPasswordConfirm, createUsersBioOrEmployeeId);

        Button applyBtn = new Button("Create User");
        applyBtn.getStyleClass().add("primary-btn");
        applyBtn.setOnAction(event -> handleUserCreate());

        card.getChildren().addAll(heading, hint, fields1, fields2, applyBtn);

        return card;
    }

    private VBox buildBorrowedBooksView() {
        VBox wrapper = new VBox(10);
        wrapper.setPadding(new Insets(8, 18, 18, 18));
        Label heading = new Label("Borrowed Books");
        heading.getStyleClass().add("section-title");

        borrowedBooksTable = new TableView<>();
        borrowedBooksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<BorrowedBookRecordView, String> IdCol = new TableColumn<>("Id");
        IdCol.setCellValueFactory(new PropertyValueFactory<>("bookId"));

        TableColumn<BorrowedBookRecordView, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("bookTitle"));

        TableColumn<BorrowedBookRecordView, String> borrowedByCol = new TableColumn<>("Borrowed By");
        borrowedByCol.setCellValueFactory(new PropertyValueFactory<>("borrowerUsername"));

        TableColumn<BorrowedBookRecordView, Object> borrowDateCol = new TableColumn<>("Borrowed Date");
        borrowDateCol.setCellValueFactory(new PropertyValueFactory<>("borrowDate"));

        TableColumn<BorrowedBookRecordView, Object> returnDateCol = new TableColumn<>("Returning Date");
        returnDateCol.setCellValueFactory(new PropertyValueFactory<>("returnDate"));

        TableColumn<BorrowedBookRecordView, String> stateCol = new TableColumn<>("State");
        stateCol.setCellValueFactory(new PropertyValueFactory<>("statusAlt"));

        TableColumn<BorrowedBookRecordView, String> overdueCol = new TableColumn<>("Overdue");
        overdueCol.setCellValueFactory(new PropertyValueFactory<>("overdue"));

        titleCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                BorrowedBookRecordView rowBook = getTableRow() == null ? null : getTableRow().getItem();

                setText(item);
                if (rowBook != null && rowBook.overdue()) {
                    setStyle("-fx-text-fill: #dc2626; -fx-font-weight: 600;");
                } else {
                    setStyle("-fx-text-fill: #111827;");
                }
            }
        });

        borrowedBooksTable.getColumns().addAll(IdCol, titleCol, borrowedByCol, borrowDateCol, returnDateCol, stateCol, overdueCol);

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(320);

        Label cardHeading = new Label("Borrowed Books");
        cardHeading.getStyleClass().add("card-title");
        Label hint = new Label("Use filters to search for borrowed books.");
        hint.getStyleClass().add("muted");



        bookTableTitleFilter = new TextField();
        bookTableBorrowedByFilter = new TextField();
        bookTableStatusFilter = new ComboBox<>(FXCollections.observableArrayList("ALL", "BORROWED", "RETURNED", "OVERDUE"));
        bookTableStatusFilter.setValue("ALL");

        Button refreshBtn = new Button("Refresh Table");
        refreshBtn.getStyleClass().add("primary-btn");
        refreshBtn.setOnAction(event -> refreshBorrowedBooks());
        Button exportBtn = new Button("Export Record");
        exportBtn.getStyleClass().add("primary-btn");
        exportBtn.setOnAction(event -> handleExportBorrowedBooks());

        HBox filters = new HBox(5);
        HBox actions = new HBox(5);
        filters.getChildren().add(new Label("Title: "));
        filters.getChildren().add(bookTableTitleFilter);
        filters.getChildren().add(new Label("Borrowed By: "));
        filters.getChildren().add(bookTableBorrowedByFilter);
        filters.getChildren().add(new Label("Status: "));
        filters.getChildren().add(bookTableStatusFilter);
        actions.getChildren().add(refreshBtn);
        actions.getChildren().add(exportBtn);

        card.getChildren().addAll(heading, hint, filters, actions);

        VBox.setVgrow(borrowedBooksTable, Priority.ALWAYS);
        VBox requestCard = new VBox(10);
        requestCard.getStyleClass().add("card");
        Label requestHeading = new Label("Student/Staff Book Requests");
        requestHeading.getStyleClass().add("card-title");
        Label requestHint = new Label("Review and approve/reject requests for new books.");
        requestHint.getStyleClass().add("muted");

        HBox requestFilters = new HBox(8);
        bookRequestKeywordFilter = new TextField();
        bookRequestKeywordFilter.setPromptText("Keyword (ID/title/author/user)");
        bookRequestStatusFilter = new ComboBox<>(FXCollections.observableArrayList("ALL", "PENDING", "APPROVED", "REJECTED"));
        bookRequestStatusFilter.setValue("PENDING");
        Button refreshRequestsBtn = new Button("Refresh Requests");
        refreshRequestsBtn.getStyleClass().add("secondary-btn");
        refreshRequestsBtn.setOnAction(e -> refreshBookRequests());
        requestFilters.getChildren().addAll(new Label("Filter:"), bookRequestKeywordFilter, bookRequestStatusFilter, refreshRequestsBtn);

        bookRequestTable = new TableView<>();
        bookRequestTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        TableColumn<BookRequestView, String> requestIdCol = new TableColumn<>("Request ID");
        requestIdCol.setCellValueFactory(new PropertyValueFactory<>("RequestId"));
        TableColumn<BookRequestView, String> requestUserCol = new TableColumn<>("Requester");
        requestUserCol.setCellValueFactory(new PropertyValueFactory<>("Username"));
        TableColumn<BookRequestView, String> requestTitleCol = new TableColumn<>("Title");
        requestTitleCol.setCellValueFactory(new PropertyValueFactory<>("Title"));
        TableColumn<BookRequestView, String> requestAuthorCol = new TableColumn<>("Author");
        requestAuthorCol.setCellValueFactory(new PropertyValueFactory<>("Author"));
        TableColumn<BookRequestView, String> requestGenreCol = new TableColumn<>("Genre");
        requestGenreCol.setCellValueFactory(new PropertyValueFactory<>("Genre"));
        TableColumn<BookRequestView, String> requestStatusCol = new TableColumn<>("Status");
        requestStatusCol.setCellValueFactory(new PropertyValueFactory<>("Status"));
        TableColumn<BookRequestView, String> requestReasonCol = new TableColumn<>("Reason");
        requestReasonCol.setCellValueFactory(new PropertyValueFactory<>("Reason"));
        requestReasonCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                String compact = item.length() > 70 ? item.substring(0, 70) + "..." : item;
                setText(compact);
                setTooltip(new Tooltip(item));
            }
        });
        bookRequestTable.getColumns().addAll(requestIdCol, requestUserCol, requestTitleCol, requestAuthorCol, requestGenreCol, requestStatusCol, requestReasonCol);
        bookRequestTable.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null && requestActionIdField != null) {
                requestActionIdField.setText(val.requestId());
            }
        });

        HBox requestActionRow = new HBox(8);
        requestActionIdField = new TextField();
        requestActionIdField.setPromptText("Request ID");
        requestActionTypeBox = new ComboBox<>(FXCollections.observableArrayList("APPROVE", "REJECT"));
        requestActionTypeBox.setValue("APPROVE");
        requestActionCommentField = new TextField();
        requestActionCommentField.setPromptText("Optional comment");
        requestFilePathField = new TextField();
        requestFilePathField.setPromptText("Book PDF file path:");
        Button selectFileBtn = new Button("Select Book PDF File");
        selectFileBtn.getStyleClass().add("primary-btn");
        selectFileBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Book File");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files (*.pdf)","*.pdf"));
            File selectedFile = chooser.showOpenDialog(stage);
            if (selectedFile != null) {
                String filePath = selectedFile.getAbsolutePath();
                requestFilePathField.setText(filePath);
            }
        });
        Button applyRequestActionBtn = new Button("Apply");
        applyRequestActionBtn.getStyleClass().add("primary-btn");
        applyRequestActionBtn.setOnAction(e -> handleBookRequestAction());
        Button downloadBookBtn = new Button("Download Book File");
        downloadBookBtn.getStyleClass().add("primary-btn");
        downloadBookBtn.setOnAction(e -> handleDownloadBook());
        requestActionRow.getChildren().addAll(requestActionIdField, requestActionTypeBox, requestActionCommentField, selectFileBtn, applyRequestActionBtn, downloadBookBtn);

        requestCard.getChildren().addAll(requestHeading, requestHint, requestFilters, bookRequestTable, requestActionRow);

        wrapper.getChildren().addAll(card, heading, borrowedBooksTable, requestCard);

        return wrapper;
    }

    private VBox buildPublishedBooksView() {
        VBox wrapper = new VBox(10);
        wrapper.setPadding(new Insets(8, 18, 18, 18));
        Label heading = new Label("Published Books");
        heading.getStyleClass().add("section-title");

        publishedBooksTable = new TableView<>();
        publishedBooksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Book, String> IdCol = new TableColumn<>("Id");
        IdCol.setCellValueFactory(new PropertyValueFactory<>("Id"));

        TableColumn<Book, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("Title"));

        TableColumn<Book, String> authorNameCol = new TableColumn<>("Author");
        authorNameCol.setCellValueFactory(new PropertyValueFactory<>("Author"));

        TableColumn<Book, String> genreCol = new TableColumn<>("Genre");
        genreCol.setCellValueFactory(new PropertyValueFactory<>("Genre"));

        TableColumn<Book, String> descriptionCol = new TableColumn<>("Description");
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("Summary"));

        TableColumn<Book, String> availableCol = new TableColumn<>("Available");
        availableCol.setCellValueFactory(new PropertyValueFactory<>("Available"));

        publishedBooksTable.getColumns().addAll(IdCol, titleCol, authorNameCol, genreCol, descriptionCol, availableCol);
        publishedBooksTable.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<Book>() {
            @Override
            public void onChanged(Change<? extends Book> s) {
                publishedBookSelectedId.setText(String.join(", ",
                        publishedBooksTable.getSelectionModel().getSelectedItems().stream()
                                .map(book -> book.getId()).toList()));
            }
        });
        publishedBooksTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(320);

        Label cardHeading = new Label("Published Books");
        cardHeading.getStyleClass().add("card-title");
        Label hint = new Label("Modify book details or create new books.");
        hint.getStyleClass().add("muted");


        publishedBookSelectedId = new TextField();
        publishedBookSelectedId.setPromptText("Book Id: ");
        publishedBookTitle = new TextField();
        publishedBookTitle.setPromptText("Title: ");
        publishedBookAuthorName = new TextField();
        publishedBookAuthorName.setPromptText("Author Username: ");
        publishedBookGenre = new TextField();
        publishedBookGenre.setPromptText("Genre: ");
        publishedBookDescription = new TextField();
        publishedBookDescription.setPromptText("Description: ");
        publishedBookFilePath = new TextField();
        publishedBookFilePath.setPromptText("Book File");
        Button selectBookFileBtn = new Button("Select Book PDF");
        selectBookFileBtn.getStyleClass().add("primary-btn");
        selectBookFileBtn.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Book File");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files (*.pdf)","*.pdf"));
            File selectedFile = chooser.showOpenDialog(stage);
            if (selectedFile != null) {
                String filePath = selectedFile.getAbsolutePath();
                publishedBookFilePath.setText(filePath);
            }
        });
        publishedBookCoverPath = new TextField();
        publishedBookCoverPath.setPromptText("Book Cover File");
        Button selectBookCoverBtn = new Button("Select Book Cover Image");
        selectBookCoverBtn.getStyleClass().add("primary-btn");
        selectBookCoverBtn.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select Book Cover File");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("image files (*.png, *.jpg)", "*.png", "*.jpg"));
            File selectedFile = chooser.showOpenDialog(stage);
            if (selectedFile != null) {
                String filePath = selectedFile.getAbsolutePath();
                publishedBookCoverPath.setText(filePath);
            }
        });
        Button viewChangeLogBtn = new Button("View Change Logs");
        viewChangeLogBtn.getStyleClass().add("primary-btn");
        viewChangeLogBtn.setOnAction(event -> handleViewChangeLogs());

        //adjust the length of description text field dynamically
        publishedBookDescription.textProperty().addListener((obs, oldText, newText) -> {
            double width = Math.clamp((long)newText.length() * 8 + 20, 100, 500);
            publishedBookDescription.setPrefWidth(width);
        });

        Button modifyBtn = new Button("Modify Book");
        modifyBtn.getStyleClass().add("primary-btn");
        modifyBtn.setOnAction(event -> handleModifyBook());
        Button createBtn = new Button("Create Book");
        createBtn.getStyleClass().add("primary-btn");
        createBtn.setOnAction(event -> handleCreateBook());
        publishedBookGenerateButton = new Button("Generate Description");
        publishedBookGenerateButton.getStyleClass().add("primary-btn");
        publishedBookGenerateButton.setOnAction(event -> handleGenerateDescription());

        HBox fields1 = new HBox(5);
        HBox fields2 = new HBox(5);
        HBox actions = new HBox(5);
        fields1.getChildren().add(publishedBookSelectedId);
        fields1.getChildren().add(publishedBookTitle);
        fields1.getChildren().add(publishedBookAuthorName);
        fields1.getChildren().add(publishedBookGenre);
        fields2.getChildren().add(publishedBookDescription);
        fields2.getChildren().add(publishedBookFilePath);
        fields2.getChildren().add(selectBookFileBtn);
        fields2.getChildren().add(publishedBookCoverPath);
        fields2.getChildren().add(selectBookCoverBtn);
        actions.getChildren().add(modifyBtn);
        actions.getChildren().add(createBtn);
        actions.getChildren().add(publishedBookGenerateButton);

        card.getChildren().addAll(heading, hint, fields1, fields2, actions);

        VBox.setVgrow(publishedBooksTable, Priority.ALWAYS);
        wrapper.getChildren().addAll(card, heading, publishedBooksTable);

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
        Button publishedBooks = new Button("Published Books");
        Button logoutBtn = new Button("Logout");
        Button crashBtn = new Button("Crash");

        acceptReject.getStyleClass().add("primary-btn");
        acceptReject.setOnAction(event -> { stage.setScene(acceptRejectScene); refreshSubmissions(); });
        acceptReject.setPrefWidth(180);
        profile.getStyleClass().add("primary-btn");
        profile.setOnAction(event -> stage.setScene(profileScene));
        profile.setPrefWidth(180);
        notification.getStyleClass().add("primary-btn");
        notification.setOnAction(event -> { stage.setScene(notificationScene); refreshNotifications(); });
        notification.setPrefWidth(140);
        manageUsers.getStyleClass().add("primary-btn");
        manageUsers.setOnAction(event -> { stage.setScene(manageUsersScene); refreshEditUsers(); });
        manageUsers.setPrefWidth(140);
        borrowedBooks.getStyleClass().add("primary-btn");
        borrowedBooks.setOnAction(event -> { stage.setScene(borrowedBooksScene); refreshBorrowedBooks(); refreshBookRequests(); });
        borrowedBooks.setPrefWidth(140);
        publishedBooks.getStyleClass().add("primary-btn");
        publishedBooks.setOnAction(event -> { stage.setScene(publishedBooksScene); refreshPublishedBooks(); });
        publishedBooks.setPrefWidth(140);
        logoutBtn.getStyleClass().add("secondary-btn");
        logoutBtn.setOnAction(event -> handleLogout());
        crashBtn.getStyleClass().add("secondary-btn");
        crashBtn.setOnAction(event -> { exit(); });

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
            break;
            case 5: publishedBooks.setDisable(true);
            break;
        }

        selector.getChildren().addAll(acceptReject, profile, notification, manageUsers, borrowedBooks, publishedBooks, logoutBtn, crashBtn);

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
        loginPasswordField.clear();
        stage.setScene(acceptRejectScene);
        showTask3NewNotificationPopup();
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

        //Manage multiple submissions at once, get their IDs
        String[] IDRaw = approveSubmissionIdField.getText().split(",");
        String[] IDs = new String[IDRaw.length];
        for (int i = 0; i < IDRaw.length; i++)  IDs[i] = IDRaw[i].trim();

        //Check whether there are invalid IDs, if there are, show error message for the first invalid one
        for (String subId : IDs) {
            LibrarianPortalService.OperationResult result = portalService.validateBookSubmissionId(subId);
            if (!result.success()) {
                showErrorPopup("Approve Submission", "Action failed", result.message());
                return;
            }
        }

        //Show all the information in the confirmation popup
        StringBuilder allConfirmDetails = new StringBuilder();
        for (String subId : IDs) {
            allConfirmDetails.append(portalService.getConfirmDetail(subId));
            allConfirmDetails.append("\n\n");//Separate information by an empty line
        }
        allConfirmDetails.delete(allConfirmDetails.length() - 2, allConfirmDetails.length());
        if (!showConfirmPopup(
                "Confirm Approval",
                "Approve these book submission(s)?",
                allConfirmDetails.toString()
        )) return;

        //If at least one operation fails, show all the error messages in the popup
        StringBuilder failInfo = new StringBuilder();
        for (String subId : IDs) {
            LibrarianPortalService.OperationResult result = portalService.approveBookSubmission(subId, currentUser.username());
            if (!result.success()) {
                failInfo.append(result.message());
                failInfo.append("\n\n");
            }
        }
        if (failInfo.isEmpty()) setStatus("Approve successful: The selected submissions are all approved.");
        else {
            failInfo.delete(failInfo.length() - 2, failInfo.length());
            showErrorPopup("Approve Submission", "At least one action failed", failInfo.toString());
            setStatus("Approve failed: At least one of the selected submissions cannot be approved.");
        }
        refreshSubmissions();
        approveSubmissionIdField.clear();
    }

    private void handleReject() {
        if (currentUser == null) {
            showErrorPopup("Reject Submission", "User not logged in", "Please log in first.");
            return;
        }

        //Manage multiple submissions at once, get their IDs
        String[] IDRaw = approveSubmissionIdField.getText().split(",");
        String[] IDs = new String[IDRaw.length];
        for (int i = 0; i < IDRaw.length; i++) IDs[i] = IDRaw[i].trim();

        //Check whether there are invalid IDs, if there are, show error message for the first invalid one
        for (String subId : IDs) {
            LibrarianPortalService.OperationResult result = portalService.validateBookSubmissionId(subId);
            if (!result.success()) {
                showErrorPopup("Reject Submission", "Action failed", result.message());
                return;
            }
        }

        //Show all the information in the confirmation popup
        String rReason = rejectReasonField.getText();
        StringBuilder allConfirmDetails = new StringBuilder();
        for (String subId : IDs) {
            allConfirmDetails.append(portalService.getConfirmDetail(subId));
            allConfirmDetails.append("\n\n");//Separate information by an empty line
        }
        allConfirmDetails.delete(allConfirmDetails.length() - 2, allConfirmDetails.length());
        if (!showConfirmPopup(
                "Confirm Rejection",
                "Reject these book submission(s)?",
                allConfirmDetails.toString() + "\n\nRejection reason: " + (rReason.isEmpty() ? "Empty" : rReason
        ))) return;

        //If at least one operation fails, show all the error messages in the popup
        StringBuilder failInfo = new StringBuilder();
        for (String subId : IDs) {
            LibrarianPortalService.OperationResult result = portalService.rejectBookSubmission(subId, currentUser.username(), rReason);
            if (!result.success()) {
                failInfo.append(result.message());
                failInfo.append("\n\n");
            }
        }
        if (failInfo.isEmpty()) setStatus("Rejection successful: The selected submissions are all rejected.");
        else {
            failInfo.delete(failInfo.length() - 2, failInfo.length());
            showErrorPopup("Rejection Submission", "At least one action failed", failInfo.toString());
            setStatus("Rejection failed: At least one of the selected submissions cannot be rejected.");
        }
        refreshSubmissions();
        approveSubmissionIdField.clear();
        rejectReasonField.clear();
    }

    private void handlePreviewFile() {
        if (currentUser == null) {
            showErrorPopup("Reject Submission", "User not logged in", "Please log in first.");
            return;
        }

        //Manage multiple submissions at once, get their IDs
        String[] IDRaw = approveSubmissionIdField.getText().split(",");
        String[] IDs = new String[IDRaw.length];
        for (int i = 0; i < IDRaw.length; i++) IDs[i] = IDRaw[i].trim();

        //Check whether there are invalid IDs, if there are, show error message for the first invalid one
        for (String subId : IDs) {
            LibrarianPortalService.OperationResult result = portalService.validateBookSubmissionId(subId);
            if (!result.success()) {
                showErrorPopup("Reject Submission", "Action failed", result.message());
                return;
            }
        }

        //If at least one operation fails, show all the error messages in the popup
        StringBuilder failInfo = new StringBuilder();
        for (String subId : IDs) {
            LibrarianPortalService.OperationResult result = portalService.previewBookSubmission(subId);
            if (!result.success()) {
                failInfo.append(result.message());
                failInfo.append("\n\n");
            }
        }
        if (failInfo.isEmpty()) setStatus("Preview successful: All the files were opened.");
        else {
            failInfo.delete(failInfo.length() - 2, failInfo.length());
            showErrorPopup("Preview Submission", "At least one action failed", failInfo.toString());
            setStatus("Preview failed: At least one of the selected submissions cannot be previewed.");
        }
    }

    private void handleProfileUpdate() {
        if (currentUser == null) {
            showErrorPopup("Profile Update", "User not logged in.", "Please log in first.");
            return;
        }

        String password = profilePasswordField == null ? "" : profilePasswordField.getText();
        String passwordConfirm = profileConfirmPasswordField == null ? "" : profileConfirmPasswordField.getText();

        //If the password will be changed, let the user confirm first as it may logout the account.
        if (!(password.isEmpty() && passwordConfirm.isEmpty())) {
            if (!showConfirmPopup(
                    "Profile Update",
                    "Update the profile with password changed?",
                    "This action may logout your account. Proceed?")) return;
        }

        LibrarianPortalService.OperationResult result = portalService.updateProfile(
                currentUser.username(),
                profileFullNameField == null ? "" : profileFullNameField.getText(),
                profileOldPasswordField == null ? "" : profileOldPasswordField.getText(),
                password,
                passwordConfirm,
                profileEmployeeIDField == null ? "" : profileEmployeeIDField.getText(),
                profilePicturePathField == null ? "" : profilePicturePathField.getText()
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
        if (profileOldPasswordField != null) profileOldPasswordField.clear();
        if (profilePasswordField != null) profilePasswordField.clear();
        if (profileConfirmPasswordField != null) profileConfirmPasswordField.clear();
        if (profileEmployeeIDField != null) profileEmployeeIDField.clear();
        //If the password is changed, logout automatically, show popup otherwise.
        if (result.message().equals("Profile updated successfully with password changed.")) handleLogout();
        else showInfoPopup("Profile Update", "Success", result.message());
    }

    private void handleProfilePictureUpdate() {
        try {
            String path = profilePicturePathField.getText();
            if (Files.size(Path.of(path)) > 3 * 1024 * 1024) {
                showErrorPopup("Set Profile Picture", "Action Failed", "The picture file should be smaller than 5MB.");
                return;
            }
            String url = "file:" + path;
            Image img = path.isEmpty() ? null : new Image(url);
            profilePicture.setImage(img);
            profilePicture.setFitWidth(300);
            profilePicture.setFitHeight(200);
            profilePicture.setPreserveRatio(true);

        } catch (Exception e) {
            showErrorPopup("Set Profile Picture", "Action Failed", "File does not exist or has invalid format.");
            profilePicturePathField.setText("");
            profilePicture.setImage(null);
        }
    }

    private void handleUserEdit() {
        if (currentUser == null) {
            showErrorPopup("Edit User", "No user logged in.", "Please log in first.");
            return;
        }

        String[] nameRaw = manageUsersSelectedName.getText().split(",");
        String[] names = new String[nameRaw.length];
        for (int i = 0; i < nameRaw.length; i++) {
            names[i] = nameRaw[i].trim();
            if (currentUser.username().equals(names[i])) {
                showErrorPopup("Edit User", "Unsupported action.", "Please edit your account in the Personal Profile page.");
                return;
            }
        }

        for (String name : names) {
            LibrarianPortalService.OperationResult result = portalService.validateUsername(name);
            if (!result.success()) {
                showErrorPopup("Edit User", "Action failed", result.message());
                return;
            }
        }

        StringBuilder allConfirmDetails = new StringBuilder();
        for (String name : names) {
            allConfirmDetails.append(portalService.getUserEditConfirmDetail(name, manageUsersNewFullName.getText()));
            allConfirmDetails.append("\n\n");//Separate information by an empty line
        }
        allConfirmDetails.delete(allConfirmDetails.length() - 2, allConfirmDetails.length());
        if (!showConfirmPopup(
                "Edit User",
                "Edit these user(s)?",
                allConfirmDetails.toString())) return;

        //If at least one operation fails, show all the error messages in the popup
        StringBuilder failInfo = new StringBuilder();
        for (String name : names) {
            LibrarianPortalService.OperationResult result = portalService.editUserAccount(name,
                    manageUsersNewFullName.getText(), manageUsersNewPassword.getText(), manageUsersNewPasswordConfirm.getText());
            if (!result.success()) {
                failInfo.append(result.message());
                failInfo.append("\n\n");
            }
        }
        if (failInfo.isEmpty()) setStatus("Edit successful: The selected user accounts are all edited.");
        else {
            failInfo.delete(failInfo.length() - 2, failInfo.length());
            showErrorPopup("Edit User", "At least one action failed", failInfo.toString());
            setStatus("Edit failed: At least one of the selected user accounts cannot be edited.");
        }

        refreshEditUsers();
    }

    private void handleUserCreate() {
        if (currentUser == null) {
            showErrorPopup("Create User", "No user logged in.", "Please log in first.");
            return;
        }

        String type = createUsersType.getValue();
        String username = createUsersUsername.getText().trim();
        String fullName = createUsersFullName.getText().trim();
        String password = createUsersPassword.getText().trim();
        String passwordConfirm = createUsersPasswordConfirm.getText().trim();
        String additionalPrompt = createUsersBioOrEmployeeId.getPromptText();
        String bioOrEid = createUsersBioOrEmployeeId.getText().trim();
        if (!showConfirmPopup(
                "Create User",
                "Create the user?",
                "Type" + type +
                "\nUsername: " + username +
                "\nFull Name: " + fullName +
                (additionalPrompt.isEmpty() ? "" : '\n' + additionalPrompt + ": " + bioOrEid))) return;

        LibrarianPortalService.OperationResult result = portalService.createUser(
                type,
                username,
                fullName,
                password,
                passwordConfirm,
                bioOrEid
        );
        if (result.success()) setStatus("Creation successful: The user \"" + username + "\" was created.");
        else {
            showErrorPopup("Create User", "Action failed", result.message());
            setStatus(result.message());
        }

        refreshEditUsers();
    }

    private void handleDisableUser() {
        if (currentUser == null) {
            showErrorPopup("Disable User", "No user logged in.", "Please log in first.");
            return;
        }

        String[] nameRaw = manageUsersSelectedName.getText().split(",");
        String[] names = new String[nameRaw.length];
        for (int i = 0; i < nameRaw.length; i++) {
            names[i] = nameRaw[i].trim();
            if (currentUser.username().equals(names[i])) {
                showErrorPopup("Disable User", "Unsupported action.", "You cannot disable yourself.");
                return;
            }
        }

        for (String name : names) {
            LibrarianPortalService.OperationResult result = portalService.validateDisabledUsername(name);
            if (!result.success()) {
                showErrorPopup("Disable User", "Action failed", result.message());
                return;
            }
        }

        StringBuilder allConfirmDetails = new StringBuilder();
        for (String name : names) {
            allConfirmDetails.append(portalService.getUserDisableConfirmDetail(name));
            allConfirmDetails.append("\n\n");//Separate information by an empty line
        }
        allConfirmDetails.delete(allConfirmDetails.length() - 2, allConfirmDetails.length());
        if (!showConfirmPopup(
                "Disable User",
                "Disable these user(s)?",
                allConfirmDetails.toString())) return;

        //If at least one operation fails, show all the error messages in the popup
        StringBuilder failInfo = new StringBuilder();
        for (String name : names) {
            LibrarianPortalService.OperationResult result = portalService.disableUser(name);
            if (!result.success()) {
                failInfo.append(result.message());
                failInfo.append("\n\n");
            }
        }
        if (failInfo.isEmpty()) setStatus("Disable successful: The selected user accounts are all disabled.");
        else {
            failInfo.delete(failInfo.length() - 2, failInfo.length());
            showErrorPopup("Disable User", "At least one action failed", failInfo.toString());
            setStatus("Disable failed: At least one of the selected user accounts cannot be disabled.");
        }

        refreshEditUsers();
    }

    private void handleActivateUser() {
        if (currentUser == null) {
            showErrorPopup("Activate User", "No user logged in.", "Please log in first.");
            return;
        }

        String[] nameRaw = manageUsersSelectedName.getText().split(",");
        String[] names = new String[nameRaw.length];
        for (int i = 0; i < nameRaw.length; i++) names[i] = nameRaw[i].trim();

        for (String name : names) {
            LibrarianPortalService.OperationResult result = portalService.validateActivatedUsername(name);
            if (!result.success()) {
                showErrorPopup("Activate User", "Action failed", result.message());
                return;
            }
        }

        StringBuilder allConfirmDetails = new StringBuilder();
        for (String name : names) {
            allConfirmDetails.append(portalService.getUserDisableConfirmDetail(name));
            allConfirmDetails.append("\n\n");//Separate information by an empty line
        }
        allConfirmDetails.delete(allConfirmDetails.length() - 2, allConfirmDetails.length());
        if (!showConfirmPopup(
                "Activate User",
                "Activate these user(s)?",
                allConfirmDetails.toString())) return;

        //If at least one operation fails, show all the error messages in the popup
        StringBuilder failInfo = new StringBuilder();
        for (String name : names) {
            LibrarianPortalService.OperationResult result = portalService.activateUser(name);
            if (!result.success()) {
                failInfo.append(result.message());
                failInfo.append("\n\n");
            }
        }
        if (failInfo.isEmpty()) setStatus("Activation successful: The selected user accounts are all activated.");
        else {
            failInfo.delete(failInfo.length() - 2, failInfo.length());
            showErrorPopup("Activate User", "At least one action failed", failInfo.toString());
            setStatus("Activation failed: At least one of the selected user accounts cannot be activated.");
        }

        refreshEditUsers();
    }

    private void handleExportBorrowedBooks() {
        FileChooser chooser = new FileChooser();//Let user select file to save first
        chooser.setTitle("Export Borrowed Books Records");
        chooser.setInitialFileName("Records.xls");
        java.io.File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        LibrarianPortalService.OperationResult result = portalService.exportBorrowedBooksData(
                file,
                bookTableTitleFilter.getText(),
                bookTableBorrowedByFilter.getText(),
                bookTableStatusFilter.getValue()
        );
        if (!result.success()) showErrorPopup("Export Borrowed Books", "Action failed", result.message());
        setStatus(result.message());
    }

    private void handleModifyBook() {
        if (currentUser == null) {
            showErrorPopup("Modify Book", "User not logged in", "Please log in first.");
            return;
        }

        String[] IDRaw = publishedBookSelectedId.getText().split(",");
        String[] IDs = new String[IDRaw.length];
        for (int i = 0; i < IDRaw.length; i++)  IDs[i] = IDRaw[i].trim();

        for (String bookId : IDs) {
            LibrarianPortalService.OperationResult result = portalService.validateBookId(bookId);
            if (!result.success()) {
                showErrorPopup("Modify Book", "Action failed", result.message());
                return;
            }
        }

        //If at least one operation fails, show all the error messages in the popup
        StringBuilder failInfo = new StringBuilder();
        for (String subId : IDs) {
            LibrarianPortalService.OperationResult result = portalService.modifyBook(
                    subId,
                    publishedBookTitle.getText(),
                    publishedBookAuthorName.getText(),
                    publishedBookGenre.getText(),
                    publishedBookDescription.getText(),
                    publishedBookFilePath.getText(),
                    publishedBookCoverPath.getText());
            if (!result.success()) {
                failInfo.append(result.message());
                failInfo.append("\n\n");
            }
        }
        if (failInfo.isEmpty()) setStatus("Modification successful: The selected books are all modified.");
        else {
            failInfo.delete(failInfo.length() - 2, failInfo.length());
            showErrorPopup("Modify Book", "At least one action failed", failInfo.toString());
            setStatus("Modification failed: At least one of the selected books cannot be modified.");
        }
        refreshPublishedBooks();
        approveSubmissionIdField.clear();
    }

    private void handleCreateBook() {
        LibrarianPortalService.OperationResult result = portalService.createBook(publishedBookTitle.getText(),
                publishedBookAuthorName.getText(),
                publishedBookGenre.getText(),
                publishedBookDescription.getText(),
                publishedBookFilePath.getText(),
                publishedBookCoverPath.getText());

        setStatus(result.message());
    }

    private class generateThread extends Thread {
        public generateThread(String filePath, TextField resultField, Button generateButton, String recoverText) {
            this.filePath = filePath;
            this.resultField = resultField;
            this.generateButton = generateButton;
            this.recoverText = recoverText;
        }

        @Override
        public void run() {
            try {
                String result = new SummaryGenerator().generate(filePath);

                Platform.runLater(() -> {
                    resultField.setText(result);
                    generateButton.setText(recoverText);
                    generateButton.setDisable(false);
                    setStatus("Successfully generated description for the book file.");
                });
            }
            catch (Exception e) {
                Platform.runLater(() -> {
                    setStatus(e.getMessage());
                    showErrorPopup("Generate Description", "Action Failed", "The file is invalid, or internet issues.");
                });
            }
        }

        private final String filePath;
        private final TextField resultField;
        private final Button generateButton;
        private final String recoverText;
    }
    private void handleGenerateDescription() {
        String path = publishedBookFilePath.getText();
        if (path.isEmpty()) showErrorPopup("Generate Description", "Action Failed", "Please select a file to generate first.");

        String recoverText = publishedBookGenerateButton.getText();
        publishedBookGenerateButton.setText("Generating...");
        publishedBookGenerateButton.setDisable(true);
        new generateThread(path, publishedBookDescription, publishedBookGenerateButton, recoverText).start();
    }

    private void handleDownloadBook() {
        String requestId = requestActionIdField == null ? "" : requestActionIdField.getText().trim();
        if (requestId.isEmpty()) {
            showErrorPopup("Download Requested Book", "Request ID required.", "Select a request or type a request ID.");
            return;
        }

        Desktop desktop = Desktop.getDesktop();
        String link;
        try {
            link = BookDownloadHelper.getDownloadURL(portalService.getBookRequestTitle(requestId));
            if (!link.isEmpty()) desktop.browse(new URI(link));
        } catch (Exception e) {
            showErrorPopup("Download Book", "Action Failed", "Could not access target webpage.");
            setStatus("Download failed: Unable to access webpage.");
            return;
        }
        if (link.isEmpty()) {
            showErrorPopup("Download Book", "Action Failed", "Could not find available books.");
            setStatus("Download failed: No available books found.");
        }
        else setStatus("Download successful.");
    }

    private void handleViewChangeLogs() {
        String[] vals = publishedBookSelectedId.getText().split(",");
        if (vals.length > 1) {
            showErrorPopup("View Change Logs", "Action failed", "You can only view one book's change log at once.");
            return;
        }
        String id = vals[0];
        if (id.isEmpty()) {
            showErrorPopup("View Change Logs", "Action failed", "Please select a book to view.");
            return;
        }
        String logs = portalService.getBookChangeLog(id);
        if (logs == null) {
            showErrorPopup("View Change Logs", "Action failed", "Invalid Book Id, or the book was not changed.");
            return;
        }
        showInfoPopup("Change Logs", "Change Log for book with id " + id, logs);
    }


    private void refreshBorrowedBooks() {
        borrowedBooksTable.setItems(FXCollections.observableArrayList(
                portalService.getBorrowedBookRecords(
                        bookTableTitleFilter.getText(),
                        bookTableBorrowedByFilter.getText(),
                        bookTableStatusFilter.getValue())));
    }

    private void refreshBookRequests() {
        if (bookRequestTable == null) {
            return;
        }
        String status = bookRequestStatusFilter == null ? "ALL" : bookRequestStatusFilter.getValue();
        String keyword = bookRequestKeywordFilter == null ? "" : bookRequestKeywordFilter.getText();
        bookRequestTable.setItems(FXCollections.observableArrayList(
                portalService.getBookRequests(status, keyword)
        ));
    }

    private void handleBookRequestAction() {
        if (currentUser == null) {
            showErrorPopup("Book Request", "No user logged in.", "Please log in first.");
            return;
        }
        String requestId = requestActionIdField == null ? "" : requestActionIdField.getText().trim();
        if (requestId.isEmpty()) {
            showErrorPopup("Book Request", "Request ID required.", "Select a request or type a request ID.");
            return;
        }
        String action = requestActionTypeBox == null ? "APPROVE" : requestActionTypeBox.getValue();
        String comment = requestActionCommentField == null ? "" : requestActionCommentField.getText();
        LibrarianPortalService.OperationResult result;
        if ("REJECT".equalsIgnoreCase(action)) {
            result = portalService.rejectBookRequest(requestId, currentUser.username(), comment);
        } else {
            result = portalService.approveBookRequest(requestId, currentUser.username(), comment);
        }
        setStatus(result.message());
        if (!result.success()) {
            showErrorPopup("Book Request", "Action failed", result.message());
            return;
        }
        if (requestActionCommentField != null) requestActionCommentField.clear();
        if (requestActionIdField != null) requestActionIdField.clear();
        refreshBookRequests();
        refreshBorrowedBooks();
    }

    private void refreshPublishedBooks() {
        publishedBooksTable.setItems(FXCollections.observableArrayList(portalService.getPublishedBooksScreenData()));
    }

    private void refreshEditUsers() {
        for (LibrarianPortalService.BorrowedBookRecordView b : portalService.getBorrowedBookRecords("", "", "")) {
            AtomicInteger v = manageUsersBorrowCounts.get(b.borrowerUsername());
            if (v == null) manageUsersBorrowCounts.put(b.borrowerUsername(), new AtomicInteger(1));
            else v.set(v.intValue() + 1);
        }
        allUsersTable.setItems(FXCollections.observableArrayList(portalService.getUsersScreenData(manageUsersType.getValue())));
        allUsersTable.refresh();
    }

    private void refreshSubmissions() {
        LocalDate mind = tableSubmissionMin.getValue();
        LocalDate maxd = tableSubmissionMax.getValue();
        List<BookSubmission> l = portalService.getBookSubmissionScreenData(tableTitleFilter.getText(), tableAuthorUsernameFilter.getText(), tableGenreFilter.getText(), mind != null ? mind.atStartOfDay() : null, maxd != null ? tableSubmissionMax.getValue().atTime(23, 59) : null, tableStatusFilter.getValue());
        bookSubmissionTable.setItems(FXCollections.observableArrayList(l));
    }

    private void refreshNotifications() {
        if (notificationList == null || currentUser == null) return;
        LocalDate minDate = notificationDateMin.getValue();
        LocalDate maxDate = notificationDateMax.getValue();
        List<LibrarianPortalService.NotificationView> rows = portalService.getNotificationBoard(
                        currentUser.username(),
                        notificationCategoryFilter.getValue(),
                        minDate == null ? null : minDate.atStartOfDay(),
                        maxDate == null ? null : maxDate.atTime(23, 59, 59),
                        notificationUrgencyFilter.getValue());
        if (rows.isEmpty()) {
            notificationList.setItems(FXCollections.observableArrayList());
            return;
        }
        notificationList.setItems(FXCollections.observableArrayList(rows));
    }

    private void refreshNewBookRequests() {
        //TO DO: implement this
    }


    private String formatNotificationRow(LocalDateTime timestamp, String category, String message) {
        String c = category == null ? "" : category.toUpperCase();
        boolean urgent = c.equals("NEW_BOOK_SUBMISSION")
                || c.equals("USER_ACCOUNT_UPDATE")
                || c.equals("BOOK_REJECTED")
                || c.equals("RESPONSE")
                || c.contains("URGENT");
        String urgentTag = urgent ? "[URGENT] " : "";
        return urgentTag
                + "[" + timestamp.toLocalDate() + " " + timestamp.toLocalTime().withNano(0) + "] "
                + "[" + category + "] " + message;
    }

    private void handleMarkTask3NotificationRead() {
        if (currentUser == null || notificationList == null) {
            return;
        }
        LibrarianPortalService.NotificationView selected = notificationList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorPopup("Notifications", "No selection.", "Select a notification first.");
            return;
        }
        portalService.markNotificationAsRead(currentUser.username(), selected.notificationId());
        refreshNotifications();
    }

    private void handleDeleteTask3Notification() {
        if (currentUser == null || notificationList == null) {
            return;
        }
        LibrarianPortalService.NotificationView selected = notificationList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorPopup("Notifications", "No selection.", "Select a notification first.");
            return;
        }
        portalService.deleteNotification(currentUser.username(), selected.notificationId());
        refreshNotifications();
    }

    private void handleDeleteTask3ReadNotifications() {
        if (currentUser == null) {
            return;
        }
        portalService.deleteReadNotifications(currentUser.username());
        refreshNotifications();
    }

    private void showTask3NewNotificationPopup() {
        if (currentUser == null) {
            return;
        }
        List<LibrarianPortalService.NotificationView> unread = portalService.getUnreadNotifications(currentUser.username(), 5);
        if (unread.isEmpty()) {
            return;
        }
        String content = unread.stream()
                .map(n -> "[" + n.category() + "] " + n.message())
                .collect(java.util.stream.Collectors.joining("\n\n"));
        showInfoPopup(
                "New Notifications",
                "You have " + unread.size() + " unread notification(s)",
                content
        );
    }

    private void setStatus(String message) {
        registerStatusLabel.setText(message);
        loginStatusLabel.setText(message);
        approveStatusLabel.setText(message);
        profileStatusLabel.setText(message);
        notificationStatusLabel.setText(message);
        manageUsersStatusLabel.setText(message);
        bookTableStatusLabel.setText(message);
        publishedBookStatusLabel.setText(message);
        //requestStatusLabel.setText(message);
    }

    private void updateRegisterPasswordHint() {
        updatePasswordHint(registerPasswordField, registerConfirmPasswordField, registerPasswordHintLabel);
    }

    private void updateProfilePasswordHint() {
        updatePasswordHint(profilePasswordField, profileConfirmPasswordField, profilePasswordHintLabel);
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


    private String getFieldValue(String fieldName) {
        switch (fieldName) {
            case "currentUser" -> { return currentUser == null ? "" : currentUser.username(); }
            case "currentScene" -> {
                Scene scene = stage.getScene();
                if (scene == loginRegisterScene) return "loginRegister";
                if (scene == registerScene) return "register";
                if (scene == acceptRejectScene) return "acceptReject";
                if (scene == profileScene) return "profile";
                if (scene == notificationScene) return "notification";
                if (scene == manageUsersScene) return "manageUsers";
                if (scene == borrowedBooksScene) return "borrowedBooks";
                return null;
            }

            case "tableTitleFilter" -> { return tableTitleFilter.getText(); }
            case "tableAuthorUsernameFilter" -> { return tableAuthorUsernameFilter.getText(); }
            case "tableGenreFilter" -> { return tableGenreFilter.getText(); }
            case "tableSubmissionMin" -> {
                LocalDate v = tableSubmissionMin.getValue();
                return v == null ? "" : v.toString();
            }
            case "tableSubmissionMax" -> {
                LocalDate v = tableSubmissionMax.getValue();
                return v == null ? "" : v.toString();
            }
            case "tableStatusFilter" -> { return tableStatusFilter.getValue(); }

            case "approveSubmissionIdField" -> { return approveSubmissionIdField.getText(); }
            case "actionBox" -> { return actionBox.getValue(); }
            case "rejectReasonField" -> { return rejectReasonField.getText(); }

            case "registerUsernameField" -> { return registerUsernameField.getText(); }
            case "registerFullNameField" -> { return registerFullNameField.getText(); }
            case "registerPasswordField" -> { return registerPasswordField.getText(); }
            case "registerConfirmPasswordField" -> { return registerConfirmPasswordField.getText(); }
            case "registerStaffIDField" -> { return registerStaffIDField.getText(); }

            case "loginUsernameField" -> { return loginUsernameField.getText(); }
            case "loginPasswordField" -> { return loginPasswordField.getText(); }

            case "profileFullNameField" -> { return profileFullNameField.getText(); }
            case "profilePasswordField" -> { return profilePasswordField.getText(); }
            case "profileConfirmPasswordField" -> { return profileConfirmPasswordField.getText(); }
            case "profileEmployeeIDField" -> { return profileEmployeeIDField.getText(); }
            case "profileOldPasswordField" -> { return profileOldPasswordField.getText(); }

            case "manageUsersType" -> { return manageUsersType.getValue(); }
            case "manageUsersSelectedName" -> { return manageUsersSelectedName.getText(); }
            case "manageUsersNewFullName" -> { return manageUsersNewFullName.getText(); }
            case "manageUsersNewPassword" -> { return manageUsersNewPassword.getText(); }
            case "manageUsersNewPasswordConfirm" -> { return manageUsersNewPasswordConfirm.getText(); }

            case "createUsersType" -> { return createUsersType.getValue(); }
            case "createUsersUsername" -> { return createUsersUsername.getText(); }
            case "createUsersFullName" -> { return createUsersFullName.getText(); }
            case "createUsersPassword" -> { return createUsersPassword.getText(); }
            case "createUsersPasswordConfirm" -> { return createUsersPasswordConfirm.getText(); }
            case "createUsersBioOrEmployeeId" -> { return createUsersBioOrEmployeeId.getText(); }

            case "notificationCategoryFilter" -> { return notificationCategoryFilter.getValue(); }
            case "notificationDateMin" -> {
                LocalDate v = notificationDateMin.getValue();
                return v == null ? "" : v.toString();
            }
            case "notificationDateMax" -> {
                LocalDate v = notificationDateMax.getValue();
                return v == null ? "" : v.toString();
            }
            case "notificationUrgencyFilter" -> { return notificationUrgencyFilter.getValue(); }

            case "bookTableTitleFilter" -> { return bookTableTitleFilter.getText(); }
            case "bookTableBorrowedByFilter" -> { return bookTableBorrowedByFilter.getText(); }
            case "bookTableStatusFilter" -> { return bookTableStatusFilter.getValue(); }
        }
        return null;
    }
    private boolean setFieldValue(String fieldName, String value) {
        try {
            switch (fieldName) {
                case "currentUser" -> { if (currentUser == null) currentUser = portalService.getLibrarianPrinciple(value); }
                case "currentScene" -> {
                    switch (value) {
                        case "loginRegister" -> { stage.setScene(loginRegisterScene); }
                        case "register" -> { stage.setScene(registerScene); }
                        case "acceptReject" -> { stage.setScene(acceptRejectScene); refreshSubmissions(); }
                        case "profile" -> { stage.setScene(profileScene); }
                        case "notification" -> { stage.setScene(notificationScene); refreshNotifications(); }
                        case "manageUsers" -> { stage.setScene(manageUsersScene); refreshEditUsers(); }
                        case "borrowedBooks" -> { stage.setScene(borrowedBooksScene); refreshBorrowedBooks(); }
                    }
                }

                case "tableTitleFilter" -> { tableTitleFilter.setText(value); }
                case "tableAuthorUsernameFilter" -> { tableAuthorUsernameFilter.setText(value); }
                case "tableGenreFilter" -> { tableGenreFilter.setText(value); }
                case "tableSubmissionMin" -> { tableSubmissionMin.setValue(value.isEmpty() ? null : LocalDate.parse(value)); }
                case "tableSubmissionMax" -> { tableSubmissionMax.setValue(value.isEmpty() ? null : LocalDate.parse(value)); }
                case "tableStatusFilter" -> { tableStatusFilter.setValue(value); }

                case "approveSubmissionIdField" -> { approveSubmissionIdField.setText(value); }
                case "actionBox" -> { actionBox.setValue(value); }
                case "rejectReasonField" -> { rejectReasonField.setText(value); }

                case "registerUsernameField" -> { registerUsernameField.setText(value); }
                case "registerFullNameField" -> { registerFullNameField.setText(value); }
                case "registerPasswordField" -> { registerPasswordField.setText(value); }
                case "registerConfirmPasswordField" -> { registerConfirmPasswordField.setText(value); }
                case "registerStaffIDField" -> { registerStaffIDField.setText(value); }

                case "loginUsernameField" -> { loginUsernameField.setText(value); }
                case "loginPasswordField" -> { loginPasswordField.setText(value); }

                case "profileFullNameField" -> { profileFullNameField.setText(value); }
                case "profilePasswordField" -> { profilePasswordField.setText(value); }
                case "profileConfirmPasswordField" -> { profileConfirmPasswordField.setText(value); }
                case "profileEmployeeIDField" -> { profileEmployeeIDField.setText(value); }
                case "profileOldPasswordField" -> { profileOldPasswordField.setText(value); }

                case "manageUsersType" -> { manageUsersType.setValue(value); }
                case "manageUsersSelectedName" -> { manageUsersSelectedName.setText(value); }
                case "manageUsersNewFullName" -> { manageUsersNewFullName.setText(value); }
                case "manageUsersNewPassword" -> { manageUsersNewPassword.setText(value); }
                case "manageUsersNewPasswordConfirm" -> { manageUsersNewPasswordConfirm.setText(value); }

                case "createUsersType" -> { createUsersType.setValue(value); }
                case "createUsersUsername" -> { createUsersUsername.setText(value); }
                case "createUsersFullName" -> { createUsersFullName.setText(value); }
                case "createUsersPassword" -> { createUsersPassword.setText(value); }
                case "createUsersPasswordConfirm" -> { createUsersPasswordConfirm.setText(value); }
                case "createUsersBioOrEmployeeId" -> { createUsersBioOrEmployeeId.setText(value); }

                case "notificationCategoryFilter" -> { notificationCategoryFilter.setValue(value); }
                case "notificationDateMin" -> { notificationDateMin.setValue(value.isEmpty() ? null : LocalDate.parse(value)); }
                case "notificationDateMax" -> { notificationDateMax.setValue(value.isEmpty() ? null : LocalDate.parse(value)); }
                case "notificationUrgencyFilter" -> { notificationUrgencyFilter.setValue(value); }

                case "bookTableTitleFilter" -> { bookTableTitleFilter.setText(value); }
                case "bookTableBorrowedByFilter" -> { bookTableBorrowedByFilter.setText(value); }
                case "bookTableStatusFilter" -> { bookTableStatusFilter.setValue(value); }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private void stopAutoSave() {
        if (autoSaveTimer != null) {
            autoSaveTimer.cancel();
            autoSaveTimer = null;
        }
    }
}