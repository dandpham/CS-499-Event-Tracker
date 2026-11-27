package com.zybooks.eventplanning_danpham;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

/**
 * AuditChain links audit entries into a hash chain. Each entry stores the hash
 * of the entry before it, so changing or removing any entry breaks the chain
 * from that point on. It uses no Android classes, so it can be tested with plain JUnit.
 */
public final class AuditChain {

    // The "previous hash" of the very first entry
    public static final String START = "START";

    private AuditChain() {
        // Prevent creating instances of this class
    }

    /**
     * Compute an entry's hash: SHA-256 of the previous hash plus the entry's contents.
     * Each field is written with its length first, so different entries can never
     * produce the same text. Without that, "ab" + "c" and "a" + "bc" would match.
     * @return the hash as 64 hex characters
     */
    public static String computeHash(String prevHash, int taskId, int userId, String action,
                                     String details, long createdAt) {
        StringBuilder text = new StringBuilder();
        appendField(text, prevHash);
        appendField(text, String.valueOf(taskId));
        appendField(text, String.valueOf(userId));
        appendField(text, action);
        appendField(text, details);
        appendField(text, String.valueOf(createdAt));
        return sha256(text.toString());
    }

    private static void appendField(StringBuilder text, String value) {
        String safe = (value == null) ? "" : value;
        text.append(safe.length()).append(':').append(safe).append(';');
    }

    private static String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // Every Java and Android version has SHA-256, so this should never happen
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /**
     * Check the whole log, oldest entry first.
     * @param entries every audit entry, in the order they were saved
     * @return whether the log is intact, and where it broke if not
     */
    public static Result verify(List<AuditEntry> entries) {
        String expectedPrev = START;
        for (AuditEntry entry : entries) {
            // Each entry must point to the one before it
            if (!expectedPrev.equals(entry.getPrevHash())) {
                return Result.broken(entry.getAuditId(),
                        "Chain broken at entry " + entry.getAuditId()
                                + ". An entry before it was changed or removed.");
            }

            // The entry's contents must still match its hash
            String recomputed = computeHash(entry.getPrevHash(), entry.getTaskId(),
                    entry.getUserId(), entry.getAction(), entry.getDetails(), entry.getCreatedAt());
            if (!recomputed.equals(entry.getEntryHash())) {
                return Result.broken(entry.getAuditId(),
                        "Entry " + entry.getAuditId() + " was altered.");
            }
            expectedPrev = entry.getEntryHash();
        }
        return Result.ok(entries.size());
    }

    /**
     * The result of verifying the log
     */
    public static final class Result {
        private final boolean valid;
        private final long brokenAtId; // NO_ID when the log is valid
        private final String message;

        private Result(boolean valid, long brokenAtId, String message) {
            this.valid = valid;
            this.brokenAtId = brokenAtId;
            this.message = message;
        }

        static Result ok(int count) {
            return new Result(true, AppConstants.NO_ID,
                    "Audit log verified. All " + count + " entries are intact.");
        }

        static Result broken(long auditId, String message) {
            return new Result(false, auditId, message);
        }

        public boolean isValid() {
            return valid;
        }

        public long getBrokenAtId() {
            return brokenAtId;
        }

        public String getMessage() {
            return message;
        }
    }
}
