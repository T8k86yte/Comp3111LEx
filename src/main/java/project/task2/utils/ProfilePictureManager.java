package project.task2.utils;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.geometry.Pos;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ProfilePictureManager {
    
    private static final String PROFILE_PICTURES_DIR = "data/profile_pictures/";
    private static final String DEFAULT_AVATAR_DIR = "data/default_avatars/";
    
    static {
        File dir = new File(PROFILE_PICTURES_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    /**
     * Get the profile picture path for a user
     */
    public static String getProfilePicturePath(String username) {
        // Check for .jpg
        String jpgPath = PROFILE_PICTURES_DIR + username + ".jpg";
        File jpgFile = new File(jpgPath);
        if (jpgFile.exists()) {
            return jpgPath;
        }
        
        // Check for .png
        String pngPath = PROFILE_PICTURES_DIR + username + ".png";
        File pngFile = new File(pngPath);
        if (pngFile.exists()) {
            return pngPath;
        }
        
        return null;
    }
    
    /**
     * Load profile picture as Image
     */
    public static Image loadProfilePicture(String username, double width, double height) {
        String path = getProfilePicturePath(username);
        if (path != null) {
            try {
                Image image = new Image(new File(path).toURI().toString(), width, height, true, true);
                if (!image.isError()) {
                    return image;
                }
            } catch (Exception e) {
                // Fall through to null
            }
        }
        return null;
    }
    
    /**
     * Create a circular avatar with user's initial
     */
    public static StackPane createAvatar(String username, String fullName, double size) {
        StackPane avatarContainer = new StackPane();
        avatarContainer.setPrefSize(size, size);
        avatarContainer.setMaxSize(size, size);
        
        Image profileImage = loadProfilePicture(username, size, size);
        
        if (profileImage != null) {
            ImageView imageView = new ImageView(profileImage);
            imageView.setFitWidth(size);
            imageView.setFitHeight(size);
            imageView.setClip(new Circle(size / 2, size / 2, size / 2));
            avatarContainer.getChildren().add(imageView);
        } else {
            // Create default avatar with initial
            String initial = (fullName != null && !fullName.isEmpty()) 
                ? fullName.substring(0, 1).toUpperCase() 
                : username.substring(0, 1).toUpperCase();
            
            Canvas canvas = new Canvas(size, size);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            // Draw circle background
            gc.setFill(Color.web("#2563eb"));
            gc.fillOval(0, 0, size, size);
            
            // Draw text
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Segoe UI", size * 0.45));
            double textWidth = gc.getFont().getSize() * initial.length() * 0.6;
            gc.fillText(initial, (size - textWidth) / 2, size * 0.65);
            
            avatarContainer.getChildren().add(new ImageView(canvas.snapshot(null, null)));
        }
        
        return avatarContainer;
    }
    
    /**
     * Save profile picture for a user
     */
    public static boolean saveProfilePicture(String username, File sourceFile) {
        String extension = "";
        String fileName = sourceFile.getName().toLowerCase();
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            extension = ".jpg";
        } else if (fileName.endsWith(".png")) {
            extension = ".png";
        } else {
            return false;
        }
        
        String destPath = PROFILE_PICTURES_DIR + username + extension;
        Path dest = Paths.get(destPath);
        
        try {
            // Delete existing profile pictures of other formats
            File existingJpg = new File(PROFILE_PICTURES_DIR + username + ".jpg");
            File existingPng = new File(PROFILE_PICTURES_DIR + username + ".png");
            if (existingJpg.exists()) existingJpg.delete();
            if (existingPng.exists()) existingPng.delete();
            
            Files.copy(sourceFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Delete profile picture for a user
     */
    public static boolean deleteProfilePicture(String username) {
        File jpgFile = new File(PROFILE_PICTURES_DIR + username + ".jpg");
        File pngFile = new File(PROFILE_PICTURES_DIR + username + ".png");
        
        boolean deleted = false;
        if (jpgFile.exists()) {
            deleted = jpgFile.delete() || deleted;
        }
        if (pngFile.exists()) {
            deleted = pngFile.delete() || deleted;
        }
        return deleted;
    }
}
