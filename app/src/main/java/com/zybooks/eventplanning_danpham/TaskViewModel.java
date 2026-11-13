package com.zybooks.eventplanning_danpham;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.LocalDateTime;
import java.util.function.Supplier;

/**
 * TaskViewModel holds the data and logic for a single task.
 * Used by the add/edit screen and the detail screen. It loads, validates,
 * saves, and deletes a task. The screens only display what this provides.
 */
public class TaskViewModel extends ViewModel {

    private final TaskRepository repository;
    private final Supplier<LocalDateTime> clock; // Gives the current time; tests can fix it

    // Data the screens observe
    private final MutableLiveData<MaintenanceTask> task = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> doneMessage = new MutableLiveData<>();

    public TaskViewModel(TaskRepository repository, Supplier<LocalDateTime> clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** The loaded task */
    public LiveData<MaintenanceTask> getTask() {
        return task;
    }

    /** A problem to show the user, such as a validation error */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /** Set after a save or delete succeeds; the screen shows it and closes */
    public LiveData<String> getDoneMessage() {
        return doneMessage;
    }

    /** Call after showing an error, so it isn't shown again */
    public void clearError() {
        errorMessage.setValue(null);
    }

    /**
     * Load an existing task
     */
    public void loadTask(int taskId) {
        repository.getTask(taskId, new TaskRepository.Callback<MaintenanceTask>() {
            @Override
            public void onSuccess(MaintenanceTask result) {
                task.setValue(result);
            }

            @Override
            public void onError(Exception e) {
                errorMessage.setValue("Couldn't load task");
            }
        });
    }

    /**
     * Validate the input, then add a new task or update an existing one.
     * @param taskId NO_ID for a new task, otherwise the task being edited
     */
    public void saveTask(int taskId, int userId, String taskName, String tailNumber,
                         String taskType, String dueDate, String dueTime,
                         String notes, boolean complete) {
        boolean isNewTask = (taskId == AppConstants.NO_ID);

        // New tasks can't be due in the past. Existing tasks can be,
        // since an overdue task still needs to be edited.
        LocalDateTime earliestAllowed = isNewTask ? clock.get() : LocalDateTime.MIN;

        String error = Validator.checkTask(taskName, tailNumber, dueDate, dueTime, earliestAllowed);
        if (error != null) {
            errorMessage.setValue(error);
            return;
        }

        MaintenanceTask newTask = new MaintenanceTask(taskId, userId, taskName.trim(),
                dueDate, dueTime, tailNumber.trim(), notes);
        newTask.setTaskType(taskType);
        newTask.setStatus(complete ? MaintenanceTask.STATUS_COMPLETE : MaintenanceTask.STATUS_OPEN);

        if (isNewTask) {
            repository.addTask(newTask, new TaskRepository.Callback<Long>() {
                @Override
                public void onSuccess(Long result) {
                    doneMessage.setValue("Task added successfully");
                }

                @Override
                public void onError(Exception e) {
                    errorMessage.setValue("Couldn't save task");
                }
            });
        } else {
            repository.updateTask(newTask, new TaskRepository.Callback<Integer>() {
                @Override
                public void onSuccess(Integer result) {
                    doneMessage.setValue("Task updated successfully");
                }

                @Override
                public void onError(Exception e) {
                    errorMessage.setValue("Couldn't save task");
                }
            });
        }
    }

    /**
     * Delete a task
     */
    public void deleteTask(int taskId) {
        repository.deleteTask(taskId, new TaskRepository.Callback<Integer>() {
            @Override
            public void onSuccess(Integer result) {
                doneMessage.setValue("Task deleted successfully");
            }

            @Override
            public void onError(Exception e) {
                errorMessage.setValue("Couldn't delete task");
            }
        });
    }
}
