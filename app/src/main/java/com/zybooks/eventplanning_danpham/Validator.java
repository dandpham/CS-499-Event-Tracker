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
     * Check event input
     * @param now the current date and time, passed in so tests can control it
     * @return an error message, or null if valid
     */
    public static String checkEvent(String name, String date, String time, LocalDateTime now) {
        // Required fields
        if (name == null || name.trim().isEmpty()) {
            return "Event name is required";
        }

        if (date == null || date.isEmpty()) {
            return "Please select a date";
        }

        if (time == null || time.isEmpty()) {
            return "Please select a time";
        }

        // Make sure the date and time can be read
        LocalDateTime eventDateTime;
        try {
            eventDateTime = LocalDateTime.parse(date + " " + time, DATE_TIME_FORMAT);
        } catch (DateTimeParseException e) {
            return "Invalid date or time";
        }

        // Events can't be scheduled in the past
        if (eventDateTime.isBefore(now)) {
            return "Date and time can't be in the past";
        }

        return null;
    }
}