package project.task1.repo;

import project.task1.model.NewBookSubmission;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SubmissionRepository {
    List<NewBookSubmission> findAll();

    //Approve the submission at index
    void approveSubmission(int index, BookRepository books);

    void addSubmission(String title, String author, LocalDate publishDate, String summary, String genre);
}
