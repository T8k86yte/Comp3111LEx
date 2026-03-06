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

public class PublishBookFX {
    private AuthorPortalService authorService;
    private AuthorAccount currentAuthor;
    private Stage stage;

    public PublishBookFX(AuthorAccount author) {
        this.currentAuthor = author;
        this.authorService = new AuthorPortalService();
        this.stage = new Stage();
    }

    public void show() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        HBox topBar = new HBox();
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: white; -fx-border-color: #dbe6f2; -fx-border-width: 0 0 1 0;");

        Label titleLabel = new Label("📚 Publish New Book");
        titleLabel.getStyleClass().add("page-title");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().addAll("button", "secondary-btn");
        closeBtn.setOnAction(e -> stage.close());

        topBar.getChildren().addAll(titleLabel, spacer, closeBtn);
        root.setTop(topBar);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");

        VBox centerContent = new VBox(20);
        centerContent.setAlignment(Pos.TOP_CENTER);
        centerContent.setPadding(new Insets(30));

        VBox infoCard = new VBox(10);
        infoCard.getStyleClass().add("card");
        infoCard.setMaxWidth(700);
        infoCard.setPadding(new Insets(20));

        Label infoTitle = new Label("📋 Book Submission Guidelines");
        infoTitle.getStyleClass().add("card-title");

        Label info1 = new Label("• Fill in all required fields marked with *");
        Label info2 = new Label("• Book file must be in PDF, TXT, DOC, or DOCX format");
        Label info3 = new Label("• Your book will be reviewed by a librarian");
        Label info4 = new Label("• You can track the status in 'My Submissions'");

        info1.getStyleClass().add("muted");
        info2.getStyleClass().add("muted");
        info3.getStyleClass().add("muted");
        info4.getStyleClass().add("muted");

        infoCard.getChildren().addAll(infoTitle, info1, info2, info3, info4);

        VBox formCard = new VBox(20);
        formCard.getStyleClass().add("card");
        formCard.setMaxWidth(700);
        formCard.setPadding(new Insets(30));

        Label formTitle = new Label("Book Details");
        formTitle.getStyleClass().add("card-title");

        VBox titleBox = new VBox(5);
        Label titleLabel2 = new Label("Book Title *");
        titleLabel2.getStyleClass().add("muted");
        TextField titleField = new TextField();
        titleField.setPromptText("Enter the title of your book");
        titleField.getStyleClass().add("text-field");
        titleBox.getChildren().addAll(titleLabel2, titleField);

        VBox authorBox = new VBox(5);
        Label authorLabel = new Label("Author Name");
        authorLabel.getStyleClass().add("muted");
        TextField authorField = new TextField(currentAuthor.getFullName());
        authorField.setEditable(false);
        authorField.getStyleClass().add("text-field");
        authorBox.getChildren().addAll(authorLabel, authorField);

        VBox genreBox = new VBox(5);
        Label genreLabel = new Label("Genre *");
        genreLabel.getStyleClass().add("muted");
        
        ComboBox<String> genreCombo = new ComboBox<>();
        genreCombo.getItems().addAll(
            "Fiction", "Non-Fiction", "Science Fiction", "Fantasy",
            "Mystery", "Biography", "History", "Technology", "Other"
        );
        genreCombo.setPromptText("Select a genre");
        genreCombo.getStyleClass().add("combo-box");
        genreCombo.setMaxWidth(Double.MAX_VALUE);
        genreBox.getChildren().addAll(genreLabel, genreCombo);

        VBox descBox = new VBox(5);
        Label descLabel = new Label("Description/Abstract *");
        descLabel.getStyleClass().add("muted");
        TextArea descArea = new TextArea();
        descArea.setPromptText("Write a brief description of your book");
        descArea.setPrefRowCount(6);
        descArea.setWrapText(true);
        descArea.getStyleClass().add("text-area");
        descBox.getChildren().addAll(descLabel, descArea);

        VBox fileBox = new VBox(5);
        Label fileLabel = new Label("Book File *");
        fileLabel.getStyleClass().add("muted");
        
        HBox fileInputBox = new HBox(10);
        TextField fileField = new TextField();
        fileField.setPromptText("Choose your book file (PDF, TXT, DOC, DOCX)");
        fileField.getStyleClass().add("text-field");
        fileField.setEditable(false);
        HBox.setHgrow(fileField, Priority.ALWAYS);

        Button browseBtn = new Button("Browse");
        browseBtn.getStyleClass().addAll("button", "secondary-btn");
        browseBtn.setPrefWidth(100);

        fileInputBox.getChildren().addAll(fileField, browseBtn);
        fileBox.getChildren().addAll(fileLabel, fileInputBox);

        Label selectedFileLabel = new Label();
        selectedFileLabel.getStyleClass().add("status-approved");
        selectedFileLabel.setVisible(false);

        Label messageLabel = new Label();
        messageLabel.setWrapText(true);
        messageLabel.setVisible(false);

        Button submitBtn = new Button("Submit for Approval");
        submitBtn.getStyleClass().addAll("button", "primary-btn");
        submitBtn.setMaxWidth(300);
        submitBtn.setPrefHeight(40);

        formCard.getChildren().addAll(formTitle, titleBox, authorBox, genreBox, 
                                      descBox, fileBox, selectedFileLabel, 
                                      messageLabel, submitBtn);

        centerContent.getChildren().addAll(infoCard, formCard);
        scrollPane.setContent(centerContent);
        root.setCenter(scrollPane);

        browseBtn.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select Book File");
            
            // Add filters for allowed file types
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
                
                // Validate file type
                String fileName = selectedFile.getName();
                if (isValidFileType(fileName)) {
                    selectedFileLabel.setText("✅ Selected: " + fileName);
                    selectedFileLabel.getStyleClass().setAll("status", "status-approved");
                    selectedFileLabel.setVisible(true);
                } else {
                    selectedFileLabel.setText("Invalid file type. Allowed: PDF, TXT, DOC, DOCX");
                    selectedFileLabel.getStyleClass().setAll("status", "status-rejected");
                    selectedFileLabel.setVisible(true);
                    fileField.clear();
                }
            }
        });

        submitBtn.setOnAction(e -> {
            String title = titleField.getText().trim();
            String genre = genreCombo.getValue();
            String description = descArea.getText().trim();
            String filePath = fileField.getText().trim();

            if (title.isEmpty()) {
                showMessage(messageLabel, "Book title is required", "status-rejected");
                return;
            }
            if (genre == null || genre.isEmpty()) {
                showMessage(messageLabel, "Genre is required", "status-rejected");
                return;
            }
            if (description.isEmpty()) {
                showMessage(messageLabel, "Description is required", "status-rejected");
                return;
            }
            if (filePath.isEmpty()) {
                showMessage(messageLabel, "Book file is required", "status-rejected");
                return;
            }

            // Submit book
            AuthorPortalService.SubmissionResult result = authorService.submitBookForApproval(
                currentAuthor.getUsername(),
                currentAuthor.getFullName(),
                title,
                genre,
                description,
                filePath
            );

            if (result.isSuccess()) {
                showMessage(messageLabel,result.getMessage(), "status-approved");
                
                // Clear form
                titleField.clear();
                genreCombo.setValue(null);
                descArea.clear();
                fileField.clear();
                selectedFileLabel.setVisible(false);
            } else {
                showMessage(messageLabel, result.getMessage(), "status-rejected");
            }
        });

        Scene scene = new Scene(root, 800, 700);
        scene.getStylesheets().add(getClass().getResource("/project/task2/css/author-portal.css").toExternalForm());
        
        stage.setTitle("Publish New Book");
        stage.setScene(scene);
        stage.show();
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
}
