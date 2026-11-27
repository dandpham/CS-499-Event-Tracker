package com.zybooks.eventplanning_danpham;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * TaskViewModel holds the data and logic for a single task.
 * Used by the add/edit screen and the detail screen. It loads, validates,
 * saves, signs off, and deletes a task, and loads its audit history.
 * The screens only display what this provides.
 */
public class TaskViewModel extends ViewModel {

    private final TaskRepository repository;
    private final Supplier<LocalDateTime> clock; // Gives the current time; tests can fix it

    // Data the screens observe
    private final MutableLiveData<MaintenanceTask> task = new MutableLiveData<>();
    private final MutableLiveData<List<AuditEntry>> history = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> doneMessage = new MutableLiveData<>();
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public TaskViewModel(TaskRepository repository, Supplier<LocalDateTime> clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** The loaded task */
    public LiveData<MaintenanceTask> getTask() {
        return task;
    }

    /** The loaded task's audit history, oldest first */
    public LiveData<List<AuditEntry>> getHistory() {
        return history;
    }

    /** A problem to show the user, such as a validation error */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /** Set after a save or delete succeeds; the screen shows it and closes */
    public LiveData<String> getDoneMessage() {
        return doneMessage;
    }

    /** A message to show while the screen stays open, such as after a sign-off */
    public LiveData<String> getMessage() {
        return message;
    }

    /** Call after showing an error, so it isn't shown again */
    public void clearError() {
        errorMessage.setValue(null);
    }

    /** Call after showing a message, so it isn't shown again */
    public void clearMessage() {
        message.setValue(null);
    }

    /**
     * Load an existing task and its history
     */
    public void loadTask(int taskId, int userId) {
        repository.getTask(taskId, userId, new RepositoryCallback<MaintenanceTask>() {
            @Override
            public void onSuccess(MaintenanceTask result) {
                task.setValue(result);
                loadHistory(taskId, userId);
            }

            @Override
            public void onError(Exception e) {
                errorMessage.setValue("Couldn't load task");
            }
        });
    }

    private void loadHistory(int taskId, int userId) {
        repository.getTaskHistory(taskId, userId, new RepositoryCallback<List<AuditEntry>>() {
            @Override
            public void onSuccess(List<AuditEntry> result) {
                history.setValue(result);
            }

            @Override
            public void onError(Exception e) {
                errorMessage.setValue("Couldn't load task history");
            }
        });
    }

    /**
     * Validate the input, then add a new task or update an existing one.
     * Saving never changes a task's status. Tasks are completed by signing off.
     * @param taskId NO_ID for a new task, otherwise the task being edited
     * @param repeatDays text from the form; blank means the task doesn't repeat
     */
    public void saveTask(int taskId, int userId, String taskName, String tailNumber,
                         String aircraftModel, String taskType, String dueDate, String dueTime,
                         String notes, String repeatDays) {
        boolean isNewTask = (taskId == AppConstants.NO_ID);

        // A signed-off task is a finished record, so it can't be edited
        MaintenanceTask loaded = task.getValue();
        if (!isNewTask && loaded != null && loaded.isComplete()) {
            errorMessage.setValue("Signed-off tasks can't be edited");
            return;
        }

        // New tasks can't be due in the past. Existing tasks can be,
        // since an overdue task still needs to be edited.
        LocalDateTime earliestAllowed = isNewTask ? clock.get() : LocalDateTime.MIN;

        String error = Validator.checkTask(taskName, tailNumber, dueDate, dueTime, earliestAllowed);
        if (error == null) {
            error = Validator.checkAircraftModel(aircraftModel);
        }
        if (error == null) {
            error = Validator.checkRepeatDays(repeatDays);
        }
        if (error != null) {
            errorMessage.setValue(error);
            return;
        }

        MaintenanceTask newTask = new MaintenanceTask(taskId, userId, taskName.trim(),
                dueDate, dueTime, tailNumber.trim(), notes);
        newTask.setTaskType(taskType);
        newTask.setAircraftModel(aircraftModel == null ? null : aircraftModel.trim());
        boolean repeats = repeatDays != null && !repeatDays.trim().isEmpty();
        newTask.setRepeatDays(repeats ? Integer.parseInt(repeatDays.trim()) : null);

        if (isNewTask) {
            repository.addTask(newTask, new RepositoryCallback<Long>() {
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
            repository.updateTask(newTask, new RepositoryCallback<Integer>() {
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
     * Sign off a task, then reload it so the screen shows who signed it off.
     * If the task repeats, the message says the next one was created.
     */
    public void signOffTask(int taskId, int userId) {
        repository.signOffTask(taskId, userId, new RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long nextTaskId) {
                message.setValue(nextTaskId == AppConstants.NO_ID
                        ? "Task signed off"
                        : "Task signed off. Next recurring task created.");
                loadTask(taskId, userId);
            }

            @Override
            public void onError(Exception e) {
                errorMessage.setValue("Couldn't sign off task");
            }
        });
    }

    /**
     * Delete a task
     */
    public void deleteTask(int taskId, int userId) {
        repository.deleteTask(taskId, userId, new RepositoryCallback<Integer>() {
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
