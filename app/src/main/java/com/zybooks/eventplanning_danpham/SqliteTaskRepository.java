package com.zybooks.eventplanning_danpham;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SqliteTaskRepository is the only class that talks to DatabaseHelper.
 * It runs all database work on a background thread so the screen never freezes,
 * keeps task reminders in sync with the database, and reports each result
 * back on the main thread.
 */
public class SqliteTaskRepository implements TaskRepository {

    // One shared background thread, so database operations run one at a time, in order
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    // Used to send results back to the main (screen) thread
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final DatabaseHelper databaseHelper;
    private final ReminderScheduler reminderScheduler;

    public SqliteTaskRepository(Context context) {
        // Use the application context so this doesn't hold onto a screen
        Context appContext = context.getApplicationContext();
        databaseHelper = new DatabaseHelper(appContext);
        reminderScheduler = new ReminderScheduler(appContext);
    }

    /**
     * Run work on the background thread, then send the result or error
     * back to the main thread through the callback
     */
    private <T> void runInBackground(Callable<T> work, Callback<T> callback) {
        EXECUTOR.execute(() -> {
            try {
                T result = work.call();
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    @Override
    public void getTasks(int userId, Callback<List<MaintenanceTask>> callback) {
        runInBackground(() -> databaseHelper.getAllEvents(userId), callback);
    }

    @Override
    public void getTask(int taskId, Callback<MaintenanceTask> callback) {
        runInBackground(() -> {
            MaintenanceTask task = databaseHelper.getEvent(taskId);
            if (task == null) {
                throw new IllegalStateException("Task not found");
            }
            return task;
        }, callback);
    }

    @Override
    public void addTask(MaintenanceTask task, Callback<Long> callback) {
        runInBackground(() -> {
            long newId = databaseHelper.addEvent(task);
            if (newId == AppConstants.NO_ID) {
                throw new IllegalStateException("Could not save task");
            }
            task.setTaskId((int) newId);
            reminderScheduler.schedule(task);
            return newId;
        }, callback);
    }

    @Override
    public void updateTask(MaintenanceTask task, Callback<Integer> callback) {
        runInBackground(() -> {
            int rows = databaseHelper.updateEvent(task);
            if (rows == 0) {
                throw new IllegalStateException("Task not found");
            }
            // Replace the old reminder. schedule() skips completed tasks.
            reminderScheduler.cancel(task.getTaskId());
            reminderScheduler.schedule(task);
            return rows;
        }, callback);
    }

    @Override
    public void deleteTask(int taskId, Callback<Integer> callback) {
        runInBackground(() -> {
            // Always cancel the reminder, so a deleted task never sends one
            reminderScheduler.cancel(taskId);
            int rows = databaseHelper.deleteEvent(taskId);
            if (rows == 0) {
                throw new IllegalStateException("Task not found");
            }
            return rows;
        }, callback);
    }
}