package com.zybooks.eventplanning_danpham;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * LoginActivity handles user authentication and registration.
 * Provides login functionality and allows new users to create accounts.
 */
public class LoginActivity extends AppCompatActivity {

    // UI components
    private EditText editTextUsername;
    private EditText editTextPassword;
    private Button buttonLogin;
    private Button buttonRegister;
    private TextView textViewError;
    private ProgressBar progressBar;

    // Database helper
    private DatabaseHelper databaseHelper;

    // Tracks the logged-in user
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize database helper
        databaseHelper = new DatabaseHelper(this);

        // Initialize session manager
        sessionManager = new SessionManager(this);

        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            navigateToMainActivity();
            return;
        }

        // Initialize UI components
        initializeViews();

        // Set up button click listeners
        setupListeners();
    }

    /**
     * Initialize all UI components by finding them by ID
     */
    private void initializeViews() {
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewError = findViewById(R.id.textViewError);
        progressBar = findViewById(R.id.progressBar);
    }

    /**
     * Set up click listeners for login and register buttons
     */
    private void setupListeners() {
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleRegistration();
            }
        });
    }

    /**
     * Handle login button click
     * Validates user credentials against database
     */
    private void handleLogin() {
        // Get input values
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Validate input
        if (!validateInput(username, password)) {
            return;
        }

        // Show progress bar
        showProgress(true);

        // Validate credentials against database
        User user = databaseHelper.validateUser(username, password);

        if (user != null) {
            // Login successful
            sessionManager.saveSession(user.getUserId(), user.getUsername());
            showProgress(false);
            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
            navigateToMainActivity();
        } else {
            // Login failed
            showProgress(false);
            showError("Invalid username or password");
        }
    }

    /**
     * Handle register button click
     * Creates new user account if username doesn't exist
     */
    private void handleRegistration() {
        // Get input values
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Validate input
        if (!validateInput(username, password)) {
            return;
        }

        // Show progress bar
        showProgress(true);

        // Check if username already exists
        if (databaseHelper.checkUsernameExists(username)) {
            showProgress(false);
            showError("Username already exists. Please choose a different username.");
            return;
        }

        // Create new user
        User newUser = new User(username, password);
        long userId = databaseHelper.addUser(newUser);

        if (userId != AppConstants.NO_ID) {
            // Registration successful
            sessionManager.saveSession((int) userId, username);
            showProgress(false);
            Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show();
            navigateToMainActivity();
        } else {
            // Registration failed
            showProgress(false);
            showError("Registration failed. Please try again.");
        }
    }

    /**
     * Validate username and password input
     * @param username Username input
     * @param password Password input
     * @return true if input is valid, false otherwise
     */
    private boolean validateInput(String username, String password) {
        // Check for empty username
        if (username.isEmpty()) {
            showError("Please enter a username");
            editTextUsername.requestFocus();
            return false;
        }

        // Check username length (minimum 3 characters)
        if (username.length() < 3) {
            showError("Username must be at least 3 characters");
            editTextUsername.requestFocus();
            return false;
        }

        // Check for empty password
        if (password.isEmpty()) {
            showError("Please enter a password");
            editTextPassword.requestFocus();
            return false;
        }

        // Check password length (minimum 6 characters)
        if (password.length() < 6) {
            showError("Password must be at least 6 characters");
            editTextPassword.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Display error message to user
     * @param message Error message to display
     */
    private void showError(String message) {
        textViewError.setText(message);
        textViewError.setVisibility(View.VISIBLE);
    }

    /**
     * Show or hide progress bar during authentication
     * @param show true to show progress bar, false to hide
     */
    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        buttonLogin.setEnabled(!show);
        buttonRegister.setEnabled(!show);
    }

    /**
     * Navigate to MainActivity (Dashboard)
     */
    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Close login activity
    }
}
