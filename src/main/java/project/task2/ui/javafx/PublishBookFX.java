package project.task2.ui.javafx;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import project.task2.model.AuthorAccount;
import project.task2.service.AuthorPortalService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Arrays;

public class PublishBookFX {
    private AuthorPortalService authorService;
    private AuthorAccount currentAuthor;
    private Stage stage;
    
    // Form fields
    private TextField titleField;
    private List<CheckBox> genreCheckBoxes;
    private TextArea descArea;
    private TextField fileField;
    private Label selectedFileLabel;
    private Label messageLabel;
    private Label draftIndicator;
    
    // Preview components
    private VBox previewCard;
    private Label previewTitle;
    private Label previewAuthor;
    private Label previewGenres;
    private Label previewDescription;
    private Label previewFile;
    
    // Draft auto-save
    private Timer autoSaveTimer;
    private boolean hasUnsavedChanges = false;
    private static final int AUTO_SAVE_DELAY = 5000; // 5 seconds
    private boolean isLoadingDraft = false; // Prevent auto-save while loading

    public PublishBookFX(AuthorAccount author) {
        this.currentAuthor = author;
        this.authorService = new AuthorPortalService();
        this.stage = new Stage();
        this.genreCheckBoxes = new ArrayList<>();
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        // Top bar - removed preview button
        HBox topBar = createTopBar();
        root.setTop(topBar);

        // Center content
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox centerContent = new VBox(20);
        centerContent.setAlignment(Pos.TOP_CENTER);
        centerContent.setPadding(new Insets(30));

        // Guidelines card
        centerContent.getChildren().add(createGuidelinesCard());

        // Form card
        VBox formCard = createFormCard();
        centerContent.getChildren().add(formCard);

        // Preview card (initially hidden)
        previewCard = createPreviewCard();
        previewCard.setVisible(false);
        centerContent.getChildren().add(previewCard);

        // Draft indicator
        draftIndicator = new Label("📝 Draft auto-saved");
        draftIndicator.getStyleClass().addAll("status", "status-approved");
        draftIndicator.setVisible(false);
        centerContent.getChildren().add(draftIndicator);

        scrollPane.setContent(centerContent);
        root.setCenter(scrollPane);

        // Load any existing draft
        loadDraft();

        // Setup auto-save
        setupAutoSave();

        Scene scene = new Scene(root, 850, 800);
        scene.getStylesheets().add(getClass().getResource("/project/task2/css/author-portal.css").toExternalForm());
        
        stage.setTitle("Publish New Book - " + currentAuthor.getFullName());
        stage.setScene(scene);
        stage.show();

        // Cleanup on close
        stage.setOnCloseRequest(e -> {
            if (autoSaveTimer != null) {
                autoSaveTimer.cancel();
            }
            saveDraft(); // Save one last time before closing
        });
    }

    private HBox createTopBar() {
        HBox topBar = new HBox();
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: white; -fx-border-color: #dbe6f2; -fx-border-width: 0 0 1 0;");

        Label titleLabel = new Label("📚 Publish New Book");
        titleLabel.getStyleClass().add("page-title");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Removed preview button - only close button remains
        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().addAll("button", "secondary-btn");
        closeBtn.setOnAction(e -> {
            saveDraft();
            stage.close();
        });

        topBar.getChildren().addAll(titleLabel, spacer, closeBtn);
        return topBar;
    }

    private VBox createGuidelinesCard() {
        VBox infoCard = new VBox(10);
        infoCard.getStyleClass().add("card");
        infoCard.setMaxWidth(700);
        infoCard.setPadding(new Insets(20));

        Label infoTitle = new Label("📋 Book Submission Guidelines");
        infoTitle.getStyleClass().add("card-title");

        Label info1 = new Label("• Fill in all required fields marked with *");
        Label info2 = new Label("• You can select multiple genres");
        Label info3 = new Label("• Book file must be in PDF, TXT, DOC, or DOCX format");
        Label info4 = new Label("• Your book will be reviewed by a librarian");
        Label info5 = new Label("• Form auto-saves every 5 seconds - you can close and return later");

        info1.getStyleClass().add("muted");
        info2.getStyleClass().add("muted");
        info3.getStyleClass().add("muted");
        info4.getStyleClass().add("muted");
        info5.getStyleClass().add("muted");

        infoCard.getChildren().addAll(infoTitle, info1, info2, info3, info4, info5);
        return infoCard;
    }

    private VBox createFormCard() {
        VBox formCard = new VBox(20);
        formCard.getStyleClass().add("card");
        formCard.setMaxWidth(700);
        formCard.setPadding(new Insets(30));

        Label formTitle = new Label("Book Details");
        formTitle.getStyleClass().add("card-title");

        // Title field
        VBox titleBox = new VBox(5);
        Label titleLabel2 = new Label("Book Title *");
        titleLabel2.getStyleClass().add("muted");
        titleField = new TextField();
        titleField.setPromptText("Enter the title of your book");
        titleField.getStyleClass().add("text-field");
        titleField.textProperty().addListener((obs, old, newVal) -> {
            if (!isLoadingDraft) hasUnsavedChanges = true;
        });
        titleBox.getChildren().addAll(titleLabel2, titleField);

        // Author field (pre-filled)
        VBox authorBox = new VBox(5);
        Label authorLabel = new Label("Author Name");
        authorLabel.getStyleClass().add("muted");
        TextField authorField = new TextField(currentAuthor.getFullName());
        authorField.setEditable(false);
        authorField.getStyleClass().add("text-field");
        authorBox.getChildren().addAll(authorLabel, authorField);

        // Multiple Genre Selection
        VBox genreBox = new VBox(5);
        Label genreLabel = new Label("Genres * (select multiple)");
        genreLabel.getStyleClass().add("muted");
        
        FlowPane genreFlowPane = new FlowPane();
        genreFlowPane.setHgap(10);
        genreFlowPane.setVgap(10);
        genreFlowPane.setPadding(new Insets(10, 0, 10, 0));

        String[] genres = {"Fiction", "Non-Fiction", "Science Fiction", "Fantasy",
                          "Mystery", "Biography", "History", "Technology", "Romance",
                          "Thriller", "Poetry", "Children", "Young Adult", "Other"};

        for (String genre : genres) {
            CheckBox checkBox = new CheckBox(genre);
            checkBox.getStyleClass().add("muted");
            checkBox.setOnAction(e -> {
                if (!isLoadingDraft) hasUnsavedChanges = true;
            });
            genreCheckBoxes.add(checkBox);
            genreFlowPane.getChildren().add(checkBox);
        }

        genreBox.getChildren().addAll(genreLabel, genreFlowPane);

        // Description field
        VBox descBox = new VBox(5);
        Label descLabel = new Label("Description/Abstract *");
        descLabel.getStyleClass().add("muted");
        descArea = new TextArea();
        descArea.setPromptText("Write a brief description of your book");
        descArea.setPrefRowCount(6);
        descArea.setWrapText(true);
        descArea.getStyleClass().add("text-area");
        descArea.textProperty().addListener((obs, old, newVal) -> {
            if (!isLoadingDraft) hasUnsavedChanges = true;
        });
        descBox.getChildren().addAll(descLabel, descArea);

        // File upload field
        VBox fileBox = new VBox(5);
        Label fileLabel = new Label("Book File *");
        fileLabel.getStyleClass().add("muted");
        
        HBox fileInputBox = new HBox(10);
        fileField = new TextField();
        fileField.setPromptText("Choose your book file (PDF, TXT, DOC, DOCX)");
        fileField.getStyleClass().add("text-field");
        fileField.setEditable(false);
        HBox.setHgrow(fileField, Priority.ALWAYS);

        Button browseBtn = new Button("Browse");
        browseBtn.getStyleClass().addAll("button", "secondary-btn");
        browseBtn.setPrefWidth(100);

        fileInputBox.getChildren().addAll(fileField, browseBtn);
        fileBox.getChildren().addAll(fileLabel, fileInputBox);

        selectedFileLabel = new Label();
        selectedFileLabel.getStyleClass().add("status-approved");
        selectedFileLabel.setVisible(false);

        messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setVisible(false);

        // Action buttons
        HBox actionBox = new HBox(15);
        actionBox.setAlignment(Pos.CENTER);

        Button previewBtn = new Button("👁️ Preview Book");
        previewBtn.getStyleClass().addAll("button", "secondary-btn");
        previewBtn.setPrefWidth(150);
        previewBtn.setOnAction(e -> togglePreview());

        Button submitBtn = new Button("📤 Submit for Approval");
        submitBtn.getStyleClass().addAll("button", "primary-btn");
        submitBtn.setPrefWidth(200);
        submitBtn.setPrefHeight(40);

        Button clearDraftBtn = new Button("🗑️ Clear Draft");
        clearDraftBtn.getStyleClass().addAll("button", "danger-btn");
        clearDraftBtn.setPrefWidth(150);
        clearDraftBtn.setOnAction(e -> clearDraft());

        actionBox.getChildren().addAll(previewBtn, submitBtn, clearDraftBtn);

        formCard.getChildren().addAll(formTitle, titleBox, authorBox, genreBox, 
                                      descBox, fileBox, selectedFileLabel, 
                                      messageLabel, actionBox);

        // Browse button action
        browseBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Book File");
            
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("Word Documents", "*.doc", "*.docx"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
            );

            File selectedFile = fileChooser.showOpenDialog(stage);
            if (selectedFile != null) {
                String filePath = selectedFile.getAbsolutePath();
                fileField.setText(filePath);
                hasUnsavedChanges = true;
                
                String fileName = selectedFile.getName();
                if (isValidFileType(fileName)) {
                    selectedFileLabel.setText("✅ Selected: " + fileName);
                    selectedFileLabel.getStyleClass().setAll("status", "status-approved");
                    selectedFileLabel.setVisible(true);
                } else {
                    selectedFileLabel.setText("❌ Invalid file type. Allowed: PDF, TXT, DOC, DOCX");
                    selectedFileLabel.getStyleClass().setAll("status", "status-rejected");
                    selectedFileLabel.setVisible(true);
                    fileField.clear();
                }
            }
        });

        // Submit button action
        submitBtn.setOnAction(e -> {
            if (validateForm()) {
                submitBook();
            }
        });

        return formCard;
    }

    private VBox createPreviewCard() {
        VBox card = new VBox(15);
        card.getStyleClass().add("card");
        card.setMaxWidth(700);
        card.setPadding(new Insets(20));

        Label previewTitle2 = new Label("📖 Book Preview");
        previewTitle2.getStyleClass().add("card-title");

        previewTitle = new Label();
        previewTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        previewAuthor = new Label();
        previewAuthor.getStyleClass().add("muted");

        previewGenres = new Label();
        previewGenres.getStyleClass().add("muted");

        previewDescription = new Label();
        previewDescription.setWrapText(true);
        previewDescription.setStyle("-fx-padding: 10 0 0 0;");

        previewFile = new Label();
        previewFile.getStyleClass().add("muted");

        // Close preview button
        Button closePreviewBtn = new Button("Hide Preview");
        closePreviewBtn.getStyleClass().addAll("button", "secondary-btn");
        closePreviewBtn.setMaxWidth(200);
        closePreviewBtn.setOnAction(e -> previewCard.setVisible(false));

        card.getChildren().addAll(previewTitle2, previewTitle, previewAuthor, 
                                  previewGenres, previewDescription, previewFile, closePreviewBtn);
        return card;
    }

    private void togglePreview() {
        if (previewCard.isVisible()) {
            previewCard.setVisible(false);
        } else {
            updatePreview();
            previewCard.setVisible(true);
            
            // Scroll to preview
            ScrollPane scrollPane = (ScrollPane) previewCard.getScene().lookup(".scroll-pane");
            if (scrollPane != null) {
                scrollPane.setVvalue(1.0); // Scroll to bottom where preview is
            }
        }
    }

    private void updatePreview() {
        String title = titleField.getText().trim();
        List<String> selectedGenres = getSelectedGenres();
        String description = descArea.getText().trim();
        String file = fileField.getText().trim();

        previewTitle.setText(title.isEmpty() ? "[No title provided]" : "📌 " + title);
        previewAuthor.setText("✍️ By: " + currentAuthor.getFullName());
        previewGenres.setText("🏷️ Genres: " + (selectedGenres.isEmpty() ? "None selected" : 
                              String.join(", ", selectedGenres)));
        previewDescription.setText("📝 " + (description.isEmpty() ? "[No description provided]" : description));
        previewFile.setText("📁 File: " + (file.isEmpty() ? "No file selected" : file));
    }

    private boolean validateForm() {
        String title = titleField.getText().trim();
        List<String> selectedGenres = getSelectedGenres();
        String description = descArea.getText().trim();
        String filePath = fileField.getText().trim();

        if (title.isEmpty()) {
            showMessage(messageLabel, "❌ Book title is required", "status-rejected");
            return false;
        }
        if (selectedGenres.isEmpty()) {
            showMessage(messageLabel, "❌ Please select at least one genre", "status-rejected");
            return false;
        }
        if (description.isEmpty()) {
            showMessage(messageLabel, "❌ Description is required", "status-rejected");
            return false;
        }
        if (filePath.isEmpty()) {
            showMessage(messageLabel, "❌ Book file is required", "status-rejected");
            return false;
        }
        return true;
    }

    private void submitBook() {
        String title = titleField.getText().trim();
        List<String> selectedGenres = getSelectedGenres();
        String description = descArea.getText().trim();
        String filePath = fileField.getText().trim();

        AuthorPortalService.SubmissionResult result = authorService.submitBookForApproval(
            currentAuthor.getUsername(),
            currentAuthor.getFullName(),
            title,
            String.join(",", selectedGenres),
            description,
            filePath
        );

        if (result.isSuccess()) {
            showMessage(messageLabel, "✅ " + result.getMessage(), "status-approved");
            // Clear form
            titleField.clear();
            clearGenreSelections();
            descArea.clear();
            fileField.clear();
            selectedFileLabel.setVisible(false);
            hasUnsavedChanges = false;
            draftIndicator.setVisible(false);
            previewCard.setVisible(false);
            authorService.clearDraft(currentAuthor.getUsername());
        } else {
            showMessage(messageLabel, "❌ " + result.getMessage(), "status-rejected");
        }
    }

    private List<String> getSelectedGenres() {
        List<String> selected = new ArrayList<>();
        for (CheckBox checkBox : genreCheckBoxes) {
            if (checkBox.isSelected()) {
                selected.add(checkBox.getText());
            }
        }
        return selected;
    }

    private void setSelectedGenres(String genresStr) {
        if (genresStr == null || genresStr.isEmpty()) return;
        
        List<String> genres = Arrays.asList(genresStr.split(","));
        for (CheckBox checkBox : genreCheckBoxes) {
            if (genres.contains(checkBox.getText())) {
                checkBox.setSelected(true);
            }
        }
    }

    private void clearGenreSelections() {
        for (CheckBox checkBox : genreCheckBoxes) {
            checkBox.setSelected(false);
        }
    }

    private boolean isValidFileType(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".pdf") || lower.endsWith(".txt") || 
               lower.endsWith(".doc") || lower.endsWith(".docx");
    }

    private void showMessage(Label label, String message, String styleClass) {
        label.setText(message);
        label.getStyleClass().setAll("status", styleClass);
        label.setVisible(true);
    }

    // ========== AUTO-SAVE FEATURE ==========
    private void setupAutoSave() {
        autoSaveTimer = new Timer(true);
        autoSaveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (hasUnsavedChanges && !isLoadingDraft) {
                    javafx.application.Platform.runLater(() -> {
                        saveDraft();
                        hasUnsavedChanges = false;
                    });
                }
            }
        }, AUTO_SAVE_DELAY, AUTO_SAVE_DELAY);
    }

    private void saveDraft() {
        String title = titleField.getText().trim();
        List<String> selectedGenres = getSelectedGenres();
        String description = descArea.getText().trim();
        String filePath = fileField.getText().trim();

        // Save to service
        authorService.saveDraft(currentAuthor.getUsername(), title, selectedGenres, description, filePath);
        
        // Show indicator
        if (!title.isEmpty() || !selectedGenres.isEmpty() || !description.isEmpty() || !filePath.isEmpty()) {
            draftIndicator.setText("📝 Draft saved at " + 
                java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
            draftIndicator.setVisible(true);
            
            // Fade out after 3 seconds
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    javafx.application.Platform.runLater(() -> {
                        draftIndicator.setVisible(false);
                    });
                }
            }, 3000);
        }
    }

    private void loadDraft() {
        isLoadingDraft = true;
        
        String[] draft = authorService.loadDraft(currentAuthor.getUsername());
        if (draft != null && draft.length == 4) {
            // [title, genres, description, filePath]
            titleField.setText(draft[0]);
            setSelectedGenres(draft[1]);
            descArea.setText(draft[2]);
            if (!draft[3].isEmpty()) {
                fileField.setText(draft[3]);
                File file = new File(draft[3]);
                selectedFileLabel.setText("✅ Loaded draft: " + file.getName());
                selectedFileLabel.getStyleClass().setAll("status", "status-approved");
                selectedFileLabel.setVisible(true);
            }
            
            draftIndicator.setText("📝 Draft loaded from previous session");
            draftIndicator.getStyleClass().setAll("status", "status-approved");
            draftIndicator.setVisible(true);
            
            // Hide after 5 seconds
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    javafx.application.Platform.runLater(() -> {
                        draftIndicator.setVisible(false);
                    });
                }
            }, 5000);
        }
        
        isLoadingDraft = false;
    }

    private void clearDraft() {
        authorService.clearDraft(currentAuthor.getUsername());
        titleField.clear();
        clearGenreSelections();
        descArea.clear();
        fileField.clear();
        selectedFileLabel.setVisible(false);
        previewCard.setVisible(false);
        draftIndicator.setText("🗑️ Draft cleared");
        draftIndicator.getStyleClass().setAll("status", "status-rejected");
        draftIndicator.setVisible(true);
        
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                javafx.application.Platform.runLater(() -> {
                    draftIndicator.setVisible(false);
                    draftIndicator.getStyleClass().setAll("status", "status-approved");
                });
            }
        }, 3000);
    }
}
