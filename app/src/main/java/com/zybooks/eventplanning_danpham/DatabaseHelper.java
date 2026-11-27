package com.zybooks.eventplanning_danpham;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * DatabaseHelper manages the SQLite database for the maintenance tracker.
 *
 * Version 3 (the Databases enhancement) redesigns the database:
 * - Users store a salted PBKDF2 password hash instead of the password.
 * - Aircraft have their own table, and each task belongs to one aircraft.
 * - Due dates are stored as timestamps, so they sort and compare correctly.
 * - Every change to a task is written to an append-only audit log, which
 *   triggers protect and a hash chain makes tamper-evident.
 * - Every task query checks that the task's aircraft belongs to the user.
 * - Changes that touch more than one table run inside a transaction,
 *   so they either fully happen or don't happen at all.
 *
 * Only the repositories use this class, so the screens didn't need to change
 * when the database did.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";

    // Database information
    static final String DATABASE_NAME = "EventTracker.db";
    // Version 2 added task_type and status. Version 3 is the full redesign.
    static final int DATABASE_VERSION = 3;

    // Table names
    static final String TABLE_USERS = "users";
    static final String TABLE_AIRCRAFT = "aircraft";
    static final String TABLE_TASKS = "tasks";
    static final String TABLE_AUDIT = "audit_log";

    // Version 2 table that version 3 replaces, used only when upgrading
    private static final String OLD_TABLE_EVENTS = "events";

    // ===== SCHEMA =====

    private static final String CREATE_USERS_TABLE =
            "CREATE TABLE users (" +
                    "user_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT NOT NULL UNIQUE, " +
                    "password_hash TEXT NOT NULL, " +
                    "salt TEXT NOT NULL)";

    // UNIQUE (owner_id, tail_number) stops duplicate aircraft for one user.
    // SQLite builds an index for it that starts with owner_id, so it also
    // serves as the index for finding a user's aircraft.
    private static final String CREATE_AIRCRAFT_TABLE =
            "CREATE TABLE aircraft (" +
                    "aircraft_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "owner_id INTEGER NOT NULL REFERENCES users(user_id), " +
                    "tail_number TEXT NOT NULL, " +
                    "model TEXT, " +
                    "UNIQUE (owner_id, tail_number))";

    // CHECK constraints keep bad values out even if app code has a bug.
    // An open task can never have sign-off information.
    private static final String CREATE_TASKS_TABLE =
            "CREATE TABLE tasks (" +
                    "task_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "aircraft_id INTEGER NOT NULL REFERENCES aircraft(aircraft_id), " +
                    "task_name TEXT NOT NULL, " +
                    "task_type TEXT NOT NULL, " +
                    "status TEXT NOT NULL DEFAULT 'Open' CHECK (status IN ('Open', 'Complete')), " +
                    "notes TEXT, " +
                    "needs_review INTEGER NOT NULL DEFAULT 0 CHECK (needs_review IN (0, 1)), " +
                    "due_at INTEGER NOT NULL, " +
                    "repeat_days INTEGER CHECK (repeat_days IS NULL OR repeat_days > 0), " +
                    "signed_off_by INTEGER REFERENCES users(user_id), " +
                    "signed_off_at INTEGER, " +
                    "CHECK (status = 'Complete' OR (signed_off_by IS NULL AND signed_off_at IS NULL)))";

    // Matches how tasks are searched: by aircraft, then status, then due time.
    // Used by the dashboard, the overdue query, and the reports.
    private static final String CREATE_TASKS_INDEX =
            "CREATE INDEX idx_tasks_aircraft_status_due ON tasks (aircraft_id, status, due_at)";

    // task_id has no foreign key on purpose, so a task's history stays
    // in the log after the task is deleted
    private static final String CREATE_AUDIT_TABLE =
            "CREATE TABLE audit_log (" +
                    "audit_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "task_id INTEGER NOT NULL, " +
                    "user_id INTEGER NOT NULL REFERENCES users(user_id), " +
                    "action TEXT NOT NULL, " +
                    "details TEXT NOT NULL, " +
                    "created_at INTEGER NOT NULL, " +
                    "prev_hash TEXT NOT NULL, " +
                    "entry_hash TEXT NOT NULL UNIQUE)";

    private static final String CREATE_AUDIT_INDEX =
            "CREATE INDEX idx_audit_task ON audit_log (task_id)";

    // The audit log is append-only. These triggers stop any UPDATE or DELETE,
    // so not even a bug in this app can change or erase its history.
    private static final String CREATE_AUDIT_NO_UPDATE_TRIGGER =
            "CREATE TRIGGER audit_log_no_update BEFORE UPDATE ON audit_log " +
                    "BEGIN SELECT RAISE(ABORT, 'The audit log is append-only'); END";

    private static final String CREATE_AUDIT_NO_DELETE_TRIGGER =
            "CREATE TRIGGER audit_log_no_delete BEFORE DELETE ON audit_log " +
                    "BEGIN SELECT RAISE(ABORT, 'The audit log is append-only'); END";

    // ===== QUERIES =====

    // Columns every task query returns. The joins add the tail number and model
    // from the aircraft table and the sign-off username from the users table.
    private static final String SELECT_TASKS =
            "SELECT t.task_id, a.owner_id, t.task_name, t.task_type, t.status, t.notes, " +
                    "t.needs_review, t.due_at, t.repeat_days, t.signed_off_at, " +
                    "a.tail_number, a.model, u.username AS signed_off_name " +
                    "FROM tasks t " +
                    "JOIN aircraft a ON a.aircraft_id = t.aircraft_id " +
                    "LEFT JOIN users u ON u.user_id = t.signed_off_by ";

    // Ownership check added to every update and delete
    private static final String OWNED_BY_USER =
            "aircraft_id IN (SELECT aircraft_id FROM aircraft WHERE owner_id = ?)";

    // Open tasks on the user's aircraft that are past due, soonest first.
    // Package-private so a test can confirm with EXPLAIN QUERY PLAN that it uses the indexes.
    static final String OVERDUE_QUERY = SELECT_TASKS +
            "WHERE a.owner_id = ? AND t.status = 'Open' AND t.due_at < ? " +
            "ORDER BY t.due_at ASC";

    // The one shared instance, so the whole app uses one open connection
    private static DatabaseHelper instance;

    // Time zone used to turn timestamps back into date and time text
    private final ZoneId zone;

    /**
     * Get the shared DatabaseHelper.
     * The connection stays open for the life of the app instead of being
     * opened and closed for every query, which was slow in version 2.
     */
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext(),
                    DATABASE_NAME, ZoneId.systemDefault());
        }
        return instance;
    }

    /**
     * Package-private so tests can make their own database.
     * A null name makes an in-memory database that disappears when closed.
     */
    DatabaseHelper(Context context, String name, ZoneId zone) {
        super(context, name, null, DATABASE_VERSION);
        this.zone = zone;
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // SQLite ignores foreign keys unless they are turned on for each connection
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createSchema(db);
    }

    private void createSchema(SQLiteDatabase db) {
        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_AIRCRAFT_TABLE);
        db.execSQL(CREATE_TASKS_TABLE);
        db.execSQL(CREATE_TASKS_INDEX);
        db.execSQL(CREATE_AUDIT_TABLE);
        db.execSQL(CREATE_AUDIT_INDEX);
        db.execSQL(CREATE_AUDIT_NO_UPDATE_TRIGGER);
        db.execSQL(CREATE_AUDIT_NO_DELETE_TRIGGER);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Upgrade one version at a time so existing data is kept.
        // Android runs onUpgrade inside a transaction, so if any step fails,
        // the whole upgrade is rolled back and no data is lost.
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + OLD_TABLE_EVENTS + " ADD COLUMN task_type " +
                    "TEXT NOT NULL DEFAULT '" + MaintenanceTask.TYPE_INSPECTION + "'");
            db.execSQL("ALTER TABLE " + OLD_TABLE_EVENTS + " ADD COLUMN status " +
                    "TEXT NOT NULL DEFAULT '" + MaintenanceTask.STATUS_OPEN + "'");

            // Old events with no location have no aircraft, so flag them for review
            db.execSQL("UPDATE " + OLD_TABLE_EVENTS + " SET location = '" +
                    MaintenanceTask.UNASSIGNED + "' WHERE location IS NULL OR TRIM(location) = ''");
        }
        if (oldVersion < 3) {
            upgradeToVersion3(db, System.currentTimeMillis());
        }
    }

    /**
     * Move version 2 data into the version 3 design without losing any of it.
     * Plain text passwords are hashed, each tail number becomes an aircraft,
     * text dates become timestamps, and every task gets a first audit entry.
     * User and task IDs are kept, so logged-in sessions and reminders still work.
     */
    private void upgradeToVersion3(SQLiteDatabase db, long now) {
        // Rename the old tables so the new ones can use their names
        db.execSQL("ALTER TABLE users RENAME TO users_v2");
        db.execSQL("ALTER TABLE " + OLD_TABLE_EVENTS + " RENAME TO events_v2");
        createSchema(db);

        // Users: replace each plain text password with a salted hash
        try (Cursor users = db.rawQuery("SELECT user_id, username, password FROM users_v2", null)) {
            while (users.moveToNext()) {
                String salt = PasswordHasher.newSalt();
                ContentValues values = new ContentValues();
                values.put("user_id", users.getInt(0));
                values.put("username", users.getString(1));
                values.put("password_hash", PasswordHasher.hash(users.getString(2), salt));
                values.put("salt", salt);
                db.insertOrThrow(TABLE_USERS, null, values);
            }
        }

        // Tasks: only events with a real owner can be moved. The old database never
        // turned on foreign keys, so an event could point to a user that doesn't exist.
        try (Cursor orphans = db.rawQuery("SELECT COUNT(*) FROM events_v2 " +
                "WHERE user_id NOT IN (SELECT user_id FROM users_v2)", null)) {
            if (orphans.moveToFirst() && orphans.getInt(0) > 0) {
                Log.w(TAG, "Skipped " + orphans.getInt(0) + " events with no matching user");
            }
        }

        try (Cursor events = db.rawQuery("SELECT e.event_id, e.user_id, e.event_name, " +
                "e.event_date, e.event_time, e.location, e.description, e.task_type, e.status " +
                "FROM events_v2 e JOIN users_v2 u ON u.user_id = e.user_id " +
                "ORDER BY e.event_id", null)) {
            while (events.moveToNext()) {
                migrateEvent(db, events, now);
            }
        }

        // Drop the child table before the parent so no foreign key is left pointing at nothing
        db.execSQL("DROP TABLE events_v2");
        db.execSQL("DROP TABLE users_v2");
    }

    /**
     * Copy one version 2 event into the tasks table
     */
    private void migrateEvent(SQLiteDatabase db, Cursor event, long now) {
        int taskId = event.getInt(0);
        int ownerId = event.getInt(1);
        String date = event.getString(3);
        String time = event.getString(4);
        String notes = event.getString(6);
        String status = MaintenanceTask.STATUS_COMPLETE.equals(event.getString(8))
                ? MaintenanceTask.STATUS_COMPLETE : MaintenanceTask.STATUS_OPEN;

        String tail = event.getString(5);
        tail = (tail == null || tail.trim().isEmpty())
                ? MaintenanceTask.UNASSIGNED : tail.trim().toUpperCase(Locale.US);
        boolean needsReview = MaintenanceTask.UNASSIGNED.equals(tail);

        // Convert the text date. If it can't be read, keep the original text in
        // the notes, use the upgrade time, and flag the task for review.
        long dueAt;
        try {
            dueAt = DueDateConverter.toMillis(date, time, zone);
        } catch (DateTimeParseException | NullPointerException e) {
            dueAt = now;
            needsReview = true;
            String original = "[Original due date could not be read: " + date + " " + time + "]";
            notes = (notes == null || notes.isEmpty()) ? original : notes + "\n" + original;
        }

        ContentValues values = new ContentValues();
        values.put("task_id", taskId);
        values.put("aircraft_id", findOrCreateAircraft(db, ownerId, tail, null));
        values.put("task_name", event.getString(2));
        values.put("task_type", event.getString(7));
        values.put("status", status);
        values.put("notes", notes);
        values.put("needs_review", needsReview ? 1 : 0);
        values.put("due_at", dueAt);
        db.insertOrThrow(TABLE_TASKS, null, values);

        String details = "Moved from the version 2 database";
        if (needsReview) {
            details += "; flagged for review";
        }
        addAuditEntry(db, taskId, ownerId, AuditEntry.ACTION_MIGRATED, details, now);
    }

    // ===== USER OPERATIONS =====

    /**
     * Check if a username is already taken
     */
    public boolean checkUsernameExists(String username) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT 1 FROM users WHERE username = ?", new String[]{username})) {
            return cursor.moveToFirst();
        }
    }

    /**
     * Create a new user. Only the salted hash of the password is saved.
     * @return the new user, or null if the username is taken
     */
    public User registerUser(String username, String password) {
        String salt = PasswordHasher.newSalt();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password_hash", PasswordHasher.hash(password, salt));
        values.put("salt", salt);

        // insert() returns -1 instead of throwing if the UNIQUE username rule fails
        long userId = getWritableDatabase().insert(TABLE_USERS, null, values);
        return (userId == AppConstants.NO_ID) ? null : new User((int) userId, username);
    }

    /**
     * Check a username and password
     * @return the user if the password is correct, null otherwise
     */
    public User validateUser(String username, String password) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT user_id, password_hash, salt FROM users WHERE username = ?",
                new String[]{username})) {
            if (!cursor.moveToFirst()) {
                return null;
            }
            if (!PasswordHasher.matches(password, cursor.getString(2), cursor.getString(1))) {
                return null;
            }
            return new User(cursor.getInt(0), username);
        }
    }

    /**
     * Change a user's password after checking their current one.
     * A new salt is made, so the new hash shares nothing with the old one.
     * @return true if changed, false if the current password was wrong
     */
    public boolean changePassword(int userId, String currentPassword, String newPassword) {
        SQLiteDatabase db = getWritableDatabase();
        try (Cursor cursor = db.rawQuery("SELECT password_hash, salt FROM users WHERE user_id = ?",
                new String[]{String.valueOf(userId)})) {
            if (!cursor.moveToFirst()
                    || !PasswordHasher.matches(currentPassword, cursor.getString(1), cursor.getString(0))) {
                return false;
            }
        }

        String salt = PasswordHasher.newSalt();
        ContentValues values = new ContentValues();
        values.put("password_hash", PasswordHasher.hash(newPassword, salt));
        values.put("salt", salt);
        return db.update(TABLE_USERS, values, "user_id = ?",
                new String[]{String.valueOf(userId)}) == 1;
    }

    // ===== AIRCRAFT =====

    /**
     * Find the user's aircraft with this tail number, or add it if it's new.
     * If a model is given, it's saved on the aircraft.
     * @return the aircraft ID
     */
    private long findOrCreateAircraft(SQLiteDatabase db, int ownerId, String tailNumber, String model) {
        boolean hasModel = model != null && !model.trim().isEmpty();

        try (Cursor cursor = db.rawQuery(
                "SELECT aircraft_id FROM aircraft WHERE owner_id = ? AND tail_number = ?",
                new String[]{String.valueOf(ownerId), tailNumber})) {
            if (cursor.moveToFirst()) {
                long aircraftId = cursor.getLong(0);
                if (hasModel) {
                    ContentValues values = new ContentValues();
                    values.put("model", model.trim());
                    db.update(TABLE_AIRCRAFT, values, "aircraft_id = ?",
                            new String[]{String.valueOf(aircraftId)});
                }
                return aircraftId;
            }
        }

        ContentValues values = new ContentValues();
        values.put("owner_id", ownerId);
        values.put("tail_number", tailNumber);
        values.put("model", hasModel ? model.trim() : null);
        return db.insertOrThrow(TABLE_AIRCRAFT, null, values);
    }

    // ===== TASK OPERATIONS =====

    /**
     * Build a MaintenanceTask from the cursor's current row (from SELECT_TASKS)
     */
    private MaintenanceTask cursorToTask(Cursor cursor) {
        long dueAt = cursor.getLong(cursor.getColumnIndexOrThrow("due_at"));
        MaintenanceTask task = new MaintenanceTask(
                cursor.getInt(cursor.getColumnIndexOrThrow("task_id")),
                cursor.getInt(cursor.getColumnIndexOrThrow("owner_id")),
                cursor.getString(cursor.getColumnIndexOrThrow("task_name")),
                DueDateConverter.formatDate(dueAt, zone),
                DueDateConverter.formatTime(dueAt, zone),
                cursor.getString(cursor.getColumnIndexOrThrow("tail_number")),
                cursor.getString(cursor.getColumnIndexOrThrow("notes")));
        task.setTaskType(cursor.getString(cursor.getColumnIndexOrThrow("task_type")));
        task.setStatus(cursor.getString(cursor.getColumnIndexOrThrow("status")));
        task.setAircraftModel(cursor.getString(cursor.getColumnIndexOrThrow("model")));
        task.setNeedsReview(cursor.getInt(cursor.getColumnIndexOrThrow("needs_review")) == 1);

        int repeatIndex = cursor.getColumnIndexOrThrow("repeat_days");
        task.setRepeatDays(cursor.isNull(repeatIndex) ? null : cursor.getInt(repeatIndex));

        int signedAtIndex = cursor.getColumnIndexOrThrow("signed_off_at");
        task.setSignedOffAt(cursor.isNull(signedAtIndex) ? null : cursor.getLong(signedAtIndex));
        task.setSignedOffBy(cursor.getString(cursor.getColumnIndexOrThrow("signed_off_name")));
        return task;
    }

    /**
     * Put a task's editable fields into ContentValues for an insert or update
     */
    private ContentValues taskToValues(MaintenanceTask task, long aircraftId) {
        ContentValues values = new ContentValues();
        values.put("aircraft_id", aircraftId);
        values.put("task_name", task.getTaskName());
        values.put("task_type", task.getTaskType());
        values.put("notes", task.getNotes());
        values.put("due_at", DueDateConverter.toMillis(task.getDueDate(), task.getDueTime(), zone));
        values.put("repeat_days", task.isRecurring() ? task.getRepeatDays() : null);
        // Saving a task is its review. It only stays flagged if it still has no aircraft.
        values.put("needs_review", MaintenanceTask.UNASSIGNED.equals(task.getTailNumber()) ? 1 : 0);
        return values;
    }

    /**
     * Run a task query and collect every row
     */
    private List<MaintenanceTask> queryTasks(SQLiteDatabase db, String sql, String[] args) {
        List<MaintenanceTask> tasks = new ArrayList<>();
        try (Cursor cursor = db.rawQuery(sql, args)) {
            while (cursor.moveToNext()) {
                tasks.add(cursorToTask(cursor));
            }
        }
        return tasks;
    }

    /**
     * Find one task, but only if its aircraft belongs to this user
     * @return the task, or null if it doesn't exist or isn't the user's
     */
    private MaintenanceTask findOwnedTask(SQLiteDatabase db, int taskId, int userId) {
        List<MaintenanceTask> found = queryTasks(db,
                SELECT_TASKS + "WHERE t.task_id = ? AND a.owner_id = ?",
                new String[]{String.valueOf(taskId), String.valueOf(userId)});
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * READ: Get all of a user's tasks, soonest due first.
     * Sorting by the due_at timestamp fixes the version 2 bug where text dates
     * put January 2027 before December 2026.
     */
    public List<MaintenanceTask> getAllTasks(int userId) {
        return queryTasks(getReadableDatabase(),
                SELECT_TASKS + "WHERE a.owner_id = ? ORDER BY t.due_at ASC",
                new String[]{String.valueOf(userId)});
    }

    /**
     * READ: Get one task, checking that it belongs to the user
     * @return the task, or null if not found
     */
    public MaintenanceTask getTask(int taskId, int userId) {
        return findOwnedTask(getReadableDatabase(), taskId, userId);
    }

    /**
     * READ: Get the user's overdue tasks using the indexed query
     */
    public List<MaintenanceTask> getOverdueTasks(int userId, long now) {
        return queryTasks(getReadableDatabase(), OVERDUE_QUERY,
                new String[]{String.valueOf(userId), String.valueOf(now)});
    }

    /**
     * CREATE: Add a task and its audit entry in one transaction
     * @return the new task's ID
     */
    public long addTask(MaintenanceTask task, long now) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            long aircraftId = findOrCreateAircraft(db, task.getUserId(),
                    task.getTailNumber(), task.getAircraftModel());
            long taskId = db.insertOrThrow(TABLE_TASKS, null, taskToValues(task, aircraftId));

            addAuditEntry(db, (int) taskId, task.getUserId(), AuditEntry.ACTION_CREATED,
                    AuditDetails.describe(task), now);
            db.setTransactionSuccessful();
            return taskId;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * UPDATE: Save changes to an open task the user owns, and log what changed.
     * Status isn't changed here. Tasks can only be completed by signing them off.
     * @return 1 if updated, 0 if the task wasn't found or isn't the user's
     * @throws IllegalStateException if the task is already signed off
     */
    public int updateTask(MaintenanceTask task, long now) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            MaintenanceTask before = findOwnedTask(db, task.getTaskId(), task.getUserId());
            if (before == null) {
                return 0;
            }
            if (before.isComplete()) {
                throw new IllegalStateException("Signed-off tasks can't be edited");
            }

            long aircraftId = findOrCreateAircraft(db, task.getUserId(),
                    task.getTailNumber(), task.getAircraftModel());
            int rows = db.update(TABLE_TASKS, taskToValues(task, aircraftId),
                    "task_id = ? AND status = 'Open' AND " + OWNED_BY_USER,
                    new String[]{String.valueOf(task.getTaskId()), String.valueOf(task.getUserId())});

            if (rows == 1) {
                addAuditEntry(db, task.getTaskId(), task.getUserId(), AuditEntry.ACTION_UPDATED,
                        AuditDetails.describeChanges(before, task), now);
                db.setTransactionSuccessful();
            }
            return rows;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * DELETE: Remove a task the user owns. The audit log keeps its history.
     * @return 1 if deleted, 0 if the task wasn't found or isn't the user's
     */
    public int deleteTask(int taskId, int userId, long now) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            MaintenanceTask before = findOwnedTask(db, taskId, userId);
            if (before == null) {
                return 0;
            }
            int rows = db.delete(TABLE_TASKS, "task_id = ? AND " + OWNED_BY_USER,
                    new String[]{String.valueOf(taskId), String.valueOf(userId)});

            if (rows == 1) {
                addAuditEntry(db, taskId, userId, AuditEntry.ACTION_DELETED,
                        AuditDetails.describe(before), now);
                db.setTransactionSuccessful();
            }
            return rows;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Sign off an open task: mark it complete, record who and when, log it,
     * and create the next task if it repeats. All of it happens in one
     * transaction, so a failure part way through leaves nothing half done.
     * @return the ID of the next recurring task, or NO_ID if it doesn't repeat
     * @throws IllegalStateException if the task isn't found, isn't the user's,
     *                               or is already signed off
     */
    public long signOffTask(int taskId, int userId, long now) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            MaintenanceTask task = findOwnedTask(db, taskId, userId);
            if (task == null || task.isComplete()) {
                throw new IllegalStateException("Task not found or already signed off");
            }

            ContentValues values = new ContentValues();
            values.put("status", MaintenanceTask.STATUS_COMPLETE);
            values.put("signed_off_by", userId);
            values.put("signed_off_at", now);
            int rows = db.update(TABLE_TASKS, values,
                    "task_id = ? AND status = 'Open' AND " + OWNED_BY_USER,
                    new String[]{String.valueOf(taskId), String.valueOf(userId)});
            if (rows != 1) {
                throw new IllegalStateException("Task not found or already signed off");
            }
            addAuditEntry(db, taskId, userId, AuditEntry.ACTION_SIGNED_OFF, "Signed off", now);

            long nextTaskId = AppConstants.NO_ID;
            if (task.isRecurring()) {
                // The next one is due the repeat interval after this sign-off
                long nextDue = DueDateConverter.nextDueMillis(now, task.getRepeatDays(), zone);
                task.setDueDate(DueDateConverter.formatDate(nextDue, zone));
                task.setDueTime(DueDateConverter.formatTime(nextDue, zone));
                task.setNotes(null);

                long aircraftId = findOrCreateAircraft(db, userId, task.getTailNumber(), null);
                nextTaskId = db.insertOrThrow(TABLE_TASKS, null, taskToValues(task, aircraftId));
                addAuditEntry(db, (int) nextTaskId, userId, AuditEntry.ACTION_CREATED,
                        AuditDetails.describe(task) + "; created by sign-off of task " + taskId, now);
            }

            db.setTransactionSuccessful();
            return nextTaskId;
        } finally {
            db.endTransaction();
        }
    }

    // ===== AUDIT LOG =====

    /**
     * Add one audit entry, linked to the entry before it by its hash.
     * Always called inside the same transaction as the change it records,
     * so a change is never saved without its entry.
     */
    private void addAuditEntry(SQLiteDatabase db, int taskId, int userId, String action,
                               String details, long now) {
        String prevHash = AuditChain.START;
        try (Cursor cursor = db.rawQuery(
                "SELECT entry_hash FROM audit_log ORDER BY audit_id DESC LIMIT 1", null)) {
            if (cursor.moveToFirst()) {
                prevHash = cursor.getString(0);
            }
        }

        ContentValues values = new ContentValues();
        values.put("task_id", taskId);
        values.put("user_id", userId);
        values.put("action", action);
        values.put("details", details);
        values.put("created_at", now);
        values.put("prev_hash", prevHash);
        values.put("entry_hash", AuditChain.computeHash(prevHash, taskId, userId, action, details, now));
        db.insertOrThrow(TABLE_AUDIT, null, values);
    }

    private List<AuditEntry> queryAudit(String sql, String[] args) {
        List<AuditEntry> entries = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, args)) {
            while (cursor.moveToNext()) {
                entries.add(new AuditEntry(
                        cursor.getLong(cursor.getColumnIndexOrThrow("audit_id")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("task_id")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("user_id")),
                        cursor.getString(cursor.getColumnIndexOrThrow("username")),
                        cursor.getString(cursor.getColumnIndexOrThrow("action")),
                        cursor.getString(cursor.getColumnIndexOrThrow("details")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("created_at")),
                        cursor.getString(cursor.getColumnIndexOrThrow("prev_hash")),
                        cursor.getString(cursor.getColumnIndexOrThrow("entry_hash"))));
            }
        }
        return entries;
    }

    /**
     * Get one task's history, oldest first, if the task belongs to the user
     */
    public List<AuditEntry> getTaskHistory(int taskId, int userId) {
        return queryAudit("SELECT l.*, u.username FROM audit_log l " +
                        "LEFT JOIN users u ON u.user_id = l.user_id " +
                        "WHERE l.task_id = ? AND EXISTS (SELECT 1 FROM tasks t " +
                        "JOIN aircraft a ON a.aircraft_id = t.aircraft_id " +
                        "WHERE t.task_id = l.task_id AND a.owner_id = ?) " +
                        "ORDER BY l.audit_id ASC",
                new String[]{String.valueOf(taskId), String.valueOf(userId)});
    }

    /**
     * Get every audit entry in order, for verifying the hash chain
     */
    public List<AuditEntry> getAllAuditEntries() {
        return queryAudit("SELECT l.*, u.username FROM audit_log l " +
                "LEFT JOIN users u ON u.user_id = l.user_id ORDER BY l.audit_id ASC", null);
    }

    // ===== REPORTS =====

    /**
     * Build the maintenance report for a user's aircraft: the indexed overdue
     * query plus three GROUP BY queries
     */
    public MaintenanceReport getReport(int userId, long now) {
        SQLiteDatabase db = getReadableDatabase();
        String owner = String.valueOf(userId);

        // Overdue open tasks per aircraft
        List<MaintenanceReport.CountRow> overdue = new ArrayList<>();
        try (Cursor cursor = db.rawQuery(
                "SELECT a.tail_number, COUNT(*) AS overdue_count FROM tasks t " +
                        "JOIN aircraft a ON a.aircraft_id = t.aircraft_id " +
                        "WHERE a.owner_id = ? AND t.status = 'Open' AND t.due_at < ? " +
                        "GROUP BY a.aircraft_id " +
                        "ORDER BY overdue_count DESC, a.tail_number ASC",
                new String[]{owner, String.valueOf(now)})) {
            while (cursor.moveToNext()) {
                overdue.add(new MaintenanceReport.CountRow(cursor.getString(0), cursor.getInt(1)));
            }
        }

        // Most frequent repairs, matching names regardless of capitalization
        List<MaintenanceReport.CountRow> repairs = new ArrayList<>();
        try (Cursor cursor = db.rawQuery(
                "SELECT MIN(t.task_name), COUNT(*) AS repair_count FROM tasks t " +
                        "JOIN aircraft a ON a.aircraft_id = t.aircraft_id " +
                        "WHERE a.owner_id = ? AND t.task_type = ? " +
                        "GROUP BY LOWER(t.task_name) " +
                        "ORDER BY repair_count DESC, MIN(t.task_name) ASC LIMIT 5",
                new String[]{owner, MaintenanceTask.TYPE_REPAIR})) {
            while (cursor.moveToNext()) {
                repairs.add(new MaintenanceReport.CountRow(cursor.getString(0), cursor.getInt(1)));
            }
        }

        // Open and completed tasks per aircraft. LEFT JOIN keeps aircraft with no tasks.
        List<MaintenanceReport.StatusRow> status = new ArrayList<>();
        try (Cursor cursor = db.rawQuery(
                "SELECT a.tail_number, " +
                        "SUM(CASE WHEN t.status = 'Open' THEN 1 ELSE 0 END), " +
                        "SUM(CASE WHEN t.status = 'Complete' THEN 1 ELSE 0 END) " +
                        "FROM aircraft a LEFT JOIN tasks t ON t.aircraft_id = a.aircraft_id " +
                        "WHERE a.owner_id = ? " +
                        "GROUP BY a.aircraft_id ORDER BY a.tail_number ASC",
                new String[]{owner})) {
            while (cursor.moveToNext()) {
                status.add(new MaintenanceReport.StatusRow(
                        cursor.getString(0), cursor.getInt(1), cursor.getInt(2)));
            }
        }

        return new MaintenanceReport(getOverdueTasks(userId, now), overdue, repairs, status);
    }
}
