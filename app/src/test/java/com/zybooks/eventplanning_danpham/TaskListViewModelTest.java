package com.zybooks.eventplanning_danpham;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Unit tests for TaskListViewModel, using a fake repository and a fixed time
 */
public class TaskListViewModelTest {

    // Makes LiveData update instantly on the test thread
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static final int USER_ID = 1;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 12, 9, 0);

    private FakeTaskRepository repository;
    private TaskListViewModel viewModel;

    @Before
    public void setUp() {
        repository = new FakeTaskRepository();
        viewModel = new TaskListViewModel(repository, () -> NOW);
    }

    private List<TaskListItem> currentItems() {
        return viewModel.getTasks().getValue();
    }

    @Test
    public void loadTasks_sortsByDueDateAcrossYears() {
        // Added out of order. As text, "01/05/2027" would sort before "12/15/2026".
        repository.addSample(1, USER_ID, "January task", "N100", "01/05/2027", "08:00 AM", MaintenanceTask.STATUS_OPEN);
        repository.addSample(2, USER_ID, "December task", "N200", "12/15/2026", "01:30 PM", MaintenanceTask.STATUS_OPEN);

        viewModel.loadTasks(USER_ID);

        List<TaskListItem> items = currentItems();
        assertEquals("December task", items.get(0).getTask().getTaskName());
        assertEquals("January task", items.get(1).getTask().getTaskName());
    }

    @Test
    public void loadTasks_sortsAfternoonAfterMorningOnSameDay() {
        // As text, "01:00 PM" would sort before "11:00 AM"
        repository.addSample(1, USER_ID, "Afternoon", "N100", "12/15/2026", "01:00 PM", MaintenanceTask.STATUS_OPEN);
        repository.addSample(2, USER_ID, "Morning", "N200", "12/15/2026", "11:00 AM", MaintenanceTask.STATUS_OPEN);

        viewModel.loadTasks(USER_ID);

        assertEquals("Morning", currentItems().get(0).getTask().getTaskName());
    }

    @Test
    public void loadTasks_openPastDueTask_isOverdue() {
        repository.addSample(1, USER_ID, "Late inspection", "N100", "11/01/2026", "01:00 PM", MaintenanceTask.STATUS_OPEN);

        viewModel.loadTasks(USER_ID);

        assertTrue(currentItems().get(0).isOverdue());
    }

    @Test
    public void loadTasks_completedPastDueTask_isNotOverdue() {
        repository.addSample(1, USER_ID, "Finished repair", "N100", "11/01/2026", "01:00 PM", MaintenanceTask.STATUS_COMPLETE);

        viewModel.loadTasks(USER_ID);

        assertFalse(currentItems().get(0).isOverdue());
    }

    @Test
    public void loadTasks_futureTask_isNotOverdue() {
        repository.addSample(1, USER_ID, "Upcoming", "N100", "12/15/2026", "01:30 PM", MaintenanceTask.STATUS_OPEN);

        viewModel.loadTasks(USER_ID);

        assertFalse(currentItems().get(0).isOverdue());
    }

    @Test
    public void loadTasks_onlyShowsThisUsersTasks() {
        repository.addSample(1, USER_ID, "Mine", "N100", "12/15/2026", "01:30 PM", MaintenanceTask.STATUS_OPEN);
        repository.addSample(2, 99, "Someone else's", "N200", "12/15/2026", "01:30 PM", MaintenanceTask.STATUS_OPEN);

        viewModel.loadTasks(USER_ID);

        assertEquals(1, currentItems().size());
        assertEquals("Mine", currentItems().get(0).getTask().getTaskName());
    }

    @Test
    public void loadTasks_repositoryFails_setsErrorMessage() {
        repository.shouldFail = true;

        viewModel.loadTasks(USER_ID);

        assertEquals("Couldn't load tasks", viewModel.getErrorMessage().getValue());
    }

    @Test
    public void deleteTask_removesTaskAndShowsMessage() {
        repository.addSample(1, USER_ID, "Keep", "N100", "12/15/2026", "01:30 PM", MaintenanceTask.STATUS_OPEN);
        repository.addSample(2, USER_ID, "Remove", "N200", "12/16/2026", "01:30 PM", MaintenanceTask.STATUS_OPEN);
        viewModel.loadTasks(USER_ID);

        viewModel.deleteTask(2);

        assertEquals(1, currentItems().size());
        assertEquals("Keep", currentItems().get(0).getTask().getTaskName());
        assertEquals("Task deleted", viewModel.getMessage().getValue());
    }

    @Test
    public void deleteTask_repositoryFails_setsErrorMessage() {
        repository.addSample(1, USER_ID, "Task", "N100", "12/15/2026", "01:30 PM", MaintenanceTask.STATUS_OPEN);
        viewModel.loadTasks(USER_ID);
        repository.shouldFail = true;

        viewModel.deleteTask(1);

        assertEquals("Couldn't delete task", viewModel.getErrorMessage().getValue());
    }

    @Test
    public void search_matchesTailNumberIgnoringCase() {
        repository.addSample(1, USER_ID, "Oil change", "N12345", "12/15/2026", "01:30 PM", MaintenanceTask.STATUS_OPEN);
        repository.addSample(2, USER_ID, "Tire swap", "N999", "12/16/2026", "01:30 PM", MaintenanceTask.STATUS_OPEN);
        viewModel.loadTasks(USER_ID);

        viewModel.setSearchQuery("n123");

        assertEquals(1, currentItems().size());
        assertEquals("N12345", currentItems().get(0).getTask().getTailNumber());
        assertTrue(viewModel.isSearching());
    }

    @Test
    public void search_matchesStatus() {
        repository.addSample(1, USER_ID, "Open task", "N100", "12/15/2026", "01:30 PM", MaintenanceTask.STATUS_OPEN);
        repository.addSample(2, USER_ID, "Done task", "N200", "12/16/2026", "01:30 PM", MaintenanceTask.STATUS_COMPLETE);
        viewModel.loadTasks(USER_ID);

        viewModel.setSearchQuery("complete");

        assertEquals(1, currentItems().size());
        assertEquals("Done task", currentItems().get(0).getTask().getTaskName());
    }

    @Test
    public void search_noMatches_returnsEmptyList() {
        repository.addSample(1, USER_ID, "Oil change", "N100", "12/15/2026", "01:30 PM", MaintenanceTask.STATUS_OPEN);
        viewModel.loadTasks(USER_ID);

        viewModel.setSearchQuery("zzz");

        assertTrue(currentItems().isEmpty());
    }

    @Test
    public void search_clearingQuery_showsAllTasks() {
        repository.addSample(1, USER_ID, "Oil change", "N100", "12/15/2026", "01:30 PM", MaintenanceTask.STATUS_OPEN);
        repository.addSample(2, USER_ID, "Tire swap", "N200", "12/16/2026", "01:30 PM", MaintenanceTask.STATUS_OPEN);
        viewModel.loadTasks(USER_ID);
        viewModel.setSearchQuery("oil");

        viewModel.setSearchQuery("");

        assertEquals(2, currentItems().size());
        assertFalse(viewModel.isSearching());
    }

    @Test
    public void clearMessages_removesMessages() {
        repository.shouldFail = true;
        viewModel.loadTasks(USER_ID);

        viewModel.clearMessages();

        assertNull(viewModel.getErrorMessage().getValue());
        assertNull(viewModel.getMessage().getValue());
    }
}
