package com.example.cs360weighttracker.data;

import androidx.annotation.NonNull;

import java.util.Arrays;

import at.favre.lib.crypto.bcrypt.BCrypt;

/**
 * Utility class for hashing and verifying passwords using bcrypt.
 *
 * <p>Char arrays are zeroed after use to reduce the window during
 * which the plain-text password resides in memory.</p>
 */
public class PasswordUtils {

    private static final int BCRYPT_COST = 12;

    /**
     * Hashes a plain-text password with bcrypt.
     *
     * @param plainPassword the plain-text password to hash
     * @return the bcrypt hash string
     */
    public static String hashPassword(@NonNull String plainPassword) {
        char[] chars = plainPassword.toCharArray();
        try {
            return BCrypt.withDefaults().hashToString(BCRYPT_COST, chars);
        } finally {
            Arrays.fill(chars, '\0');
        }
    }

    /**
     * Verifies a plain-text password against a stored bcrypt hash.
     *
     * @param plainPassword  the plain-text password to check
     * @param hashedPassword the stored bcrypt hash
     * @return {@code true} if the password matches
     */
    public static boolean checkPassword(@NonNull String plainPassword, String hashedPassword) {
        if (hashedPassword == null || hashedPassword.isEmpty()) {
            return false;
        }
        char[] chars = plainPassword.toCharArray();
        try {
            BCrypt.Result result = BCrypt.verifyer().verify(chars, hashedPassword);
            return result.verified;
        } catch (IllegalArgumentException e) {
            return false;
        } finally {
            Arrays.fill(chars, '\0');
        }
    }
}
