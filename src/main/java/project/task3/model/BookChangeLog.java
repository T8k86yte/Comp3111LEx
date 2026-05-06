package project.task3.model;

import java.time.LocalDate;

public class BookChangeLog {
    private final String id;
    private final String title;
    private final String author;
    private final String genre;
    private final String pdfFilePath;
    private final String coverImagePath;
    private final LocalDate modifiedDate;
    private final String summary;

    public BookChangeLog(String id, String title, String author, String genre, String pdfFilePath, String coverImagePath, LocalDate modifiedDate, String summary) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.pdfFilePath = pdfFilePath;
        this.coverImagePath = coverImagePath;
        this.modifiedDate = modifiedDate;
        this.summary = summary;
    }

    public String toString() {
        return String.join("|", id, title, author, genre, pdfFilePath, coverImagePath, modifiedDate.toString(), summary);
    }
    static public BookChangeLog fromString(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length > 8) return null;
        return new BookChangeLog(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], LocalDate.parse(parts[6]), parts[7]);
    }

    public String getId() { return id; }

    public String description() {
        return "Title: " + title + "\nAuthor: " + author + "\nGenre: " + genre + "\nFile Path: " + pdfFilePath +
                "\nCover Image Path: " + coverImagePath + "\nModified Date: " + modifiedDate.toString() + "\nSummary: " + summary;
    }
}
