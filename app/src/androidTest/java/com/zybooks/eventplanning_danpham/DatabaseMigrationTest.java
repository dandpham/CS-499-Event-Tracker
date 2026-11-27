package com.zybooks.eventplanning_danpham;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.ZoneId;
import java.util.List;

/**
 * Instrumented test for upgrading a version 2 database to version 3.
 * It builds a real version 2 database with the old schema and old data,
 * opens it with the new DatabaseHelper, and checks that nothing was lost.
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseMigrationTest {

    private static final String TEST_DB = "migration-test.db";
    private static final ZoneId ZONE = ZoneId.of("America/New_York");

    private Context context;
    private DatabaseHelper helper;

    @Before
    public void createVersion2Database() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(TEST_DB);

        // The exact version 2 schema, with plain text passwords and text dates
        SQLiteDatabase old = SQLiteDatabase.openOrCreateDatabase(
                context.getDatabasePath(TEST_DB), null);
        old.execSQL("CREATE TABLE users (user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE NOT NULL, password TEXT NOT NULL)");
        old.execSQL("CREATE TABLE events (event_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL, event_name TEXT NOT NULL, event_date TEXT NOT NULL, " +
                "event_time TEXT NOT NULL, location TEXT, description TEXT, " +
                "task_type TEXT NOT NULL DEFAULT 'Inspection', status TEXT NOT NULL DEFAULT 'Open', " +
                "FOREIGN KEY(user_id) REFERENCES users(user_id))");

        old.execSQL("INSERT INTO users (user_id, username, password) VALUES (1, 'dan', 'secret123')");
        old.execSQL("INSERT INTO users (user_id, username, password) VALUES (2, 'sam', 'hunter22')");

        old.execSQL("INSERT INTO events VALUES (5, 1, 'Oil change', '12/15/2026', '01:30 PM', " +
                "'n12345', 'Use 15W-50', 'Inspection', 'Open')");
        old.execSQL("INSERT INTO events VALUES (6, 1, 'January check', '01/05/2027', '08:00 AM', " +
                "'N12345', NULL, 'Inspection', 'Open')");
        old.execSQL("INSERT INTO events VALUES (7, 1, 'Old event', '11/01/2026', '09:00 AM', " +
                "'UNASSIGNED', '', 'Inspection', 'Open')");
        old.execSQL("INSERT INTO events VALUES (8, 1, 'Bad date', 'someday', 'soon', " +
                "'N12345', 'Original note', 'Repair', 'Open')");
        old.execSQL("INSERT INTO events VALUES (9, 2, 'Finished repair', '10/01/2026', '10:00 AM', " +
                "'N777', '', 'Repair', 'Complete')");
        // An event whose user doesn't exist. Version 2 never turned on foreign keys.
        old.execSQL("INSERT INTO events VALUES (10, 99, 'Orphan', '12/01/2026', '10:00 AM', " +
                "'N1', '', 'Repair', 'Open')");

        old.setVersion(2);
        old.close();

        // Opening with the new helper runs onUpgrade from version 2 to 3
        helper = new DatabaseHelper(context, TEST_DB, ZONE);
    }

    @After
    public void tearDown() {
        helper.close();
        context.deleteDatabase(TEST_DB);
    }

    private int count(String sql) {
        try (Cursor cursor = helper.getReadableDatabase().rawQuery(sql, null)) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        }
    }

    @Test
    public void upgrade_setsVersion3() {
        assertEquals(3, helper.getReadableDatabase().getVersion());
    }

    @Test
    public void upgrade_hashesPasswordsAndOldPasswordsStillWork() {
        assertEquals(1, helper.validateUser("dan", "secret123").getUserId());
        assertEquals(2, helper.validateUser("sam", "hunter22").getUserId());
        assertNull(helper.validateUser("dan", "wrongpass"));

        // No plain text passwords are left anywhere
        try (Cursor cursor = helper.getReadableDatabase().rawQuery("SELECT * FROM users", null)) {
            assertEquals(-1, cursor.getColumnIndex("password"));
            while (cursor.moveToNext()) {
                String hash = cursor.getString(cursor.getColumnIndexOrThrow("password_hash"));
                assertFalse(hash.equals("secret123") || hash.equals("hunter22"));
            }
        }
    }

    @Test
    public void upgrade_keepsEveryOwnedTaskWithItsId() {
        assertEquals(5, count("SELECT COUNT(*) FROM tasks"));
        assertEquals("Oil change", helper.getTask(5, 1).getTaskName());
        assertEquals("Use 15W-50", helper.getTask(5, 1).getNotes());
        assertEquals("Finished repair", helper.getTask(9, 2).getTaskName());
    }

    @Test
    public void upgrade_skipsEventsWithNoUser() {
        assertEquals(0, count("SELECT COUNT(*) FROM tasks WHERE task_name = 'Orphan'"));
    }

    @Test
    public void upgrade_makesOneAircraftPerTailNumber() {
        // "n12345" and "N12345" become the same aircraft
        assertEquals(1, count("SELECT COUNT(*) FROM aircraft WHERE owner_id = 1 AND tail_number = 'N12345'"));
        assertEquals("N12345", helper.getTask(5, 1).getTailNumber());
        assertEquals(3, count("SELECT COUNT(*) FROM aircraft")); // N12345 and UNASSIGNED for dan, N777 for sam
    }

    @Test
    public void upgrade_convertsTextDatesSoTheySortCorrectly() {
        List<MaintenanceTask> tasks = helper.getAllTasks(1);
        // Version 2 put January 2027 before December 2026
        int december = -1;
        int january = -1;
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getTaskId() == 5) december = i;
            if (tasks.get(i).getTaskId() == 6) january = i;
        }
        assertTrue(december < january);
        assertEquals("12/15/2026", helper.getTask(5, 1).getDueDate());
        assertEquals("01:30 PM", helper.getTask(5, 1).getDueTime());
    }

    @Test
    public void upgrade_flagsUnassignedTasksForReview() {
        MaintenanceTask task = helper.getTask(7, 1);
        assertEquals(MaintenanceTask.UNASSIGNED, task.getTailNumber());
        assertTrue(task.isNeedsReview());
        assertFalse(helper.getTask(5, 1).isNeedsReview());
    }

    @Test
    public void upgrade_unreadableDate_keepsOriginalTextAndFlagsForReview() {
        MaintenanceTask task = helper.getTask(8, 1);
        assertTrue(task.isNeedsReview());
        assertTrue(task.getNotes().startsWith("Original note"));
        assertTrue(task.getNotes().contains("[Original due date could not be read: someday soon]"));
    }

    @Test
    public void upgrade_completedTaskStaysCompleteWithoutInventedSignOff() {
        MaintenanceTask task = helper.getTask(9, 2);
        assertTrue(task.isComplete());
        assertNull(task.getSignedOffAt());
        assertNull(task.getSignedOffBy());
    }

    @Test
    public void upgrade_logsEveryTaskInAValidAuditChain() {
        assertEquals(5, count("SELECT COUNT(*) FROM audit_log WHERE action = 'MIGRATED'"));
        assertTrue(AuditChain.verify(helper.getAllAuditEntries()).isValid());
    }

    @Test
    public void upgrade_removesOldTables() {
        assertEquals(0, count("SELECT COUNT(*) FROM sqlite_master WHERE name IN " +
                "('events', 'events_v2', 'users_v2')"));
    }

    @Test
    public void upgrade_newTasksGetIdsAfterTheOldOnes() {
        MaintenanceTask task = new MaintenanceTask(1, "New task", "12/20/2026", "09:00 AM", "N12345", "");
        long id = helper.addTask(task, System.currentTimeMillis());
        assertTrue(id > 9); // The highest migrated task ID was 9
        assertNotNull(helper.getTask((int) id, 1));
    }
}
