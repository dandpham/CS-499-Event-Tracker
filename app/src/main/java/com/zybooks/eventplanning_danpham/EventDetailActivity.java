package com.zybooks.eventplanning_danpham;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.time.ZoneId;
import java.util.List;

/**
 * EventDetailActivity shows one maintenance task's full details, its sign-off,
 * and its audit history. It loads the task through TaskViewModel, so it
 * always shows the latest data. Open tasks are completed here by signing off.
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
    private TextView textViewDetailNeedsReview;
    private TextView textViewDetailRepeat;
    private TextView textViewDetailSignOff;
    private TextView textViewDetailHistory;
    private Button buttonSignOff;
    private Button buttonEdit;
    private Button buttonDelete;
    private Button buttonShare;

    // Logic and data
    private TaskViewModel viewModel;
    private int taskId;
    private int userId;
    private MaintenanceTask currentTask; // The task on screen, used for sharing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        taskId = getIntent().getIntExtra(AppConstants.EXTRA_EVENT_ID, AppConstants.NO_ID);
        userId = new SessionManager(this).getUserId();
        if (taskId == AppConstants.NO_ID) {
            Toast.makeText(this, "Error loading task", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this, new ViewModelFactory(this))
                .get(TaskViewModel.class);

        initializeViews();
        setupListeners();
        observeViewModel();
    }

    /**
     * Reload the task whenever this screen appears,
     * so changes made on the edit screen show up right away
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.loadTask(taskId, userId);
        }
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
        textViewDetailNeedsReview = findViewById(R.id.textViewDetailNeedsReview);
        textViewDetailRepeat = findViewById(R.id.textViewDetailRepeat);
        textViewDetailSignOff = findViewById(R.id.textViewDetailSignOff);
        textViewDetailHistory = findViewById(R.id.textViewDetailHistory);
        buttonSignOff = findViewById(R.id.buttonSignOff);
        buttonEdit = findViewById(R.id.buttonEdit);
        buttonDelete = findViewById(R.id.buttonDelete);
        buttonShare = findViewById(R.id.buttonShare);
    }

    /**
     * Set up button click listeners
     */
    private void setupListeners() {
        buttonEdit.setOnClickListener(v -> {
            Intent intent = new Intent(EventDetailActivity.this, AddEditEventActivity.class);
            intent.putExtra(AppConstants.EXTRA_EVENT_ID, taskId);
            startActivity(intent);
        });
        buttonSignOff.setOnClickListener(v -> showSignOffConfirmation());
        buttonDelete.setOnClickListener(v -> showDeleteConfirmation());
        buttonShare.setOnClickListener(v -> shareTask());
    }

    /**
     * Update the screen whenever the ViewModel's data changes
     */
    private void observeViewModel() {
        viewModel.getTask().observe(this, this::displayTaskData);
        viewModel.getHistory().observe(this, this::displayHistory);

        // Messages that don't close the screen, such as after a sign-off
        viewModel.getMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                viewModel.clearError();
                // If the task never loaded, there's nothing to show
                if (currentTask == null) {
                    finish();
                }
            }
        });

        // The task was deleted, so close the screen
        viewModel.getDoneMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /**
     * Display task information on screen
     */
    private void displayTaskData(MaintenanceTask task) {
        if (task == null) {
            return;
        }
        currentTask = task;

        textViewDetailTitle.setText(task.getTaskName());
        // Show the aircraft model after the tail number when there is one
        String model = task.getAircraftModel();
        textViewDetailTailNumber.setText(model == null || model.isEmpty()
                ? task.getTailNumber() : task.getTailNumber() + " (" + model + ")");
        textViewDetailTaskType.setText(task.getTaskType());
        textViewDetailStatus.setText(task.getStatus());
        textViewDetailDate.setText(task.getDueDate());
        textViewDetailTime.setText(task.getDueTime());

        textViewDetailRepeat.setText(task.isRecurring()
                ? "Every " + task.getRepeatDays() + " days after sign-off" : "Does not repeat");

        // Notes are optional
        if (task.getNotes() != null && !task.getNotes().isEmpty()) {
            textViewDetailNotes.setText(task.getNotes());
        } else {
            textViewDetailNotes.setText("No notes");
        }

        // Tasks moved from old data with missing information are flagged
        textViewDetailNeedsReview.setVisibility(task.isNeedsReview() ? View.VISIBLE : View.GONE);

        textViewDetailSignOff.setText(signOffText(task));

        // Only open tasks can be signed off or edited
        int openOnly = task.isComplete() ? View.GONE : View.VISIBLE;
        buttonSignOff.setVisibility(openOnly);
        buttonEdit.setVisibility(openOnly);
    }

    /**
     * Describe who signed off the task and when
     */
    private String signOffText(MaintenanceTask task) {
        if (!task.isComplete()) {
            return "Not signed off";
        }
        if (task.getSignedOffAt() == null) {
            return "Completed before sign-offs were recorded";
        }
        return "Signed off by " + task.getSignedOffBy() + " on "
                + DueDateConverter.formatDateTime(task.getSignedOffAt(), ZoneId.systemDefault());
    }

    /**
     * Show the task's audit history, one entry per line, oldest first
     */
    private void displayHistory(List<AuditEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            textViewDetailHistory.setText("No history");
            return;
        }
        StringBuilder text = new StringBuilder();
        for (AuditEntry entry : entries) {
            text.append(DueDateConverter.formatDateTime(entry.getCreatedAt(), ZoneId.systemDefault()))
                    .append("  ").append(entry.getAction())
                    .append(" by ").append(entry.getUsername())
                    .append("\n").append(entry.getDetails()).append("\n\n");
        }
        textViewDetailHistory.setText(text.toString().trim());
    }

    /**
     * Confirm before signing off, since a sign-off can't be undone
     */
    private void showSignOffConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Off Task")
                .setMessage("Sign off this task as complete? Your name and the time will be " +
                        "recorded, and the task can no longer be edited.")
                .setPositiveButton("Sign Off", (dialog, which) -> viewModel.signOffTask(taskId, userId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Show confirmation dialog before deleting the task
     */
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Delete", (dialog, which) -> viewModel.deleteTask(taskId, userId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Share task details using the Android share sheet
     */
    private void shareTask() {
        if (currentTask == null) {
            return;
        }

        String shareText = "Task: " + currentTask.getTaskName() + "\n" +
                "Tail Number: " + currentTask.getTailNumber() + "\n" +
                "Type: " + currentTask.getTaskType() + "\n" +
                "Status: " + currentTask.getStatus() + "\n" +
                "Due: " + currentTask.getDueDate() + " at " + currentTask.getDueTime() + "\n" +
                "Sign-off: " + signOffText(currentTask) + "\n";

        if (currentTask.getNotes() != null && !currentTask.getNotes().isEmpty()) {
            shareText += "Notes: " + currentTask.getNotes();
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Maintenance Task: " + currentTask.getTaskName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        startActivity(Intent.createChooser(shareIntent, "Share task via"));
    }
}