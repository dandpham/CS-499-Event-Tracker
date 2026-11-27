package com.zybooks.eventplanning_danpham;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for AuditDetails, the text saved with each audit entry
 */
public class AuditDetailsTest {

    private MaintenanceTask sampleTask() {
        MaintenanceTask task = new MaintenanceTask(5, 1, "Oil change", "12/15/2026", "01:30 PM",
                "N12345", "");
        task.setTaskType(MaintenanceTask.TYPE_INSPECTION);
        return task;
    }

    @Test
    public void describe_listsMainFields() {
        assertEquals("Name: Oil change; Tail: N12345; Type: Inspection; Due: 12/15/2026 01:30 PM",
                AuditDetails.describe(sampleTask()));
    }

    @Test
    public void describe_recurringTask_includesInterval() {
        MaintenanceTask task = sampleTask();
        task.setRepeatDays(30);

        assertEquals("Name: Oil change; Tail: N12345; Type: Inspection; Due: 12/15/2026 01:30 PM; "
                + "Repeats every 30 days", AuditDetails.describe(task));
    }

    @Test
    public void describeChanges_listsOnlyChangedFields() {
        MaintenanceTask before = sampleTask();
        MaintenanceTask after = sampleTask();
        after.setDueDate("12/20/2026");
        after.setNotes("Parts arrived");

        assertEquals("Due: 12/15/2026 01:30 PM to 12/20/2026 01:30 PM; Notes: (none) to Parts arrived",
                AuditDetails.describeChanges(before, after));
    }

    @Test
    public void describeChanges_repeatAdded() {
        MaintenanceTask after = sampleTask();
        after.setRepeatDays(90);

        assertEquals("Repeat: none to 90 days", AuditDetails.describeChanges(sampleTask(), after));
    }

    @Test
    public void describeChanges_nothingChanged() {
        assertEquals("No changes", AuditDetails.describeChanges(sampleTask(), sampleTask()));
    }
}
