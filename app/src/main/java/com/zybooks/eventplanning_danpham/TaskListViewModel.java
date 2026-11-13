package com.zybooks.eventplanning_danpham;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/**
 * TaskListViewModel holds the dashboard's data and logic.
 * It loads the user's tasks, sorts them by due date, flags overdue tasks,
 * and handles deletes. The dashboard screen only displays what this provides.
 */
public class TaskListViewModel extends ViewModel {

    private final TaskRepository repository;
    private final Supplier<LocalDateTime> clock; // Gives the current time; tests can fix it

    // Data the screen observes
    private final MutableLiveData<List<TaskListItem>> tasks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private int userId = AppConstants.NO_ID;

    public TaskListViewModel(TaskRepository repository, Supplier<LocalDateTime> clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public LiveData<List<TaskListItem>> getTasks() {
        return tasks;
    }

    /** One-time messages, such as "Task deleted" */
    public LiveData<String> getMessage() {
        return message;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /** Call after showing a message, so it isn't shown again */
    public void clearMessages() {
        message.setValue(null);
        errorMessage.setValue(null);
    }

    /**
     * Load all tasks for a user
     */
    public void loadTasks(int userId) {
        this.userId = userId;
        repository.getTasks(userId, new TaskRepository.Callback<List<MaintenanceTask>>() {
            @Override
            public void onSuccess(List<MaintenanceTask> result) {
                tasks.setValue(buildListItems(result));
            }

            @Override
            public void onError(Exception e) {
                errorMessage.setValue("Couldn't load tasks");
            }
        });
    }

    /**
     * Delete a task, then reload the list
     */
    public void deleteTask(int taskId) {
        repository.deleteTask(taskId, new TaskRepository.Callback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                message.setValue("Task deleted");
                loadTasks(userId);
            }

            @Override
            public void onError(Exception e) {
                errorMessage.setValue("Couldn't delete task");
            }
        });
    }

    /**
     * Sort tasks by due date and time, and flag the overdue ones.
     * Sorting here uses real dates, so tasks appear in the right order even
     * though the database still stores dates as text.
     */
    List<TaskListItem> buildListItems(List<MaintenanceTask> taskList) {
        LocalDateTime now = clock.get();

        List<MaintenanceTask> sorted = new ArrayList<>(taskList);
        sorted.sort(Comparator.comparing(MaintenanceTask::getDueDateTime,
                Comparator.nullsLast(Comparator.naturalOrder())));

        List<TaskListItem> items = new ArrayList<>();
        for (MaintenanceTask task : sorted) {
            items.add(new TaskListItem(task, isOverdue(task, now)));
        }
        return items;
    }

    /**
     * A task is overdue if it's still open and its due time has passed
     */
    static boolean isOverdue(MaintenanceTask task, LocalDateTime now) {
        LocalDateTime due = task.getDueDateTime();
        return !task.isComplete() && due != null && due.isBefore(now);
    }
}