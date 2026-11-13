package com.zybooks.eventplanning_danpham;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

/**
 * AddEditEventActivity handles creating new maintenance tasks and editing existing ones.
 * Provides form input for all task details.
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

    // Data components
    private DatabaseHelper databaseHelper;
    private int userId;
    private int taskId = AppConstants.NO_ID; // NO_ID means a new task; otherwise editing an existing one

    // Selected date and time
    private String selectedDate = "";
    private String selectedTime = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_event);

        // Initialize database helper
        databaseHelper = new DatabaseHelper(this);

        // Get the logged-in user's ID
        userId = new SessionManager(this).getUserId();

        // Get task ID from intent (if editing)
        taskId = getIntent().getIntExtra(AppConstants.EXTRA_EVENT_ID, AppConstants.NO_ID);

        // Initialize UI components
        initializeViews();
        setupTaskTypeSpinner();

        // Check if we're editing an existing task
        if (taskId != AppConstants.NO_ID) {
            loadTaskData();
        } else {
            textViewScreenTitle.setText("Add Task");
            buttonDelete.setVisibility(View.GONE);
            checkBoxComplete.setVisibility(View.GONE); // New tasks always start as Open
        }

        // Set up listeners
        setupListeners();
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
     * Load existing task data for editing
     */
    private void loadTaskData() {
        MaintenanceTask existingTask = databaseHelper.getEvent(taskId);

        if (existingTask != null) {
            textViewScreenTitle.setText("Edit Task");
            editTextTaskName.setText(existingTask.getTaskName());
            editTextTailNumber.setText(existingTask.getTailNumber());
            selectedDate = existingTask.getDueDate();
            textViewSelectedDate.setText(selectedDate);
            selectedTime = existingTask.getDueTime();
            textViewSelectedTime.setText(selectedTime);
            editTextNotes.setText(existingTask.getNotes());

            // Select the saved task type, or the first type if it isn't found
            int typeIndex = Arrays.asList(MaintenanceTask.TASK_TYPES).indexOf(existingTask.getTaskType());
            spinnerTaskType.setSelection(Math.max(typeIndex, 0));

            checkBoxComplete.setChecked(existingTask.isComplete());
            checkBoxComplete.setVisibility(View.VISIBLE);
            buttonDelete.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Set up all button click listeners
     */
    private void setupListeners() {
        buttonSelectDate.setOnClickListener(v -> showDatePicker());
        buttonSelectTime.setOnClickListener(v -> showTimePicker());
        buttonSave.setOnClickListener(v -> saveTask());
        buttonCancel.setOnClickListener(v -> finish());
        buttonDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    /**
     * Show date picker dialog
     */
    private void showDatePicker() {
        // Get current date
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create and show date picker dialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int selectedYear, int selectedMonth, int selectedDay) {
                        // Format date as MM/DD/YYYY
                        selectedDate = String.format(Locale.US, "%02d/%02d/%04d",
                                selectedMonth + 1, selectedDay, selectedYear);
                        textViewSelectedDate.setText(selectedDate);
                    }
                },
                year, month, day
        );

        datePickerDialog.show();
    }

    /**
     * Show time picker dialog
     */
    private void showTimePicker() {
        // Get current time
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // Create and show time picker dialog
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int selectedHour, int selectedMinute) {
                        // Format time as HH:MM AM/PM
                        String amPm = selectedHour >= 12 ? "PM" : "AM";
                        int displayHour = selectedHour % 12;
                        if (displayHour == 0) displayHour = 12;
                        selectedTime = String.format(Locale.US, "%02d:%02d %s",
                                displayHour, selectedMinute, amPm);
                        textViewSelectedTime.setText(selectedTime);
                    }
                },
                hour, minute, false // false for 12-hour format
        );

        timePickerDialog.show();
    }

    /**
     * Validate and save the task to the database
     */
    @SuppressLint("ScheduleExactAlarm")
    private void saveTask() {
        // Get input values
        String taskName = editTextTaskName.getText().toString().trim();
        String tailNumber = editTextTailNumber.getText().toString().trim().toUpperCase(Locale.US);
        String notes = editTextNotes.getText().toString().trim();
        String taskType = (String) spinnerTaskType.getSelectedItem();
        String status = checkBoxComplete.isChecked()
                ? MaintenanceTask.STATUS_COMPLETE
                : MaintenanceTask.STATUS_OPEN;

        // Validate input using the shared Validator rules.
        // New tasks can't be due in the past. Existing tasks can be,
        // since an overdue task still needs to be edited.
        boolean isNewTask = (taskId == AppConstants.NO_ID);
        LocalDateTime earliestAllowed = isNewTask ? LocalDateTime.now() : LocalDateTime.MIN;

        String error = Validator.checkTask(taskName, tailNumber, selectedDate, selectedTime, earliestAllowed);
        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check exact alarm permission before saving
        checkExactAlarmPermission();

        if (isNewTask) {
            // Create new task
            MaintenanceTask newTask = new MaintenanceTask(userId, taskName, selectedDate,
                    selectedTime, tailNumber, notes);
            newTask.setTaskType(taskType);
            newTask.setStatus(status);
            long result = databaseHelper.addEvent(newTask);

            if (result != AppConstants.NO_ID) {
                scheduleEventNotification((int) result, taskName, selectedDate, selectedTime);

                Toast.makeText(this, "Task added successfully", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Failed to add task", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Update existing task
            MaintenanceTask updatedTask = new MaintenanceTask(taskId, userId, taskName,
                    selectedDate, selectedTime, tailNumber, notes);
            updatedTask.setTaskType(taskType);
            updatedTask.setStatus(status);
            int result = databaseHelper.updateEvent(updatedTask);

            if (result > 0) {
                // Replace the old reminder. Completed tasks don't get a reminder.
                cancelEventNotification(taskId);
                if (!updatedTask.isComplete()) {
                    scheduleEventNotification(taskId, taskName, selectedDate, selectedTime);
                }

                Toast.makeText(this, "Task updated successfully", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Failed to update task", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Check if exact alarm permission is granted (Android 12+)
     */
    private boolean checkExactAlarmPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                // Show dialog explaining why we need this permission
                new AlertDialog.Builder(this)
                        .setTitle("Alarm Permission Required")
                        .setMessage("This app needs permission to schedule exact alarms for task reminders. " +
                                "Please grant this permission in the next screen.")
                        .setPositiveButton("Grant Permission", (dialog, which) -> {
                            // Open settings to grant permission
                            Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            startActivity(intent);
                        })
                        .setNegativeButton("Skip", null)
                        .show();
                return false;
            }
        }
        return true;
    }

    /**
     * Schedule a reminder notification for a task
     */
    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    private void scheduleEventNotification(int eventId, String eventName, String eventDate, String eventTime) {
        try {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            if (alarmManager == null) {
                Log.e("AddEditEventActivity", "AlarmManager is null!");
                return;
            }

            // Check if we can schedule exact alarms (Android 12+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.w("AddEditEventActivity", "Cannot schedule exact alarms - permission not granted");
                    Toast.makeText(this, "Exact alarm permission needed for notifications", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            // Parse the date to schedule notification
            // Format: MM/DD/YYYY
            String[] dateParts = eventDate.split("/");
            int month = Integer.parseInt(dateParts[0]) - 1; // Month is 0-based
            int day = Integer.parseInt(dateParts[1]);
            int year = Integer.parseInt(dateParts[2]);

            // Set notification time to 9:00 AM on event day
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            calendar.set(Calendar.HOUR_OF_DAY, 9);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);

            long triggerTime = calendar.getTimeInMillis();

            // Only schedule if the event is in the future
            if (triggerTime > System.currentTimeMillis()) {
                Log.i("AddEditEventActivity", "Scheduling notification for: " + eventName);
                Log.i("AddEditEventActivity", "Will trigger at: " + calendar.getTime());

                // Create intent for notification
                Intent intent = new Intent(this, EventNotificationReceiver.class);
                intent.putExtra(AppConstants.EXTRA_EVENT_NAME, eventName);
                intent.putExtra(AppConstants.EXTRA_EVENT_DATE, eventDate);
                intent.putExtra(AppConstants.EXTRA_EVENT_TIME, eventTime);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this,
                        eventId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                // Schedule notification using AlarmManager
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                    );

                    Log.i("AddEditEventActivity", "Notification scheduled successfully!");
                    Toast.makeText(this, "Task reminder set for " + eventDate + " at 9:00 AM", Toast.LENGTH_SHORT).show();
                } catch (SecurityException e) {
                    Log.e("AddEditEventActivity", "SecurityException: Cannot schedule exact alarm", e);
                    Toast.makeText(this, "Cannot schedule notifications - permission denied", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.i("AddEditEventActivity", "Event is in the past - not scheduling notification");
            }
        } catch (Exception e) {
            Log.e("AddEditEventActivity", "Error scheduling notification: " + e.getMessage());
            e.printStackTrace();
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
     * Delete the current task from the database
     */
    private void deleteTask() {
        // Cancel the scheduled notification
        cancelEventNotification(taskId);

        int result = databaseHelper.deleteEvent(taskId);

        if (result > 0) {
            Toast.makeText(this, "Task deleted successfully", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Failed to delete task", Toast.LENGTH_SHORT).show();
        }
    }
}