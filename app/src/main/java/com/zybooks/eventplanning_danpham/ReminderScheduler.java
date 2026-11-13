package com.zybooks.eventplanning_danpham;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

/**
 * ReminderScheduler schedules and cancels task reminders in one place.
 * Replaces the copies of this code that were spread across three screens.
 */
public class ReminderScheduler {

    private static final String TAG = "ReminderScheduler";

    // How long before the due time the reminder fires
    public static final long REMINDER_LEAD_MINUTES = 60;

    private final Context context;

    public ReminderScheduler(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * @return true if the app is allowed to schedule exact alarms
     */
    public boolean canScheduleReminders() {
        AlarmManager alarmManager = getAlarmManager();
        return alarmManager != null && alarmManager.canScheduleExactAlarms();
    }

    /**
     * Schedule a reminder before the task's due time.
     * Completed tasks and tasks already past due don't get a reminder.
     * @return true if a reminder was scheduled
     */
    public boolean schedule(MaintenanceTask task) {
        if (task.isComplete()) {
            return false;
        }

        LocalDateTime due = task.getDueDateTime();
        if (due == null) {
            Log.w(TAG, "Can't read due date for task " + task.getTaskId());
            return false;
        }

        long dueMillis = due.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long now = System.currentTimeMillis();
        if (dueMillis <= now) {
            return false; // Already past due
        }

        // Remind ahead of the due time. If that moment has already passed, remind now.
        long triggerMillis = Math.max(dueMillis - TimeUnit.MINUTES.toMillis(REMINDER_LEAD_MINUTES), now);

        if (!canScheduleReminders()) {
            Log.w(TAG, "Exact alarm permission not granted");
            return false;
        }

        try {
            getAlarmManager().setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerMillis, buildPendingIntent(task.getTaskId(), task));
            Log.i(TAG, "Reminder scheduled for task " + task.getTaskId());
            return true;
        } catch (SecurityException e) {
            Log.e(TAG, "Not allowed to schedule exact alarm", e);
            return false;
        }
    }

    /**
     * Cancel a task's reminder, if it has one
     */
    public void cancel(int taskId) {
        AlarmManager alarmManager = getAlarmManager();
        if (alarmManager != null) {
            alarmManager.cancel(buildPendingIntent(taskId, null));
        }
    }

    private AlarmManager getAlarmManager() {
        return (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * Build the PendingIntent that identifies a task's reminder.
     * The task ID is the request code, so cancel() finds the same reminder.
     */
    private PendingIntent buildPendingIntent(int taskId, MaintenanceTask task) {
        Intent intent = new Intent(context, EventNotificationReceiver.class);
        if (task != null) {
            intent.putExtra(AppConstants.EXTRA_EVENT_NAME, task.getTaskName());
            intent.putExtra(AppConstants.EXTRA_EVENT_DATE, task.getDueDate());
            intent.putExtra(AppConstants.EXTRA_EVENT_TIME, task.getDueTime());
        }
        return PendingIntent.getBroadcast(context, taskId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}