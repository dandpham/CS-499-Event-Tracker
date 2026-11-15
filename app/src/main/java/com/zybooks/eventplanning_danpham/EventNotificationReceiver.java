package com.zybooks.eventplanning_danpham;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

/**
 * EventNotificationReceiver runs when a task's reminder fires.
 * It sends an SMS reminder when SMS permission is granted,
 * and always shows an on-screen reminder.
 */
public class EventNotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "EventNotificationReceiver";

    // The Android emulator's built-in test number. A production version of this
    // app would let the user enter their own phone number in Settings.
    private static final String TEST_PHONE_NUMBER = "5556";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Get task details from the reminder
        String taskName = intent.getStringExtra(AppConstants.EXTRA_EVENT_NAME);
        String dueDate = intent.getStringExtra(AppConstants.EXTRA_EVENT_DATE);
        String dueTime = intent.getStringExtra(AppConstants.EXTRA_EVENT_TIME);

        Log.i(TAG, "Reminder for task: " + taskName + " due " + dueDate + " at " + dueTime);

        // Respect the notifications setting
        SharedPreferences prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean(AppConstants.KEY_NOTIFICATIONS_ENABLED, true);
        if (!notificationsEnabled) {
            Log.i(TAG, "Notifications disabled in settings");
            return;
        }

        // Send an SMS reminder if permission is granted
        SmsHelper smsHelper = new SmsHelper(context);
        if (smsHelper.hasSmsPermission()
                && smsHelper.sendEventNotification(TEST_PHONE_NUMBER, taskName, dueDate, dueTime)) {
            Log.i(TAG, "SMS reminder sent");
        } else {
            Log.w(TAG, "SMS not available, showing on-screen reminder only");
        }

        showReminderToast(context, taskName, dueTime);
    }

    /**
     * Show a short on-screen reminder
     */
    private void showReminderToast(Context context, String taskName, String dueTime) {
        Toast.makeText(context,
                "Task Reminder: " + taskName + " is due at " + dueTime,
                Toast.LENGTH_LONG).show();
    }
}