package com.zybooks.eventplanning_danpham;

import java.util.List;

/**
 * TaskRepository defines every operation the app can perform on maintenance tasks.
 * ViewModels depend on this interface instead of a specific class, so tests can
 * pass in a fake version that doesn't need a real database.
 *
 * Every operation takes the user's ID, so the database can check that the
 * task belongs to that user before reading or changing it.
 */
public interface TaskRepository {

    void getTasks(int userId, RepositoryCallback<List<MaintenanceTask>> callback);

    void getTask(int taskId, int userId, RepositoryCallback<MaintenanceTask> callback);

    /** The task's user ID is the owner */
    void addTask(MaintenanceTask task, RepositoryCallback<Long> callback);

    /** The task's user ID is the owner */
    void updateTask(MaintenanceTask task, RepositoryCallback<Integer> callback);

    void deleteTask(int taskId, int userId, RepositoryCallback<Integer> callback);

    /**
     * Sign off a task. The result is the ID of the next recurring task,
     * or NO_ID if the task doesn't repeat.
     */
    void signOffTask(int taskId, int userId, RepositoryCallback<Long> callback);

    void getTaskHistory(int taskId, int userId, RepositoryCallback<List<AuditEntry>> callback);

    void getReport(int userId, RepositoryCallback<MaintenanceReport> callback);

    void verifyAuditLog(RepositoryCallback<AuditChain.Result> callback);
}
