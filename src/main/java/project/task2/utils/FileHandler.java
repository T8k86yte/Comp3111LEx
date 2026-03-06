package project.task2.utils;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;

public class FileHandler {
    private static final String BOOKS_DIRECTORY = "books/";
    private static final String[] ALLOWED_EXTENSIONS = {".pdf", ".txt", ".doc", ".docx"};

    static {
        // Create books directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(BOOKS_DIRECTORY));
        } catch (IOException e) {
            System.err.println("Error creating books directory: " + e.getMessage());
        }
    }

    /**
     * Save an uploaded book file
     * @param fileContent The content of the file
     * @param originalFileName Original filename
     * @param authorUsername Username of the author
     * @return The path where the file was saved
     */
    public static String saveBookFile(byte[] fileContent, String originalFileName, String authorUsername) 
            throws IOException {
        
        // Validate file type
        if (!isValidFileType(originalFileName)) {
            throw new IOException("Invalid file type. Allowed: PDF, TXT, DOC, DOCX");
        }

        // Generate unique filename
        String fileExtension = getFileExtension(originalFileName);
        String uniqueFileName = authorUsername + "_" + 
                               UUID.randomUUID().toString() + 
                               fileExtension;
        
        Path filePath = Paths.get(BOOKS_DIRECTORY + uniqueFileName);
        Files.write(filePath, fileContent);
        
        return filePath.toString();
    }

    /**
     * Read a book file
     * @param filePath Path to the file
     * @return byte array of file content
     */
    public static byte[] readBookFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.readAllBytes(path);
    }

    /**
     * Check if file type is allowed
     */
    public static boolean isValidFileType(String fileName) {
        String extension = getFileExtension(fileName).toLowerCase();
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get file extension from filename
     */
    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            return fileName.substring(lastDot);
        }
        return "";
    }

    /**
     * Get allowed file types as string for display
     */
    public static String getAllowedFileTypes() {
        return String.join(", ", ALLOWED_EXTENSIONS);
    }
}
