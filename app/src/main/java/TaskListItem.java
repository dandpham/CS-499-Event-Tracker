package com.zybooks.eventplanning_danpham;

/**
 * TaskListItem pairs a task with information the dashboard needs to show it,
 * such as whether it's overdue. The ViewModel decides this, not the screen.
 */
public class TaskListItem {

    private final MaintenanceTask task;
    private final boolean overdue;

    public TaskListItem(MaintenanceTask task, boolean overdue) {
        this.task = task;
        this.overdue = overdue;
    }

    public MaintenanceTask getTask() {
        return task;
    }

    public boolean isOverdue() {
        return overdue;
    }
}
