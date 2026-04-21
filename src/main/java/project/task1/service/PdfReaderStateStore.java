package project.task1.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class PdfReaderStateStore {
    private static final String STATE_FILE = "data/task1/pdf_reader_state.txt";

    public PdfReaderState load(String username, String bookId, String pdfPath) {
        String normalizedUser = safeTrim(username);
        String normalizedBookId = safeTrim(bookId).toUpperCase(Locale.ROOT);
        String normalizedPdfPath = safeTrim(pdfPath);
        for (StoredRow row : loadRows()) {
            if (normalizedUser.equals(row.username())
                    && normalizedBookId.equalsIgnoreCase(row.bookId())
                    && normalizedPdfPath.equals(row.pdfPath())) {
                return new PdfReaderState(
                        clampPageIndex(row.lastPageIndex()),
                        parseBookmarks(row.bookmarksCsv()),
                        parseHighlights(row.highlightsCsv())
                );
            }
        }
        return PdfReaderState.empty();
    }

    public void save(String username, String bookId, String pdfPath, PdfReaderState state) {
        String normalizedUser = safeTrim(username);
        String normalizedBookId = safeTrim(bookId).toUpperCase(Locale.ROOT);
        String normalizedPdfPath = safeTrim(pdfPath);
        if (normalizedUser.isEmpty() || normalizedBookId.isEmpty() || normalizedPdfPath.isEmpty() || state == null) {
            return;
        }

        List<StoredRow> rows = loadRows();
        StoredRow replacement = new StoredRow(
                normalizedUser,
                normalizedBookId,
                normalizedPdfPath,
                clampPageIndex(state.lastPageIndex()),
                toBookmarksCsv(state.bookmarkPages()),
                toHighlightsCsv(state.highlights())
        );

        boolean updated = false;
        for (int i = 0; i < rows.size(); i++) {
            StoredRow existing = rows.get(i);
            if (normalizedUser.equals(existing.username())
                    && normalizedBookId.equalsIgnoreCase(existing.bookId())
                    && normalizedPdfPath.equals(existing.pdfPath())) {
                rows.set(i, replacement);
                updated = true;
                break;
            }
        }
        if (!updated) {
            rows.add(replacement);
        }
        saveRows(rows);
    }

    private List<StoredRow> loadRows() {
        Path path = Paths.get(STATE_FILE);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try {
            List<StoredRow> rows = new ArrayList<>();
            for (String line : Files.readAllLines(path)) {
                if (line == null || line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\|", -1);
                if (parts.length < 6) {
                    continue;
                }
                rows.add(new StoredRow(
                        decode(parts[0]),
                        decode(parts[1]),
                        decode(parts[2]),
                        Integer.parseInt(parts[3]),
                        decode(parts[4]),
                        decode(parts[5])
                ));
            }
            return rows;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private void saveRows(List<StoredRow> rows) {
        try {
            Files.createDirectories(Paths.get("data/task1"));
            List<String> lines = new ArrayList<>();
            for (StoredRow row : rows) {
                lines.add(String.join("|",
                        encode(row.username()),
                        encode(row.bookId()),
                        encode(row.pdfPath()),
                        Integer.toString(clampPageIndex(row.lastPageIndex())),
                        encode(row.bookmarksCsv()),
                        encode(row.highlightsCsv())
                ));
            }
            Files.write(Paths.get(STATE_FILE), lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {
        }
    }

    private List<Integer> parseBookmarks(String csv) {
        if (csv == null || csv.isBlank()) {
            return new ArrayList<>();
        }
        List<Integer> bookmarks = new ArrayList<>();
        for (String token : csv.split(",")) {
            try {
                int page = Integer.parseInt(token.trim());
                if (page >= 0 && !bookmarks.contains(page)) {
                    bookmarks.add(page);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        Collections.sort(bookmarks);
        return bookmarks;
    }

    private String toBookmarksCsv(List<Integer> bookmarks) {
        if (bookmarks == null || bookmarks.isEmpty()) {
            return "";
        }
        return bookmarks.stream()
                .filter(page -> page != null && page >= 0)
                .distinct()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private List<HighlightRegion> parseHighlights(String csv) {
        if (csv == null || csv.isBlank()) {
            return new ArrayList<>();
        }
        List<HighlightRegion> highlights = new ArrayList<>();
        for (String token : csv.split(";")) {
            if (token.isBlank()) {
                continue;
            }
            String[] parts = token.split(":", -1);
            if (parts.length < 5) {
                continue;
            }
            try {
                int pageIndex = Integer.parseInt(parts[0]);
                double x = clamp01(Double.parseDouble(parts[1]));
                double y = clamp01(Double.parseDouble(parts[2]));
                double width = clamp01(Double.parseDouble(parts[3]));
                double height = clamp01(Double.parseDouble(parts[4]));
                String colorName = parts.length >= 6 ? normalizeColor(parts[5]) : "BLUE";
                if (pageIndex >= 0 && width > 0.001 && height > 0.001) {
                    highlights.add(new HighlightRegion(pageIndex, x, y, width, height, colorName));
                }
            } catch (Exception ignored) {
            }
        }
        return highlights;
    }

    private String toHighlightsCsv(List<HighlightRegion> highlights) {
        if (highlights == null || highlights.isEmpty()) {
            return "";
        }
        return highlights.stream()
                .filter(h -> h != null && h.pageIndex() >= 0 && h.width() > 0.001 && h.height() > 0.001)
                .map(h -> h.pageIndex()
                        + ":" + format(h.x())
                        + ":" + format(h.y())
                        + ":" + format(h.width())
                        + ":" + format(h.height())
                        + ":" + normalizeColor(h.colorName()))
                .collect(Collectors.joining(";"));
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.6f", clamp01(value));
    }

    private int clampPageIndex(int pageIndex) {
        return Math.max(0, pageIndex);
    }

    private static double clamp01(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String normalizeColor(String value) {
        String normalized = safeTrim(value).toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "RED", "GREEN", "BLUE" -> normalized;
            default -> "BLUE";
        };
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private record StoredRow(
            String username,
            String bookId,
            String pdfPath,
            int lastPageIndex,
            String bookmarksCsv,
            String highlightsCsv
    ) {}

    public record HighlightRegion(
            int pageIndex,
            double x,
            double y,
            double width,
            double height,
            String colorName
    ) {}

    public record PdfReaderState(
            int lastPageIndex,
            List<Integer> bookmarkPages,
            List<HighlightRegion> highlights
    ) {
        public static PdfReaderState empty() {
            return new PdfReaderState(0, new ArrayList<>(), new ArrayList<>());
        }
    }
}
