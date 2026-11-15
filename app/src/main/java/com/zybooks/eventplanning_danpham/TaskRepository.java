package com.zybooks.eventplanning_danpham;

import java.util.List;

/**
 * TaskRepository defines every operation the app can perform on maintenance tasks.
 * ViewModels depend on this interface instead of a specific class, so tests can
 * pass in a fake version that doesn't need a real database.
 */
public interface TaskRepository {

    /**
     * Receives the result of a background operation.
     * Exactly one of these methods is called, on the main thread.
     */
    interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    void getTasks(int userId, Callback<List<MaintenanceTask>> callback);

    void getTask(int taskId, Callback<MaintenanceTask> callback);

    void addTask(MaintenanceTask task, Callback<Long> callback);

    void updateTask(MaintenanceTask task, Callback<Integer> callback);

    void deleteTask(int taskId, Callback<Integer> callback);
}