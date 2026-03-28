package project.task2.service;

import project.task2.model.BookSubmission;
import project.task2.repo.SubmissionRepository;
import java.util.Optional;

/**
 * Service to track borrowing and returning activity.
 * This bridges the task1 borrowing system with task2 submission tracking.
 */
public class BorrowTrackingService {
    private final SubmissionRepository submissionRepository;
    
    public BorrowTrackingService() {
        this.submissionRepository = new SubmissionRepository();
    }
    
    /**
     * Called when a book is borrowed.
     * Increments both total and currently borrowed counts.
     * 
     * @param bookTitle The title of the book being borrowed
     * @param authorUsername The username of the author who wrote the book
     * @return true if successfully updated, false otherwise
     */
    public boolean onBookBorrowed(String bookTitle, String authorUsername) {
        submissionRepository.refreshFromFile();
        
        Optional<BookSubmission> submissionOpt = submissionRepository.findAll().stream()
            .filter(sub -> sub.getTitle().equalsIgnoreCase(bookTitle) 
                    && sub.getAuthorUsername().equals(authorUsername)
                    && sub.isApproved())
            .findFirst();
        
        if (submissionOpt.isPresent()) {
            BookSubmission submission = submissionOpt.get();
            submission.incrementBorrowedCount();
            submissionRepository.update(submission);
            System.out.println("📖 Book borrowed: " + bookTitle + 
                               " | Currently borrowed: " + submission.getCurrentlyBorrowedCount() +
                               " | Total: " + submission.getTotalBorrowedCount());
            return true;
        }
        
        System.out.println("⚠️ Could not find approved submission for book: " + bookTitle);
        return false;
    }
    
    /**
     * Called when a book is returned.
     * Decrements the currently borrowed count.
     * 
     * @param bookTitle The title of the book being returned
     * @param authorUsername The username of the author who wrote the book
     * @return true if successfully updated, false otherwise
     */
    public boolean onBookReturned(String bookTitle, String authorUsername) {
        submissionRepository.refreshFromFile();
        
        Optional<BookSubmission> submissionOpt = submissionRepository.findAll().stream()
            .filter(sub -> sub.getTitle().equalsIgnoreCase(bookTitle) 
                    && sub.getAuthorUsername().equals(authorUsername)
                    && sub.isApproved())
            .findFirst();
        
        if (submissionOpt.isPresent()) {
            BookSubmission submission = submissionOpt.get();
            submission.decrementBorrowedCount();
            submissionRepository.update(submission);
            System.out.println("📚 Book returned: " + bookTitle + 
                               " | Currently borrowed: " + submission.getCurrentlyBorrowedCount());
            return true;
        }
        
        System.out.println("⚠️ Could not find approved submission for book: " + bookTitle);
        return false;
    }
    
    /**
     * Check if a book can be deleted.
     * A book can be deleted if:
     * - Status is PENDING, OR
     * - Status is APPROVED and currentlyBorrowedCount == 0 (no copies currently borrowed)
     * 
     * @param submissionId The submission ID to check
     * @return true if book can be deleted
     */
    public boolean canDeleteBook(String submissionId) {
        Optional<BookSubmission> submissionOpt = submissionRepository.findById(submissionId);
        if (submissionOpt.isEmpty()) {
            return false;
        }
        
        return submissionOpt.get().canBeDeleted();
    }
    
    /**
     * Get the number of books currently borrowed
     */
    public int getCurrentlyBorrowedCount(String submissionId) {
        Optional<BookSubmission> submissionOpt = submissionRepository.findById(submissionId);
        return submissionOpt.map(BookSubmission::getCurrentlyBorrowedCount).orElse(0);
    }
    
    /**
     * Get the total historical borrow count
     */
    public int getTotalBorrowedCount(String submissionId) {
        Optional<BookSubmission> submissionOpt = submissionRepository.findById(submissionId);
        return submissionOpt.map(BookSubmission::getTotalBorrowedCount).orElse(0);
    }
    
    /**
     * Check if a book is currently borrowed by anyone
     */
    public boolean isCurrentlyBorrowed(String submissionId) {
        Optional<BookSubmission> submissionOpt = submissionRepository.findById(submissionId);
        return submissionOpt.map(BookSubmission::isCurrentlyBorrowed).orElse(false);
    }
}
