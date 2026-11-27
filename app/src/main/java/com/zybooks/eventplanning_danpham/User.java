package com.zybooks.eventplanning_danpham;

/**
 * User represents a logged-in user account.
 * It no longer holds a password. Passwords are only hashed and checked
 * inside DatabaseHelper, so they never travel through the rest of the app.
 */
public class User {
    private final int userId;
    private final String username;

    public User(int userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
}
