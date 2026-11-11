package com.zybooks.eventplanning_danpham;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;

/**
 * DatabaseHelper manages the SQLite database for the Event Tracking application.
 * Handles all CRUD operations for Users and Events tables.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    // Database information
    private static final String DATABASE_NAME = "EventTracker.db";
    private static final int DATABASE_VERSION = 1;

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String COLUMN_USER_ID = "user_id";
    private static final String COLUMN_USERNAME = "username";
    private static final String COLUMN_PASSWORD = "password";

    // Events table
    private static final String TABLE_EVENTS = "events";
    private static final String COLUMN_EVENT_ID = "event_id";
    private static final String COLUMN_EVENT_USER_ID = "user_id";
    private static final String COLUMN_EVENT_NAME = "event_name";
    private static final String COLUMN_EVENT_DATE = "event_date";
    private static final String COLUMN_EVENT_TIME = "event_time";
    private static final String COLUMN_LOCATION = "location";
    private static final String COLUMN_DESCRIPTION = "description";// Events table

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
                    "FOREIGN KEY(" + COLUMN_EVENT_USER_ID + ") REFERENCES " +
                    TABLE_USERS + "(" + COLUMN_USER_ID + "))";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create tables when database is first created
        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_EVENTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop existing tables and recreate on version upgrade
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        onCreate(db);
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
            // User found - create User object from cursor data
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

    // ===== EVENT OPERATIONS (CRUD) =====

    /**
     * CREATE: Add a new event to the database
     * @param event Event object containing event details
     * @return event ID of inserted event, or -1 if insertion failed
     */
    public long addEvent(Event event) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EVENT_USER_ID, event.getUserId());
        values.put(COLUMN_EVENT_NAME, event.getEventName());
        values.put(COLUMN_EVENT_DATE, event.getEventDate());
        values.put(COLUMN_EVENT_TIME, event.getEventTime());
        values.put(COLUMN_LOCATION, event.getLocation());
        values.put(COLUMN_DESCRIPTION, event.getDescription());

        long eventId = db.insert(TABLE_EVENTS, null, values);
        db.close();
        return eventId;
    }

    /**
     * READ: Get all events for a specific user
     * @param userId User ID to filter events
     * @return List of Event objects for the user
     */
    public List<Event> getAllEvents(int userId) {
        List<Event> eventList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // Query to get all events for user, ordered by date and time
        String query = "SELECT * FROM " + TABLE_EVENTS +
                " WHERE " + COLUMN_EVENT_USER_ID + " = ?" +
                " ORDER BY " + COLUMN_EVENT_DATE + " ASC, " +
                COLUMN_EVENT_TIME + " ASC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        // Iterate through cursor and create Event objects
        if (cursor.moveToFirst()) {
            do {
                int eventId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_EVENT_ID));
                String eventName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_NAME));
                String eventDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_DATE));
                String eventTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_TIME));
                String location = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION));
                String description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION));

                Event event = new Event(eventId, userId, eventName, eventDate,
                        eventTime, location, description);
                eventList.add(event);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return eventList;
    }

    /**
     * READ: Get a single event by ID
     * @param eventId Event ID to retrieve
     * @return Event object, or null if not found
     */
    public Event getEvent(int eventId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Event event = null;

        String query = "SELECT * FROM " + TABLE_EVENTS +
                " WHERE " + COLUMN_EVENT_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(eventId)});

        if (cursor.moveToFirst()) {
            int userId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_EVENT_USER_ID));
            String eventName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_NAME));
            String eventDate = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_DATE));
            String eventTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_EVENT_TIME));
            String location = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION));
            String description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION));

            event = new Event(eventId, userId, eventName, eventDate,
                    eventTime, location, description);
        }

        cursor.close();
        db.close();
        return event;
    }

    /**
     * UPDATE: Update an existing event in the database
     * @param event Event object with updated information
     * @return number of rows affected (1 if successful, 0 if failed)
     */
    public int updateEvent(Event event) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EVENT_NAME, event.getEventName());
        values.put(COLUMN_EVENT_DATE, event.getEventDate());
        values.put(COLUMN_EVENT_TIME, event.getEventTime());
        values.put(COLUMN_LOCATION, event.getLocation());
        values.put(COLUMN_DESCRIPTION, event.getDescription());

        int rowsAffected = db.update(TABLE_EVENTS, values,
                COLUMN_EVENT_ID + " = ?",
                new String[]{String.valueOf(event.getEventId())});
        db.close();
        return rowsAffected;
    }

    /**
     * DELETE: Remove an event from the database
     * @param eventId Event ID to delete
     * @return number of rows affected (1 if successful, 0 if failed)
     */
    public int deleteEvent(int eventId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rowsAffected = db.delete(TABLE_EVENTS,
                COLUMN_EVENT_ID + " = ?",
                new String[]{String.valueOf(eventId)});
        db.close();
        return rowsAffected;
    }
}
