package com.zybooks.eventplanning_danpham;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * PasswordHasher turns passwords into salted PBKDF2 hashes, so the database
 * never stores a password in plain text. It uses no Android classes, so it
 * can be tested with plain JUnit.
 */
public final class PasswordHasher {

    // OWASP's recommended work factor for PBKDF2 with HMAC-SHA256.
    // More iterations make each guess slower for an attacker.
    public static final int ITERATIONS = 600_000;

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_BYTES = 16;  // 128-bit salt
    private static final int HASH_BITS = 256;  // 256-bit hash

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
        // Prevent creating instances of this class
    }

    /**
     * Make a new random salt. Every user gets their own, so two users with
     * the same password still get different hashes.
     * @return the salt as Base64 text
     */
    public static String newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hash a password with a salt
     * @return the hash as Base64 text
     */
    public static String hash(String password, String salt) {
        byte[] saltBytes = Base64.getDecoder().decode(salt);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, HASH_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // Every supported Android version has this algorithm, so this should never happen
            throw new IllegalStateException("PBKDF2 is not available", e);
        } finally {
            spec.clearPassword(); // Don't leave the password sitting in memory
        }
    }

    /**
     * Check a password against a stored hash.
     * Uses a constant-time comparison, so the time taken doesn't reveal
     * how many characters of the hash matched.
     * @return true if the password is correct
     */
    public static boolean matches(String password, String salt, String expectedHash) {
        if (password == null || salt == null || expectedHash == null) {
            return false;
        }
        byte[] actual = Base64.getDecoder().decode(hash(password, salt));
        byte[] expected = Base64.getDecoder().decode(expectedHash);
        return MessageDigest.isEqual(actual, expected);
    }
}
