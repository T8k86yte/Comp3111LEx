package project.task1.repo;

import project.task1.model.UserAccount;

import java.util.Optional;

public interface UserRepository {
    boolean existsByUsername(String username);

    void save(UserAccount userAccount);

    Optional<UserAccount> findByUsername(String username);
}
