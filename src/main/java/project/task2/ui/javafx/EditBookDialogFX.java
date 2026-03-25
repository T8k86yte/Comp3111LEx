package project.task2.ui.javafx;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import project.task2.model.BookSubmission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class EditBookDialogFX {
    private final BookSubmission original;

    public EditBookDialogFX(BookSubmission submission) {
        this.original = submission;
    }

    public Optional<BookSubmission> showAndWait() {
        Dialog<BookSubmission> dialog = new Dialog<>();
        dialog.setTitle("Edit Book");
        dialog.setHeaderText("Edit: " + original.getTitle());

        ButtonType saveButtonType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Title field
        TextField titleField = new TextField(original.getTitle());
        titleField.setPromptText("Book Title");
        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);

        // Genre selection (multiple)
        VBox genreBox = new VBox(5);
        Label genreLabel = new Label("Genres (select multiple):");
        
        List<String> selectedGenres = original.getGenres();
        String[] allGenres = {"Fiction", "Non-Fiction", "Science Fiction", "Fantasy",
                              "Mystery", "Biography", "History", "Technology", "Romance",
                              "Thriller", "Poetry", "Children", "Young Adult", "Other"};

        VBox checkBoxContainer = new VBox(5);
        List<CheckBox> checkBoxes = new ArrayList<>();
        for (String genre : allGenres) {
            CheckBox cb = new CheckBox(genre);
            cb.setSelected(selectedGenres.contains(genre));
            checkBoxes.add(cb);
            checkBoxContainer.getChildren().add(cb);
        }

        genreBox.getChildren().addAll(genreLabel, checkBoxContainer);
        grid.add(new Label("Genres:"), 0, 1);
        grid.add(genreBox, 1, 1);

        // Description field
        TextArea descArea = new TextArea(original.getDescription());
        descArea.setPrefRowCount(5);
        descArea.setWrapText(true);
        grid.add(new Label("Description:"), 0, 2);
        grid.add(descArea, 1, 2);

        // Show edit count if any
        if (original.getEditCount() > 0) {
            Label editInfo = new Label("Previously edited " + original.getEditCount() + " time(s)");
            editInfo.getStyleClass().add("muted");
            grid.add(editInfo, 1, 3);
        }

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String newTitle = titleField.getText().trim();
                if (newTitle.isEmpty()) {
                    showAlert("Error", "Title cannot be empty", Alert.AlertType.ERROR);
                    return null;
                }

                List<String> newGenres = checkBoxes.stream()
                    .filter(CheckBox::isSelected)
                    .map(CheckBox::getText)
                    .collect(Collectors.toList());

                if (newGenres.isEmpty()) {
                    showAlert("Error", "Please select at least one genre", Alert.AlertType.ERROR);
                    return null;
                }

                String newDescription = descArea.getText().trim();
                if (newDescription.isEmpty()) {
                    showAlert("Error", "Description cannot be empty", Alert.AlertType.ERROR);
                    return null;
                }

                // Create updated submission
                BookSubmission updated = new BookSubmission(
                    original.getSubmissionId(),
                    newTitle,
                    original.getAuthorUsername(),
                    original.getAuthorFullName(),
                    String.join(",", newGenres),
                    newDescription,
                    original.getFilePath(),
                    original.getSubmissionDate(),
                    original.getStatus(),
                    original.getRejectionReason(),
                    original.getReviewedDate(),
                    original.getReviewedBy(),
                    false,
                    original.getEditCount() + 1
                );
                return updated;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
