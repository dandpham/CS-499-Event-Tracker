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

    private static final String TAIL_ERROR =
            "Tail number must be 2 to 10 letters, numbers, or dashes";

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

    // ===== Task tests =====

    @Test
    public void checkTask_futureTask_returnsNull() {
        assertNull(Validator.checkTask("Engine inspection", "N12345", "12/15/2026", "01:30 PM", NOW));
    }

    @Test
    public void checkTask_pastDate_returnsError() {
        assertEquals("Date and time can't be in the past",
                Validator.checkTask("Engine inspection", "N12345", "11/01/2026", "01:30 PM", NOW));
    }

    @Test
    public void checkTask_pastDateWhenEditing_returnsNull() {
        // Existing tasks can be overdue, so editing allows past dates
        assertNull(Validator.checkTask("Engine inspection", "N12345", "11/01/2026", "01:30 PM",
                LocalDateTime.MIN));
    }

    @Test
    public void checkTask_blankName_returnsError() {
        assertEquals("Task name is required",
                Validator.checkTask("", "N12345", "12/15/2026", "01:30 PM", NOW));
    }

    @Test
    public void checkTask_spacesOnlyName_returnsError() {
        assertEquals("Task name is required",
                Validator.checkTask("   ", "N12345", "12/15/2026", "01:30 PM", NOW));
    }

    @Test
    public void checkTask_missingTailNumber_returnsError() {
        assertEquals("Tail number is required",
                Validator.checkTask("Engine inspection", "", "12/15/2026", "01:30 PM", NOW));
    }

    @Test
    public void checkTask_tailNumberWithSpace_returnsError() {
        assertEquals(TAIL_ERROR,
                Validator.checkTask("Engine inspection", "N12 345", "12/15/2026", "01:30 PM", NOW));
    }

    @Test
    public void checkTask_tailNumberTooLong_returnsError() {
        // 11 characters, one over the limit
        assertEquals(TAIL_ERROR,
                Validator.checkTask("Engine inspection", "N123456789X", "12/15/2026", "01:30 PM", NOW));
    }

    @Test
    public void checkTask_tailNumberTooShort_returnsError() {
        assertEquals(TAIL_ERROR,
                Validator.checkTask("Engine inspection", "N", "12/15/2026", "01:30 PM", NOW));
    }

    @Test
    public void checkTask_militaryTailNumberWithDash_returnsNull() {
        assertNull(Validator.checkTask("Engine inspection", "165-432", "12/15/2026", "01:30 PM", NOW));
    }

    @Test
    public void checkTask_missingDate_returnsError() {
        assertEquals("Please select a date",
                Validator.checkTask("Engine inspection", "N12345", "", "01:30 PM", NOW));
    }

    @Test
    public void checkTask_missingTime_returnsError() {
        assertEquals("Please select a time",
                Validator.checkTask("Engine inspection", "N12345", "12/15/2026", "", NOW));
    }

    @Test
    public void checkTask_badDateFormat_returnsError() {
        assertEquals("Invalid date or time",
                Validator.checkTask("Engine inspection", "N12345", "2026-12-15", "01:30 PM", NOW));
    }

    @Test
    public void checkTask_exactlyNow_returnsNull() {
        // Boundary test: a task due at the current minute is not in the past
        assertNull(Validator.checkTask("Engine inspection", "N12345", "11/12/2026", "09:00 AM", NOW));
    }
}