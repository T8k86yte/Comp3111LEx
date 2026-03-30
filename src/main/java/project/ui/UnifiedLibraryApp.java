package project.ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import project.task1.ui.StudentStaffPortalApp;
import project.task2.ui.javafx.AuthorLoginFX;
import project.task3.ui.LibrarianPortalApp;
import project.shared.SessionRecoveryStore;

import java.util.Optional;

public class UnifiedLibraryApp extends Application {
    @Override
    public void start(Stage stage) {
        VBox root = new VBox(18);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("root-pane");

        Label title = new Label("Library System - Unified Portal");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Open Student/Staff, Author, and Librarian functions from one application.");
        subtitle.getStyleClass().add("page-subtitle");

        VBox cards = new VBox(12,
                buildPortalCard(
                        "Student/Staff Portal",
                        "Registration, login, available books, borrow/return, recommendations.",
                        this::openTask1Portal
                ),
                buildPortalCard(
                        "Author Portal",
                        "Author registration/login, publish new book, preview, draft autosave.",
                        this::openTask2Portal
                ),
                buildPortalCard(
                        "Librarian Portal",
                        "Librarian registration/login, review submissions, approve/reject with filters.",
                        this::openTask3Portal
                )
        );
        Button crashBtn = new Button("Crash Test");
        crashBtn.getStyleClass().add("secondary-btn");
        crashBtn.setOnAction(e -> {
            SessionRecoveryStore.saveLastPortal("HOME");
            Runtime.getRuntime().halt(1);
        });
        Button randomCrashBtn = new Button("Random Crash Test (5-15s)");
        randomCrashBtn.getStyleClass().add("secondary-btn");
        randomCrashBtn.setOnAction(e -> {
            SessionRecoveryStore.saveLastPortal("HOME");
            Thread t = new Thread(() -> {
                try {
                    long delayMs = 5000L + (long) (Math.random() * 10000L);
                    Thread.sleep(delayMs);
                } catch (InterruptedException ignored) {
                }
                Runtime.getRuntime().halt(1);
            }, "random-crash-simulator");
            t.setDaemon(true);
            t.start();
            Alert scheduled = new Alert(Alert.AlertType.INFORMATION);
            scheduled.setTitle("Crash Simulator");
            scheduled.setHeaderText("Random crash scheduled");
            scheduled.setContentText("App will crash in 5-15 seconds for recovery testing.");
            scheduled.showAndWait();
        });
        cards.getChildren().addAll(crashBtn, randomCrashBtn);

        root.getChildren().addAll(title, subtitle, cards);
        Scene scene = new Scene(root, 900, 520);
        scene.getStylesheets().add(getClass().getResource("/project/task1/ui/light-theme.css").toExternalForm());

        stage.setTitle("Unified Library Application");
        stage.setScene(scene);
        stage.show();

        tryRestoreLastPortal();
    }

    private HBox buildPortalCard(String heading, String details, Runnable onOpen) {
        HBox card = new HBox(12);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(14));

        VBox text = new VBox(4);
        Label title = new Label(heading);
        title.getStyleClass().add("card-title");
        Label body = new Label(details);
        body.getStyleClass().add("muted");
        body.setWrapText(true);
        text.getChildren().addAll(title, body);
        HBox.setHgrow(text, Priority.ALWAYS);

        Button open = new Button("Open");
        open.getStyleClass().add("primary-btn");
        open.setOnAction(e -> onOpen.run());

        card.getChildren().addAll(text, open);
        return card;
    }

    private void openTask1Portal() {
        try {
            SessionRecoveryStore.saveLastPortal("TASK1");
            new StudentStaffPortalApp().start(new Stage());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openTask2Portal() {
        try {
            SessionRecoveryStore.saveLastPortal("TASK2");
            new AuthorLoginFX().start(new Stage());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openTask3Portal() {
        try {
            SessionRecoveryStore.saveLastPortal("TASK3");
            new LibrarianPortalApp().start(new Stage());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void tryRestoreLastPortal() {
        Optional<String> lastPortal = SessionRecoveryStore.loadLastPortal();
        if (lastPortal.isEmpty()) {
            return;
        }
        try {
            switch (lastPortal.get()) {
                case "TASK1" -> openTask1Portal();
                case "TASK2" -> openTask2Portal();
                case "TASK3" -> openTask3Portal();
                default -> {
                    return;
                }
            }
            Alert restored = new Alert(Alert.AlertType.INFORMATION);
            restored.setTitle("Session Recovery");
            restored.setHeaderText("Previous session restored");
            restored.setContentText("Recovered portal: " + lastPortal.get());
            restored.showAndWait();
        } catch (Exception ex) {
            Alert failed = new Alert(Alert.AlertType.ERROR);
            failed.setTitle("Session Recovery");
            failed.setHeaderText("Restore failed");
            failed.setContentText("Unable to restore last portal. Falling back to home screen.");
            failed.showAndWait();
        }
    }
}
