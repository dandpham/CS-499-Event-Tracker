package com.zybooks.eventplanning_danpham;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * AuditDetails writes the plain-English details saved with each audit entry,
 * such as "Due: 12/15/2026 01:30 PM to 12/20/2026 01:30 PM".
 * It uses no Android classes, so it can be tested with plain JUnit.
 */
public final class AuditDetails {

    private AuditDetails() {
        // Prevent creating instances of this class
    }

    /**
     * Describe a whole task, for created, deleted, and migrated entries
     */
    public static String describe(MaintenanceTask task) {
        StringBuilder text = new StringBuilder();
        text.append("Name: ").append(task.getTaskName())
                .append("; Tail: ").append(task.getTailNumber())
                .append("; Type: ").append(task.getTaskType())
                .append("; Due: ").append(task.getDueDate()).append(' ').append(task.getDueTime());
        if (task.isRecurring()) {
            text.append("; Repeats every ").append(task.getRepeatDays()).append(" days");
        }
        return text.toString();
    }

    /**
     * Describe only the fields that changed in an edit
     * @return the changes, or "No changes" if nothing changed
     */
    public static String describeChanges(MaintenanceTask before, MaintenanceTask after) {
        List<String> changes = new ArrayList<>();
        addIfChanged(changes, "Name", before.getTaskName(), after.getTaskName());
        addIfChanged(changes, "Tail", before.getTailNumber(), after.getTailNumber());
        addIfChanged(changes, "Type", before.getTaskType(), after.getTaskType());
        addIfChanged(changes, "Due",
                before.getDueDate() + " " + before.getDueTime(),
                after.getDueDate() + " " + after.getDueTime());
        addIfChanged(changes, "Notes", blankToNone(before.getNotes()), blankToNone(after.getNotes()));
        addIfChanged(changes, "Repeat", repeatText(before), repeatText(after));
        return changes.isEmpty() ? "No changes" : String.join("; ", changes);
    }

    private static void addIfChanged(List<String> changes, String field, String before, String after) {
        if (!Objects.equals(before, after)) {
            changes.add(field + ": " + before + " to " + after);
        }
    }

    private static String blankToNone(String value) {
        return (value == null || value.trim().isEmpty()) ? "(none)" : value;
    }

    private static String repeatText(MaintenanceTask task) {
        return task.isRecurring() ? task.getRepeatDays() + " days" : "none";
    }
}
