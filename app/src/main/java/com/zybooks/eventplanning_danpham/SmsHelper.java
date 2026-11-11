package com.zybooks.eventplanning_danpham;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.util.Log;
import androidx.core.content.ContextCompat;

/**
 * SmsHelper manages SMS notification functionality.
 * Handles sending SMS messages for event reminders.
 */
public class SmsHelper {

    private static final String TAG = "SmsHelper";
    private Context context;

    public SmsHelper(Context context) {
        this.context = context;
    }

    /**
     * Check if SMS permission is granted
     * @return true if permission granted, false otherwise
     */
    public boolean hasSmsPermission() {
        boolean hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;

        Log.i(TAG, "SMS permission check: " + hasPermission);
        return hasPermission;
    }

    /**
     * Send SMS notification for an event
     * @param phoneNumber Phone number to send SMS to
     * @param eventName Name of the event
     * @param eventDate Date of the event
     * @param eventTime Time of the event
     * @return true if SMS sent successfully, false otherwise
     */
    public boolean sendEventNotification(String phoneNumber, String eventName,
                                         String eventDate, String eventTime) {
        if (!hasSmsPermission()) {
            Log.w(TAG, "Cannot send SMS - permission not granted");
            return false;
        }

        try {
            // Create SMS message
            String message = "Event Reminder: " + eventName +
                    " is scheduled for " + eventDate + " at " + eventTime;

            Log.i(TAG, "Sending SMS notification for event: " + eventName);

            // Get SMS manager and send message
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);

            Log.i(TAG, "SMS sent successfully");
            return true;
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException when sending SMS: " + e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid phone number or message: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS: " + e.getMessage());
            return false;
        }
    }

    /**
     * Send a simple test SMS
     * @param phoneNumber Phone number to send SMS to
     * @param message Message content
     * @return true if SMS sent successfully, false otherwise
     */
    public boolean sendTestSms(String phoneNumber, String message) {
        if (!hasSmsPermission()) {
            Log.w(TAG, "SMS permission not granted");
            return false;
        }

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Log.i(TAG, "Test SMS sent successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error sending test SMS: " + e.getMessage());
            return false;
        }
    }
}
