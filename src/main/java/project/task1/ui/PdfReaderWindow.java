package project.task1.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.embed.swing.SwingFXUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import project.task1.service.PdfReaderStateStore;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PdfReaderWindow {
    private final String username;
    private final String bookId;
    private final String bookTitle;
    private final String pdfPath;
    private final PdfReaderStateStore stateStore = new PdfReaderStateStore();

    private final Stage stage = new Stage();
    private final ImageView pageImageView = new ImageView();
    private final Pane overlayPane = new Pane();
    private final ListView<String> bookmarkListView = new ListView<>();
    private final ListView<String> highlightListView = new ListView<>();
    private final Label pageInfoLabel = new Label();
    private final Label statusLabel = new Label();
    private final TextField pageJumpField = new TextField();
    private final ComboBox<String> highlightColorBox = new ComboBox<>();
    private ScrollPane viewerScrollPane;

    private PDDocument document;
    private PDFRenderer renderer;
    private int totalPages;
    private int currentPageIndex;
    private double zoom = 1.0;
    private boolean drawHighlightMode = false;
    private Rectangle draftHighlightRect;
    private double dragStartX;
    private double dragStartY;
    private String selectedHighlightKey;
    private boolean closedByBorrowExpiry;

    private final List<Integer> bookmarks = new ArrayList<>();
    private final List<PdfReaderStateStore.HighlightRegion> highlights = new ArrayList<>();

    public PdfReaderWindow(String username, String bookId, String bookTitle, String pdfPath) {
        this.username = username;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.pdfPath = pdfPath;
    }

    public ReaderSessionResult showAndWait(Window ownerWindow) {
        closedByBorrowExpiry = false;
        try {
            loadPdfAndState();
        } catch (Exception ex) {
            showError("Cannot open PDF", ex.getMessage());
            return ReaderSessionResult.empty();
        }

        if (ownerWindow != null) {
            stage.initOwner(ownerWindow);
            stage.initModality(Modality.WINDOW_MODAL);
        }
        stage.setTitle("Reader - " + bookTitle + " (" + bookId + ")");
        stage.setScene(new Scene(buildRoot(), 1200, 860));
        stage.setOnCloseRequest(event -> {
            persistState();
            closeResources();
        });
        stage.showAndWait();
        return buildSummaryResult();
    }

    public boolean isOpen() {
        return stage.isShowing();
    }

    public boolean wasClosedByBorrowExpiry() {
        return closedByBorrowExpiry;
    }

    public void forceCloseForBorrowExpiry(String message) {
        if (!stage.isShowing()) {
            return;
        }
        closedByBorrowExpiry = true;
        statusLabel.setText(message == null || message.isBlank()
                ? "Borrow period expired. Closing reader."
                : message);
        persistState();
        closeResources();
        stage.close();
    }

    private BorderPane buildRoot() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        root.setTop(buildToolbar());
        root.setCenter(buildViewerArea());
        root.setRight(buildSidebar());
        root.setBottom(buildStatusBar());

        renderCurrentPage();
        return root;
    }

    private HBox buildToolbar() {
        Button prevBtn = new Button("< Prev");
        prevBtn.setOnAction(e -> navigateTo(currentPageIndex - 1));

        Button nextBtn = new Button("Next >");
        nextBtn.setOnAction(e -> navigateTo(currentPageIndex + 1));

        pageJumpField.setPromptText("Page #");
        pageJumpField.setPrefWidth(80);
        Button jumpBtn = new Button("Go");
        jumpBtn.setOnAction(e -> {
            try {
                int targetPage = Integer.parseInt(pageJumpField.getText().trim());
                navigateTo(targetPage - 1);
            } catch (Exception ignored) {
                statusLabel.setText("Invalid page number.");
            }
        });

        Button zoomOutBtn = new Button("Zoom -");
        zoomOutBtn.setOnAction(e -> {
            zoom = Math.max(0.50, zoom - 0.10);
            renderCurrentPage();
        });

        Button zoomInBtn = new Button("Zoom +");
        zoomInBtn.setOnAction(e -> {
            zoom = Math.min(3.00, zoom + 0.10);
            renderCurrentPage();
        });

        Button zoomResetBtn = new Button("100%");
        zoomResetBtn.setOnAction(e -> {
            zoom = 1.0;
            renderCurrentPage();
        });

        HBox tools = new HBox(8, prevBtn, nextBtn, pageInfoLabel, pageJumpField, jumpBtn, zoomOutBtn, zoomInBtn, zoomResetBtn);
        tools.setAlignment(Pos.CENTER_LEFT);
        tools.setPadding(new Insets(6, 0, 8, 0));
        return tools;
    }

    private ScrollPane buildViewerArea() {
        pageImageView.setPreserveRatio(true);
        overlayPane.setPickOnBounds(true);
        installHighlightDrawingHandlers();

        StackPane pageStack = new StackPane(pageImageView, overlayPane);
        pageStack.setAlignment(Pos.TOP_CENTER);
        pageStack.setStyle("-fx-background-color: #f8fafc;");
        viewerScrollPane = new ScrollPane(pageStack);
        viewerScrollPane.setPannable(true);
        viewerScrollPane.setFitToWidth(false);
        viewerScrollPane.setFitToHeight(false);
        viewerScrollPane.setStyle("-fx-background: #eef2f7;");
        return viewerScrollPane;
    }

    private VBox buildSidebar() {
        Label bmTitle = new Label("Bookmarks");
        bmTitle.setStyle("-fx-font-weight: bold;");
        Button addBookmarkBtn = new Button("Add Current Page");
        addBookmarkBtn.setMaxWidth(Double.MAX_VALUE);
        addBookmarkBtn.setOnAction(e -> addCurrentPageBookmark());
        Button removeBookmarkBtn = new Button("Remove Selected");
        removeBookmarkBtn.setMaxWidth(Double.MAX_VALUE);
        removeBookmarkBtn.setOnAction(e -> removeSelectedBookmark());
        bookmarkListView.setPrefHeight(220);
        bookmarkListView.setOnMouseClicked(e -> {
            if (e.getClickCount() >= 2) {
                String selected = bookmarkListView.getSelectionModel().getSelectedItem();
                if (selected == null || selected.isBlank()) {
                    return;
                }
                int page = Integer.parseInt(selected.replace("Page ", "").trim());
                navigateTo(page - 1);
            }
        });

        Label hlTitle = new Label("Highlights");
        hlTitle.setStyle("-fx-font-weight: bold;");
        highlightColorBox.setItems(FXCollections.observableArrayList("RED", "GREEN", "BLUE"));
        highlightColorBox.setValue("BLUE");
        Button toggleDrawBtn = new Button("Toggle Draw Highlight");
        toggleDrawBtn.setMaxWidth(Double.MAX_VALUE);
        toggleDrawBtn.setOnAction(e -> {
            drawHighlightMode = !drawHighlightMode;
            if (viewerScrollPane != null) {
                viewerScrollPane.setPannable(!drawHighlightMode);
            }
            statusLabel.setText(drawHighlightMode
                    ? "Draw mode ON: drag over page to add " + highlightColorBox.getValue() + " highlight."
                    : "Draw mode OFF.");
        });
        Button removeHighlightBtn = new Button("Remove Selected");
        removeHighlightBtn.setMaxWidth(Double.MAX_VALUE);
        removeHighlightBtn.setOnAction(e -> removeSelectedHighlight());

        highlightListView.setPrefHeight(260);
        highlightListView.setOnMouseClicked(e -> {
            String selected = highlightListView.getSelectionModel().getSelectedItem();
            selectedHighlightKey = selected;
            refreshOverlay();
            if (selected == null || selected.isBlank()) {
                return;
            }
            int page = Integer.parseInt(selected.substring(2, selected.indexOf(" ")).trim());
            navigateTo(page - 1);
        });

        VBox sidebar = new VBox(8,
                bmTitle,
                addBookmarkBtn,
                removeBookmarkBtn,
                bookmarkListView,
                hlTitle,
                new Label("Highlight Color"),
                highlightColorBox,
                toggleDrawBtn,
                removeHighlightBtn,
                highlightListView
        );
        sidebar.setPadding(new Insets(0, 0, 0, 10));
        sidebar.setPrefWidth(260);
        VBox.setVgrow(bookmarkListView, Priority.ALWAYS);
        VBox.setVgrow(highlightListView, Priority.ALWAYS);
        return sidebar;
    }

    private HBox buildStatusBar() {
        statusLabel.setText("Reader ready.");
        HBox bar = new HBox(statusLabel);
        bar.setPadding(new Insets(8, 0, 0, 2));
        return bar;
    }

    private void loadPdfAndState() throws IOException {
        File pdfFile = new File(pdfPath);
        if (!pdfFile.exists()) {
            throw new IOException("PDF file does not exist: " + pdfPath);
        }
        document = Loader.loadPDF(pdfFile);
        renderer = new PDFRenderer(document);
        totalPages = Math.max(1, document.getNumberOfPages());

        PdfReaderStateStore.PdfReaderState state = stateStore.load(username, bookId, pdfPath);
        bookmarks.clear();
        bookmarks.addAll(state.bookmarkPages());
        highlights.clear();
        highlights.addAll(state.highlights());
        currentPageIndex = Math.max(0, Math.min(totalPages - 1, state.lastPageIndex()));
        refreshBookmarkList();
        refreshHighlightList();
    }

    private void closeResources() {
        try {
            if (document != null) {
                document.close();
            }
        } catch (Exception ignored) {
        }
    }

    private void navigateTo(int pageIndex) {
        int clamped = Math.max(0, Math.min(totalPages - 1, pageIndex));
        if (clamped == currentPageIndex && pageImageView.getImage() != null) {
            return;
        }
        currentPageIndex = clamped;
        renderCurrentPage();
    }

    private void renderCurrentPage() {
        if (renderer == null) {
            return;
        }
        try {
            float dpi = (float) (120.0 * zoom);
            BufferedImage buffered = renderer.renderImageWithDPI(currentPageIndex, dpi, ImageType.RGB);
            pageImageView.setImage(SwingFXUtils.toFXImage(buffered, null));
            overlayPane.setPrefSize(buffered.getWidth(), buffered.getHeight());
            overlayPane.setMinSize(buffered.getWidth(), buffered.getHeight());
            overlayPane.setMaxSize(buffered.getWidth(), buffered.getHeight());
            pageInfoLabel.setText("Page " + (currentPageIndex + 1) + " / " + totalPages);
            refreshOverlay();
            persistState();
        } catch (Exception ex) {
            statusLabel.setText("Render failed: " + ex.getMessage());
        }
    }

    private void installHighlightDrawingHandlers() {
        overlayPane.setOnMousePressed(event -> {
            if (!drawHighlightMode || event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            event.consume();
            dragStartX = event.getX();
            dragStartY = event.getY();
            draftHighlightRect = new Rectangle();
            draftHighlightRect.setX(dragStartX);
            draftHighlightRect.setY(dragStartY);
            draftHighlightRect.setFill(getColorFill(highlightColorBox.getValue(), false));
            draftHighlightRect.setStroke(getColorStroke(highlightColorBox.getValue()));
            draftHighlightRect.setStrokeWidth(1.2);
            overlayPane.getChildren().add(draftHighlightRect);
        });

        overlayPane.setOnMouseDragged(event -> {
            if (draftHighlightRect == null) {
                return;
            }
            event.consume();
            double x = Math.min(dragStartX, event.getX());
            double y = Math.min(dragStartY, event.getY());
            double w = Math.abs(event.getX() - dragStartX);
            double h = Math.abs(event.getY() - dragStartY);
            draftHighlightRect.setX(x);
            draftHighlightRect.setY(y);
            draftHighlightRect.setWidth(w);
            draftHighlightRect.setHeight(h);
        });

        overlayPane.setOnMouseReleased(event -> {
            if (draftHighlightRect == null) {
                return;
            }
            event.consume();
            Rectangle finished = draftHighlightRect;
            draftHighlightRect = null;
            overlayPane.getChildren().remove(finished);

            if (finished.getWidth() < 6 || finished.getHeight() < 6) {
                statusLabel.setText("Highlight ignored: selection too small.");
                return;
            }

            double width = overlayPane.getWidth() <= 0 ? overlayPane.getPrefWidth() : overlayPane.getWidth();
            double height = overlayPane.getHeight() <= 0 ? overlayPane.getPrefHeight() : overlayPane.getHeight();
            if (width <= 0 || height <= 0) {
                return;
            }
            PdfReaderStateStore.HighlightRegion region = new PdfReaderStateStore.HighlightRegion(
                    currentPageIndex,
                    clamp01(finished.getX() / width),
                    clamp01(finished.getY() / height),
                    clamp01(finished.getWidth() / width),
                    clamp01(finished.getHeight() / height),
                    highlightColorBox.getValue()
            );
            highlights.add(region);
            refreshHighlightList();
            refreshOverlay();
            persistState();
            statusLabel.setText("Highlight added on page " + (currentPageIndex + 1) + ".");
        });
    }

    private void refreshOverlay() {
        overlayPane.getChildren().clear();
        double width = overlayPane.getWidth() <= 0 ? overlayPane.getPrefWidth() : overlayPane.getWidth();
        double height = overlayPane.getHeight() <= 0 ? overlayPane.getPrefHeight() : overlayPane.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }
        for (PdfReaderStateStore.HighlightRegion highlight : highlights) {
            if (highlight.pageIndex() != currentPageIndex) {
                continue;
            }
            Rectangle rect = new Rectangle(
                    highlight.x() * width,
                    highlight.y() * height,
                    highlight.width() * width,
                    highlight.height() * height
            );
            String key = toHighlightLabel(highlight);
            boolean selected = key.equals(selectedHighlightKey);
            rect.setFill(getColorFill(highlight.colorName(), selected));
            rect.setStroke(getColorStroke(highlight.colorName()));
            rect.setStrokeWidth(selected ? 2.0 : 1.1);
            rect.setOnMouseClicked(e -> {
                selectedHighlightKey = key;
                highlightListView.getSelectionModel().select(key);
                refreshOverlay();
            });
            overlayPane.getChildren().add(rect);
        }
    }

    private void addCurrentPageBookmark() {
        if (!bookmarks.contains(currentPageIndex)) {
            bookmarks.add(currentPageIndex);
            bookmarks.sort(Comparator.naturalOrder());
            refreshBookmarkList();
            persistState();
            statusLabel.setText("Bookmark added: page " + (currentPageIndex + 1) + ".");
        } else {
            statusLabel.setText("Bookmark already exists for this page.");
        }
    }

    private void removeSelectedBookmark() {
        String selected = bookmarkListView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isBlank()) {
            statusLabel.setText("No bookmark selected.");
            return;
        }
        int page = Integer.parseInt(selected.replace("Page ", "").trim()) - 1;
        bookmarks.removeIf(index -> index == page);
        refreshBookmarkList();
        persistState();
        statusLabel.setText("Bookmark removed.");
    }

    private void removeSelectedHighlight() {
        String selected = highlightListView.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isBlank()) {
            statusLabel.setText("No highlight selected.");
            return;
        }
        for (int i = 0; i < highlights.size(); i++) {
            if (toHighlightLabel(highlights.get(i)).equals(selected)) {
                highlights.remove(i);
                selectedHighlightKey = null;
                refreshHighlightList();
                refreshOverlay();
                persistState();
                statusLabel.setText("Highlight removed.");
                return;
            }
        }
    }

    private void refreshBookmarkList() {
        List<String> labels = bookmarks.stream()
                .distinct()
                .sorted()
                .map(page -> "Page " + (page + 1))
                .collect(Collectors.toList());
        bookmarkListView.setItems(FXCollections.observableArrayList(labels));
    }

    private void refreshHighlightList() {
        List<String> labels = highlights.stream()
                .map(this::toHighlightLabel)
                .collect(Collectors.toList());
        highlightListView.setItems(FXCollections.observableArrayList(labels));
    }

    private String toHighlightLabel(PdfReaderStateStore.HighlightRegion region) {
        return "P" + (region.pageIndex() + 1)
                + " [" + region.colorName() + "]"
                + " @ (" + percent(region.x()) + ", " + percent(region.y()) + ") "
                + percent(region.width()) + "x" + percent(region.height());
    }

    private String percent(double normalized) {
        int v = (int) Math.round(Math.max(0.0, Math.min(1.0, normalized)) * 100.0);
        return v + "%";
    }

    private ReaderSessionResult buildSummaryResult() {
        String bookmarkSummary = bookmarks.isEmpty()
                ? ""
                : "Pages: " + bookmarks.stream().sorted().map(p -> Integer.toString(p + 1)).collect(Collectors.joining(", "));
        String highlightSummary;
        if (highlights.isEmpty()) {
            highlightSummary = "";
        } else {
            String pages = highlights.stream()
                    .map(h -> h.pageIndex() + 1)
                    .distinct()
                    .sorted()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            highlightSummary = "Highlights: " + highlights.size() + " region(s) on page(s) " + pages;
        }
        return new ReaderSessionResult(bookmarkSummary, highlightSummary);
    }

    private void persistState() {
        stateStore.save(username, bookId, pdfPath, new PdfReaderStateStore.PdfReaderState(
                Math.max(0, currentPageIndex),
                new ArrayList<>(bookmarks),
                new ArrayList<>(highlights)
        ));
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private double clamp01(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private Color getColorFill(String colorName, boolean selected) {
        return switch (safeColor(colorName)) {
            case "RED" -> selected ? Color.rgb(252, 165, 165, 0.55) : Color.rgb(248, 113, 113, 0.35);
            case "GREEN" -> selected ? Color.rgb(134, 239, 172, 0.55) : Color.rgb(74, 222, 128, 0.35);
            default -> selected ? Color.rgb(147, 197, 253, 0.55) : Color.rgb(96, 165, 250, 0.35);
        };
    }

    private Color getColorStroke(String colorName) {
        return switch (safeColor(colorName)) {
            case "RED" -> Color.web("#b91c1c");
            case "GREEN" -> Color.web("#15803d");
            default -> Color.web("#1d4ed8");
        };
    }

    private String safeColor(String colorName) {
        if (colorName == null) {
            return "BLUE";
        }
        String normalized = colorName.trim().toUpperCase();
        return switch (normalized) {
            case "RED", "GREEN", "BLUE" -> normalized;
            default -> "BLUE";
        };
    }

    public record ReaderSessionResult(String bookmarkSummary, String highlightSummary) {
        public static ReaderSessionResult empty() {
            return new ReaderSessionResult("", "");
        }

        public boolean hasData() {
            return !(bookmarkSummary == null || bookmarkSummary.isBlank())
                    || !(highlightSummary == null || highlightSummary.isBlank());
        }
    }
}
