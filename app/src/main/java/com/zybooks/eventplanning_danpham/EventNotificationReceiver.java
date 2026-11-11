package com.zybooks.eventplanning_danpham;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

/**
 * BroadcastReceiver that triggers SMS notifications for events
 */
public class EventNotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "EventNotificationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Event notification triggered");

        // Get event details from intent
        String eventName = intent.getStringExtra(AppConstants.EXTRA_EVENT_NAME);
        String eventDate = intent.getStringExtra(AppConstants.EXTRA_EVENT_DATE);
        String eventTime = intent.getStringExtra(AppConstants.EXTRA_EVENT_TIME);

        Log.i(TAG, "Event: " + eventName + " on " + eventDate + " at " + eventTime);

        // Check if notifications are enabled
        SharedPreferences prefs = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean(AppConstants.KEY_NOTIFICATIONS_ENABLED, true);

        if (!notificationsEnabled) {
            Log.i(TAG, "Notifications disabled in settings");
            return;
        }

        // Send SMS notification
        SmsHelper smsHelper = new SmsHelper(context);

        if (smsHelper.hasSmsPermission()) {
            Log.i(TAG, "SMS permission granted - sending notification");

            // Use emulator test number or get from user settings in production
            String phoneNumber = "5556";

            boolean sent = smsHelper.sendEventNotification(phoneNumber, eventName, eventDate, eventTime);

            if (sent) {
                Log.i(TAG, "SMS notification sent successfully");
                Toast.makeText(context, "Event Reminder: " + eventName, Toast.LENGTH_LONG).show();
            } else {
                Log.e(TAG, "Failed to send SMS notification");
                showSystemNotification(context, eventName, eventDate, eventTime);
            }
        } else {
            Log.w(TAG, "SMS permission not granted - using fallback notification");
            showSystemNotification(context, eventName, eventDate, eventTime);
        }
    }

    /**
     * Show system notification as fallback when SMS is not available
     */
    private void showSystemNotification(Context context, String eventName, String eventDate, String eventTime) {
        Log.i(TAG, "Showing system notification");
        Toast.makeText(context,
                "Event Reminder: " + eventName + " at " + eventTime,
                Toast.LENGTH_LONG).show();
    }
}