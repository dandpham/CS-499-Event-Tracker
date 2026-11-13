package com.zybooks.eventplanning_danpham;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        // Use the application context so this doesn't hold onto a screen
        prefs = context.getApplicationContext()
                .getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Save the logged-in user's ID and username
     */
    public void saveSession(int userId, String username) {
        prefs.edit()
                .putInt(AppConstants.KEY_USER_ID, userId)
                .putString(AppConstants.KEY_USERNAME, username)
                .apply();
    }

    /**
     * Get the logged-in user's ID
     * @return the user ID, or NO_ID if nobody is logged in
     */
    public int getUserId () {
        return prefs.getInt(AppConstants.KEY_USER_ID, AppConstants.NO_ID);
    }

    /**
     * Get the logged-in user's username
     * @return the username, or "User" if none is saved
     */
    public String getUsername() {
        return prefs.getString(AppConstants.KEY_USERNAME, "User");
    }

    /**
     * Check whether a user is logged in
     * @return true if a user ID is saved
     */
    public boolean isLoggedIn() {
        return prefs.contains(AppConstants.KEY_USER_ID);
    }

    /**
     * Clear the session at logout
     */
    public void clearSession() {
        prefs.edit()
                .remove(AppConstants.KEY_USER_ID)
                .remove(AppConstants.KEY_USERNAME)
                .apply();
    }
}