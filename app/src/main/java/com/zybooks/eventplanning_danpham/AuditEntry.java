package com.zybooks.eventplanning_danpham;

/**
 * AuditEntry is one row of the audit log: who did what to a task, and when.
 * Entries are never changed after they are saved.
 */
public class AuditEntry {

    // Actions recorded in the log
    public static final String ACTION_CREATED = "CREATED";
    public static final String ACTION_UPDATED = "UPDATED";
    public static final String ACTION_SIGNED_OFF = "SIGNED_OFF";
    public static final String ACTION_DELETED = "DELETED";
    public static final String ACTION_MIGRATED = "MIGRATED";

    private final long auditId;
    private final int taskId;
    private final int userId;
    private final String username;   // For display only; not part of the hash
    private final String action;
    private final String details;
    private final long createdAt;
    private final String prevHash;
    private final String entryHash;

    public AuditEntry(long auditId, int taskId, int userId, String username, String action,
                      String details, long createdAt, String prevHash, String entryHash) {
        this.auditId = auditId;
        this.taskId = taskId;
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.details = details;
        this.createdAt = createdAt;
        this.prevHash = prevHash;
        this.entryHash = entryHash;
    }

    public long getAuditId() {
        return auditId;
    }

    public int getTaskId() {
        return taskId;
    }

    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getPrevHash() {
        return prevHash;
    }

    public String getEntryHash() {
        return entryHash;
    }
}
