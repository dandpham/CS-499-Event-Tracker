package com.zybooks.eventplanning_danpham;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/**
 * DatabaseHelper manages the SQLite database for the maintenance tracker.
 * Handles all CRUD operations for the users and tasks tables.
 *
 * Note: The tasks table and its columns keep their original "events" names
 * for now. The full database redesign is planned for the Databases enhancement.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // Database information
    private static final String DATABASE_NAME = "EventTracker.db";
    // Version 2 adds the task_type and status columns
    private static final int DATABASE_VERSION = 2;

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";

    // Tasks table (originally the events table)
    private static final String TABLE_EVENTS = "events";
    private static final String COLUMN_EVENT_ID = "event_id";
    private static final String COLUMN_EVENT_USER_ID = "user_id";
    private static final String COLUMN_EVENT_NAME = "event_name";
    private static final String COLUMN_EVENT_DATE = "event_date";
    private static final String COLUMN_EVENT_TIME = "event_time";
    private static final String COLUMN_LOCATION = "location";       // holds the tail number
    private static final String COLUMN_DESCRIPTION = "description"; // holds the notes
    private static final String COLUMN_TASK_TYPE = "task_type";     // added in version 2
    private static final String COLUMN_STATUS = "status";           // added in version 2

    // SQL statements to create tables
    private static final String CREATE_USERS_TABLE =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COLUMN_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_USERNAME + " TEXT UNIQUE NOT NULL, " +
                    COLUMN_PASSWORD + " TEXT NOT NULL)";

    private static final String CREATE_EVENTS_TABLE =
            "CREATE TABLE " + TABLE_EVENTS + " (" +
                    COLUMN_EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_EVENT_USER_ID + " INTEGER NOT NULL, " +
                    COLUMN_EVENT_NAME + " TEXT NOT NULL, " +
                    COLUMN_EVENT_DATE + " TEXT NOT NULL, " +
                    COLUMN_EVENT_TIME + " TEXT NOT NULL, " +
                    COLUMN_LOCATION + " TEXT, " +
                    COLUMN_DESCRIPTION + " TEXT, " +
                    COLUMN_TASK_TYPE + " TEXT NOT NULL DEFAULT '" + MaintenanceTask.TYPE_INSPECTION + "', " +
                    COLUMN_STATUS + " TEXT NOT NULL DEFAULT '" + MaintenanceTask.STATUS_OPEN + "', " +
                    "FOREIGN KEY(" + COLUMN_EVENT_USER_ID + ") REFERENCES " +
                    TABLE_USERS + "(" + COLUMN_USER_ID + "))";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create tables when the database is first created
        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_EVENTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Upgrade one version at a time so existing data is kept.
        // Android runs onUpgrade inside a transaction, so if any step fails,
        // the whole upgrade is rolled back and no data is lost.
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_EVENTS + " ADD COLUMN " + COLUMN_TASK_TYPE +
                    " TEXT NOT NULL DEFAULT '" + MaintenanceTask.TYPE_INSPECTION + "'");
            db.execSQL("ALTER TABLE " + TABLE_EVENTS + " ADD COLUMN " + COLUMN_STATUS +
                    " TEXT NOT NULL DEFAULT '" + MaintenanceTask.STATUS_OPEN + "'");

            // Old events with no location have no aircraft, so flag them for review
            db.execSQL("UPDATE " + TABLE_EVENTS + " SET " + COLUMN_LOCATION + " = '" +
                    MaintenanceTask.UNASSIGNED + "' WHERE " + COLUMN_LOCATION +
                    " IS NULL OR TRIM(" + COLUMN_LOCATION + ") = ''");
        }
    }

    // ===== USER OPERATIONS =====

    /**
     * Add a new user to the database
     * @param user User object containing username and password
     * @return user ID of inserted user, or -1 if insertion failed
     */
    public long addUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, user.getUsername());
        values.put(COLUMN_PASSWORD, user.getPassword());

        long userId = db.insert(TABLE_USERS, null, values);
        db.close();
        return userId;
    }

    /**
     * Check if a user exists with given username and password
     * @param username Username to check
     * @param password Password to verify
     * @return User object if credentials are valid, null otherwise
     */
    public User validateUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        User user = null;

        // Query to find user with matching credentials
        String query = "SELECT * FROM " + TABLE_USERS +
                " WHERE " + COLUMN_USERNAME + " = ? AND " +
                COLUMN_PASSWORD + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{username, password});

        if (cursor.moveToFirst()) {
            int userId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID));
            user = new User(userId, username, password);
        }

        cursor.close();
        db.close();
        return user;
    }

    /**
     * Check if a username already exists in the database
     * @param username Username to check
     * @return true if username exists, false otherwise
     */
    public boolean checkUsernameExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_USERS +
                " WHERE " + COLUMN_USERNAME + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{username});

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    // ===== TASK OPERATIONS (CRUD) =====

    /**
     * Put a task's fields into ContentValues for an insert or update
     */
    private ContentValues taskToValues(MaintenanceTask task) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_EVENT_NAME, task.getTaskName());
        values.put(COLUMN_EVENT_DATE, task.getDueDate());
        values.put(COLUMN_EVENT_TIME, task.getDueTime());
        values.put(COLUMN_LOCATION, task.getTailNumber());
        values.put(COLUMN_DESCRIPTION, task.getNotes());
        values.put(COLUMN_TASK_TYPE, task.getTaskType());
        values.put(COLUMN_STATUS, task.getStatus());
        return values;
    }

    /**
     * Build a MaintenanceTask from the cursor's current row.
     * Replaces the row-reading code that was repeated in two methods.
     */
    private MaintenanceTask cursorToTask(Cursor cursor) {
        MaintenanceTask task = new MaintenanceTask(
                cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_EVENT_ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_EVENT_USER_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_DATE)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_TIME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION)),
                cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
        task.setTaskType(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TASK_TYPE)));
        task.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS)));
        return task;
    }

    /**
     * CREATE: Add a new task to the database
     * @param task Task to add
     * @return ID of the inserted task, or -1 if insertion failed
     */
    public long addEvent(MaintenanceTask task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = taskToValues(task);
        values.put(COLUMN_EVENT_USER_ID, task.getUserId());

        long taskId = db.insert(TABLE_EVENTS, null, values);
        db.close();
        return taskId;
    }

    /**
     * READ: Get all tasks for a specific user
     * @param userId User ID to filter tasks
     * @return List of the user's tasks
     */
    public List<MaintenanceTask> getAllEvents(int userId) {
        List<MaintenanceTask> taskList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_EVENTS +
                " WHERE " + COLUMN_EVENT_USER_ID + " = ?" +
                " ORDER BY " + COLUMN_EVENT_DATE + " ASC, " +
                COLUMN_EVENT_TIME + " ASC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                taskList.add(cursorToTask(cursor));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return taskList;
    }

    /**
     * READ: Get a single task by ID
     * @param taskId Task ID to retrieve
     * @return The task, or null if not found
     */
    public MaintenanceTask getEvent(int taskId) {
        SQLiteDatabase db = this.getReadableDatabase();
        MaintenanceTask task = null;

        String query = "SELECT * FROM " + TABLE_EVENTS +
                " WHERE " + COLUMN_EVENT_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(taskId)});

        if (cursor.moveToFirst()) {
            task = cursorToTask(cursor);
        }

        cursor.close();
        db.close();
        return task;
    }

    /**
     * UPDATE: Update an existing task in the database
     * @param task Task with updated information
     * @return number of rows affected (1 if successful, 0 if failed)
     */
    public int updateEvent(MaintenanceTask task) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = taskToValues(task);

        int rowsAffected = db.update(TABLE_EVENTS, values,
                COLUMN_EVENT_ID + " = ?",
                new String[]{String.valueOf(task.getTaskId())});
        db.close();
        return rowsAffected;
    }

    /**
     * DELETE: Remove a task from the database
     * @param taskId Task ID to delete
     * @return number of rows affected (1 if successful, 0 if failed)
     */
    public int deleteEvent(int taskId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_EVENTS,
                COLUMN_EVENT_ID + " = ?",
                new String[]{String.valueOf(taskId)});
        db.close();
        return rowsAffected;
    }
}