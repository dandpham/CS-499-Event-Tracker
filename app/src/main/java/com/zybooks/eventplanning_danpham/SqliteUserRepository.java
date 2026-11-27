package com.zybooks.eventplanning_danpham;

import android.content.Context;

/**
 * SqliteUserRepository runs account operations on the background thread.
 * Password hashing is slow on purpose, so running it on the main thread
 * would freeze the login screen. This also moves login and registration
 * off the main thread, which version 2 still did there.
 */
public class SqliteUserRepository implements UserRepository {

    // Error messages the login screen shows
    public static final String ERROR_INVALID_LOGIN = "Invalid username or password";
    public static final String ERROR_USERNAME_TAKEN =
            "Username already exists. Please choose a different username.";
    public static final String ERROR_WRONG_PASSWORD = "Current password is incorrect";

    private final DatabaseHelper databaseHelper;

    public SqliteUserRepository(Context context) {
        databaseHelper = DatabaseHelper.getInstance(context.getApplicationContext());
    }

    @Override
    public void login(String username, String password, RepositoryCallback<User> callback) {
        BackgroundRunner.run(() -> {
            User user = databaseHelper.validateUser(username, password);
            if (user == null) {
                // Same message whether the username or the password was wrong,
                // so the screen doesn't reveal which usernames exist
                throw new IllegalArgumentException(ERROR_INVALID_LOGIN);
            }
            return user;
        }, callback);
    }

    @Override
    public void register(String username, String password, RepositoryCallback<User> callback) {
        BackgroundRunner.run(() -> {
            if (databaseHelper.checkUsernameExists(username)) {
                throw new IllegalArgumentException(ERROR_USERNAME_TAKEN);
            }
            User user = databaseHelper.registerUser(username, password);
            if (user == null) {
                // The UNIQUE rule caught a username taken at the same moment
                throw new IllegalArgumentException(ERROR_USERNAME_TAKEN);
            }
            return user;
        }, callback);
    }

    @Override
    public void changePassword(int userId, String currentPassword, String newPassword,
                               RepositoryCallback<Boolean> callback) {
        BackgroundRunner.run(() -> {
            if (!databaseHelper.changePassword(userId, currentPassword, newPassword)) {
                throw new IllegalArgumentException(ERROR_WRONG_PASSWORD);
            }
            return true;
        }, callback);
    }
}
