package com.zybooks.eventplanning_danpham;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for AuditChain, the audit log's hash chain
 */
public class AuditChainTest {

    /** Build a valid chain of entries the same way DatabaseHelper does */
    private List<AuditEntry> buildChain(int count) {
        List<AuditEntry> entries = new ArrayList<>();
        String prev = AuditChain.START;
        for (int i = 1; i <= count; i++) {
            String details = "Entry " + i;
            String hash = AuditChain.computeHash(prev, 10, 1, AuditEntry.ACTION_UPDATED, details, 1000L * i);
            entries.add(new AuditEntry(i, 10, 1, "dan", AuditEntry.ACTION_UPDATED, details,
                    1000L * i, prev, hash));
            prev = hash;
        }
        return entries;
    }

    /** Copy an entry with different details but its original hashes, like an outside edit */
    private AuditEntry withDetails(AuditEntry entry, String details) {
        return new AuditEntry(entry.getAuditId(), entry.getTaskId(), entry.getUserId(),
                entry.getUsername(), entry.getAction(), details, entry.getCreatedAt(),
                entry.getPrevHash(), entry.getEntryHash());
    }

    @Test
    public void computeHash_isSha256Hex() {
        String hash = AuditChain.computeHash(AuditChain.START, 1, 1, "CREATED", "x", 0);

        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]+"));
    }

    @Test
    public void computeHash_sameInput_givesSameHash() {
        assertEquals(AuditChain.computeHash("p", 1, 2, "A", "d", 3),
                AuditChain.computeHash("p", 1, 2, "A", "d", 3));
    }

    @Test
    public void computeHash_fieldsCantBeShiftedToMatch() {
        // Without length prefixes, "ab" + "c" and "a" + "bc" would hash the same
        assertNotEquals(AuditChain.computeHash("p", 1, 2, "ab", "c", 3),
                AuditChain.computeHash("p", 1, 2, "a", "bc", 3));
    }

    @Test
    public void verify_emptyLog_isValid() {
        assertTrue(AuditChain.verify(new ArrayList<>()).isValid());
    }

    @Test
    public void verify_intactChain_isValid() {
        AuditChain.Result result = AuditChain.verify(buildChain(5));

        assertTrue(result.isValid());
        assertEquals("Audit log verified. All 5 entries are intact.", result.getMessage());
    }

    @Test
    public void verify_editedEntry_reportsThatEntry() {
        List<AuditEntry> entries = buildChain(5);
        entries.set(2, withDetails(entries.get(2), "Signed off by someone else"));

        AuditChain.Result result = AuditChain.verify(entries);

        assertFalse(result.isValid());
        assertEquals(3, result.getBrokenAtId());
        assertEquals("Entry 3 was altered.", result.getMessage());
    }

    @Test
    public void verify_deletedEntry_reportsBreakAtNextEntry() {
        List<AuditEntry> entries = buildChain(5);
        entries.remove(1); // Remove entry 2

        AuditChain.Result result = AuditChain.verify(entries);

        assertFalse(result.isValid());
        assertEquals(3, result.getBrokenAtId());
    }

    @Test
    public void verify_reorderedEntries_isInvalid() {
        List<AuditEntry> entries = buildChain(3);
        AuditEntry first = entries.remove(0);
        entries.add(first);

        assertFalse(AuditChain.verify(entries).isValid());
    }
}
