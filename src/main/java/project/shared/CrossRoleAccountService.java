package project.shared;

import project.task1.model.StudentStaffAccount;
import project.task1.repo.StudentStaffRepository;
import project.task2.model.AuthorAccount;
import project.task2.repo.AuthorRepository;
import project.task3.model.LibrarianAccount;
import project.task3.repo.LibrarianRepository;

import java.util.Optional;

public class CrossRoleAccountService {
    private final StudentStaffRepository studentStaffRepository;
    private final AuthorRepository authorRepository;
    private final LibrarianRepository librarianRepository;

    public CrossRoleAccountService(
            StudentStaffRepository studentStaffRepository,
            AuthorRepository authorRepository,
            LibrarianRepository librarianRepository
    ) {
        this.studentStaffRepository = studentStaffRepository;
        this.authorRepository = authorRepository;
        this.librarianRepository = librarianRepository;
    }

    public boolean usernameExistsAcrossAllRoles(String username) {
        return studentStaffRepository.existsByUsername(username)
                || authorRepository.existsByUsername(username)
                || librarianRepository.existsByUsername(username);
    }

    public Optional<String> findRoleByUsername(String username) {
        Optional<StudentStaffAccount> ss = studentStaffRepository.findByUsername(username);
        if (ss.isPresent()) {
            return Optional.of(ss.get().getRole().name());
        }

        Optional<AuthorAccount> author = authorRepository.findByUsername(username);
        if (author.isPresent()) {
            return Optional.of("AUTHOR");
        }

        Optional<LibrarianAccount> librarian = librarianRepository.findByUsername(username);
        if (librarian.isPresent()) {
            return Optional.of("LIBRARIAN");
        }

        return Optional.empty();
    }
}
