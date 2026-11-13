package com.zybooks.eventplanning_danpham;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;

/**
 * MainActivity displays the main dashboard with a grid of upcoming events.
 * Allows users to view, add, edit, and delete events.
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
    private EventAdapter eventAdapter;
    private List<MaintenanceTask> eventList;
    private int currentUserId;

    // Tracks the logged-in user
    private SessionManager sessionManager;

    // Request codes for activities
    private static final int REQUEST_ADD_EVENT = 1;
    private static final int REQUEST_EDIT_EVENT = 2;
    private static final int REQUEST_VIEW_EVENT = 3;

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

        // Initialize UI components
        initializeViews();

        // Set up toolbar
        setupToolbar();

        // Set up RecyclerView
        setupRecyclerView();

        // Load events from database
        loadEvents();

        // Set up listeners
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
        eventList = new ArrayList<>();
        eventAdapter = new EventAdapter(eventList, this);
        recyclerViewEvents.setAdapter(eventAdapter);
        recyclerViewEvents.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * Set up click listeners for FAB and swipe refresh
     */
    private void setupListeners() {
        // FAB click listener - navigate to add event screen
        fabAddEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddEditEventActivity.class);
                startActivityForResult(intent, REQUEST_ADD_EVENT);
            }
        });

        // Swipe refresh listener - reload events
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadEvents();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    /**
     * Load all events for current user from database
     */
    private void loadEvents() {
        eventList = databaseHelper.getAllEvents(currentUserId);
        eventAdapter.updateEvents(eventList);

        // Show or hide empty state message
        if (eventList.isEmpty()) {
            textViewEmptyState.setVisibility(View.VISIBLE);
            recyclerViewEvents.setVisibility(View.GONE);
        } else {
            textViewEmptyState.setVisibility(View.GONE);
            recyclerViewEvents.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Handle event card click - navigate to event detail screen
     */
    @Override
    public void onEventClick(MaintenanceTask event) {
        Intent intent = new Intent(MainActivity.this, EventDetailActivity.class);
        intent.putExtra(AppConstants.EXTRA_EVENT, event);
        startActivityForResult(intent, REQUEST_VIEW_EVENT);
    }

    /**
     * Handle edit button click - navigate to edit event screen
     */
    @Override
    public void onEditClick(MaintenanceTask event) {
        Intent intent = new Intent(MainActivity.this, AddEditEventActivity.class);
        intent.putExtra(AppConstants.EXTRA_EVENT_ID, event.getTaskId());
        startActivityForResult(intent, REQUEST_EDIT_EVENT);
    }

    /**
     * Handle delete button click - show confirmation dialog
     */
    @Override
    public void onDeleteClick(MaintenanceTask event) {
        // Show confirmation dialog before deleting
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteEvent(event.getTaskId());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Delete an event from the database
     * @param eventId ID of event to delete
     */
    private void deleteEvent(int eventId) {
        // Cancel scheduled notification
        cancelEventNotification(eventId);

        int rowsDeleted = databaseHelper.deleteEvent(eventId);

        if (rowsDeleted > 0) {
            Toast.makeText(this, "Event deleted", Toast.LENGTH_SHORT).show();
            loadEvents(); // Refresh the list
        } else {
            Toast.makeText(this, "Failed to delete event", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Cancel a scheduled notification
     */
    private void cancelEventNotification(int eventId) {
        Intent intent = new Intent(this, EventNotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                eventId,
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
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // Navigate to settings
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Handle results from child activities
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            // Reload events after add, edit, or delete
            loadEvents();
        }
    }

    /**
     * Reload events when returning to this activity
     */
    @Override
    protected void onResume() {
        super.onResume();
        loadEvents();
    }

    /**
     * Navigate to login activity
     */
    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}