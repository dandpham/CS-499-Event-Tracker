package com.zybooks.eventplanning_danpham;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
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
 * Instrumented tests for DatabaseHelper. They run on a device or emulator
 * against a real SQLite database, so they test the actual SQL: the schema,
 * constraints, triggers, transactions, indexes, and reports.
 * Each test gets a fresh in-memory database.
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseHelperTest {

    private static final ZoneId ZONE = ZoneId.of("America/New_York");

    // A fixed "now" of 11/12/2026 09:00 AM, so overdue checks always give the same result
    private static final long NOW = DueDateConverter.toMillis("11/12/2026", "09:00 AM", ZONE);

    private DatabaseHelper helper;
    private SQLiteDatabase db;
    private int dan;
    private int otherUser;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        helper = new DatabaseHelper(context, null, ZONE); // null name = in-memory database
        db = helper.getWritableDatabase();
        dan = helper.registerUser("dan", "secret123").getUserId();
        otherUser = helper.registerUser("other", "password1").getUserId();
    }

    @After
    public void tearDown() {
        helper.close();
    }

    /** Add an open task and return its ID */
    private int addTask(int userId, String name, String tail, String type, String date,
                        String time, Integer repeatDays) {
        MaintenanceTask task = new MaintenanceTask(userId, name, date, time, tail, "");
        task.setTaskType(type);
        task.setRepeatDays(repeatDays);
        return (int) helper.addTask(task, NOW);
    }

    private int addTask(int userId, String name, String tail, String date, String time) {
        return addTask(userId, name, tail, MaintenanceTask.TYPE_INSPECTION, date, time, null);
    }

    private int count(String sql, String... args) {
        try (Cursor cursor = db.rawQuery(sql, args)) {
            cursor.moveToFirst();
            return cursor.getInt(0);
        }
    }

    // ===== USERS AND PASSWORDS =====

    @Test
    public void registerUser_storesSaltedHashNotPassword() {
        try (Cursor cursor = db.rawQuery("SELECT * FROM users WHERE username = 'dan'", null)) {
            assertTrue(cursor.moveToFirst());
            // The old password column is gone, and the hash isn't the password
            assertEquals(-1, cursor.getColumnIndex("password"));
            String hash = cursor.getString(cursor.getColumnIndexOrThrow("password_hash"));
            assertNotEquals("secret123", hash);
            assertNotNull(cursor.getString(cursor.getColumnIndexOrThrow("salt")));
        }
    }

    @Test
    public void validateUser_correctAndWrongPasswords() {
        assertEquals(dan, helper.validateUser("dan", "secret123").getUserId());
        assertNull(helper.validateUser("dan", "wrongpass"));
        assertNull(helper.validateUser("nobody", "secret123"));
    }

    @Test
    public void registerUser_takenUsername_returnsNull() {
        assertNull(helper.registerUser("dan", "another1"));
    }

    @Test
    public void changePassword_needsCorrectCurrentPassword() {
        assertFalse(helper.changePassword(dan, "wrongpass", "newpass1"));
        assertTrue(helper.changePassword(dan, "secret123", "newpass1"));

        assertNull(helper.validateUser("dan", "secret123"));
        assertNotNull(helper.validateUser("dan", "newpass1"));
    }

    // ===== AIRCRAFT AND TASKS =====

    @Test
    public void addTask_sameTailNumber_sharesOneAircraft() {
        addTask(dan, "Oil change", "N12345", "12/15/2026", "01:30 PM");
        addTask(dan, "Tire swap", "N12345", "12/16/2026", "01:30 PM");

        assertEquals(1, count("SELECT COUNT(*) FROM aircraft WHERE owner_id = ?", String.valueOf(dan)));
    }

    @Test
    public void addTask_sameTailForDifferentUsers_makesSeparateAircraft() {
        addTask(dan, "Oil change", "N12345", "12/15/2026", "01:30 PM");
        addTask(otherUser, "Oil change", "N12345", "12/15/2026", "01:30 PM");

        assertEquals(2, count("SELECT COUNT(*) FROM aircraft"));
    }

    @Test
    public void addTask_withModel_savesModelOnAircraft() {
        MaintenanceTask task = new MaintenanceTask(dan, "Oil change", "12/15/2026", "01:30 PM", "N12345", "");
        task.setAircraftModel("Cessna 172");
        int id = (int) helper.addTask(task, NOW);

        assertEquals("Cessna 172", helper.getTask(id, dan).getAircraftModel());
    }

    @Test
    public void getAllTasks_sortsByTimestampAcrossYearsAndHours() {
        // As text, both of these sorted wrong in version 2
        addTask(dan, "January", "N100", "01/05/2027", "08:00 AM");
        addTask(dan, "Afternoon", "N100", "12/15/2026", "01:00 PM");
        addTask(dan, "Morning", "N100", "12/15/2026", "11:00 AM");

        List<MaintenanceTask> tasks = helper.getAllTasks(dan);

        assertEquals("Morning", tasks.get(0).getTaskName());
        assertEquals("Afternoon", tasks.get(1).getTaskName());
        assertEquals("January", tasks.get(2).getTaskName());
    }

    @Test
    public void getTask_returnsDatesAsTheScreensExpect() {
        int id = addTask(dan, "Oil change", "N12345", "12/15/2026", "01:30 PM");

        MaintenanceTask task = helper.getTask(id, dan);

        assertEquals("12/15/2026", task.getDueDate());
        assertEquals("01:30 PM", task.getDueTime());
        assertEquals("N12345", task.getTailNumber());
        assertEquals(MaintenanceTask.STATUS_OPEN, task.getStatus());
    }

    // ===== OWNERSHIP CHECKS =====

    @Test
    public void otherUser_cantReadChangeOrDeleteTask() {
        int id = addTask(dan, "Oil change", "N12345", "12/15/2026", "01:30 PM");

        assertNull(helper.getTask(id, otherUser));
        assertTrue(helper.getAllTasks(otherUser).isEmpty());

        MaintenanceTask stolen = new MaintenanceTask(id, otherUser, "Hacked", "12/15/2026",
                "01:30 PM", "N12345", "");
        assertEquals(0, helper.updateTask(stolen, NOW));
        assertEquals(0, helper.deleteTask(id, otherUser, NOW));
        assertThrows(IllegalStateException.class, () -> helper.signOffTask(id, otherUser, NOW));

        // Dan's task is unchanged
        assertEquals("Oil change", helper.getTask(id, dan).getTaskName());
        assertTrue(helper.getTaskHistory(id, otherUser).isEmpty());
    }

    // ===== UPDATES AND SIGN-OFFS =====

    @Test
    public void updateTask_logsWhatChanged() {
        int id = addTask(dan, "Oil change", "N12345", "12/15/2026", "01:30 PM");
        MaintenanceTask edited = helper.getTask(id, dan);
        edited.setDueDate("12/20/2026");

        assertEquals(1, helper.updateTask(edited, NOW));

        List<AuditEntry> history = helper.getTaskHistory(id, dan);
        assertEquals(2, history.size());
        assertEquals(AuditEntry.ACTION_UPDATED, history.get(1).getAction());
        assertEquals("Due: 12/15/2026 01:30 PM to 12/20/2026 01:30 PM", history.get(1).getDetails());
    }

    @Test
    public void signOffTask_recordsWhoAndWhen() {
        int id = addTask(dan, "Oil change", "N12345", "12/15/2026", "01:30 PM");

        assertEquals(AppConstants.NO_ID, helper.signOffTask(id, dan, NOW));

        MaintenanceTask task = helper.getTask(id, dan);
        assertTrue(task.isComplete());
        assertEquals("dan", task.getSignedOffBy());
        assertEquals(Long.valueOf(NOW), task.getSignedOffAt());
        assertEquals(AuditEntry.ACTION_SIGNED_OFF, helper.getTaskHistory(id, dan).get(1).getAction());
    }

    @Test
    public void signOffTask_twice_throws() {
        int id = addTask(dan, "Oil change", "N12345", "12/15/2026", "01:30 PM");
        helper.signOffTask(id, dan, NOW);

        assertThrows(IllegalStateException.class, () -> helper.signOffTask(id, dan, NOW));
    }

    @Test
    public void updateTask_signedOffTask_throwsAndChangesNothing() {
        int id = addTask(dan, "Oil change", "N12345", "12/15/2026", "01:30 PM");
        helper.signOffTask(id, dan, NOW);
        MaintenanceTask edited = helper.getTask(id, dan);
        edited.setTaskName("Changed after sign-off");

        assertThrows(IllegalStateException.class, () -> helper.updateTask(edited, NOW));
        assertEquals("Oil change", helper.getTask(id, dan).getTaskName());
    }

    @Test
    public void signOffTask_recurring_createsNextTaskDueAfterInterval() {
        int id = addTask(dan, "Monthly inspection", "N12345", MaintenanceTask.TYPE_INSPECTION,
                "11/10/2026", "09:00 AM", 30);

        long nextId = helper.signOffTask(id, dan, NOW);

        MaintenanceTask next = helper.getTask((int) nextId, dan);
        assertEquals("Monthly inspection", next.getTaskName());
        assertEquals(MaintenanceTask.STATUS_OPEN, next.getStatus());
        assertEquals("12/12/2026", next.getDueDate()); // 30 days after the 11/12 sign-off
        assertEquals("09:00 AM", next.getDueTime());
        assertEquals(Integer.valueOf(30), next.getRepeatDays());
        assertTrue(helper.getTaskHistory((int) nextId, dan).get(0).getDetails()
                .endsWith("created by sign-off of task " + id));
    }

    @Test
    public void signOffTask_failurePartWay_rollsBackEverything() {
        int id = addTask(dan, "Force failure", "N12345", MaintenanceTask.TYPE_INSPECTION,
                "11/10/2026", "09:00 AM", 30);
        int entriesBefore = count("SELECT COUNT(*) FROM audit_log");

        // Make creating the next recurring task fail, after the sign-off update already ran
        db.execSQL("CREATE TRIGGER test_fail BEFORE INSERT ON tasks " +
                "WHEN NEW.task_name = 'Force failure' BEGIN SELECT RAISE(ABORT, 'forced'); END");

        assertThrows(SQLException.class, () -> helper.signOffTask(id, dan, NOW));

        // The transaction undid the sign-off and its audit entry too
        MaintenanceTask task = helper.getTask(id, dan);
        assertEquals(MaintenanceTask.STATUS_OPEN, task.getStatus());
        assertNull(task.getSignedOffAt());
        assertEquals(entriesBefore, count("SELECT COUNT(*) FROM audit_log"));
    }

    @Test
    public void deleteTask_keepsHistoryInAuditLog() {
        int id = addTask(dan, "Oil change", "N12345", "12/15/2026", "01:30 PM");

        assertEquals(1, helper.deleteTask(id, dan, NOW));

        assertNull(helper.getTask(id, dan));
        assertEquals(2, count("SELECT COUNT(*) FROM audit_log WHERE task_id = ?", String.valueOf(id)));
        assertEquals(1, count("SELECT COUNT(*) FROM audit_log WHERE task_id = ? AND action = ?",
                String.valueOf(id), AuditEntry.ACTION_DELETED));
    }

    // ===== AUDIT LOG PROTECTION =====

    @Test
    public void auditLog_triggersBlockUpdateAndDelete() {
        addTask(dan, "Oil change", "N12345", "12/15/2026", "01:30 PM");

        assertThrows(SQLException.class, () -> db.execSQL("UPDATE audit_log SET details = 'changed'"));
        assertThrows(SQLException.class, () -> db.execSQL("DELETE FROM audit_log"));
        assertEquals(1, count("SELECT COUNT(*) FROM audit_log"));
    }

    @Test
    public void auditLog_chainIsValidAfterChanges() {
        int id = addTask(dan, "Oil change", "N12345", "12/15/2026", "01:30 PM");
        MaintenanceTask edited = helper.getTask(id, dan);
        edited.setNotes("Parts arrived");
        helper.updateTask(edited, NOW);
        helper.signOffTask(id, dan, NOW);

        AuditChain.Result result = AuditChain.verify(helper.getAllAuditEntries());

        assertTrue(result.isValid());
        assertEquals("Audit log verified. All 3 entries are intact.", result.getMessage());
    }

    @Test
    public void auditLog_editOutsideTheApp_isDetected() {
        int id = addTask(dan, "Oil change", "N12345", "12/15/2026", "01:30 PM");
        helper.signOffTask(id, dan, NOW);

        // Someone with direct file access drops the trigger and rewrites history
        db.execSQL("DROP TRIGGER audit_log_no_update");
        db.execSQL("UPDATE audit_log SET user_id = ? WHERE action = 'SIGNED_OFF'",
                new Object[]{otherUser});

        AuditChain.Result result = AuditChain.verify(helper.getAllAuditEntries());
        assertFalse(result.isValid());
        assertEquals(2, result.getBrokenAtId());
    }

    // ===== CONSTRAINTS =====

    @Test
    public void foreignKeys_areEnforced() {
        // There's no aircraft 999, so the database must refuse this task
        assertThrows(SQLException.class, () -> db.execSQL(
                "INSERT INTO tasks (aircraft_id, task_name, task_type, due_at) VALUES (999, 'x', 'Repair', 0)"));
    }

    @Test
    public void checkConstraints_rejectBadValues() {
        addTask(dan, "Oil change", "N12345", "12/15/2026", "01:30 PM");

        assertThrows(SQLException.class, () -> db.execSQL("UPDATE tasks SET status = 'Done'"));
        assertThrows(SQLException.class, () -> db.execSQL("UPDATE tasks SET repeat_days = 0"));
        // An open task can't have a sign-off
        assertThrows(SQLException.class, () -> db.execSQL("UPDATE tasks SET signed_off_at = 1"));
    }

    // ===== OVERDUE QUERY AND REPORTS =====

    @Test
    public void getOverdueTasks_onlyOpenPastDueTasksForThisUser() {
        addTask(dan, "Late", "N100", "11/01/2026", "09:00 AM");
        int done = addTask(dan, "Late but done", "N100", "11/02/2026", "09:00 AM");
        helper.signOffTask(done, dan, NOW);
        addTask(dan, "Upcoming", "N100", "12/15/2026", "09:00 AM");
        addTask(otherUser, "Someone else's", "N200", "11/01/2026", "09:00 AM");

        List<MaintenanceTask> overdue = helper.getOverdueTasks(dan, NOW);

        assertEquals(1, overdue.size());
        assertEquals("Late", overdue.get(0).getTaskName());
    }

    @Test
    public void overdueQuery_usesIndexes() {
        StringBuilder plan = new StringBuilder();
        try (Cursor cursor = db.rawQuery("EXPLAIN QUERY PLAN " + DatabaseHelper.OVERDUE_QUERY,
                new String[]{String.valueOf(dan), String.valueOf(NOW)})) {
            while (cursor.moveToNext()) {
                plan.append(cursor.getString(cursor.getColumnIndexOrThrow("detail"))).append('\n');
            }
        }
        String text = plan.toString();

        // The tasks table is searched with the new index, and the aircraft table
        // with the index SQLite builds for UNIQUE (owner_id, tail_number)
        assertTrue(text, text.contains("idx_tasks_aircraft_status_due"));
        assertTrue(text, text.contains("sqlite_autoindex_aircraft_1"));
        assertFalse(text, text.contains("SCAN t"));
    }

    @Test
    public void getReport_groupsByAircraftAndRepair() {
        addTask(dan, "Late A", "N100", "11/01/2026", "09:00 AM");
        addTask(dan, "Late B", "N100", "11/02/2026", "09:00 AM");
        addTask(dan, "Late C", "N200", "11/03/2026", "09:00 AM");
        addTask(dan, "Replace tire", "N200", MaintenanceTask.TYPE_REPAIR, "12/01/2026", "09:00 AM", null);
        int repair = addTask(dan, "replace TIRE", "N100", MaintenanceTask.TYPE_REPAIR,
                "12/02/2026", "09:00 AM", null);
        addTask(dan, "Fix light", "N100", MaintenanceTask.TYPE_REPAIR, "12/03/2026", "09:00 AM", null);
        helper.signOffTask(repair, dan, NOW);
        addTask(otherUser, "Replace tire", "N900", MaintenanceTask.TYPE_REPAIR, "12/01/2026", "09:00 AM", null);

        MaintenanceReport report = helper.getReport(dan, NOW);

        assertEquals(3, report.getOverdueTasks().size());

        // N100 has 2 overdue, N200 has 1
        assertEquals("N100", report.getOverdueByAircraft().get(0).getLabel());
        assertEquals(2, report.getOverdueByAircraft().get(0).getCount());
        assertEquals(1, report.getOverdueByAircraft().get(1).getCount());

        // "Replace tire" and "replace TIRE" count as the same repair; the other user's doesn't count
        assertEquals(2, report.getFrequentRepairs().get(0).getCount());
        assertEquals("Fix light", report.getFrequentRepairs().get(1).getLabel());

        // N100: Late A, Late B, Fix light open; the tire repair complete
        assertEquals("N100", report.getStatusByAircraft().get(0).getTailNumber());
        assertEquals(3, report.getStatusByAircraft().get(0).getOpenCount());
        assertEquals(1, report.getStatusByAircraft().get(0).getCompleteCount());
    }
}
