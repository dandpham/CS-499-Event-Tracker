package com.zybooks.eventplanning_danpham;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * EventDetailActivity displays complete information for a single maintenance task.
 * Provides options to edit, delete, or share the task.
 */
public class EventDetailActivity extends AppCompatActivity {

    // UI components
    private TextView textViewDetailTitle;
    private TextView textViewDetailTailNumber;
    private TextView textViewDetailTaskType;
    private TextView textViewDetailStatus;
    private TextView textViewDetailDate;
    private TextView textViewDetailTime;
    private TextView textViewDetailNotes;
    private Button buttonEdit;
    private Button buttonDelete;
    private Button buttonShare;

    // Data
    private MaintenanceTask task;
    private DatabaseHelper databaseHelper;

    private static final int REQUEST_EDIT_TASK = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        // Initialize database helper
        databaseHelper = new DatabaseHelper(this);

        // Get task from intent
        task = getIntent().getParcelableExtra(AppConstants.EXTRA_EVENT);

        if (task == null) {
            Toast.makeText(this, "Error loading task", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        displayTaskData();
        setupListeners();
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        textViewDetailTitle = findViewById(R.id.textViewDetailTitle);
        textViewDetailTailNumber = findViewById(R.id.textViewDetailTailNumber);
        textViewDetailTaskType = findViewById(R.id.textViewDetailTaskType);
        textViewDetailStatus = findViewById(R.id.textViewDetailStatus);
        textViewDetailDate = findViewById(R.id.textViewDetailDate);
        textViewDetailTime = findViewById(R.id.textViewDetailTime);
        textViewDetailNotes = findViewById(R.id.textViewDetailNotes);
        buttonEdit = findViewById(R.id.buttonEdit);
        buttonDelete = findViewById(R.id.buttonDelete);
        buttonShare = findViewById(R.id.buttonShare);
    }

    /**
     * Display task information on screen
     */
    private void displayTaskData() {
        textViewDetailTitle.setText(task.getTaskName());
        textViewDetailTailNumber.setText(task.getTailNumber());
        textViewDetailTaskType.setText(task.getTaskType());
        textViewDetailStatus.setText(task.getStatus());
        textViewDetailDate.setText(task.getDueDate());
        textViewDetailTime.setText(task.getDueTime());

        // Notes are optional
        if (task.getNotes() != null && !task.getNotes().isEmpty()) {
            textViewDetailNotes.setText(task.getNotes());
        } else {
            textViewDetailNotes.setText("No notes");
        }
    }

    /**
     * Set up button click listeners
     */
    private void setupListeners() {
        buttonEdit.setOnClickListener(v -> {
            Intent intent = new Intent(EventDetailActivity.this, AddEditEventActivity.class);
            intent.putExtra(AppConstants.EXTRA_EVENT_ID, task.getTaskId());
            startActivityForResult(intent, REQUEST_EDIT_TASK);
        });
        buttonDelete.setOnClickListener(v -> showDeleteConfirmation());
        buttonShare.setOnClickListener(v -> shareTask());
    }

    /**
     * Show confirmation dialog before deleting the task
     */
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Delete", (dialog, which) -> deleteTask())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Delete the task from the database
     */
    private void deleteTask() {
        int result = databaseHelper.deleteEvent(task.getTaskId());

        if (result > 0) {
            Toast.makeText(this, "Task deleted successfully", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Failed to delete task", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Share task details using the Android share sheet
     */
    private void shareTask() {
        String shareText = "Task: " + task.getTaskName() + "\n" +
                "Tail Number: " + task.getTailNumber() + "\n" +
                "Type: " + task.getTaskType() + "\n" +
                "Status: " + task.getStatus() + "\n" +
                "Due: " + task.getDueDate() + " at " + task.getDueTime() + "\n";

        if (task.getNotes() != null && !task.getNotes().isEmpty()) {
            shareText += "Notes: " + task.getNotes();
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Maintenance Task: " + task.getTaskName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        startActivity(Intent.createChooser(shareIntent, "Share task via"));
    }

    /**
     * Reload the task after it's edited
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_EDIT_TASK && resultCode == RESULT_OK) {
            task = databaseHelper.getEvent(task.getTaskId());
            if (task != null) {
                displayTaskData();
                setResult(RESULT_OK); // Pass result back to MainActivity
            } else {
                // Task was deleted from the edit screen
                finish();
            }
        }
    }
}