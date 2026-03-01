package project.task1.repo;

import project.task1.model.UserAccount;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryUserRepository implements UserRepository {
    private final ConcurrentMap<String, UserAccount> usersByUsername = new ConcurrentHashMap<>();

    @Override
    public boolean existsByUsername(String username) {
        return usersByUsername.containsKey(username);
    }

    @Override
    public void save(UserAccount userAccount) {
        usersByUsername.put(userAccount.getUsername(), userAccount);
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username));
    }
}
