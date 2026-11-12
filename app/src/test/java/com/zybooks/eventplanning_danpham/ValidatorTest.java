package com.zybooks.eventplanning_danpham;

import org.junit.Test;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Unit tests for the Validator class
 */
public class ValidatorTest {

    // A fixed "current time" so the tests always give the same result
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 9, 0);

    // ===== Login tests =====

    @Test
    public void checkLogin_validInput_returnsNull() {
        assertNull(Validator.checkLogin("dan", "secret1"));
    }

    @Test
    public void checkLogin_shortPassword_returnsError() {
        assertEquals("Password must be at least 6 characters",
                Validator.checkLogin("dan", "12345"));
    }

    @Test
    public void checkLogin_emptyUsername_returnsError() {
        assertEquals("Please enter a username",
                Validator.checkLogin("", "123456"));
    }

    @Test
    public void checkLogin_nullUsername_returnsError() {
        assertEquals("Please enter a username",
                Validator.checkLogin(null, "123456"));
    }

    @Test
    public void checkLogin_shortUsername_returnsError() {
        assertEquals("Username must be at least 3 characters",
                Validator.checkLogin("da", "123456"));
    }

    @Test
    public void checkLogin_threeCharUsername_returnsNull() {
        assertNull(Validator.checkLogin("cam", "123456"));
    }

    @Test
    public void checkLogin_emptyPassword_returnsError() {
        assertEquals("Please enter a password",
                Validator.checkLogin("dan", ""));
    }

    @Test
    public void checkLogin_sixCharPassword_returnsNull() {
        assertNull(Validator.checkLogin("dan", "234556"));
    }

    // ===== Event tests =====

    @Test
    public void checkEvent_futureEvent_returnsNull() {
        assertNull(Validator.checkEvent("Engine inspection", "12/15/2026", "01:30 PM", NOW));
    }

    @Test
    public void checkEvent_pastDate_returnsError() {
        assertEquals("Date and time can't be in the past",
                Validator.checkEvent("Engine inspection", "11/01/2026", "01:30 PM", NOW));
    }

    @Test
    public void checkEvent_blankName_returnsError() {
        assertEquals("Event name is required",
                Validator.checkEvent("", "12/15/2026", "01:30 PM", NOW));
    }

    @Test
    public void checkEvent_spacesOnlyName_returnsError() {
        assertEquals("Event name is required",
                Validator.checkEvent("   ", "12/15/2026", "01:30 PM", NOW));
    }

    @Test
    public void checkEvent_missingDate_returnsError() {
        assertEquals("Please select a date",
                Validator.checkEvent("Engine inspection", "", "01:30 PM", NOW));
    }

    @Test
    public void checkEvent_missingTime_returnsError() {
        assertEquals("Please select a time",
                Validator.checkEvent("Engine inspection", "12/15/2026", "", NOW));
    }

    @Test
    public void checkEvent_badDateFormat_returnsError() {
        assertEquals("Invalid date or time",
                Validator.checkEvent("Engine inspection", "2026-12-15", "01:30 PM", NOW));
    }

    @Test
    public void checkEvent_exactlyNow_returnsNull() {
        // Boundary test: an event at the current minute is not in the past
        assertNull(Validator.checkEvent("Engine inspection", "11/12/2026", "09:00 AM", NOW));
    }
}
