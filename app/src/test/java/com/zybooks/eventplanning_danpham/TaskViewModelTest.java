package com.zybooks.eventplanning_danpham;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
    private static final int OTHER_USER_ID = 99;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 9, 0);

    private FakeTaskRepository repository;
    private TaskViewModel viewModel;

    @Before
    public void setUp() {
        repository = new FakeTaskRepository();
        viewModel = new TaskViewModel(repository, () -> NOW);
    }

    /** Save a new task with typical values, changing only the repeat interval */
    private void saveNewTask(String repeatDays) {
        viewModel.saveTask(AppConstants.NO_ID, USER_ID, "Oil change", "N12345", "",
                MaintenanceTask.TYPE_INSPECTION, "12/15/2026", "01:30 PM", "", repeatDays);
    }

    // ===== SAVING =====

    @Test
    public void saveTask_newValidTask_addsTaskAsOpen() {
        saveNewTask("");

        assertEquals("Task added successfully", viewModel.getDoneMessage().getValue());
        assertEquals("Oil change", repository.lastAdded.getTaskName());
        assertEquals("N12345", repository.lastAdded.getTailNumber());
        assertEquals(MaintenanceTask.TYPE_INSPECTION, repository.lastAdded.getTaskType());
        assertEquals(MaintenanceTask.STATUS_OPEN, repository.lastAdded.getStatus());
    }

    @Test
    public void saveTask_trimsSpacesFromNameAndTailNumber() {
        viewModel.saveTask(AppConstants.NO_ID, USER_ID, "  Oil change  ", " N12345 ", "",
                MaintenanceTask.TYPE_INSPECTION, "12/15/2026", "01:30 PM", "", "");

        assertEquals("Oil change", repository.lastAdded.getTaskName());
        assertEquals("N12345", repository.lastAdded.getTailNumber());
    }

    @Test
    public void saveTask_missingTailNumber_showsErrorAndDoesNotSave() {
        viewModel.saveTask(AppConstants.NO_ID, USER_ID, "Oil change", "", "",
                MaintenanceTask.TYPE_INSPECTION, "12/15/2026", "01:30 PM", "", "");

        assertEquals("Tail number is required", viewModel.getErrorMessage().getValue());
        assertNull(repository.lastAdded);
        assertNull(viewModel.getDoneMessage().getValue());
    }

    @Test
    public void saveTask_newTaskInPast_showsError() {
        viewModel.saveTask(AppConstants.NO_ID, USER_ID, "Oil change", "N12345", "",
                MaintenanceTask.TYPE_INSPECTION, "11/01/2026", "01:30 PM", "", "");

        assertEquals("Date and time can't be in the past", viewModel.getErrorMessage().getValue());
        assertNull(repository.lastAdded);
    }

    @Test
    public void saveTask_editingOverdueTask_allowsPastDate() {
        repository.addSample(5, USER_ID, "Late inspection", "N12345", "11/01/2026", "01:30 PM",
                MaintenanceTask.STATUS_OPEN);

        viewModel.saveTask(5, USER_ID, "Late inspection", "N12345", "",
                MaintenanceTask.TYPE_INSPECTION, "11/01/2026", "01:30 PM", "Parts arrived", "");

        assertEquals("Task updated successfully", viewModel.getDoneMessage().getValue());
        assertEquals("Parts arrived", repository.lastUpdated.getNotes());
    }

    @Test
    public void saveTask_editingNeverChangesStatus() {
        // Only signing off completes a task, so an edit always saves it as Open
        repository.addSample(5, USER_ID, "Inspection", "N12345", "12/15/2026", "01:30 PM",
                MaintenanceTask.STATUS_OPEN);

        viewModel.saveTask(5, USER_ID, "Inspection", "N12345", "",
                MaintenanceTask.TYPE_INSPECTION, "12/15/2026", "01:30 PM", "", "");

        assertEquals(MaintenanceTask.STATUS_OPEN, repository.lastUpdated.getStatus());
    }

    @Test
    public void saveTask_signedOffTask_showsErrorAndDoesNotSave() {
        repository.addSample(5, USER_ID, "Inspection", "N12345", "12/15/2026", "01:30 PM",
                MaintenanceTask.STATUS_COMPLETE);
        viewModel.loadTask(5, USER_ID);

        viewModel.saveTask(5, USER_ID, "Changed", "N12345", "",
                MaintenanceTask.TYPE_INSPECTION, "12/15/2026", "01:30 PM", "", "");

        assertEquals("Signed-off tasks can't be edited", viewModel.getErrorMessage().getValue());
        assertNull(repository.lastUpdated);
    }

    @Test
    public void saveTask_withRepeatDays_savesInterval() {
        saveNewTask(" 30 ");

        assertEquals(Integer.valueOf(30), repository.lastAdded.getRepeatDays());
        assertTrue(repository.lastAdded.isRecurring());
    }

    @Test
    public void saveTask_blankRepeatDays_doesNotRepeat() {
        saveNewTask("");

        assertNull(repository.lastAdded.getRepeatDays());
    }

    @Test
    public void saveTask_invalidRepeatDays_showsError() {
        saveNewTask("0");

        assertEquals("Repeat interval must be between 1 and 3650 days",
                viewModel.getErrorMessage().getValue());
        assertNull(repository.lastAdded);
    }

    @Test
    public void saveTask_withAircraftModel_trimsAndSavesModel() {
        viewModel.saveTask(AppConstants.NO_ID, USER_ID, "Oil change", "N12345", "  Cessna 172 ",
                MaintenanceTask.TYPE_INSPECTION, "12/15/2026", "01:30 PM", "", "");

        assertEquals("Cessna 172", repository.lastAdded.getAircraftModel());
    }

    @Test
    public void saveTask_repositoryFails_showsError() {
        repository.shouldFail = true;

        saveNewTask("");

        assertEquals("Couldn't save task", viewModel.getErrorMessage().getValue());
        assertNull(viewModel.getDoneMessage().getValue());
    }

    // ===== LOADING =====

    @Test
    public void loadTask_existingTask_setsTask() {
        repository.addSample(5, USER_ID, "Inspection", "N12345", "12/15/2026", "01:30 PM",
                MaintenanceTask.STATUS_OPEN);

        viewModel.loadTask(5, USER_ID);

        assertEquals("Inspection", viewModel.getTask().getValue().getTaskName());
    }

    @Test
    public void loadTask_alsoLoadsHistory() {
        repository.addSample(5, USER_ID, "Inspection", "N12345", "12/15/2026", "01:30 PM",
                MaintenanceTask.STATUS_OPEN);
        repository.history.add(new AuditEntry(1, 5, USER_ID, "dan", AuditEntry.ACTION_CREATED,
                "Name: Inspection", 0, AuditChain.START, "hash"));

        viewModel.loadTask(5, USER_ID);

        assertEquals(1, viewModel.getHistory().getValue().size());
        assertEquals(AuditEntry.ACTION_CREATED, viewModel.getHistory().getValue().get(0).getAction());
    }

    @Test
    public void loadTask_missingTask_showsError() {
        viewModel.loadTask(404, USER_ID);

        assertEquals("Couldn't load task", viewModel.getErrorMessage().getValue());
        assertNull(viewModel.getTask().getValue());
    }

    @Test
    public void loadTask_anotherUsersTask_showsError() {
        repository.addSample(5, OTHER_USER_ID, "Not mine", "N12345", "12/15/2026", "01:30 PM",
                MaintenanceTask.STATUS_OPEN);

        viewModel.loadTask(5, USER_ID);

        assertEquals("Couldn't load task", viewModel.getErrorMessage().getValue());
        assertNull(viewModel.getTask().getValue());
    }

    // ===== SIGNING OFF =====

    @Test
    public void signOffTask_openTask_marksCompleteAndReloads() {
        repository.addSample(5, USER_ID, "Inspection", "N12345", "12/15/2026", "01:30 PM",
                MaintenanceTask.STATUS_OPEN);

        viewModel.signOffTask(5, USER_ID);

        assertEquals("Task signed off", viewModel.getMessage().getValue());
        assertTrue(viewModel.getTask().getValue().isComplete());
        assertEquals("tester", viewModel.getTask().getValue().getSignedOffBy());
    }

    @Test
    public void signOffTask_recurringTask_saysNextTaskCreated() {
        MaintenanceTask task = repository.addSample(5, USER_ID, "Monthly inspection", "N12345",
                "12/15/2026", "01:30 PM", MaintenanceTask.STATUS_OPEN);
        task.setRepeatDays(30);

        viewModel.signOffTask(5, USER_ID);

        assertEquals("Task signed off. Next recurring task created.",
                viewModel.getMessage().getValue());
        assertEquals(2, repository.tasks.size());
    }

    @Test
    public void signOffTask_alreadySignedOff_showsError() {
        repository.addSample(5, USER_ID, "Inspection", "N12345", "12/15/2026", "01:30 PM",
                MaintenanceTask.STATUS_COMPLETE);

        viewModel.signOffTask(5, USER_ID);

        assertEquals("Couldn't sign off task", viewModel.getErrorMessage().getValue());
        assertNull(viewModel.getMessage().getValue());
    }

    @Test
    public void signOffTask_anotherUsersTask_showsError() {
        repository.addSample(5, OTHER_USER_ID, "Not mine", "N12345", "12/15/2026", "01:30 PM",
                MaintenanceTask.STATUS_OPEN);

        viewModel.signOffTask(5, USER_ID);

        assertEquals("Couldn't sign off task", viewModel.getErrorMessage().getValue());
        assertEquals(MaintenanceTask.STATUS_OPEN, repository.tasks.get(0).getStatus());
    }

    // ===== DELETING =====

    @Test
    public void deleteTask_existingTask_setsDoneMessage() {
        repository.addSample(5, USER_ID, "Inspection", "N12345", "12/15/2026", "01:30 PM",
                MaintenanceTask.STATUS_OPEN);

        viewModel.deleteTask(5, USER_ID);

        assertEquals("Task deleted successfully", viewModel.getDoneMessage().getValue());
        assertEquals(0, repository.tasks.size());
    }

    @Test
    public void deleteTask_anotherUsersTask_showsErrorAndKeepsTask() {
        repository.addSample(5, OTHER_USER_ID, "Not mine", "N12345", "12/15/2026", "01:30 PM",
                MaintenanceTask.STATUS_OPEN);

        viewModel.deleteTask(5, USER_ID);

        assertEquals("Couldn't delete task", viewModel.getErrorMessage().getValue());
        assertEquals(1, repository.tasks.size());
    }

    @Test
    public void deleteTask_repositoryFails_showsError() {
        repository.shouldFail = true;

        viewModel.deleteTask(5, USER_ID);

        assertEquals("Couldn't delete task", viewModel.getErrorMessage().getValue());
    }

    @Test
    public void clearError_removesError() {
        viewModel.loadTask(404, USER_ID);

        viewModel.clearError();

        assertNull(viewModel.getErrorMessage().getValue());
    }
}
