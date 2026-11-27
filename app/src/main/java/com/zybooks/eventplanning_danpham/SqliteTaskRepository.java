package com.zybooks.eventplanning_danpham;

import android.content.Context;

import java.util.List;

/**
 * SqliteTaskRepository is the only class that talks to DatabaseHelper about tasks.
 * It runs all database work on a background thread so the screen never freezes,
 * keeps task reminders in sync with the database, and reports each result
 * back on the main thread.
 */
public class SqliteTaskRepository implements TaskRepository {

    private final DatabaseHelper databaseHelper;
    private final ReminderScheduler reminderScheduler;

    public SqliteTaskRepository(Context context) {
        // Use the application context so this doesn't hold onto a screen
        Context appContext = context.getApplicationContext();
        databaseHelper = DatabaseHelper.getInstance(appContext);
        reminderScheduler = new ReminderScheduler(appContext);
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    @Override
    public void getTasks(int userId, RepositoryCallback<List<MaintenanceTask>> callback) {
        BackgroundRunner.run(() -> databaseHelper.getAllTasks(userId), callback);
    }

    @Override
    public void getTask(int taskId, int userId, RepositoryCallback<MaintenanceTask> callback) {
        BackgroundRunner.run(() -> {
            MaintenanceTask task = databaseHelper.getTask(taskId, userId);
            if (task == null) {
                throw new IllegalStateException("Task not found");
            }
            return task;
        }, callback);
    }

    @Override
    public void addTask(MaintenanceTask task, RepositoryCallback<Long> callback) {
        BackgroundRunner.run(() -> {
            long newId = databaseHelper.addTask(task, now());
            task.setTaskId((int) newId);
            reminderScheduler.schedule(task);
            return newId;
        }, callback);
    }

    @Override
    public void updateTask(MaintenanceTask task, RepositoryCallback<Integer> callback) {
        BackgroundRunner.run(() -> {
            int rows = databaseHelper.updateTask(task, now());
            if (rows == 0) {
                throw new IllegalStateException("Task not found");
            }
            // Replace the old reminder
            reminderScheduler.cancel(task.getTaskId());
            reminderScheduler.schedule(task);
            return rows;
        }, callback);
    }

    @Override
    public void deleteTask(int taskId, int userId, RepositoryCallback<Integer> callback) {
        BackgroundRunner.run(() -> {
            int rows = databaseHelper.deleteTask(taskId, userId, now());
            if (rows == 0) {
                throw new IllegalStateException("Task not found");
            }
            // Only cancel after the delete succeeds, so a blocked delete keeps its reminder
            reminderScheduler.cancel(taskId);
            return rows;
        }, callback);
    }

    @Override
    public void signOffTask(int taskId, int userId, RepositoryCallback<Long> callback) {
        BackgroundRunner.run(() -> {
            long nextTaskId = databaseHelper.signOffTask(taskId, userId, now());
            // A signed-off task no longer needs a reminder. The next one does.
            reminderScheduler.cancel(taskId);
            if (nextTaskId != AppConstants.NO_ID) {
                MaintenanceTask next = databaseHelper.getTask((int) nextTaskId, userId);
                if (next != null) {
                    reminderScheduler.schedule(next);
                }
            }
            return nextTaskId;
        }, callback);
    }

    @Override
    public void getTaskHistory(int taskId, int userId, RepositoryCallback<List<AuditEntry>> callback) {
        BackgroundRunner.run(() -> databaseHelper.getTaskHistory(taskId, userId), callback);
    }

    @Override
    public void getReport(int userId, RepositoryCallback<MaintenanceReport> callback) {
        BackgroundRunner.run(() -> databaseHelper.getReport(userId, now()), callback);
    }

    @Override
    public void verifyAuditLog(RepositoryCallback<AuditChain.Result> callback) {
        BackgroundRunner.run(() -> AuditChain.verify(databaseHelper.getAllAuditEntries()), callback);
    }
}
