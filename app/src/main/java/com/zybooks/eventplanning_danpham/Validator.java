package com.zybooks.eventplanning_danpham;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * Validator holds all input rules in one place.
 * It uses no Android classes, so it can be tested with plain JUnit.
 * Each method returns an error message, or null if the input is valid.
 */
public final class Validator {

    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_REPEAT_DAYS = 3650; // 10 years
    public static final int MAX_MODEL_LENGTH = 30;

    // 2 to 10 letters, numbers, or dashes, such as N12345 or 165432
    private static final String TAIL_NUMBER_PATTERN = "[A-Za-z0-9-]{2,10}";

    private Validator() {
        // Prevent creating instances of this class
    }

    /**
     * Check login or registration input
     * @return an error message, or null if valid
     */
    public static String checkLogin(String username, String password) {
        if (username == null || username.isEmpty()) {
            return "Please enter a username";
        }
        if (username.length() < MIN_USERNAME_LENGTH) {
            return "Username must be at least " + MIN_USERNAME_LENGTH + " characters";
        }
        if (password == null || password.isEmpty()) {
            return "Please enter a password";
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters";
        }
        return null;
    }

    /**
     * Check maintenance task input
     * @param earliestAllowed the earliest allowed due date and time, passed in
     *                        so tests can control it. Use LocalDateTime.MIN to
     *                        allow past dates when editing an existing task.
     * @return an error message, or null if valid
     */
    public static String checkTask(String taskName, String tailNumber, String date,
                                   String time, LocalDateTime earliestAllowed) {
        // Required fields
        if (taskName == null || taskName.trim().isEmpty()) {
            return "Task name is required";
        }
        if (tailNumber == null || tailNumber.trim().isEmpty()) {
            return "Tail number is required";
        }
        if (!tailNumber.trim().matches(TAIL_NUMBER_PATTERN)) {
            return "Tail number must be 2 to 10 letters, numbers, or dashes";
        }
        if (date == null || date.isEmpty()) {
            return "Please select a date";
        }
        if (time == null || time.isEmpty()) {
            return "Please select a time";
        }

        // Make sure the date and time can be read
        LocalDateTime dueDateTime;
        try {
            dueDateTime = LocalDateTime.parse(date + " " + time, AppConstants.DUE_DATE_TIME_FORMAT);
        } catch (DateTimeParseException e) {
            return "Invalid date or time";
        }

        // New tasks can't be due in the past
        if (dueDateTime.isBefore(earliestAllowed)) {
            return "Date and time can't be in the past";
        }

        return null;
    }

    /**
     * Check the optional repeat interval. Blank means the task doesn't repeat.
     * @return an error message, or null if valid
     */
    public static String checkRepeatDays(String repeatDays) {
        if (repeatDays == null || repeatDays.trim().isEmpty()) {
            return null;
        }
        int days;
        try {
            days = Integer.parseInt(repeatDays.trim());
        } catch (NumberFormatException e) {
            return "Repeat interval must be a whole number of days";
        }
        if (days < 1 || days > MAX_REPEAT_DAYS) {
            return "Repeat interval must be between 1 and " + MAX_REPEAT_DAYS + " days";
        }
        return null;
    }

    /**
     * Check the optional aircraft model
     * @return an error message, or null if valid
     */
    public static String checkAircraftModel(String model) {
        if (model != null && model.trim().length() > MAX_MODEL_LENGTH) {
            return "Aircraft model must be " + MAX_MODEL_LENGTH + " characters or less";
        }
        return null;
    }

    /**
     * Check a password change. The current password is checked against
     * the database later; this only checks the new one.
     * @return an error message, or null if valid
     */
    public static String checkNewPassword(String currentPassword, String newPassword,
                                          String confirmPassword) {
        if (currentPassword == null || currentPassword.isEmpty()) {
            return "Please enter your current password";
        }
        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            return "New password must be at least " + MIN_PASSWORD_LENGTH + " characters";
        }
        if (!newPassword.equals(confirmPassword)) {
            return "New passwords don't match";
        }
        if (newPassword.equals(currentPassword)) {
            return "New password must be different from the current one";
        }
        return null;
    }
}