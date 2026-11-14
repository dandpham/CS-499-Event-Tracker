package com.zybooks.eventplanning_danpham;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDateTime;

/**
 * Unit tests for TaskViewModel, using a fake repository and a fixed time
 */
public class TaskViewModelTest {

    // Makes LiveData update instantly on the test thread
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static final int USER_ID = 1;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 9, 0);

    private FakeTaskRepository repository;
    private TaskViewModel viewModel;

    @Before
    public void setUp() {
        repository = new FakeTaskRepository();
        viewModel = new TaskViewModel(repository, () -> NOW);
    }

    @Test
    public void saveTask_newValidTask_addsTaskAsOpen() {
        viewModel.saveTask(AppConstants.NO_ID, USER_ID, "Oil change", "N12345",
                MaintenanceTask.TYPE_INSPECTION, "12/15/2026", "01:30 PM", "", false);

        assertEquals("Task added successfully", viewModel.getDoneMessage().getValue());
        assertEquals("Oil change", repository.lastAdded.getTaskName());
        assertEquals("N12345", repository.lastAdded.getTailNumber());
        assertEquals(MaintenanceTask.TYPE_INSPECTION, repository.lastAdded.getTaskType());
        assertEquals(MaintenanceTask.STATUS_OPEN, repository.lastAdded.getStatus());
    }

    @Test
    public void saveTask_trimsSpacesFromNameAndTailNumber() {
        viewModel.saveTask(AppConstants.NO_ID, USER_ID, "  Oil change  ", " N12345 ",
                MaintenanceTask.TYPE_INSPECTION, "12/15/2026", "01:30 PM", "", false);

        assertEquals("Oil change", repository.lastAdded.getTaskName());
        assertEquals("N12345", repository.lastAdded.getTailNumber());
    }

    @Test
    public void saveTask_missingTailNumber_showsErrorAndDoesNotSave() {
        viewModel.saveTask(AppConstants.NO_ID, USER_ID, "Oil change", "",
                MaintenanceTask.TYPE_INSPECTION, "12/15/2026", "01:30 PM", "", false);

        assertEquals("Tail number is required", viewModel.getErrorMessage().getValue());
        assertNull(repository.lastAdded);
        assertNull(viewModel.getDoneMessage().getValue());
    }

    @Test
    public void saveTask_newTaskInPast_showsError() {
        viewModel.saveTask(AppConstants.NO_ID, USER_ID, "Oil change", "N12345",
                MaintenanceTask.TYPE_INSPECTION, "11/01/2026", "01:30 PM", "", false);

        assertEquals("Date and time can't be in the past", viewModel.getErrorMessage().getValue());
        assertNull(repository.lastAdded);
    }

    @Test
    public void saveTask_editingOverdueTask_allowsPastDate() {
        repository.addSample(5, USER_ID, "Late inspection", "N12345", "11/01/2026", "01:30 PM",
                MaintenanceTask.STATUS_OPEN);

        viewModel.saveTask(5, USER_ID, "Late inspection", "N12345",
                MaintenanceTask.TYPE_INSPECTION, "11/01/2026", "01:30 PM", "Parts arrived", false);

        assertEquals("Task updated successfully", viewModel.getDoneMessage().getValue());
        assertEquals("Parts arrived", repository.lastUpdated.getNotes());
    }

    @Test
    public void saveTask_completeChecked_setsStatusComplete() {
        repository.addSample(5, USER_ID, "Inspection", "N12345", "12/15/2026", "01:30 PM",
                MaintenanceTask.STATUS_OPEN);

        viewModel.saveTask(5, USER_ID, "Inspection", "N12345",
                MaintenanceTask.TYPE_INSPECTION, "12/15/2026", "01:30 PM", "", true);

        assertEquals(MaintenanceTask.STATUS_COMPLETE, repository.lastUpdated.getStatus());
    }

    @Test
    public void saveTask_repositoryFails_showsError() {
        repository.shouldFail = true;

        viewModel.saveTask(AppConstants.NO_ID, USER_ID, "Oil change", "N12345",
                MaintenanceTask.TYPE_INSPECTION, "12/15/2026", "01:30 PM", "", false);

        assertEquals("Couldn't save task", viewModel.getErrorMessage().getValue());
        assertNull(viewModel.getDoneMessage().getValue());
    }

    @Test
    public void loadTask_existingTask_setsTask() {
        repository.addSample(5, USER_ID, "Inspection", "N12345", "12/15/2026", "01:30 PM",
                MaintenanceTask.STATUS_OPEN);

        viewModel.loadTask(5);

        assertEquals("Inspection", viewModel.getTask().getValue().getTaskName());
    }

    @Test
    public void loadTask_missingTask_showsError() {
        viewModel.loadTask(404);

        assertEquals("Couldn't load task", viewModel.getErrorMessage().getValue());
        assertNull(viewModel.getTask().getValue());
    }

    @Test
    public void deleteTask_existingTask_setsDoneMessage() {
        repository.addSample(5, USER_ID, "Inspection", "N12345", "12/15/2026", "01:30 PM",
                MaintenanceTask.STATUS_OPEN);

        viewModel.deleteTask(5);

        assertEquals("Task deleted successfully", viewModel.getDoneMessage().getValue());
        assertEquals(0, repository.tasks.size());
    }

    @Test
    public void deleteTask_repositoryFails_showsError() {
        repository.shouldFail = true;

        viewModel.deleteTask(5);

        assertEquals("Couldn't delete task", viewModel.getErrorMessage().getValue());
    }

    @Test
    public void clearError_removesError() {
        viewModel.loadTask(404);

        viewModel.clearError();

        assertNull(viewModel.getErrorMessage().getValue());
    }
}
