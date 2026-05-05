package project.task1.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import project.task1.model.Book;
import project.task1.repo.InMemoryBookRepository;
import project.task1.repo.StudentStaffRepository;
import project.task1.service.StudentStaffPortalService;
import project.shared.SharedAuthFacade;
import project.shared.CrashSimulationManager;
import project.task2.repo.AuthorRepository;
import project.task2.utils.SessionManager;
import project.task3.repo.LibrarianRepository;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class StudentStaffPortalApp extends Application {
    private final StudentStaffRepository studentStaffRepository = new StudentStaffRepository();
    private final AuthorRepository authorRepository = new AuthorRepository();
    private final LibrarianRepository librarianRepository = new LibrarianRepository();
    private final StudentStaffPortalService portalService = new StudentStaffPortalService(
            studentStaffRepository,
            new InMemoryBookRepository(),
            authorRepository,
            librarianRepository
    );
    private final SharedAuthFacade authFacade = new SharedAuthFacade(
            studentStaffRepository,
            authorRepository,
            librarianRepository
    );

    private SharedAuthFacade.UserPrincipal currentUser;

    private BorderPane root;
    private Label statusLabel;
    private Label currentUserLabel;
    private VBox authPage;
    private BorderPane studentDashboard;
    private StackPane contentPane;

    private TextField loginUsernameField;
    private PasswordField loginPasswordField;
    private TextField registerUsernameField;
    private TextField registerFullNameField;
    private PasswordField registerPasswordField;
    private PasswordField registerConfirmPasswordField;
    private Label registerPasswordHintLabel;
    private ComboBox<String> registerRoleBox;

    private TableView<Book> bookTable;
    private TextField borrowBookIdField;
    private TextField borrowDurationField;
    private TextField returnBookIdField;
    private ListView<Book> returnBookListView;
    private VBox recommendationBox;
    private ListView<String> borrowHistoryList;
    private TableView<StudentStaffPortalService.BorrowRecordView> borrowedRecordTable;
    private TextField borrowedRecordFilterField;
    private TextField bookmarkField;
    private TextArea highlightField;
    private Label readingInfoLabel;
    private TextField profileFullNameField;
    private PasswordField profilePasswordField;
    private PasswordField profileConfirmPasswordField;
    private Label profilePasswordHintLabel;
    private ListView<StudentStaffPortalService.NotificationView> notificationList;
    private PasswordField profileCurrentPasswordField;
    private TextField bookFilterTitleField;
    private TextField bookFilterAuthorField;
    private TextField bookFilterGenreField;
    private DatePicker bookFilterPublishDatePicker;
    private ComboBox<String> bookFilterAvailabilityBox;
    private TextField notificationSearchField;
    private ComboBox<String> notificationCategoryFilterBox;
    private Timer autoSaveTimer;
    private boolean randomCrashEnabled;

    @Override
    public void start(Stage stage) {
        SessionManager.loadSession();
        root = new BorderPane();
        root.getStyleClass().add("root-pane");
        authPage = buildStudentLoginPage();
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

        if (SessionManager.hasSavedSession()) {
            showInfoPopup(
                    "Session Restored",
                    "Previous session detected",
                    "The app recovered your last session context. Please log in to continue."
            );
        }

        setStatus("Please log in or register.");
    }

    private VBox buildStudentLoginPage() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(50));
        page.setAlignment(Pos.CENTER);

        Label title = new Label("🎓 Student/Staff Portal");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Sign in with your student/staff account.");
        subtitle.getStyleClass().add("page-subtitle");
        page.getChildren().addAll(title, subtitle, buildStudentLoginCard());
        return page;
    }

    private VBox buildStudentLoginCard() {
        VBox card = new VBox(20);
        card.getStyleClass().add("card");
        card.setMaxWidth(400);
        card.setPadding(new Insets(30));

        Label heading = new Label("Student/Staff Login");
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

        Button loginBtn = new Button("Sign In");
        loginBtn.getStyleClass().add("primary-btn");
        loginBtn.setOnAction(event -> handleStudentLogin());
        loginBtn.setPrefWidth(150);

        Button registerBtn = new Button("Register");
        registerBtn.getStyleClass().add("secondary-btn");
        registerBtn.setOnAction(event -> root.setCenter(buildStudentRegisterPage()));
        registerBtn.setPrefWidth(150);

        HBox actions = new HBox(15, loginBtn, registerBtn);
        actions.setAlignment(Pos.CENTER);

        card.getChildren().addAll(heading, usernameBox, passwordBox, actions);
        return card;
    }

    private VBox buildStudentRegisterPage() {
        VBox page = new VBox(20);
        page.setPadding(new Insets(30));
        page.setAlignment(Pos.TOP_CENTER);

        Label title = new Label("📝 Student/Staff Registration");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Create a student or staff account.");
        subtitle.getStyleClass().add("page-subtitle");

        VBox card = new VBox(20);
        card.getStyleClass().add("card");
        card.setMaxWidth(500);
        card.setPadding(new Insets(30));

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
        registerRoleBox = new ComboBox<>(FXCollections.observableArrayList("Student", "Staff"));
        registerRoleBox.setValue("Student");
        registerRoleBox.setMaxWidth(Double.MAX_VALUE);

        grid.add(new Label("Username"), 0, 0);
        grid.add(registerUsernameField, 1, 0);
        grid.add(new Label("Full name"), 0, 1);
        grid.add(registerFullNameField, 1, 1);
        grid.add(new Label("Password"), 0, 2);
        grid.add(registerPasswordField, 1, 2);
        grid.add(new Label("Confirm password"), 0, 3);
        grid.add(registerConfirmPasswordField, 1, 3);
        grid.add(new Label("Role"), 0, 4);
        grid.add(registerRoleBox, 1, 4);

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
        registerPasswordField.textProperty().addListener((obs, oldValue, newValue) -> updateStudentPasswordHint());
        registerConfirmPasswordField.textProperty().addListener((obs, oldValue, newValue) -> updateStudentPasswordHint());
        updateStudentPasswordHint();

        Button submitBtn = new Button("Create Account");
        submitBtn.getStyleClass().add("primary-btn");
        submitBtn.setOnAction(event -> handleStudentRegister());
        submitBtn.setPrefWidth(180);

        Button backBtn = new Button("Back to Login");
        backBtn.getStyleClass().add("secondary-btn");
        backBtn.setOnAction(event -> root.setCenter(authPage));
        backBtn.setPrefWidth(180);

        HBox actions = new HBox(10, submitBtn, backBtn);
        actions.setAlignment(Pos.CENTER);

        card.getChildren().addAll(grid, registerPasswordHintLabel, reqBox, actions);
        page.getChildren().addAll(title, subtitle, card);
        return page;
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
        Button borrowedBtn = new Button("Borrowed Books");
        Button recBtn = new Button("Recommendations");
        Button historyBtn = new Button("Borrow History");
        Button profileBtn = new Button("Profile");
        Button noticeBtn = new Button("Notifications");
        Button crashBtn = new Button("Crash Test");
        Button randomCrashBtn = new Button("Random Crash: OFF");
        booksBtn.getStyleClass().add("secondary-btn");
        borrowBtn.getStyleClass().add("secondary-btn");
        returnBtn.getStyleClass().add("secondary-btn");
        borrowedBtn.getStyleClass().add("secondary-btn");
        recBtn.getStyleClass().add("secondary-btn");
        historyBtn.getStyleClass().add("secondary-btn");
        profileBtn.getStyleClass().add("secondary-btn");
        noticeBtn.getStyleClass().add("secondary-btn");
        crashBtn.getStyleClass().add("secondary-btn");
        randomCrashBtn.getStyleClass().add("secondary-btn");
        booksBtn.setMaxWidth(Double.MAX_VALUE);
        borrowBtn.setMaxWidth(Double.MAX_VALUE);
        returnBtn.setMaxWidth(Double.MAX_VALUE);
        borrowedBtn.setMaxWidth(Double.MAX_VALUE);
        recBtn.setMaxWidth(Double.MAX_VALUE);
        historyBtn.setMaxWidth(Double.MAX_VALUE);
        profileBtn.setMaxWidth(Double.MAX_VALUE);
        noticeBtn.setMaxWidth(Double.MAX_VALUE);
        crashBtn.setMaxWidth(Double.MAX_VALUE);
        randomCrashBtn.setMaxWidth(Double.MAX_VALUE);
        nav.getChildren().addAll(booksBtn, borrowBtn, returnBtn, borrowedBtn, recBtn, historyBtn, profileBtn, noticeBtn, crashBtn, randomCrashBtn);

        contentPane = new StackPane();
        contentPane.setPadding(new Insets(0, 0, 0, 12));

        booksBtn.setOnAction(e -> showBooksView());
        borrowBtn.setOnAction(e -> showBorrowView());
        returnBtn.setOnAction(e -> showReturnView());
        borrowedBtn.setOnAction(e -> showBorrowedBooksView());
        recBtn.setOnAction(e -> showRecommendationView());
        historyBtn.setOnAction(e -> showBorrowHistoryView());
        profileBtn.setOnAction(e -> showProfileView());
        noticeBtn.setOnAction(e -> showNotificationView());
        crashBtn.setOnAction(e -> handleCrashTest());
        randomCrashBtn.setOnAction(e -> {
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

        HBox filters = new HBox(8);
        bookFilterTitleField = new TextField();
        bookFilterTitleField.setPromptText("Title");
        bookFilterAuthorField = new TextField();
        bookFilterAuthorField.setPromptText("Author");
        bookFilterGenreField = new TextField();
        bookFilterGenreField.setPromptText("Genre");
        bookFilterPublishDatePicker = new DatePicker();
        bookFilterPublishDatePicker.setPromptText("Publish Date");
        bookFilterAvailabilityBox = new ComboBox<>(FXCollections.observableArrayList("All", "Available", "Unavailable"));
        bookFilterAvailabilityBox.setValue("All");
        Button applyFilterBtn = new Button("Apply Filter");
        applyFilterBtn.getStyleClass().add("secondary-btn");
        applyFilterBtn.setOnAction(e -> refreshBooks());
        filters.getChildren().addAll(
                new Label("Search:"),
                bookFilterTitleField,
                bookFilterAuthorField,
                bookFilterGenreField,
                bookFilterPublishDatePicker,
                bookFilterAvailabilityBox,
                applyFilterBtn
        );

        bookTable = new TableView<>();
        bookTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        bookTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<Book, String> idCol = new TableColumn<>("Book ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Book, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<Book, String> authorCol = new TableColumn<>("Author");
        authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));

        TableColumn<Book, String> genreCol = new TableColumn<>("Genre");
        genreCol.setCellValueFactory(new PropertyValueFactory<>("genre"));

        TableColumn<Book, Object> dateCol = new TableColumn<>("Publish Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("publishDate"));

        TableColumn<Book, String> availabilityCol = new TableColumn<>("Availability");
        availabilityCol.setCellValueFactory(cell -> {
            String text = cell.getValue().isAvailable() ? "Available" : "Unavailable";
            return new javafx.beans.property.SimpleStringProperty(text);
        });

        TableColumn<Book, String> summaryCol = new TableColumn<>("Summary");
        summaryCol.setCellValueFactory(new PropertyValueFactory<>("summary"));
        summaryCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                    return;
                }
                String compact = item.length() > 80 ? item.substring(0, 80) + "..." : item;
                setText(compact);
                setTooltip(new Tooltip(item));
            }
        });

        idCol.setPrefWidth(90);
        titleCol.setPrefWidth(220);
        authorCol.setPrefWidth(180);
        genreCol.setPrefWidth(120);
        dateCol.setPrefWidth(120);
        availabilityCol.setPrefWidth(120);
        summaryCol.setPrefWidth(260);

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
        Button quickReviewBtn = new Button("Quick Review");
        quickReviewBtn.getStyleClass().add("secondary-btn");
        quickReviewBtn.setOnAction(event -> handleQuickReview());
        Button borrowSelectedBtn = new Button("Borrow Selected");
        borrowSelectedBtn.getStyleClass().add("primary-btn");
        borrowSelectedBtn.setOnAction(event -> handleBorrowSelectedFromTable());

        bookTable.getColumns().addAll(idCol, titleCol, authorCol, genreCol, dateCol, availabilityCol, summaryCol);
        bookTable.setRowFactory(tv -> {
            TableRow<Book> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    showSummaryPopup(row.getItem());
                }
            });
            return row;
        });
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
        HBox actions = new HBox(10, readSummaryBtn, quickReviewBtn, borrowSelectedBtn);
        actions.setAlignment(Pos.CENTER_LEFT);
        wrapper.getChildren().addAll(heading, filters, bookTable, actions);
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
        borrowDurationField = new TextField();
        borrowDurationField.setPromptText("Borrow days (1-14, default 14)");
        Button borrowBtn = new Button("Borrow");
        borrowBtn.getStyleClass().add("primary-btn");
        borrowBtn.setOnAction(event -> handleBorrowBook());
        card.getChildren().addAll(heading, hint, borrowBookIdField, borrowDurationField, borrowBtn);
        return card;
    }

    private VBox buildReturnView() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        Label heading = new Label("Return Book");
        heading.getStyleClass().add("card-title");
        Label hint = new Label("Select book(s) currently borrowed by you, then return.");
        hint.getStyleClass().add("muted");

        returnBookListView = new ListView<>();
        returnBookListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        returnBookListView.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Book item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item.getId() + " - " + item.getTitle());
            }
        });

        returnBookIdField = new TextField();
        returnBookIdField.setPromptText("Optional manual Book ID (e.g. B001)");
        Button returnSelectedBtn = new Button("Return Selected");
        returnSelectedBtn.getStyleClass().add("primary-btn");
        returnSelectedBtn.setOnAction(event -> handleReturnSelectedFromList());
        Button returnBtn = new Button("Return");
        returnBtn.getStyleClass().add("primary-btn");
        returnBtn.setOnAction(event -> handleReturnBook());
        HBox actions = new HBox(10, returnSelectedBtn, returnBtn);
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(returnBookListView, Priority.ALWAYS);
        card.getChildren().addAll(heading, hint, returnBookListView, returnBookIdField, actions);
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

    private VBox buildBorrowHistoryView() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        Label heading = new Label("Borrow History");
        heading.getStyleClass().add("card-title");
        Label hint = new Label("Your previous borrow actions.");
        hint.getStyleClass().add("muted");
        borrowHistoryList = new ListView<>();
        VBox.setVgrow(borrowHistoryList, Priority.ALWAYS);
        card.getChildren().addAll(heading, hint, borrowHistoryList);
        return card;
    }

    private VBox buildBorrowedBooksView() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        Label heading = new Label("Borrowed Book Screen");
        heading.getStyleClass().add("card-title");
        Label hint = new Label("Read PDF, save bookmark/highlight, and return borrowed books.");
        hint.getStyleClass().add("muted");
        HBox filterRow = new HBox(8);
        borrowedRecordFilterField = new TextField();
        borrowedRecordFilterField.setPromptText("Filter by book title/id");
        Button applyFilterBtn = new Button("Apply Filter");
        applyFilterBtn.getStyleClass().add("secondary-btn");
        applyFilterBtn.setOnAction(e -> refreshBorrowedBookRecords());
        filterRow.getChildren().addAll(new Label("Filter:"), borrowedRecordFilterField, applyFilterBtn);

        borrowedRecordTable = new TableView<>();
        borrowedRecordTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        borrowedRecordTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<StudentStaffPortalService.BorrowRecordView, String> idCol = new TableColumn<>("Book ID");
        idCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().bookId()));
        TableColumn<StudentStaffPortalService.BorrowRecordView, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().bookTitle()));
        TableColumn<StudentStaffPortalService.BorrowRecordView, String> borrowDateCol = new TableColumn<>("Borrow Date");
        borrowDateCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().borrowDate().toString()));
        TableColumn<StudentStaffPortalService.BorrowRecordView, String> dueDateCol = new TableColumn<>("Due Date");
        dueDateCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().dueDate().toString()));
        TableColumn<StudentStaffPortalService.BorrowRecordView, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().status()));
        borrowedRecordTable.getColumns().addAll(idCol, titleCol, borrowDateCol, dueDateCol, statusCol);

        bookmarkField = new TextField();
        bookmarkField.setPromptText("Bookmark (e.g., page 42 / chapter marker)");
        highlightField = new TextArea();
        highlightField.setPromptText("Highlight note text");
        highlightField.setPrefRowCount(3);
        readingInfoLabel = new Label("Select one borrowed book to manage PDF/bookmark/highlight.");
        readingInfoLabel.getStyleClass().add("muted");

        Button openPdfBtn = new Button("Open PDF");
        openPdfBtn.getStyleClass().add("secondary-btn");
        openPdfBtn.setOnAction(e -> handleOpenPdfForBorrowedBook());
        Button bookmarkBtn = new Button("Save Bookmark");
        bookmarkBtn.getStyleClass().add("secondary-btn");
        bookmarkBtn.setOnAction(e -> handleSaveBookmarkForBorrowedBook());
        Button highlightBtn = new Button("Save Highlight");
        highlightBtn.getStyleClass().add("secondary-btn");
        highlightBtn.setOnAction(e -> handleSaveHighlightForBorrowedBook());
        Button returnSelectedBtn = new Button("Return Selected");
        returnSelectedBtn.getStyleClass().add("primary-btn");
        returnSelectedBtn.setOnAction(e -> handleReturnSelectedBorrowedRecords());

        borrowedRecordTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null || currentUser == null) {
                return;
            }
            String resolvedPdfPath = portalService.resolveBorrowedBookPdfPath(currentUser.username(), newValue.bookId());
            StudentStaffPortalService.ReadingProgressView reading = portalService.getReadingProgress(currentUser.username(), newValue.bookId());
            bookmarkField.setText(reading.bookmark());
            highlightField.setText(reading.highlightNotes());
            readingInfoLabel.setText(resolvedPdfPath.isBlank()
                    ? "No PDF linked yet for " + newValue.bookId() + "."
                    : "PDF linked: " + resolvedPdfPath);
        });

        HBox actions = new HBox(10, openPdfBtn, bookmarkBtn, highlightBtn, returnSelectedBtn);
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(borrowedRecordTable, Priority.ALWAYS);
        card.getChildren().addAll(heading, hint, filterRow, borrowedRecordTable, readingInfoLabel, bookmarkField, highlightField, actions);
        return card;
    }

    private VBox buildProfileView() {
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

    private VBox buildNotificationView() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        Label heading = new Label("Notification Board");
        heading.getStyleClass().add("card-title");
        Label hint = new Label("Timestamped and categorized notifications.");
        hint.getStyleClass().add("muted");
        HBox filterRow = new HBox(8);
        notificationSearchField = new TextField();
        notificationSearchField.setPromptText("Search notifications");
        notificationCategoryFilterBox = new ComboBox<>(FXCollections.observableArrayList(
                "All",
                "DUE_REMINDER",
                "AUTO_RETURN",
                "BOOK_RETURN",
                "BOOK_DELETION",
                "ANNOUNCEMENT"
        ));
        notificationCategoryFilterBox.setValue("All");
        Button applyBtn = new Button("Apply Filter");
        applyBtn.getStyleClass().add("secondary-btn");
        applyBtn.setOnAction(e -> refreshNotifications());
        filterRow.getChildren().addAll(new Label("Filter:"), notificationSearchField, notificationCategoryFilterBox, applyBtn);
        notificationList = new ListView<>();
        notificationList.setCellFactory(list -> new javafx.scene.control.ListCell<StudentStaffPortalService.NotificationView>() {
            @Override
            protected void updateItem(StudentStaffPortalService.NotificationView item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                String urgentTag = item.isUrgent() ? "[URGENT] " : "";
                String readTag = item.read() ? "" : "[NEW] ";
                setText(readTag + urgentTag
                        + "[" + item.timestamp().toLocalDate() + " " + item.timestamp().toLocalTime().withNano(0) + "] "
                        + "[" + item.category() + "] " + item.message());
            }
        });
        Button markReadBtn = new Button("Mark Selected Read");
        markReadBtn.getStyleClass().add("secondary-btn");
        markReadBtn.setOnAction(e -> handleMarkTask1NotificationRead());
        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.getStyleClass().add("secondary-btn");
        deleteBtn.setOnAction(e -> handleDeleteTask1Notification());
        Button deleteReadBtn = new Button("Delete Read");
        deleteReadBtn.getStyleClass().add("secondary-btn");
        deleteReadBtn.setOnAction(e -> handleDeleteTask1ReadNotifications());
        HBox actions = new HBox(8, markReadBtn, deleteBtn, deleteReadBtn);
        actions.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(notificationList, Priority.ALWAYS);
        card.getChildren().addAll(heading, hint, filterRow, notificationList, actions);
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
        SessionManager.setCurrentScreen("TASK1_BOOK_LIST", null);
        refreshBooks();
    }

    private void showBorrowView() {
        contentPane.getChildren().setAll(buildBorrowView());
        SessionManager.setCurrentScreen("TASK1_BORROW", null);
    }

    private void showReturnView() {
        contentPane.getChildren().setAll(buildReturnView());
        SessionManager.setCurrentScreen("TASK1_RETURN", null);
        refreshReturnBooks();
    }

    private void showRecommendationView() {
        refreshRecommendations();
        contentPane.getChildren().setAll(buildRecommendationView());
        SessionManager.setCurrentScreen("TASK1_RECOMMEND", null);
        refreshRecommendations();
    }

    private void showBorrowHistoryView() {
        contentPane.getChildren().setAll(buildBorrowHistoryView());
        SessionManager.setCurrentScreen("TASK1_HISTORY", null);
        refreshBorrowHistory();
    }

    private void showBorrowedBooksView() {
        contentPane.getChildren().setAll(buildBorrowedBooksView());
        SessionManager.setCurrentScreen("TASK1_BORROWED", null);
        refreshBorrowedBookRecords();
    }

    private void showProfileView() {
        contentPane.getChildren().setAll(buildProfileView());
        SessionManager.setCurrentScreen("TASK1_PROFILE", null);
    }

    private void showNotificationView() {
        contentPane.getChildren().setAll(buildNotificationView());
        SessionManager.setCurrentScreen("TASK1_NOTIFICATIONS", null);
        refreshNotifications();
    }

    private void handleStudentRegister() {
        String username = registerUsernameField.getText() == null ? "" : registerUsernameField.getText().trim();
        String fullName = registerFullNameField.getText() == null ? "" : registerFullNameField.getText().trim();
        String password = registerPasswordField.getText() == null ? "" : registerPasswordField.getText();
        String confirmPassword = registerConfirmPasswordField.getText() == null ? "" : registerConfirmPasswordField.getText();
        String role = registerRoleBox.getValue();

        if (username.isEmpty() || fullName.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showErrorPopup("Registration Failed", "Required fields are missing.", "Username, full name, password, and confirm password are required.");
            return;
        }
        if (!password.equals(confirmPassword)) {
            showErrorPopup("Registration Failed", "Password mismatch.", "Password and confirm password must match.");
            return;
        }

        SharedAuthFacade.AuthResult result = authFacade.register(
                username,
                fullName,
                password,
                confirmPassword,
                role,
                null,
                null
        );
        setStatus(result.message());
        if (result.success()) {
            registerUsernameField.clear();
            registerFullNameField.clear();
            registerPasswordField.clear();
            registerConfirmPasswordField.clear();
            root.setCenter(authPage);
            showInfoPopup("Registration", "Account created", result.message());
        } else {
            showErrorPopup("Registration Failed", "Unable to create account.", result.message());
        }
    }

    private void handleStudentLogin() {
        portalService.autoReturnExpiredBooks();
        SharedAuthFacade.AuthResult result =
                authFacade.login(loginUsernameField.getText(), loginPasswordField.getText(), "Student");
        if (!result.success()) {
            // Try staff automatically so login page keeps only username/password.
            result = authFacade.login(loginUsernameField.getText(), loginPasswordField.getText(), "Staff");
        }
        setStatus(result.message());
        if (!result.success()) {
            showErrorPopup("Login Failed", "Invalid credentials.", result.message());
            return;
        }
        currentUser = result.principal();
        if (!"STUDENT".equalsIgnoreCase(currentUser.role()) && !"STAFF".equalsIgnoreCase(currentUser.role())) {
            currentUser = null;
            showErrorPopup("Login Failed", "Unsupported role.", "This dashboard is only for student/staff users.");
            return;
        }
        currentUserLabel.setText("Current user: " + currentUser.username() + " (" + currentUser.role() + ")");
        String restoredScreen = SessionManager.getCurrentScreen();
        SessionManager.setCurrentUser(currentUser.username(), currentUser.fullName());
        if (restoredScreen == null || restoredScreen.isBlank()) {
            SessionManager.setCurrentScreen("TASK1_BOOK_LIST", null);
        }
        loginPasswordField.clear();
        root.setCenter(studentDashboard);
        boolean restored = restoreLastScreen(restoredScreen);
        if (restored) {
            showInfoPopup("Session Restore", "Restored successfully", "Recovered your last screen and saved in-progress data.");
        } else {
            showErrorPopup("Session Restore", "Restore failed", "Could not restore the previous screen. Returned to Book List.");
        }
        showTask1NewNotificationPopup();
        startAutoSave();
        refreshNotifications();
        showInfoPopup("Login", "Welcome", result.message());
    }

    private void handleReadSummary() {
        if (bookTable == null) {
            showErrorPopup("Read Summary", "Book list is not open.", "Open Book List first.");
            return;
        }
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorPopup("Read Summary", "No book selected.", "Select a book first.");
            return;
        }
        showSummaryPopup(selected);
    }

    private void handleQuickReview() {
        if (bookTable == null) {
            showErrorPopup("Quick Review", "Book list is not open.", "Open Book List first.");
            return;
        }
        Book selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorPopup("Quick Review", "No book selected.", "Select a book first.");
            return;
        }
        String summary = selected.getSummary() == null ? "" : selected.getSummary();
        String preview = summary.length() > 260 ? summary.substring(0, 260) + "..." : summary;
        showInfoPopup("Quick Review", selected.getTitle(), preview);
    }

    private void showSummaryPopup(Book selected) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Quick Summary");
        alert.setHeaderText(selected.getTitle() + " - " + selected.getAuthor());
        TextArea area = new TextArea(selected.getSummary());
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
        String username = currentUser.username();
        currentUser = null;
        currentUserLabel.setText("Current user: (none)");
        root.setCenter(authPage);
        stopAutoSave();
        CrashSimulationManager.disableRandomCrash();
        randomCrashEnabled = false;
        SessionManager.clearSession();
        setStatus("Logged out: " + username);
    }

    private void handleCrashTest() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Crash Test");
        confirm.setHeaderText("Simulate crash?");
        confirm.setContentText("This closes the app immediately. Reopen to test session restoration.");
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) {
            return;
        }
        persistSessionSnapshot();
        CrashSimulationManager.triggerCrashNow(this::persistSessionSnapshot);
    }

    private void handleBorrowBook() {
        if (currentUser == null) {
            showErrorPopup("Borrow Failed", "User not logged in.", "Please log in first.");
            return;
        }

        String bookId = borrowBookIdField.getText() == null ? "" : borrowBookIdField.getText().trim();
        if (bookId.isEmpty()) {
            showErrorPopup("Borrow Failed", "Book ID is required.", "Please provide a book ID.");
            return;
        }

        int borrowDays = parseBorrowDurationOrDefault();
        String confirmationDetails = portalService.buildBorrowConfirmation(currentUser.username(), bookId, borrowDays);
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Borrow");
        confirmation.setHeaderText("Borrow request for " + bookId.toUpperCase());
        confirmation.setContentText(confirmationDetails);
        Optional<ButtonType> choice = confirmation.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) {
            return;
        }

        StudentStaffPortalService.OperationResult result =
                portalService.borrowBook(currentUser.username(), bookId, borrowDays);
        setStatus(result.message());
        if (result.success()) {
            refreshBooks();
            refreshRecommendations();
            refreshReturnBooks();
            borrowBookIdField.clear();
            refreshBorrowHistory();
            showInfoPopup("Borrow", "Borrow successful", result.message());
        } else {
            showErrorPopup("Borrow Failed", "Unable to borrow book.", result.message());
        }
    }

    private void handleBorrowSelectedFromTable() {
        if (currentUser == null) {
            showErrorPopup("Borrow Failed", "User not logged in.", "Please log in first.");
            return;
        }
        if (bookTable == null) {
            showErrorPopup("Borrow Failed", "Book list is not open.", "Open Book List first.");
            return;
        }
        List<Book> selected = bookTable.getSelectionModel().getSelectedItems();
        if (selected == null || selected.isEmpty()) {
            showErrorPopup("Borrow Failed", "No books selected.", "Select one or more available books.");
            return;
        }

        int borrowDays = parseBorrowDurationOrDefault();
        List<Book> availableSelected = selected.stream().filter(Book::isAvailable).toList();
        if (availableSelected.isEmpty()) {
            showErrorPopup("Borrow Failed", "Selected books are unavailable.", "Please select available books only.");
            return;
        }

        StringBuilder detail = new StringBuilder("Books to borrow:\n");
        for (Book b : availableSelected) {
            detail.append("- ").append(b.getId()).append(" : ").append(b.getTitle()).append("\n");
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Borrow");
        confirm.setHeaderText("Borrow " + availableSelected.size() + " selected book(s)?");
        confirm.setContentText(detail.toString());
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) {
            return;
        }

        int okCount = 0;
        List<String> failures = new java.util.ArrayList<>();
        for (Book b : availableSelected) {
            StudentStaffPortalService.OperationResult result = portalService.borrowBook(currentUser.username(), b.getId(), borrowDays);
            if (result.success()) {
                okCount++;
            } else {
                failures.add(b.getId() + ": " + result.message());
            }
        }

        refreshBooks();
        refreshRecommendations();
        refreshReturnBooks();
        refreshBorrowHistory();
        if (okCount > 0 && failures.isEmpty()) {
            showInfoPopup("Borrow", "Borrow successful", "Successfully borrowed " + okCount + " book(s).");
            return;
        }
        if (okCount > 0) {
            showErrorPopup("Borrow Partial Success", "Borrowed " + okCount + " book(s).", String.join("\n", failures));
            return;
        }
        showErrorPopup("Borrow Failed", "No selected books were borrowed.", String.join("\n", failures));
    }

    private void handleReturnBook() {
        if (currentUser == null) {
            showErrorPopup("Return Failed", "User not logged in.", "Please log in first.");
            return;
        }
        String bookId = returnBookIdField.getText() == null ? "" : returnBookIdField.getText().trim();
        if (bookId.isEmpty()) {
            showErrorPopup("Return Failed", "Book ID is required.", "Please provide a book ID.");
            return;
        }

        StudentStaffPortalService.OperationResult result =
                portalService.returnBook(currentUser.username(), bookId);
        setStatus(result.message());
        if (result.success()) {
            refreshBooks();
            refreshRecommendations();
            refreshReturnBooks();
            returnBookIdField.clear();
            showInfoPopup("Return", "Return successful", result.message());
        } else {
            showErrorPopup("Return Failed", "Unable to return book.", result.message());
        }
    }

    private void handleReturnSelectedFromList() {
        if (currentUser == null) {
            showErrorPopup("Return Failed", "User not logged in.", "Please log in first.");
            return;
        }
        if (returnBookListView == null) {
            showErrorPopup("Return Failed", "Return list is not open.", "Open Return page first.");
            return;
        }
        List<Book> selected = returnBookListView.getSelectionModel().getSelectedItems();
        if (selected == null || selected.isEmpty()) {
            showErrorPopup("Return Failed", "No books selected.", "Select one or more borrowed books.");
            return;
        }

        StringBuilder detail = new StringBuilder("Books to return:\n");
        for (Book b : selected) {
            detail.append("- ").append(b.getId()).append(" : ").append(b.getTitle()).append("\n");
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Return");
        confirm.setHeaderText("Return " + selected.size() + " selected book(s)?");
        confirm.setContentText(detail.toString());
        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isEmpty() || choice.get() != ButtonType.OK) {
            return;
        }

        int okCount = 0;
        List<String> failures = new java.util.ArrayList<>();
        for (Book b : selected) {
            StudentStaffPortalService.OperationResult result = portalService.returnBook(currentUser.username(), b.getId());
            if (result.success()) {
                okCount++;
            } else {
                failures.add(b.getId() + ": " + result.message());
            }
        }

        refreshBooks();
        refreshRecommendations();
        refreshReturnBooks();
        if (okCount > 0 && failures.isEmpty()) {
            showInfoPopup("Return", "Return successful", "Successfully returned " + okCount + " book(s).");
            return;
        }
        if (okCount > 0) {
            showErrorPopup("Return Partial Success", "Returned " + okCount + " book(s).", String.join("\n", failures));
            return;
        }
        showErrorPopup("Return Failed", "No selected books were returned.", String.join("\n", failures));
    }

    private void handleOpenPdfForBorrowedBook() {
        StudentStaffPortalService.BorrowRecordView selected = getSingleSelectedBorrowedRecord();
        if (selected == null || currentUser == null) {
            return;
        }
        String resolvedPath = portalService.resolveBorrowedBookPdfPath(currentUser.username(), selected.bookId());
        if (resolvedPath == null || resolvedPath.isBlank()) {
            showErrorPopup(
                    "Open PDF Failed",
                    "No linked PDF found.",
                    "No associated file path is available for this borrowed book."
            );
            return;
        }
        File pdfFile = new File(resolvedPath);

        if (!pdfFile.exists()) {
            showErrorPopup("Open PDF Failed", "PDF file not found.", "Linked file path does not exist on this machine.");
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            showErrorPopup("Open PDF Failed", "Desktop open is unsupported.", "This machine cannot open PDF files from Java desktop API.");
            return;
        }
        try {
            Desktop.getDesktop().open(pdfFile);
            readingInfoLabel.setText("Opened PDF: " + pdfFile.getAbsolutePath());
        } catch (IOException e) {
            showErrorPopup("Open PDF Failed", "Cannot open PDF.", e.getMessage());
        }
    }

    private void handleSaveBookmarkForBorrowedBook() {
        StudentStaffPortalService.BorrowRecordView selected = getSingleSelectedBorrowedRecord();
        if (selected == null || currentUser == null) {
            return;
        }
        String value = bookmarkField == null ? "" : bookmarkField.getText().trim();
        if (value.isEmpty()) {
            value = "Auto bookmark @ " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            if (bookmarkField != null) {
                bookmarkField.setText(value);
            }
        }
        StudentStaffPortalService.OperationResult result =
                portalService.saveBookmark(currentUser.username(), selected.bookId(), value);
        if (result.success()) {
            showInfoPopup("Bookmark", "Saved", "Bookmark saved for " + selected.bookId() + ".");
        } else {
            showErrorPopup("Bookmark", "Save failed.", result.message());
        }
    }

    private void handleSaveHighlightForBorrowedBook() {
        StudentStaffPortalService.BorrowRecordView selected = getSingleSelectedBorrowedRecord();
        if (selected == null || currentUser == null) {
            return;
        }
        String value = highlightField == null ? "" : highlightField.getText().trim();
        if (value.isEmpty()) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            if (clipboard.hasString()) {
                value = clipboard.getString().trim();
                if (highlightField != null) {
                    highlightField.setText(value);
                }
            }
        }
        if (value.isEmpty()) {
            value = "Auto highlight @ " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        StudentStaffPortalService.OperationResult result =
                portalService.saveHighlight(currentUser.username(), selected.bookId(), value);
        if (result.success()) {
            showInfoPopup("Highlight", "Saved", "Highlight note saved for " + selected.bookId() + ".");
            highlightField.clear();
        } else {
            showErrorPopup("Highlight", "Save failed.", result.message());
        }
    }

    private void handleReturnSelectedBorrowedRecords() {
        if (currentUser == null || borrowedRecordTable == null) {
            showErrorPopup("Return Failed", "Borrowed book screen is not open.", "Open Borrowed Books first.");
            return;
        }
        List<StudentStaffPortalService.BorrowRecordView> selected = borrowedRecordTable.getSelectionModel().getSelectedItems();
        if (selected == null || selected.isEmpty()) {
            showErrorPopup("Return Failed", "No books selected.", "Select one or more borrowed books.");
            return;
        }

        int okCount = 0;
        List<String> failures = new java.util.ArrayList<>();
        for (StudentStaffPortalService.BorrowRecordView row : selected) {
            StudentStaffPortalService.OperationResult result = portalService.returnBook(currentUser.username(), row.bookId());
            if (result.success()) {
                okCount++;
            } else {
                failures.add(row.bookId() + ": " + result.message());
            }
        }
        refreshBooks();
        refreshReturnBooks();
        refreshBorrowedBookRecords();
        refreshRecommendations();
        if (okCount > 0 && failures.isEmpty()) {
            showInfoPopup("Return", "Return successful", "Successfully returned " + okCount + " book(s).");
            return;
        }
        if (okCount > 0) {
            showErrorPopup("Return Partial Success", "Returned " + okCount + " book(s).", String.join("\n", failures));
            return;
        }
        showErrorPopup("Return Failed", "No selected books were returned.", String.join("\n", failures));
    }

    private void handleProfileUpdate() {
        if (currentUser == null) {
            showErrorPopup("Profile Update", "User not logged in.", "Please log in first.");
            return;
        }
        StudentStaffPortalService.ProfileUpdateResult result = portalService.updateProfileWithReAuth(
                currentUser.username(),
                profileFullNameField == null ? "" : profileFullNameField.getText(),
                profileCurrentPasswordField == null ? "" : profileCurrentPasswordField.getText(),
                profilePasswordField == null ? "" : profilePasswordField.getText(),
                profileConfirmPasswordField == null ? "" : profileConfirmPasswordField.getText()
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
        if (result.passwordChanged()) {
            showInfoPopup("Profile Update", "Password changed", "Password updated. You will be logged out now.");
            handleLogout();
            return;
        }
        showInfoPopup("Profile Update", "Success", result.message());
    }

    private StudentStaffPortalService.BorrowRecordView getSingleSelectedBorrowedRecord() {
        if (borrowedRecordTable == null) {
            showErrorPopup("Borrowed Books", "Borrowed book screen is not open.", "Open Borrowed Books first.");
            return null;
        }
        StudentStaffPortalService.BorrowRecordView selected = borrowedRecordTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorPopup("Borrowed Books", "No book selected.", "Select a single borrowed book first.");
            return null;
        }
        return selected;
    }

    private int parseBorrowDurationOrDefault() {
        String raw = borrowDurationField == null ? "" : borrowDurationField.getText().trim();
        if (raw.isEmpty()) {
            return 14;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return 14;
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
        bookTable.setItems(FXCollections.observableArrayList(
                portalService.getBookScreenData(
                        bookFilterTitleField == null ? "" : bookFilterTitleField.getText(),
                        bookFilterAuthorField == null ? "" : bookFilterAuthorField.getText(),
                        bookFilterGenreField == null ? "" : bookFilterGenreField.getText(),
                        bookFilterPublishDatePicker == null ? null : bookFilterPublishDatePicker.getValue(),
                        bookFilterAvailabilityBox == null ? "All" : bookFilterAvailabilityBox.getValue()
                )
        ));
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

    private void refreshBorrowHistory() {
        if (borrowHistoryList == null || currentUser == null) {
            return;
        }
        List<String> history = portalService.getBorrowHistory(currentUser.username());
        if (history.isEmpty()) {
            borrowHistoryList.setItems(FXCollections.observableArrayList("No borrow history yet."));
            return;
        }
        borrowHistoryList.setItems(FXCollections.observableArrayList(history));
    }

    private void refreshBorrowedBookRecords() {
        if (borrowedRecordTable == null || currentUser == null) {
            return;
        }
        portalService.autoReturnExpiredBooks();
        String filter = borrowedRecordFilterField == null ? "" : borrowedRecordFilterField.getText().trim().toLowerCase();
        List<StudentStaffPortalService.BorrowRecordView> rows = portalService.getBorrowedBookRecords(currentUser.username())
                .stream()
                .filter(r -> filter.isEmpty()
                        || r.bookId().toLowerCase().contains(filter)
                        || r.bookTitle().toLowerCase().contains(filter))
                .toList();
        borrowedRecordTable.setItems(FXCollections.observableArrayList(rows));
    }

    private void refreshReturnBooks() {
        if (returnBookListView == null || currentUser == null) {
            return;
        }
        List<Book> borrowedByCurrentUser = portalService.getBookScreenData()
                .stream()
                .filter(book -> !book.isAvailable())
                .filter(book -> currentUser.username().equals(book.getBorrowedByUsername()))
                .toList();
        returnBookListView.setItems(FXCollections.observableArrayList(borrowedByCurrentUser));
    }

    private void refreshNotifications() {
        if (notificationList == null || currentUser == null) {
            return;
        }
        List<StudentStaffPortalService.NotificationView> rows = portalService.getNotificationBoard(
                        currentUser.username(),
                        notificationSearchField == null ? "" : notificationSearchField.getText(),
                        notificationCategoryFilterBox == null ? "All" : notificationCategoryFilterBox.getValue()
                );
        if (rows.isEmpty()) {
            notificationList.setItems(FXCollections.observableArrayList());
            return;
        }
        notificationList.setItems(FXCollections.observableArrayList(rows));
    }

    private void updateStudentPasswordHint() {
        if (registerPasswordHintLabel == null) {
            return;
        }
        String password = registerPasswordField == null ? "" : registerPasswordField.getText();
        String confirm = registerConfirmPasswordField == null ? "" : registerConfirmPasswordField.getText();
        if (password == null || password.isEmpty()) {
            registerPasswordHintLabel.setText("Password is required.");
            registerPasswordHintLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11px;");
            return;
        }
        boolean strong = password.length() >= 8
                && password.matches(".*[A-Za-z].*")
                && password.matches(".*\\d.*")
                && password.matches(".*[A-Z].*");
        if (!strong) {
            registerPasswordHintLabel.setText("Weak password: use at least 8 chars with letter, number, and uppercase.");
            registerPasswordHintLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11px;");
            return;
        }
        if (!confirm.isEmpty() && !password.equals(confirm)) {
            registerPasswordHintLabel.setText("Passwords do not match.");
            registerPasswordHintLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11px;");
            return;
        }
        registerPasswordHintLabel.setText("Strong password.");
        registerPasswordHintLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 11px;");
    }

    private void updateProfilePasswordHint() {
        if (profilePasswordHintLabel == null) {
            return;
        }
        String password = profilePasswordField == null ? "" : profilePasswordField.getText();
        String confirm = profileConfirmPasswordField == null ? "" : profileConfirmPasswordField.getText();
        if (password == null || password.isEmpty()) {
            profilePasswordHintLabel.setText("Leave password fields blank to keep current password.");
            profilePasswordHintLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
            return;
        }
        boolean strong = password.length() >= 8
                && password.matches(".*[A-Za-z].*")
                && password.matches(".*\\d.*")
                && password.matches(".*[A-Z].*");
        if (!strong) {
            profilePasswordHintLabel.setText("Weak password: use at least 8 chars with letter, number, and uppercase.");
            profilePasswordHintLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11px;");
            return;
        }
        if (!confirm.isEmpty() && !password.equals(confirm)) {
            profilePasswordHintLabel.setText("Passwords do not match.");
            profilePasswordHintLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11px;");
            return;
        }
        profilePasswordHintLabel.setText("Strong password.");
        profilePasswordHintLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 11px;");
    }

    private String formatNotificationRow(LocalDateTime timestamp, String category, String message) {
        boolean urgent = "AUTO_RETURN".equalsIgnoreCase(category)
                || "BOOK_DELETION".equalsIgnoreCase(category)
                || category.toUpperCase().contains("URGENT");
        String urgentTag = urgent ? "[URGENT] " : "";
        return urgentTag
                + "[" + timestamp.toLocalDate() + " " + timestamp.toLocalTime().withNano(0) + "] "
                + "[" + category + "] " + message;
    }

    private void handleMarkTask1NotificationRead() {
        if (currentUser == null || notificationList == null) {
            return;
        }
        StudentStaffPortalService.NotificationView selected = notificationList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorPopup("Notifications", "No selection.", "Select a notification first.");
            return;
        }
        if (selected.notificationId().startsWith("DUE-")) {
            showInfoPopup("Notifications", "Not applicable", "Due reminders are generated dynamically and cannot be marked as read.");
            return;
        }
        portalService.markNotificationAsRead(currentUser.username(), selected.notificationId());
        refreshNotifications();
    }

    private void handleDeleteTask1Notification() {
        if (currentUser == null || notificationList == null) {
            return;
        }
        StudentStaffPortalService.NotificationView selected = notificationList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showErrorPopup("Notifications", "No selection.", "Select a notification first.");
            return;
        }
        if (selected.notificationId().startsWith("DUE-")) {
            showInfoPopup("Notifications", "Not applicable", "Due reminders are generated dynamically and cannot be deleted.");
            return;
        }
        portalService.deleteNotification(currentUser.username(), selected.notificationId());
        refreshNotifications();
    }

    private void handleDeleteTask1ReadNotifications() {
        if (currentUser == null) {
            return;
        }
        portalService.deleteReadNotifications(currentUser.username());
        refreshNotifications();
    }

    private void showTask1NewNotificationPopup() {
        if (currentUser == null) {
            return;
        }
        List<StudentStaffPortalService.NotificationView> unread = portalService.getUnreadNotifications(currentUser.username(), 5);
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

    private boolean restoreLastScreen(String screen) {
        if (screen == null || screen.isBlank()) {
            showBooksView();
            return true;
        }
        switch (screen) {
            case "TASK1_BORROW" -> showBorrowView();
            case "TASK1_RETURN" -> showReturnView();
            case "TASK1_BORROWED" -> showBorrowedBooksView();
            case "TASK1_RECOMMEND" -> showRecommendationView();
            case "TASK1_HISTORY" -> showBorrowHistoryView();
            case "TASK1_PROFILE" -> showProfileView();
            case "TASK1_NOTIFICATIONS" -> showNotificationView();
            case "TASK1_BOOK_LIST" -> showBooksView();
            default -> {
                showBooksView();
                return false;
            }
        }
        applyScreenState(screen);
        return true;
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
        if (currentUser == null) {
            return;
        }
        String screen = SessionManager.getCurrentScreen();
        if (screen == null || screen.isBlank()) {
            return;
        }
        String state = switch (screen) {
            case "TASK1_BOOK_LIST" -> safe(bookFilterTitleField) + "|" + safe(bookFilterAuthorField) + "|" + safe(bookFilterGenreField)
                    + "|" + (bookFilterPublishDatePicker != null && bookFilterPublishDatePicker.getValue() != null ? bookFilterPublishDatePicker.getValue() : "")
                    + "|" + (bookFilterAvailabilityBox == null ? "All" : bookFilterAvailabilityBox.getValue());
            case "TASK1_BORROW" -> safe(borrowBookIdField) + "|" + safe(borrowDurationField);
            case "TASK1_RETURN" -> safe(returnBookIdField);
            case "TASK1_BORROWED" -> safe(borrowedRecordFilterField) + "|" + safe(bookmarkField) + "|" + safe(highlightField);
            case "TASK1_PROFILE" -> safe(profileFullNameField);
            case "TASK1_NOTIFICATIONS" -> safe(notificationSearchField) + "|" + (notificationCategoryFilterBox == null ? "All" : notificationCategoryFilterBox.getValue());
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
            case "TASK1_BOOK_LIST" -> {
                if (p.length > 0 && bookFilterTitleField != null) bookFilterTitleField.setText(p[0]);
                if (p.length > 1 && bookFilterAuthorField != null) bookFilterAuthorField.setText(p[1]);
                if (p.length > 2 && bookFilterGenreField != null) bookFilterGenreField.setText(p[2]);
                if (p.length > 3 && bookFilterPublishDatePicker != null && !p[3].isBlank()) bookFilterPublishDatePicker.setValue(java.time.LocalDate.parse(p[3]));
                if (p.length > 4 && bookFilterAvailabilityBox != null && !p[4].isBlank()) bookFilterAvailabilityBox.setValue(p[4]);
                refreshBooks();
            }
            case "TASK1_BORROW" -> {
                if (p.length > 0 && borrowBookIdField != null) borrowBookIdField.setText(p[0]);
                if (p.length > 1 && borrowDurationField != null) borrowDurationField.setText(p[1]);
            }
            case "TASK1_RETURN" -> {
                if (p.length > 0 && returnBookIdField != null) returnBookIdField.setText(p[0]);
            }
            case "TASK1_BORROWED" -> {
                if (p.length > 0 && borrowedRecordFilterField != null) borrowedRecordFilterField.setText(p[0]);
                if (p.length > 1 && bookmarkField != null) bookmarkField.setText(p[1]);
                if (p.length > 2 && highlightField != null) highlightField.setText(p[2]);
                refreshBorrowedBookRecords();
            }
            case "TASK1_PROFILE" -> {
                if (p.length > 0 && profileFullNameField != null) profileFullNameField.setText(p[0]);
            }
            case "TASK1_NOTIFICATIONS" -> {
                if (p.length > 0 && notificationSearchField != null) notificationSearchField.setText(p[0]);
                if (p.length > 1 && notificationCategoryFilterBox != null && !p[1].isBlank()) notificationCategoryFilterBox.setValue(p[1]);
                refreshNotifications();
            }
            default -> {
            }
        }
    }

    private String safe(TextField field) {
        return field == null || field.getText() == null ? "" : field.getText();
    }

    private String safe(TextArea field) {
        return field == null || field.getText() == null ? "" : field.getText();
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

    private void setStatus(String message) {
        statusLabel.setText(message);
    }
}
