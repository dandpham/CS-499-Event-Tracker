package com.zybooks.eventplanning_danpham;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * MaintenanceTask represents one aircraft maintenance task.
 * Implements Parcelable so a task can be passed between screens.
 */
public class MaintenanceTask implements Parcelable {

    // Task types
    public static final String TYPE_INSPECTION = "Inspection";
    public static final String TYPE_REPAIR = "Repair";
    public static final String TYPE_PART_REPLACEMENT = "Part Replacement";
    public static final String TYPE_PERIODIC = "Periodic Maintenance";
    public static final String[] TASK_TYPES = {
            TYPE_INSPECTION, TYPE_REPAIR, TYPE_PART_REPLACEMENT, TYPE_PERIODIC
    };

    // Task statuses
    public static final String STATUS_OPEN = "Open";
    public static final String STATUS_COMPLETE = "Complete";

    // Tail number used for old events that had no location
    public static final String UNASSIGNED = "UNASSIGNED";

    private int taskId;
    private int userId;
    private String taskName;
    private String dueDate;
    private String dueTime;
    private String tailNumber;
    private String notes;
    private String taskType = TYPE_INSPECTION;
    private String status = STATUS_OPEN;

    // Constructor for creating new tasks
    public MaintenanceTask(int userId, String taskName, String dueDate, String dueTime,
                           String tailNumber, String notes) {
        this.userId = userId;
        this.taskName = taskName;
        this.dueDate = dueDate;
        this.dueTime = dueTime;
        this.tailNumber = tailNumber;
        this.notes = notes;
    }

    // Constructor for existing tasks with ID
    public MaintenanceTask(int taskId, int userId, String taskName, String dueDate,
                           String dueTime, String tailNumber, String notes) {
        this(userId, taskName, dueDate, dueTime, tailNumber, notes);
        this.taskId = taskId;
    }

    // Parcelable implementation for passing tasks between screens
    protected MaintenanceTask(Parcel in) {
        taskId = in.readInt();
        userId = in.readInt();
        taskName = in.readString();
        dueDate = in.readString();
        dueTime = in.readString();
        tailNumber = in.readString();
        notes = in.readString();
        taskType = in.readString();
        status = in.readString();
    }

    public static final Creator<MaintenanceTask> CREATOR = new Creator<MaintenanceTask>() {
        @Override
        public MaintenanceTask createFromParcel(Parcel in) {
            return new MaintenanceTask(in);
        }

        @Override
        public MaintenanceTask[] newArray(int size) {
            return new MaintenanceTask[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // Must write in the same order the Parcel constructor reads
        dest.writeInt(taskId);
        dest.writeInt(userId);
        dest.writeString(taskName);
        dest.writeString(dueDate);
        dest.writeString(dueTime);
        dest.writeString(tailNumber);
        dest.writeString(notes);
        dest.writeString(taskType);
        dest.writeString(status);
    }

    /**
     * @return true if the task has been completed
     */
    public boolean isComplete() {
        return STATUS_COMPLETE.equals(status);
    }

    // Getters and setters
    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getDueTime() {
        return dueTime;
    }

    public void setDueTime(String dueTime) {
        this.dueTime = dueTime;
    }

    public String getTailNumber() {
        return tailNumber;
    }

    public void setTailNumber(String tailNumber) {
        this.tailNumber = tailNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}