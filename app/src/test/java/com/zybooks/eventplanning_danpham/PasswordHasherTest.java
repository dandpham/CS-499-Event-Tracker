package com.zybooks.eventplanning_danpham;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Base64;

/**
 * Unit tests for PasswordHasher
 */
public class PasswordHasherTest {

    @Test
    public void matches_correctPassword_returnsTrue() {
        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash("secret123", salt);

        assertTrue(PasswordHasher.matches("secret123", salt, hash));
    }

    @Test
    public void matches_wrongPassword_returnsFalse() {
        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash("secret123", salt);

        assertFalse(PasswordHasher.matches("secret124", salt, hash));
    }

    @Test
    public void matches_nullPassword_returnsFalse() {
        String salt = PasswordHasher.newSalt();

        assertFalse(PasswordHasher.matches(null, salt, PasswordHasher.hash("secret123", salt)));
    }

    @Test
    public void hash_samePasswordDifferentSalts_givesDifferentHashes() {
        // Two users with the same password must not have the same stored hash
        String first = PasswordHasher.hash("secret123", PasswordHasher.newSalt());
        String second = PasswordHasher.hash("secret123", PasswordHasher.newSalt());

        assertNotEquals(first, second);
    }

    @Test
    public void hash_sameSalt_givesSameHash() {
        String salt = PasswordHasher.newSalt();

        assertEquals(PasswordHasher.hash("secret123", salt), PasswordHasher.hash("secret123", salt));
    }

    @Test
    public void hash_neverContainsThePassword() {
        String hash = PasswordHasher.hash("secret123", PasswordHasher.newSalt());

        assertFalse(hash.contains("secret123"));
    }

    @Test
    public void hash_is256Bits() {
        String hash = PasswordHasher.hash("secret123", PasswordHasher.newSalt());

        assertEquals(32, Base64.getDecoder().decode(hash).length);
    }

    @Test
    public void newSalt_is128BitsAndRandom() {
        String first = PasswordHasher.newSalt();

        assertEquals(16, Base64.getDecoder().decode(first).length);
        assertNotEquals(first, PasswordHasher.newSalt());
    }
}
