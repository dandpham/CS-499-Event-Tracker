package com.zybooks.eventplanning_danpham;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * DueDateConverter converts between the date and time text the screens use,
 * such as "12/15/2026" and "01:30 PM", and the timestamps the database stores.
 * Timestamps are milliseconds since 1970 in UTC, so they sort and compare correctly.
 * The time zone is passed in, so tests always give the same result.
 */
public final class DueDateConverter {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.US);

    private DueDateConverter() {
        // Prevent creating instances of this class
    }

    /**
     * Convert date and time text to a timestamp
     * @throws DateTimeParseException if the text can't be read
     */
    public static long toMillis(String date, String time, ZoneId zone) {
        LocalDateTime local = LocalDateTime.parse(date + " " + time, AppConstants.DUE_DATE_TIME_FORMAT);
        return local.atZone(zone).toInstant().toEpochMilli();
    }

    /** @return the date part of a timestamp, such as "12/15/2026" */
    public static String formatDate(long millis, ZoneId zone) {
        return Instant.ofEpochMilli(millis).atZone(zone).format(DATE_FORMAT);
    }

    /** @return the time part of a timestamp, such as "01:30 PM" */
    public static String formatTime(long millis, ZoneId zone) {
        return Instant.ofEpochMilli(millis).atZone(zone).format(TIME_FORMAT);
    }

    /** @return a timestamp as one line, such as "12/15/2026 01:30 PM" */
    public static String formatDateTime(long millis, ZoneId zone) {
        return formatDate(millis, zone) + " " + formatTime(millis, zone);
    }

    /**
     * Work out when the next recurring task is due.
     * Adds calendar days rather than a fixed number of hours, so the due time
     * stays the same when daylight saving time starts or ends.
     */
    public static long nextDueMillis(long fromMillis, int repeatDays, ZoneId zone) {
        ZonedDateTime from = Instant.ofEpochMilli(fromMillis).atZone(zone);
        return from.plusDays(repeatDays).toInstant().toEpochMilli();
    }
}
