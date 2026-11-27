package com.zybooks.eventplanning_danpham;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * ReportsViewModel loads the maintenance report, turns it into text for the
 * reports screen, and verifies the audit log's hash chain.
 */
public class ReportsViewModel extends ViewModel {

    private final TaskRepository repository;

    // Data the screen observes
    private final MutableLiveData<String> reportText = new MutableLiveData<>();
    private final MutableLiveData<String> verifyMessage = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ReportsViewModel(TaskRepository repository) {
        this.repository = repository;
    }

    public LiveData<String> getReportText() {
        return reportText;
    }

    /** The result of the last audit log check */
    public LiveData<String> getVerifyMessage() {
        return verifyMessage;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void clearError() {
        errorMessage.setValue(null);
    }

    public void loadReport(int userId) {
        repository.getReport(userId, new RepositoryCallback<MaintenanceReport>() {
            @Override
            public void onSuccess(MaintenanceReport result) {
                reportText.setValue(formatReport(result));
            }

            @Override
            public void onError(Exception e) {
                errorMessage.setValue("Couldn't load report");
            }
        });
    }

    public void verifyAuditLog() {
        repository.verifyAuditLog(new RepositoryCallback<AuditChain.Result>() {
            @Override
            public void onSuccess(AuditChain.Result result) {
                verifyMessage.setValue(result.getMessage());
            }

            @Override
            public void onError(Exception e) {
                errorMessage.setValue("Couldn't verify audit log");
            }
        });
    }

    /**
     * Turn the report into the text the screen shows
     */
    static String formatReport(MaintenanceReport report) {
        StringBuilder text = new StringBuilder();

        text.append("OVERDUE TASKS\n");
        if (report.getOverdueTasks().isEmpty()) {
            text.append("No overdue tasks\n");
        }
        for (MaintenanceTask task : report.getOverdueTasks()) {
            text.append(task.getTailNumber()).append(": ").append(task.getTaskName())
                    .append(" (due ").append(task.getDueDate()).append(' ')
                    .append(task.getDueTime()).append(")\n");
        }

        text.append("\nOVERDUE BY AIRCRAFT\n");
        if (report.getOverdueByAircraft().isEmpty()) {
            text.append("No overdue tasks\n");
        }
        for (MaintenanceReport.CountRow row : report.getOverdueByAircraft()) {
            text.append(row.getLabel()).append(": ").append(row.getCount()).append(" overdue\n");
        }

        text.append("\nMOST FREQUENT REPAIRS\n");
        if (report.getFrequentRepairs().isEmpty()) {
            text.append("No repairs recorded\n");
        }
        for (MaintenanceReport.CountRow row : report.getFrequentRepairs()) {
            text.append(row.getLabel()).append(": ").append(row.getCount())
                    .append(row.getCount() == 1 ? " time\n" : " times\n");
        }

        text.append("\nTASKS BY AIRCRAFT\n");
        if (report.getStatusByAircraft().isEmpty()) {
            text.append("No aircraft yet\n");
        }
        for (MaintenanceReport.StatusRow row : report.getStatusByAircraft()) {
            text.append(row.getTailNumber()).append(": ")
                    .append(row.getOpenCount()).append(" open, ")
                    .append(row.getCompleteCount()).append(" complete\n");
        }

        return text.toString().trim();
    }
}
