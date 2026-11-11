package com.zybooks.eventplanning_danpham;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


/**
 * SettingsActivity manages app settings including notifications, SMS permissions,
 * account management, and theme selection.
 */
public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    // UI components
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch switchNotifications;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch switchSmsPermission;
    private TextView textViewSmsPermissionStatus;
    private TextView textViewAccountName;
    private Button buttonChangePassword;
    private Button buttonLogout;
    private RadioGroup radioGroupTheme;

    // SharedPreferences
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "EventTrackerPrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_THEME = "theme_preference";

    // SMS permission request code
    private static final int SMS_PERMISSION_REQUEST_CODE = 100;

    // Database helper
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Log.i(TAG, "onCreate - SettingsActivity started");

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Initialize database helper
        databaseHelper = new DatabaseHelper(this);

        // Initialize UI components
        initializeViews();

        // Load current settings
        loadSettings();

        // Set up listeners
        setupListeners();
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        switchNotifications = findViewById(R.id.switchNotifications);
        switchSmsPermission = findViewById(R.id.switchSmsPermission);
        textViewSmsPermissionStatus = findViewById(R.id.textViewSmsPermissionStatus);
        textViewAccountName = findViewById(R.id.textViewAccountName);
        buttonChangePassword = findViewById(R.id.buttonChangePassword);
        buttonLogout = findViewById(R.id.buttonLogout);
        radioGroupTheme = findViewById(R.id.radioGroupTheme);
    }

    /**
     * Load current settings from SharedPreferences
     */
    private void loadSettings() {
        // Load username
        String username = sharedPreferences.getString(KEY_USERNAME, "User");
        textViewAccountName.setText(username);

        // Load notification preference
        boolean notificationsEnabled = sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true);
        switchNotifications.setChecked(notificationsEnabled);

        // Check SMS permission status
        updateSmsPermissionStatus();

        // Load theme preference
        String theme = sharedPreferences.getString(KEY_THEME, "system");
        loadThemeSelection(theme);
    }

    /**
     * Check and update SMS permission status
     */
    private void updateSmsPermissionStatus() {
        boolean hasPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;

        Log.i(TAG, "updateSmsPermissionStatus - Has SMS permission: " + hasPermission);

        switchSmsPermission.setChecked(hasPermission);

        if (hasPermission) {
            textViewSmsPermissionStatus.setText("SMS permission granted");
            textViewSmsPermissionStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            Log.i(TAG, "✅ SMS permission is GRANTED");
        } else {
            textViewSmsPermissionStatus.setText("SMS permission not granted");
            textViewSmsPermissionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            Log.w(TAG, "❌ SMS permission is NOT granted");
        }
    }

    /**
     * Load theme selection into radio group
     */
    private void loadThemeSelection(String theme) {
        switch (theme) {
            case "light":
                radioGroupTheme.check(R.id.radioButtonLight);
                break;
            case "dark":
                radioGroupTheme.check(R.id.radioButtonDark);
                break;
            default:
                radioGroupTheme.check(R.id.radioButtonSystem);
                break;
        }
    }

    /**
     * Set up all listeners for settings controls
     */
    private void setupListeners() {
        // Notifications switch
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.i(TAG, "Notifications switch toggled to: " + isChecked);

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_NOTIFICATIONS_ENABLED, isChecked);
            editor.apply();

            String message = isChecked ? "Notifications enabled" : "Notifications disabled";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });

        // SMS permission switch
        switchSmsPermission.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.i(TAG, "SMS switch toggled to: " + isChecked);

            if (isChecked) {
                Log.i(TAG, "Requesting SMS permission");

                boolean hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                        == PackageManager.PERMISSION_GRANTED;

                if (hasPermission) {
                    Log.i(TAG, "SMS permission already granted");
                    Toast.makeText(this, "SMS permission already granted", Toast.LENGTH_SHORT).show();
                } else {
                    requestSmsPermission();
                }
            } else {
                Log.i(TAG, "SMS notifications disabled");
                Toast.makeText(this, "SMS notifications disabled in app.", Toast.LENGTH_SHORT).show();
            }
        });

        // Change password button
        buttonChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // Logout button
        buttonLogout.setOnClickListener(v -> handleLogout());

        // Theme radio group
        radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            String theme;
            int nightMode;

            if (checkedId == R.id.radioButtonLight) {
                theme = "light";
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.radioButtonDark) {
                theme = "dark";
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
            } else {
                theme = "system";
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            }

            // Save theme preference
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_THEME, theme);
            editor.apply();

            // Apply theme immediately
            AppCompatDelegate.setDefaultNightMode(nightMode);

            // Recreate activity to apply theme
            recreate();
        });
    }

    /**
     * Request SMS permission from user
     */
    private void requestSmsPermission() {
        Log.i(TAG, "Requesting SMS permission");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {

            // Check if we should show rationale
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.SEND_SMS)) {

                // Show explanation dialog
                new AlertDialog.Builder(this)
                        .setTitle("SMS Permission Required")
                        .setMessage("This app needs SMS permission to send you event reminders. " +
                                "Grant permission to enable this feature.")
                        .setPositiveButton("Grant Permission", (dialog, which) -> {
                            ActivityCompat.requestPermissions(this,
                                    new String[]{Manifest.permission.SEND_SMS},
                                    SMS_PERMISSION_REQUEST_CODE);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> {
                            switchSmsPermission.setChecked(false);
                        })
                        .show();
            } else {
                // Request permission directly
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        SMS_PERMISSION_REQUEST_CODE);
            }
        } else {
            updateSmsPermissionStatus();
        }
    }

    /**
     * Handle permission request result
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.i(TAG, "Permission result received");

        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "SMS permission granted");
                Toast.makeText(this, "SMS permission granted. " +
                        "You will now receive event reminders.", Toast.LENGTH_LONG).show();
                updateSmsPermissionStatus();
            } else {
                Log.w(TAG, "SMS permission denied");
                switchSmsPermission.setChecked(false);
                Toast.makeText(this, "SMS permission denied. " +
                        "Event reminders will not be sent.", Toast.LENGTH_LONG).show();
                updateSmsPermissionStatus();
            }
        }
    }

    /**
     * Show dialog for changing password
     */
    private void showChangePasswordDialog() {
        // For simplicity, showing a basic implementation
        // In a real app, you'd want current password verification
        Toast.makeText(this, "Change password feature - to be implemented with " +
                "current password verification", Toast.LENGTH_SHORT).show();
    }

    /**
     * Handle logout - clear session and return to login
     */
    private void handleLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    // Clear SharedPreferences
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.remove(KEY_USER_ID);
                    editor.remove(KEY_USERNAME);
                    editor.apply();

                    // Navigate to login
                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}