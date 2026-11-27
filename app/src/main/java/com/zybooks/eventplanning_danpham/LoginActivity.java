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
import androidx.lifecycle.ViewModelProvider;

/**
 * LoginActivity is the login and registration screen.
 * It only displays the form and passes input to LoginViewModel,
 * which checks passwords on a background thread.
 */
public class LoginActivity extends AppCompatActivity {

    // UI components
    private EditText editTextUsername;
    private EditText editTextPassword;
    private Button buttonLogin;
    private Button buttonRegister;
    private TextView textViewError;
    private ProgressBar progressBar;

    // Logic and data
    private LoginViewModel viewModel;

    // Tracks the logged-in user
    private SessionManager sessionManager;

    // Remembers which button was pressed, for the success message
    private boolean registering = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize session manager
        sessionManager = new SessionManager(this);

        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            navigateToMainActivity();
            return;
        }

        viewModel = new ViewModelProvider(this, new ViewModelFactory(this))
                .get(LoginViewModel.class);

        // Initialize UI components
        initializeViews();

        // Set up button click listeners
        setupListeners();

        observeViewModel();
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
     * Set up click listeners for login and register buttons.
     * The ViewModel validates the input and checks the password on a background thread.
     */
    private void setupListeners() {
        buttonLogin.setOnClickListener(v -> {
            registering = false;
            viewModel.login(getUsername(), getPassword());
        });

        buttonRegister.setOnClickListener(v -> {
            registering = true;
            viewModel.register(getUsername(), getPassword());
        });
    }

    private String getUsername() {
        return editTextUsername.getText().toString().trim();
    }

    // Trimmed the same way version 2 did when accounts were created,
    // so existing users can still log in after their passwords are hashed
    private String getPassword() {
        return editTextPassword.getText().toString().trim();
    }

    /**
     * Update the screen whenever the ViewModel's data changes
     */
    private void observeViewModel() {
        viewModel.isLoading().observe(this, this::showProgress);

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                showError(error);
                viewModel.clearError();
            }
        });

        viewModel.getLoggedInUser().observe(this, user -> {
            if (user != null) {
                sessionManager.saveSession(user.getUserId(), user.getUsername());
                Toast.makeText(this, registering ? "Registration successful!" : "Login successful!",
                        Toast.LENGTH_SHORT).show();
                navigateToMainActivity();
            }
        });
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
