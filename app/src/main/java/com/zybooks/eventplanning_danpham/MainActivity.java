package com.zybooks.eventplanning_danpham;

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
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

/**
 * MainActivity displays the dashboard of the user's maintenance tasks.
 * It only shows what TaskListViewModel provides and passes user actions to it.
 */
public class MainActivity extends AppCompatActivity implements EventAdapter.OnEventClickListener {

    // UI components
    private RecyclerView recyclerViewEvents;
    private TextView textViewEmptyState;
    private SwipeRefreshLayout swipeRefreshLayout;
    private FloatingActionButton fabAddEvent;
    private Toolbar toolbar;
    private EventAdapter taskAdapter;

    // Logic and data
    private TaskListViewModel viewModel;
    private int currentUserId = AppConstants.NO_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Apply saved theme before setting content view
        applyTheme();

        setContentView(R.layout.activity_main_dashboard);

        // Send the user to login if nobody is logged in
        SessionManager sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }
        currentUserId = sessionManager.getUserId();

        // Get the ViewModel. Android keeps the same one when the screen rotates.
        viewModel = new ViewModelProvider(this, new ViewModelFactory(this))
                .get(TaskListViewModel.class);

        initializeViews();
        setSupportActionBar(toolbar);
        setupRecyclerView();
        setupListeners();
        observeViewModel();
    }

    /**
     * Reload tasks whenever this screen appears, including after
     * returning from the add, edit, or detail screens
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadTasks(currentUserId);
        }
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
     * Set up RecyclerView with adapter and layout manager
     */
    private void setupRecyclerView() {
        taskAdapter = new EventAdapter(new ArrayList<>(), this);
        recyclerViewEvents.setAdapter(taskAdapter);
        recyclerViewEvents.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * Set up click listeners for the add button and swipe refresh
     */
    private void setupListeners() {
        fabAddEvent.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AddEditEventActivity.class)));

        swipeRefreshLayout.setOnRefreshListener(() -> viewModel.loadTasks(currentUserId));
    }

    /**
     * Update the screen whenever the ViewModel's data changes
     */
    private void observeViewModel() {
        viewModel.getTasks().observe(this, items -> {
            taskAdapter.updateItems(items);
            swipeRefreshLayout.setRefreshing(false);

            // Show or hide the empty state message
            boolean empty = items.isEmpty();
            textViewEmptyState.setText(viewModel.isSearching()
                    ? "No tasks match your search."
                    : "No tasks yet.\nTap the + button to add your first task!");
            textViewEmptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            recyclerViewEvents.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        viewModel.getMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                viewModel.clearMessages();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                viewModel.clearMessages();
            }
        });
    }

    /**
     * Tapping a task card opens the detail screen
     */
    @Override
    public void onEventClick(MaintenanceTask task) {
        Intent intent = new Intent(MainActivity.this, EventDetailActivity.class);
        intent.putExtra(AppConstants.EXTRA_EVENT_ID, task.getTaskId());
        startActivity(intent);
    }

    /**
     * Tapping Edit opens the edit screen
     */
    @Override
    public void onEditClick(MaintenanceTask task) {
        Intent intent = new Intent(MainActivity.this, AddEditEventActivity.class);
        intent.putExtra(AppConstants.EXTRA_EVENT_ID, task.getTaskId());
        startActivity(intent);
    }

    /**
     * Tapping Delete asks for confirmation, then lets the ViewModel delete it
     */
    @Override
    public void onDeleteClick(MaintenanceTask task) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Delete", (dialog, which) -> viewModel.deleteTask(task.getTaskId()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Create the toolbar menu and set up the search box
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        if (viewModel == null) {
            return true;
        }

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Search tail number, task, or notes");

        // Keep an active search after the screen rotates
        String currentQuery = viewModel.getSearchQuery();
        if (!currentQuery.isEmpty()) {
            searchItem.expandActionView();
            searchView.setQuery(currentQuery, false);
        }

        // Filter the list as the user types
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.clearFocus(); // Hide the keyboard
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                viewModel.setSearchQuery(newText);
                return true;
            }
        });
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
        if (item.getItemId() == R.id.action_reports) {
            startActivity(new Intent(MainActivity.this, ReportsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Navigate to the login screen
     */
    private void navigateToLogin() {
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        finish();
    }
}