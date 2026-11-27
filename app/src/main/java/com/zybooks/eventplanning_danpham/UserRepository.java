package com.zybooks.eventplanning_danpham;

/**
 * UserRepository defines the account operations: logging in, registering,
 * and changing a password. LoginViewModel depends on this interface,
 * so tests can use a fake version.
 */
public interface UserRepository {

    /** The result is the user, or an error if the username or password is wrong */
    void login(String username, String password, RepositoryCallback<User> callback);

    /** The result is the new user, or an error if the username is taken */
    void register(String username, String password, RepositoryCallback<User> callback);

    /** Succeeds only if the current password is correct */
    void changePassword(int userId, String currentPassword, String newPassword,
                        RepositoryCallback<Boolean> callback);
}
