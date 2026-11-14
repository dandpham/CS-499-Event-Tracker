package com.zybooks.eventplanning_danpham;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

/**
 * AddEditEventActivity is the form for adding or editing a maintenance task.
 * It only displays the form and passes input to TaskViewModel, which
 * validates and saves it.
 */
public class AddEditEventActivity extends AppCompatActivity {

    // UI components
    private TextView textViewScreenTitle;
    private EditText editTextTaskName;
    private EditText editTextTailNumber;
    private Spinner spinnerTaskType;
    private Button buttonSelectDate;
    private TextView textViewSelectedDate;
    private Button buttonSelectTime;
    private TextView textViewSelectedTime;
    private EditText editTextNotes;
    private CheckBox checkBoxComplete;
    private Button buttonSave;
    private Button buttonCancel;
    private Button buttonDelete;

    // Logic and data
    private TaskViewModel viewModel;
    private int userId;
    private int taskId = AppConstants.NO_ID; // NO_ID means a new task; otherwise editing an existing one

    // Selected date and time
    private String selectedDate = "";
    private String selectedTime = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_event);

        userId = new SessionManager(this).getUserId();
        taskId = getIntent().getIntExtra(AppConstants.EXTRA_EVENT_ID, AppConstants.NO_ID);

        viewModel = new ViewModelProvider(this, new ViewModelFactory(this))
                .get(TaskViewModel.class);

        initializeViews();
        setupTaskTypeSpinner();
        setupListeners();
        observeViewModel();

        if (taskId != AppConstants.NO_ID) {
            textViewScreenTitle.setText("Edit Task");
            buttonDelete.setVisibility(View.VISIBLE);
            checkBoxComplete.setVisibility(View.VISIBLE);

            // Load only the first time. After a rotation, the ViewModel still has the task.
            if (savedInstanceState == null) {
                viewModel.loadTask(taskId);
            }
        } else {
            textViewScreenTitle.setText("Add Task");
            buttonDelete.setVisibility(View.GONE);
            checkBoxComplete.setVisibility(View.GONE); // New tasks always start as Open
        }
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        textViewScreenTitle = findViewById(R.id.textViewScreenTitle);
        editTextTaskName = findViewById(R.id.editTextTaskName);
        editTextTailNumber = findViewById(R.id.editTextTailNumber);
        spinnerTaskType = findViewById(R.id.spinnerTaskType);
        buttonSelectDate = findViewById(R.id.buttonSelectDate);
        textViewSelectedDate = findViewById(R.id.textViewSelectedDate);
        buttonSelectTime = findViewById(R.id.buttonSelectTime);
        textViewSelectedTime = findViewById(R.id.textViewSelectedTime);
        editTextNotes = findViewById(R.id.editTextNotes);
        checkBoxComplete = findViewById(R.id.checkBoxComplete);
        buttonSave = findViewById(R.id.buttonSave);
        buttonCancel = findViewById(R.id.buttonCancel);
        buttonDelete = findViewById(R.id.buttonDelete);
    }

    /**
     * Fill the task type dropdown from the list in MaintenanceTask
     */
    private void setupTaskTypeSpinner() {
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, MaintenanceTask.TASK_TYPES);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTaskType.setAdapter(typeAdapter);
    }

    /**
     * Set up all button click listeners
     */
    private void setupListeners() {
        buttonSelectDate.setOnClickListener(v -> showDatePicker());
        buttonSelectTime.setOnClickListener(v -> showTimePicker());
        buttonSave.setOnClickListener(v -> onSaveClicked());
        buttonCancel.setOnClickListener(v -> finish());
        buttonDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    /**
     * Update the screen whenever the ViewModel's data changes
     */
    private void observeViewModel() {
        // Fill the form when an existing task finishes loading
        viewModel.getTask().observe(this, this::fillForm);

        // Show validation and save errors
        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                viewModel.clearError();
            }
        });

        // A save or delete succeeded, so close the screen
        viewModel.getDoneMessage().observe(this, message -> {
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    /**
     * Show an existing task's details in the form
     */
    private void fillForm(MaintenanceTask task) {
        if (task == null) {
            return;
        }
        editTextTaskName.setText(task.getTaskName());
        editTextTailNumber.setText(task.getTailNumber());
        selectedDate = task.getDueDate();
        textViewSelectedDate.setText(selectedDate);
        selectedTime = task.getDueTime();
        textViewSelectedTime.setText(selectedTime);
        editTextNotes.setText(task.getNotes());

        // Select the saved task type, or the first type if it isn't found
        int typeIndex = Arrays.asList(MaintenanceTask.TASK_TYPES).indexOf(task.getTaskType());
        spinnerTaskType.setSelection(Math.max(typeIndex, 0));

        checkBoxComplete.setChecked(task.isComplete());
    }

    /**
     * Show date picker dialog
     */
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    // Format date as MM/DD/YYYY
                    selectedDate = String.format(Locale.US, "%02d/%02d/%04d", month + 1, day, year);
                    textViewSelectedDate.setText(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    /**
     * Show time picker dialog
     */
    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    // Format time as HH:MM AM/PM
                    String amPm = hour >= 12 ? "PM" : "AM";
                    int displayHour = hour % 12;
                    if (displayHour == 0) displayHour = 12;
                    selectedTime = String.format(Locale.US, "%02d:%02d %s", displayHour, minute, amPm);
                    textViewSelectedTime.setText(selectedTime);
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false // false for 12-hour format
        ).show();
    }

    /**
     * Before saving, make sure reminders are allowed. If they aren't,
     * let the user open Settings or save without a reminder.
     */
    private void onSaveClicked() {
        if (new ReminderScheduler(this).canScheduleReminders()) {
            saveTask();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Reminders Are Off")
                .setMessage("This app needs permission to schedule exact alarms to remind you " +
                        "before a task is due. You can turn it on in Settings, or save " +
                        "without a reminder.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                })
                .setNegativeButton("Save Without Reminder", (dialog, which) -> saveTask())
                .show();
    }

    /**
     * Pass the form input to the ViewModel, which validates and saves it
     */
    private void saveTask() {
        viewModel.saveTask(
                taskId,
                userId,
                editTextTaskName.getText().toString(),
                editTextTailNumber.getText().toString().trim().toUpperCase(Locale.US),
                (String) spinnerTaskType.getSelectedItem(),
                selectedDate,
                selectedTime,
                editTextNotes.getText().toString().trim(),
                checkBoxComplete.isChecked());
    }

    /**
     * Show confirmation dialog before deleting the task
     */
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Delete", (dialog, which) -> viewModel.deleteTask(taskId))
                .setNegativeButton("Cancel", null)
                .show();
    }
}