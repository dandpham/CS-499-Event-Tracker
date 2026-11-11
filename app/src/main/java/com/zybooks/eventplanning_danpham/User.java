package com.zybooks.eventplanning_danpham;

/**
 * User model class representing a user account in the database.
 * Contains user credentials and identification.
 */
public class User {
    private int userId;
    private String username;
    private String password;

    // Constructor for creating new users
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Constructor for existing users with ID
    public User(int userId, String username, String password) {
        this.userId = userId;
        this.username = username;
        this.password = password;
    }

    // Getters and setters
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
