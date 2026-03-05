package project.task1.repo;

import project.task1.model.StudentStaffAccount;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class StudentStaffRepository {
    private static final String STUDENTSTAFFS_FILE = "data/studentstaffs.txt";
    private final Map<String, StudentStaffAccount> studentstaffsByUsername = new ConcurrentHashMap<>();

    public StudentStaffRepository() {
        // Create data directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get("data"));
        } catch (IOException e) {
            System.err.println("Error creating data directory: " + e.getMessage());
        }
        loadStudentStaffs();
    }

    private void loadStudentStaffs() {
        try {
            Path path = Paths.get(STUDENTSTAFFS_FILE);
            if (Files.exists(path)) {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        StudentStaffAccount ss = StudentStaffAccount.fromString(line);
                        if (ss != null) {
                            studentstaffsByUsername.put(ss.getUsername(), ss);
                        }
                    }
                }
                System.out.println("Loaded " + studentstaffsByUsername.size() + " student/staffs from file.");
            }
        } catch (IOException e) {
            System.err.println("Error loading student/staffs: " + e.getMessage());
        }
    }

    private void saveStudentStaffs() {
        try {
            Path path = Paths.get(STUDENTSTAFFS_FILE);
            List<String> lines = new ArrayList<>();
            for (StudentStaffAccount ss : studentstaffsByUsername.values()) {
                lines.add(ss.toString());
            }
            Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Saved " + lines.size() + " student/staffs to file.");
        } catch (IOException e) {
            System.err.println("Error saving student/staffs: " + e.getMessage());
        }
    }

    public boolean existsByUsername(String username) {
        return studentstaffsByUsername.containsKey(username);
    }

    public void save(StudentStaffAccount userAccount) {
        studentstaffsByUsername.put(userAccount.getUsername(), userAccount);
        saveStudentStaffs();
    }

    public Optional<StudentStaffAccount> findByUsername(String username) {
        return Optional.ofNullable(studentstaffsByUsername.get(username));
    }
}
