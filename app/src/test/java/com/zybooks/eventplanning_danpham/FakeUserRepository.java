package com.zybooks.eventplanning_danpham;

import java.util.HashMap;
import java.util.Map;

/**
 * FakeUserRepository keeps accounts in a map and answers right away,
 * so LoginViewModel can be tested without a database or slow hashing.
 */
public class FakeUserRepository implements UserRepository {

    // username to password. Plain text is fine here; this is test data only.
    final Map<String, String> passwords = new HashMap<>();

    // Set to true to simulate an unexpected failure, like a database error
    boolean shouldFail = false;

    private int nextId = 1;

    @Override
    public void login(String username, String password, RepositoryCallback<User> callback) {
        if (shouldFail) {
            callback.onError(new IllegalStateException("database is locked"));
            return;
        }
        if (password.equals(passwords.get(username))) {
            callback.onSuccess(new User(1, username));
        } else {
            callback.onError(new IllegalArgumentException(SqliteUserRepository.ERROR_INVALID_LOGIN));
        }
    }

    @Override
    public void register(String username, String password, RepositoryCallback<User> callback) {
        if (shouldFail) {
            callback.onError(new IllegalStateException("database is locked"));
            return;
        }
        if (passwords.containsKey(username)) {
            callback.onError(new IllegalArgumentException(SqliteUserRepository.ERROR_USERNAME_TAKEN));
            return;
        }
        passwords.put(username, password);
        callback.onSuccess(new User(nextId++, username));
    }

    @Override
    public void changePassword(int userId, String currentPassword, String newPassword,
                               RepositoryCallback<Boolean> callback) {
        callback.onSuccess(true);
    }
}
