package com.zybooks.eventplanning_danpham;

import java.util.List;

/**
 * MaintenanceReport holds the results of the report queries.
 * Each list comes from one query in DatabaseHelper.
 */
public class MaintenanceReport {

    private final List<MaintenanceTask> overdueTasks;
    private final List<CountRow> overdueByAircraft;
    private final List<CountRow> frequentRepairs;
    private final List<StatusRow> statusByAircraft;

    public MaintenanceReport(List<MaintenanceTask> overdueTasks, List<CountRow> overdueByAircraft,
                             List<CountRow> frequentRepairs, List<StatusRow> statusByAircraft) {
        this.overdueTasks = overdueTasks;
        this.overdueByAircraft = overdueByAircraft;
        this.frequentRepairs = frequentRepairs;
        this.statusByAircraft = statusByAircraft;
    }

    /** Every overdue open task, soonest due first */
    public List<MaintenanceTask> getOverdueTasks() {
        return overdueTasks;
    }

    /** Overdue open tasks per aircraft, most overdue first */
    public List<CountRow> getOverdueByAircraft() {
        return overdueByAircraft;
    }

    /** The most common repairs, most frequent first */
    public List<CountRow> getFrequentRepairs() {
        return frequentRepairs;
    }

    /** Open and completed task counts per aircraft */
    public List<StatusRow> getStatusByAircraft() {
        return statusByAircraft;
    }

    /**
     * A label with one count, such as a tail number and its overdue tasks
     */
    public static class CountRow {
        private final String label;
        private final int count;

        public CountRow(String label, int count) {
            this.label = label;
            this.count = count;
        }

        public String getLabel() {
            return label;
        }

        public int getCount() {
            return count;
        }
    }

    /**
     * An aircraft with its open and completed task counts
     */
    public static class StatusRow {
        private final String tailNumber;
        private final int openCount;
        private final int completeCount;

        public StatusRow(String tailNumber, int openCount, int completeCount) {
            this.tailNumber = tailNumber;
            this.openCount = openCount;
            this.completeCount = completeCount;
        }

        public String getTailNumber() {
            return tailNumber;
        }

        public int getOpenCount() {
            return openCount;
        }

        public int getCompleteCount() {
            return completeCount;
        }
    }
}
