package com.zybooks.eventplanning_danpham;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Validator holds all input rules in one place.
 * It uses no Android classes, so it can be tested with plain JUnit.
 * Each method returns an error message, or null if the input is valid.
 */
public final class Validator {

    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MIN_PASSWORD_LENGTH = 6;

    // 2 to 10 letters, numbers, or dashes, such as N12345 or 165432
    private static final String TAIL_NUMBER_PATTERN = "[A-Za-z0-9-]{2,10}";

    // Matches how the date and time pickers format their values,
    // such as "12/15/2026" and "01:30 PM"
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a", Locale.US);

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
            dueDateTime = LocalDateTime.parse(date + " " + time, DATE_TIME_FORMAT);
        } catch (DateTimeParseException e) {
            return "Invalid date or time";
        }

        // New tasks can't be due in the past
        if (dueDateTime.isBefore(earliestAllowed)) {
            return "Date and time can't be in the past";
        }

        return null;
    }
}