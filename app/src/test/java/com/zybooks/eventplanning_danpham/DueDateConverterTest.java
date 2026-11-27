package com.zybooks.eventplanning_danpham;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * Unit tests for DueDateConverter. Uses a fixed time zone so results
 * are the same on any computer.
 */
public class DueDateConverterTest {

    private static final ZoneId EASTERN = ZoneId.of("America/New_York");

    @Test
    public void toMillis_thenFormat_givesBackSameText() {
        long millis = DueDateConverter.toMillis("12/15/2026", "01:30 PM", EASTERN);

        assertEquals("12/15/2026", DueDateConverter.formatDate(millis, EASTERN));
        assertEquals("01:30 PM", DueDateConverter.formatTime(millis, EASTERN));
    }

    @Test
    public void toMillis_sortsAcrossYears() {
        // The version 2 bug: as text, January 2027 sorted before December 2026
        long december = DueDateConverter.toMillis("12/15/2026", "01:30 PM", EASTERN);
        long january = DueDateConverter.toMillis("01/05/2027", "08:00 AM", EASTERN);

        assertTrue(december < january);
    }

    @Test
    public void toMillis_sortsAfternoonAfterMorning() {
        // As text, "01:00 PM" sorted before "11:00 AM"
        long morning = DueDateConverter.toMillis("12/15/2026", "11:00 AM", EASTERN);
        long afternoon = DueDateConverter.toMillis("12/15/2026", "01:00 PM", EASTERN);

        assertTrue(morning < afternoon);
    }

    @Test(expected = DateTimeParseException.class)
    public void toMillis_badText_throws() {
        DueDateConverter.toMillis("next Tuesday", "noon", EASTERN);
    }

    @Test
    public void formatDateTime_combinesDateAndTime() {
        long millis = DueDateConverter.toMillis("12/15/2026", "01:30 PM", EASTERN);

        assertEquals("12/15/2026 01:30 PM", DueDateConverter.formatDateTime(millis, EASTERN));
    }

    @Test
    public void nextDueMillis_addsDays() {
        long from = DueDateConverter.toMillis("12/15/2026", "01:30 PM", EASTERN);

        long next = DueDateConverter.nextDueMillis(from, 30, EASTERN);

        assertEquals("01/14/2027 01:30 PM", DueDateConverter.formatDateTime(next, EASTERN));
    }

    @Test
    public void nextDueMillis_keepsTimeAcrossDaylightSavingChange() {
        // Daylight saving time ends November 1, 2026. Adding 7 x 24 hours would
        // land at 9:00 AM; adding 7 calendar days keeps it at 10:00 AM.
        long from = DueDateConverter.toMillis("10/28/2026", "10:00 AM", EASTERN);

        long next = DueDateConverter.nextDueMillis(from, 7, EASTERN);

        assertEquals("11/04/2026 10:00 AM", DueDateConverter.formatDateTime(next, EASTERN));
    }
}
