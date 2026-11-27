package com.zybooks.eventplanning_danpham;

import java.util.ArrayList;
import java.util.List;

/**
 * FakeTaskRepository stores tasks in a plain list instead of a database,
 * and calls each callback right away instead of on a background thread.
 * This lets the ViewModel tests run quickly, with no Android device.
 * Like the real database, it only returns or changes a user's own tasks.
 */
public class FakeTaskRepository implements TaskRepository {

    // The fake "database"
    final List<MaintenanceTask> tasks = new ArrayList<>();
    final List<AuditEntry> history = new ArrayList<>();

    // Set to true to make every operation fail, for testing error handling
    boolean shouldFail = false;

    // Records what the ViewModel asked for, so tests can check it
    MaintenanceTask lastAdded;
    MaintenanceTask lastUpdated;

    // Results for the report and audit checks
    MaintenanceReport report;
    AuditChain.Result verifyResult;

    private int nextId = 100;

    private boolean failed(RepositoryCallback<?> callback) {
        if (shouldFail) {
            callback.onError(new Exception("Simulated failure"));
        }
        return shouldFail;
    }

    private MaintenanceTask find(int taskId, int userId) {
        for (MaintenanceTask task : tasks) {
            if (task.getTaskId() == taskId && task.getUserId() == userId) {
                return task;
            }
        }
        return null;
    }

    @Override
    public void getTasks(int userId, RepositoryCallback<List<MaintenanceTask>> callback) {
        if (failed(callback)) {
            return;
        }
        List<MaintenanceTask> result = new ArrayList<>();
        for (MaintenanceTask task : tasks) {
            if (task.getUserId() == userId) {
                result.add(task);
            }
        }
        callback.onSuccess(result);
    }

    @Override
    public void getTask(int taskId, int userId, RepositoryCallback<MaintenanceTask> callback) {
        if (failed(callback)) {
            return;
        }
        MaintenanceTask task = find(taskId, userId);
        if (task == null) {
            callback.onError(new IllegalStateException("Task not found"));
        } else {
            callback.onSuccess(task);
        }
    }

    @Override
    public void addTask(MaintenanceTask task, RepositoryCallback<Long> callback) {
        if (failed(callback)) {
            return;
        }
        task.setTaskId(nextId++);
        tasks.add(task);
        lastAdded = task;
        callback.onSuccess((long) task.getTaskId());
    }

    @Override
    public void updateTask(MaintenanceTask task, RepositoryCallback<Integer> callback) {
        if (failed(callback)) {
            return;
        }
        MaintenanceTask existing = find(task.getTaskId(), task.getUserId());
        if (existing == null) {
            callback.onError(new IllegalStateException("Task not found"));
            return;
        }
        tasks.set(tasks.indexOf(existing), task);
        lastUpdated = task;
        callback.onSuccess(1);
    }

    @Override
    public void deleteTask(int taskId, int userId, RepositoryCallback<Integer> callback) {
        if (failed(callback)) {
            return;
        }
        MaintenanceTask existing = find(taskId, userId);
        if (existing == null) {
            callback.onError(new IllegalStateException("Task not found"));
            return;
        }
        tasks.remove(existing);
        callback.onSuccess(1);
    }

    @Override
    public void signOffTask(int taskId, int userId, RepositoryCallback<Long> callback) {
        if (failed(callback)) {
            return;
        }
        MaintenanceTask task = find(taskId, userId);
        if (task == null || task.isComplete()) {
            callback.onError(new IllegalStateException("Task not found or already signed off"));
            return;
        }
        task.setStatus(MaintenanceTask.STATUS_COMPLETE);
        task.setSignedOffBy("tester");
        task.setSignedOffAt(1L);

        long nextId = AppConstants.NO_ID;
        if (task.isRecurring()) {
            MaintenanceTask next = new MaintenanceTask(userId, task.getTaskName(), task.getDueDate(),
                    task.getDueTime(), task.getTailNumber(), null);
            next.setRepeatDays(task.getRepeatDays());
            next.setTaskId(this.nextId++);
            tasks.add(next);
            nextId = next.getTaskId();
        }
        callback.onSuccess(nextId);
    }

    @Override
    public void getTaskHistory(int taskId, int userId, RepositoryCallback<List<AuditEntry>> callback) {
        if (failed(callback)) {
            return;
        }
        List<AuditEntry> result = new ArrayList<>();
        for (AuditEntry entry : history) {
            if (entry.getTaskId() == taskId) {
                result.add(entry);
            }
        }
        callback.onSuccess(result);
    }

    @Override
    public void getReport(int userId, RepositoryCallback<MaintenanceReport> callback) {
        if (failed(callback)) {
            return;
        }
        callback.onSuccess(report);
    }

    @Override
    public void verifyAuditLog(RepositoryCallback<AuditChain.Result> callback) {
        if (failed(callback)) {
            return;
        }
        callback.onSuccess(verifyResult);
    }

    /**
     * Helper to quickly add a task for a test
     */
    MaintenanceTask addSample(int taskId, int userId, String name, String tailNumber,
                              String date, String time, String status) {
        MaintenanceTask task = new MaintenanceTask(taskId, userId, name, date, time, tailNumber, "");
        task.setStatus(status);
        tasks.add(task);
        return task;
    }
}
