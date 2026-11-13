package com.zybooks.eventplanning_danpham;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

/**
 * MainActivity displays the dashboard of the user's maintenance tasks.
 * Allows users to view, add, edit, and delete tasks.
 */
public class MainActivity extends AppCompatActivity implements EventAdapter.OnEventClickListener {

    // UI components
    private RecyclerView recyclerViewEvents;
    private TextView textViewEmptyState;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fabAddEvent;
    private Toolbar toolbar;

    // Data components
    private DatabaseHelper databaseHelper;
    private EventAdapter taskAdapter;
    private List<MaintenanceTask> taskList;
    private int currentUserId;

    // Tracks the logged-in user
    private SessionManager sessionManager;

    // Request codes for activities
    private static final int REQUEST_ADD_TASK = 1;
    private static final int REQUEST_EDIT_TASK = 2;
    private static final int REQUEST_VIEW_TASK = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply saved theme before setting content view
        applyTheme();

        setContentView(R.layout.activity_main_dashboard);

        // Check if user is logged in
        sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        // Get the logged-in user's ID
        currentUserId = sessionManager.getUserId();

        // Initialize database helper
        databaseHelper = new DatabaseHelper(this);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        loadTasks();
        setupListeners();
    }

    /**
     * Apply saved theme preference
     */
    private void applyTheme() {
        SharedPreferences prefs = getSharedPreferences(AppConstants.PREFS_NAME, MODE_PRIVATE);
        String theme = prefs.getString(AppConstants.KEY_THEME, "system");

        int nightMode;
        switch (theme) {
            case "light":
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case "dark":
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            default:
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                break;
        }

        AppCompatDelegate.setDefaultNightMode(nightMode);
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerViewEvents = findViewById(R.id.recyclerViewEvents);
        textViewEmptyState = findViewById(R.id.textViewEmptyState);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        fabAddEvent = findViewById(R.id.fabAddEvent);
    }

    /**
     * Set up the toolbar with menu
     */
    private void setupToolbar() {
        setSupportActionBar(toolbar);
    }

    /**
     * Set up RecyclerView with adapter and layout manager
     */
    private void setupRecyclerView() {
        taskList = new ArrayList<>();
        taskAdapter = new EventAdapter(taskList, this);
        recyclerViewEvents.setAdapter(taskAdapter);
        recyclerViewEvents.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * Set up click listeners for the add button and swipe refresh
     */
    private void setupListeners() {
        // Add button opens the add task screen
        fabAddEvent.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditEventActivity.class);
            startActivityForResult(intent, REQUEST_ADD_TASK);
        });

        // Swipe down to reload tasks
        swipeRefreshLayout.setOnRefreshListener(() -> {
            loadTasks();
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    /**
     * Load all tasks for the current user from the database
     */
    private void loadTasks() {
        taskList = databaseHelper.getAllEvents(currentUserId);
        taskAdapter.updateEvents(taskList);

        // Show or hide the empty state message
        if (taskList.isEmpty()) {
            textViewEmptyState.setVisibility(View.VISIBLE);
            recyclerViewEvents.setVisibility(View.GONE);
        } else {
            textViewEmptyState.setVisibility(View.GONE);
            recyclerViewEvents.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Tapping a task card opens the detail screen
     */
    @Override
    public void onEventClick(MaintenanceTask task) {
        Intent intent = new Intent(MainActivity.this, EventDetailActivity.class);
        intent.putExtra(AppConstants.EXTRA_EVENT, task);
        startActivityForResult(intent, REQUEST_VIEW_TASK);
    }

    /**
     * Tapping Edit opens the edit screen
     */
    @Override
    public void onEditClick(MaintenanceTask task) {
        Intent intent = new Intent(MainActivity.this, AddEditEventActivity.class);
        intent.putExtra(AppConstants.EXTRA_EVENT_ID, task.getTaskId());
        startActivityForResult(intent, REQUEST_EDIT_TASK);
    }

    /**
     * Tapping Delete asks for confirmation first
     */
    @Override
    public void onDeleteClick(MaintenanceTask task) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Delete", (dialog, which) -> deleteTask(task.getTaskId()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Delete a task from the database
     * @param taskId ID of the task to delete
     */
    private void deleteTask(int taskId) {
        // Cancel scheduled notification
        cancelEventNotification(taskId);

        int rowsDeleted = databaseHelper.deleteEvent(taskId);

        if (rowsDeleted > 0) {
            Toast.makeText(this, "Task deleted", Toast.LENGTH_SHORT).show();
            loadTasks(); // Refresh the list
        } else {
            Toast.makeText(this, "Failed to delete task", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Cancel a scheduled notification
     */
    private void cancelEventNotification(int taskId) {
        Intent intent = new Intent(this, EventNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                taskId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }

    /**
     * Create options menu in toolbar
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * Handle toolbar menu item clicks
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Reload tasks after an add, edit, or delete
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            loadTasks();
        }
    }

    /**
     * Reload tasks when returning to this screen
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
    }

    /**
     * Navigate to the login screen
     */
    private void navigateToLogin() {
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }
}