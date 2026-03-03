package project.task1.repo;

import project.task1.model.UserAccount;
import project.task1.model.UserRole;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryUserRepository implements UserRepository {
    private static final Path STORAGE_PATH = Paths.get("data", "task1", "users.db");
    private final ConcurrentMap<String, UserAccount> usersByUsername = new ConcurrentHashMap<>();

    public InMemoryUserRepository() {
        loadFromFile();
    }

    @Override
    public boolean existsByUsername(String username) {
        return usersByUsername.containsKey(username);
    }

    @Override
    public void save(UserAccount userAccount) {
        usersByUsername.put(userAccount.getUsername(), userAccount);
        persistToFile();
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username));
    }

    private void loadFromFile() {
        List<String> lines = RepositoryFileStorageSupport.readLines(STORAGE_PATH);
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            List<String> parts = RepositoryFileStorageSupport.splitRecord(line, 5);
            String username = parts.get(0);
            String fullName = RepositoryFileStorageSupport.decode(parts.get(1));
            String saltBase64 = parts.get(2);
            String hashBase64 = parts.get(3);
            UserRole role = UserRole.valueOf(parts.get(4));
            usersByUsername.put(
                    username,
                    new UserAccount(username, fullName, saltBase64, hashBase64, role)
            );
        }
    }

    private void persistToFile() {
        List<String> lines = usersByUsername.values()
                .stream()
                .sorted(Comparator.comparing(UserAccount::getUsername))
                .collect(ArrayList::new, (acc, user) -> acc.add(serialize(user)), ArrayList::addAll);
        RepositoryFileStorageSupport.writeLines(STORAGE_PATH, lines);
    }

    private static String serialize(UserAccount user) {
        return user.getUsername()
                + "|"
                + RepositoryFileStorageSupport.encode(user.getFullName())
                + "|"
                + user.getPasswordSaltBase64()
                + "|"
                + user.getPasswordHashBase64()
                + "|"
                + user.getRole().name();
    }
}
