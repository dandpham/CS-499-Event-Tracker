package com.zybooks.eventplanning_danpham;

import static org.junit.Assert.assertEquals;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Unit tests for ReportsViewModel, using a fake repository
 */
public class ReportsViewModelTest {

    // Makes LiveData update instantly on the test thread
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private FakeTaskRepository repository;
    private ReportsViewModel viewModel;

    @Before
    public void setUp() {
        repository = new FakeTaskRepository();
        viewModel = new ReportsViewModel(repository);
    }

    @Test
    public void loadReport_formatsEverySection() {
        MaintenanceTask late = new MaintenanceTask(1, 1, "100-hour inspection", "11/01/2026",
                "01:30 PM", "N12345", "");
        repository.report = new MaintenanceReport(
                Collections.singletonList(late),
                Collections.singletonList(new MaintenanceReport.CountRow("N12345", 1)),
                Arrays.asList(new MaintenanceReport.CountRow("Replace tire", 3),
                        new MaintenanceReport.CountRow("Fix light", 1)),
                Collections.singletonList(new MaintenanceReport.StatusRow("N12345", 2, 5)));

        viewModel.loadReport(1);

        assertEquals("OVERDUE TASKS\n"
                + "N12345: 100-hour inspection (due 11/01/2026 01:30 PM)\n\n"
                + "OVERDUE BY AIRCRAFT\n"
                + "N12345: 1 overdue\n\n"
                + "MOST FREQUENT REPAIRS\n"
                + "Replace tire: 3 times\n"
                + "Fix light: 1 time\n\n"
                + "TASKS BY AIRCRAFT\n"
                + "N12345: 2 open, 5 complete", viewModel.getReportText().getValue());
    }

    @Test
    public void loadReport_emptyReport_showsPlaceholders() {
        repository.report = new MaintenanceReport(new ArrayList<>(), new ArrayList<>(),
                new ArrayList<>(), new ArrayList<>());

        viewModel.loadReport(1);

        assertEquals("OVERDUE TASKS\nNo overdue tasks\n\n"
                + "OVERDUE BY AIRCRAFT\nNo overdue tasks\n\n"
                + "MOST FREQUENT REPAIRS\nNo repairs recorded\n\n"
                + "TASKS BY AIRCRAFT\nNo aircraft yet", viewModel.getReportText().getValue());
    }

    @Test
    public void loadReport_repositoryFails_showsError() {
        repository.shouldFail = true;

        viewModel.loadReport(1);

        assertEquals("Couldn't load report", viewModel.getErrorMessage().getValue());
    }

    @Test
    public void verifyAuditLog_showsResultMessage() {
        repository.verifyResult = AuditChain.verify(new ArrayList<>());

        viewModel.verifyAuditLog();

        assertEquals("Audit log verified. All 0 entries are intact.",
                viewModel.getVerifyMessage().getValue());
    }
}
