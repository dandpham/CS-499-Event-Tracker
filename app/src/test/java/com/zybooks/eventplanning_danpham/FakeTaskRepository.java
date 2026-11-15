package com.zybooks.eventplanning_danpham;

import java.util.ArrayList;
import java.util.List;

/**
 * FakeTaskRepository stores tasks in a plain list instead of a database,
 * and calls each callback right away instead of on a background thread.
 * This lets the ViewModel tests run quickly, with no Android device.
 */
public class FakeTaskRepository implements TaskRepository {

    // The fake "database"
    final List<MaintenanceTask> tasks = new ArrayList<>();

    // Set to true to make every operation fail, for testing error handling
    boolean shouldFail = false;

    // Records what the ViewModel asked for, so tests can check it
    MaintenanceTask lastAdded;
    MaintenanceTask lastUpdated;

    private int nextId = 100;

    @Override
    public void getTasks(int userId, Callback<List<MaintenanceTask>> callback) {
        if (shouldFail) {
            callback.onError(new Exception("Simulated failure"));
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
    public void getTask(int taskId, Callback<MaintenanceTask> callback) {
        if (shouldFail) {
            callback.onError(new Exception("Simulated failure"));
            return;
        }
        for (MaintenanceTask task : tasks) {
            if (task.getTaskId() == taskId) {
                callback.onSuccess(task);
                return;
            }
        }
        callback.onError(new IllegalStateException("Task not found"));
    }

    @Override
    public void addTask(MaintenanceTask task, Callback<Long> callback) {
        if (shouldFail) {
            callback.onError(new Exception("Simulated failure"));
            return;
        }
        task.setTaskId(nextId++);
        tasks.add(task);
        lastAdded = task;
        callback.onSuccess((long) task.getTaskId());
    }

    @Override
    public void updateTask(MaintenanceTask task, Callback<Integer> callback) {
        if (shouldFail) {
            callback.onError(new Exception("Simulated failure"));
            return;
        }
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getTaskId() == task.getTaskId()) {
                tasks.set(i, task);
                lastUpdated = task;
                callback.onSuccess(1);
                return;
            }
        }
        callback.onError(new IllegalStateException("Task not found"));
    }

    @Override
    public void deleteTask(int taskId, Callback<Integer> callback) {
        if (shouldFail) {
            callback.onError(new Exception("Simulated failure"));
            return;
        }
        boolean removed = tasks.removeIf(task -> task.getTaskId() == taskId);
        if (removed) {
            callback.onSuccess(1);
        } else {
            callback.onError(new IllegalStateException("Task not found"));
        }
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
