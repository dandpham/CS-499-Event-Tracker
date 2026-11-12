package com.zybooks.eventplanning_danpham;

/**
 * Shared constants used across the app, kept in one place
 * so each value is defined only once.
 */
public final class AppConstants {

    private AppConstants() {
        // Prevent creating instances of this class
    }

    // SharedPreferences
    public static final String PREFS_NAME = "EventTrackerPrefs";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String KEY_THEME = "theme_preference";

    // Intent extras
    public static final String EXTRA_EVENT_ID = "EVENT_ID";
    public static final String EXTRA_EVENT = "EVENT";
    public static final String EXTRA_EVENT_NAME = "EVENT_NAME";
    public static final String EXTRA_EVENT_DATE = "EVENT_DATE";
    public static final String EXTRA_EVENT_TIME = "EVENT_TIME";

    // IDs
    public static final int NO_ID = -1; // means "not ID yet", such as a new event
}



