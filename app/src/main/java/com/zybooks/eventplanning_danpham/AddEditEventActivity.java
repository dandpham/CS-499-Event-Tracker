package com.zybooks.eventplanning_danpham;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;
import android.content.Intent;
import java.util.Calendar;
import java.time.LocalDateTime;

/**
 * AddEditEventActivity handles creating new events and editing existing events.
 * Provides form input for all event details.
 */

public class AddEditEventActivity extends AppCompatActivity {

    // UI components
    private TextView textViewScreenTitle;
    private EditText editTextEventName;
    private Button buttonSelectDate;
    private TextView textViewSelectedDate;
    private Button buttonSelectTime;
    private TextView textViewSelectedTime;
    private EditText editTextLocation;
    private EditText editTextDescription;
    private Button buttonSave;
    private Button buttonCancel;
    private Button buttonDelete;

    // Data components
    private DatabaseHelper databaseHelper;
    private int userId;
    private int eventId = AppConstants.NO_ID; // NO_ID means a new event; otherwise editing an existing one

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

        // Get event ID from intent (if editing)
        eventId = getIntent().getIntExtra(AppConstants.EXTRA_EVENT_ID, AppConstants.NO_ID);

        // Initialize UI components
        initializeViews();

        // Check if we're editing an existing event
        if (eventId != AppConstants.NO_ID) {
            loadEventData();
        } else {
            textViewScreenTitle.setText("Add Event");
            buttonDelete.setVisibility(View.GONE);
        }

        // Set up listeners
        setupListeners();
    }

    /**
     * Initialize all UI components
     */
    private void initializeViews() {
        textViewScreenTitle = findViewById(R.id.textViewScreenTitle);
        editTextEventName = findViewById(R.id.editTextEventName);
        buttonSelectDate = findViewById(R.id.buttonSelectDate);
        textViewSelectedDate = findViewById(R.id.textViewSelectedDate);
        buttonSelectTime = findViewById(R.id.buttonSelectTime);
        textViewSelectedTime = findViewById(R.id.textViewSelectedTime);
        editTextLocation = findViewById(R.id.editTextLocation);
        editTextDescription = findViewById(R.id.editTextDescription);
        buttonSave = findViewById(R.id.buttonSave);
        buttonCancel = findViewById(R.id.buttonCancel);
        buttonDelete = findViewById(R.id.buttonDelete);
    }

    /**
     * Load existing event data for editing
     */
    private void loadEventData() {
        MaintenanceTask existingEvent = databaseHelper.getEvent(eventId);

        if (existingEvent != null) {
            textViewScreenTitle.setText("Edit Event");
            editTextEventName.setText(existingEvent.getTaskName());
            selectedDate = existingEvent.getDueDate();
            textViewSelectedDate.setText(selectedDate);
            selectedTime = existingEvent.getDueTime();
            textViewSelectedTime.setText(selectedTime);
            editTextLocation.setText(existingEvent.getTailNumber());
            editTextDescription.setText(existingEvent.getNotes());
            buttonDelete.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Set up all button click listeners
     */
    private void setupListeners() {
        // Date picker button
        buttonSelectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        // Time picker button
        buttonSelectTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker();
            }
        });

        // Save button
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveEvent();
            }
        });

        // Cancel button
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Delete button
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDeleteConfirmation();
            }
        });
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
                        selectedDate = String.format("%02d/%02d/%04d", selectedMonth + 1, selectedDay, selectedYear);
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
                        selectedTime = String.format("%02d:%02d %s", displayHour, selectedMinute, amPm);
                        textViewSelectedTime.setText(selectedTime);
                    }
                },
                hour, minute, false // false for 12-hour format
        );

        timePickerDialog.show();
    }

    /**
     * Validate and save event to database
     */
    @SuppressLint("ScheduleExactAlarm")
    private void saveEvent() {
        // Get input values
        String eventName = editTextEventName.getText().toString().trim();
        String location = editTextLocation.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();

        // Validate input using the shared Validator rules.
        // New events can't be in the past. Existing events can be,
        // since an overdue task still needs to be edited.
        boolean isNewEvent = (eventId == AppConstants.NO_ID);
        LocalDateTime earliestAllowed = isNewEvent ? LocalDateTime.now() : LocalDateTime.MIN;

        String error = Validator.checkEvent(eventName, selectedDate, selectedTime, earliestAllowed);
        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check exact alarm permission before saving
        checkExactAlarmPermission();

        // Create or update event
        if (eventId == AppConstants.NO_ID) {
            // Create new event
            MaintenanceTask newEvent = new MaintenanceTask(userId, eventName, selectedDate, selectedTime, location, description);
            long result = databaseHelper.addEvent(newEvent);

            if (result != AppConstants.NO_ID) {
                // Schedule SMS notification for this event
                scheduleEventNotification((int) result, eventName, selectedDate, selectedTime);

                Toast.makeText(this, "Event added successfully", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Failed to add event", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Update existing event
            MaintenanceTask updatedEvent = new MaintenanceTask(eventId, userId, eventName, selectedDate, selectedTime, location, description);
            int result = databaseHelper.updateEvent(updatedEvent);

            if (result > 0) {
                // Reschedule notification with updated details
                cancelEventNotification(eventId);
                scheduleEventNotification(eventId, eventName, selectedDate, selectedTime);

                Toast.makeText(this, "Event updated successfully", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(this, "Failed to update event", Toast.LENGTH_SHORT).show();
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
                        .setMessage("This app needs permission to schedule exact alarms for event notifications. " +
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
     * Schedule SMS notification for an event
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
                    Toast.makeText(this, "Event reminder set for " + eventDate + " at 9:00 AM", Toast.LENGTH_SHORT).show();
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
     * Show confirmation dialog before deleting event
     */
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteEvent();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Delete the current event from database
     */
    private void deleteEvent() {
        // Cancel the scheduled notification
        cancelEventNotification(eventId);

        int result = databaseHelper.deleteEvent(eventId);

        if (result > 0) {
            Toast.makeText(this, "Event deleted successfully", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Failed to delete event", Toast.LENGTH_SHORT).show();
        }
    }
}
