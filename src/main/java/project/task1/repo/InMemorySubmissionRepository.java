package project.task1.repo;

import project.task1.model.NewBookSubmission;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InMemorySubmissionRepository implements SubmissionRepository {
    private final List<NewBookSubmission> submissions = new ArrayList<>();

    public InMemorySubmissionRepository() {

    }

    public List<NewBookSubmission> findAll() {
        return submissions;
    }

    public void approveSubmission(int index, BookRepository books) {
        submissions.get(index).approve(books);
    }

    public void addSubmission(String title, String author, LocalDate publishDate, String summary, String genre) {
        submissions.add(new NewBookSubmission(title, author, publishDate, summary, genre));
    }
}
