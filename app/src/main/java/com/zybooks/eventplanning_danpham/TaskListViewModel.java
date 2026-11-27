package com.zybooks.eventplanning_danpham;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * TaskListViewModel holds the dashboard's data and logic.
 * It loads the user's tasks, filters them by the search text, sorts them
 * by due date, flags overdue tasks, and handles deletes.
 * The dashboard screen only displays what this provides.
 */
public class TaskListViewModel extends ViewModel {

    private final TaskRepository repository;
    private final Supplier<LocalDateTime> clock; // Gives the current time; tests can fix it

    // Data the screen observes
    private final MutableLiveData<List<TaskListItem>> tasks = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Every task loaded for the user, before filtering
    private List<MaintenanceTask> allTasks = new ArrayList<>();
    private String searchQuery = "";
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

    public String getSearchQuery() {
        return searchQuery;
    }

    /** @return true if a search is filtering the list */
    public boolean isSearching() {
        return !searchQuery.isEmpty();
    }

    /**
     * Filter the list to tasks matching the search text.
     * Searching uses the tasks already loaded, so it doesn't query the database.
     */
    public void setSearchQuery(String query) {
        searchQuery = (query == null) ? "" : query.trim();
        publishTasks();
    }

    /**
     * Load all tasks for a user
     */
    public void loadTasks(int userId) {
        this.userId = userId;
        repository.getTasks(userId, new RepositoryCallback<List<MaintenanceTask>>() {
            @Override
            public void onSuccess(List<MaintenanceTask> result) {
                allTasks = result;
                publishTasks();
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
        repository.deleteTask(taskId, userId, new RepositoryCallback<Integer>() {
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
     * Apply the search filter and send the result to the screen
     */
    private void publishTasks() {
        List<MaintenanceTask> matching = new ArrayList<>();
        for (MaintenanceTask task : allTasks) {
            if (matchesSearch(task, searchQuery)) {
                matching.add(task);
            }
        }
        tasks.setValue(buildListItems(matching));
    }

    /**
     * Sort tasks by due date and time, and flag the overdue ones.
     * The database already returns tasks sorted by their due_at timestamp.
     * Sorting here too keeps the order right for any repository, including the test fake.
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

    /**
     * A task matches if the search text appears in its name, tail number,
     * notes, type, or status, ignoring capitalization. An empty search matches all.
     */
    static boolean matchesSearch(MaintenanceTask task, String query) {
        if (query == null || query.isEmpty()) {
            return true;
        }
        String q = query.toLowerCase(Locale.US);
        return contains(task.getTaskName(), q)
                || contains(task.getTailNumber(), q)
                || contains(task.getNotes(), q)
                || contains(task.getTaskType(), q)
                || contains(task.getStatus(), q);
    }

    private static boolean contains(String field, String lowerCaseQuery) {
        return field != null && field.toLowerCase(Locale.US).contains(lowerCaseQuery);
    }
}