package com.example.cs360weighttracker.data;

import org.junit.Test;

import static org.junit.Assert.*;

public class PasswordUtilsTest {

    @Test
    public void hashPassword_returnsNonNullBcryptHash() {
        String hash = PasswordUtils.hashPassword("myPassword123");
        assertNotNull(hash);
        assertTrue("Hash should start with bcrypt prefix", hash.startsWith("$2a$"));
    }

    @Test
    public void hashPassword_differentInputs_produceDifferentHashes() {
        String hash1 = PasswordUtils.hashPassword("password1");
        String hash2 = PasswordUtils.hashPassword("password2");
        assertNotEquals(hash1, hash2);
    }

    @Test
    public void hashPassword_sameInput_producesDifferentHashes() {
        // bcrypt uses random salts, so same input should produce different hashes
        String hash1 = PasswordUtils.hashPassword("samePassword");
        String hash2 = PasswordUtils.hashPassword("samePassword");
        assertNotEquals("Random salt should produce different hashes", hash1, hash2);
    }

    @Test
    public void checkPassword_correctPassword_returnsTrue() {
        String hash = PasswordUtils.hashPassword("correctPassword");
        assertTrue(PasswordUtils.checkPassword("correctPassword", hash));
    }

    @Test
    public void checkPassword_wrongPassword_returnsFalse() {
        String hash = PasswordUtils.hashPassword("correctPassword");
        assertFalse(PasswordUtils.checkPassword("wrongPassword", hash));
    }

    @Test
    public void checkPassword_emptyPassword_matchesEmptyHash() {
        String hash = PasswordUtils.hashPassword("");
        assertTrue(PasswordUtils.checkPassword("", hash));
    }

    @Test
    public void checkPassword_emptyPassword_doesNotMatchNonEmptyHash() {
        String hash = PasswordUtils.hashPassword("realPassword");
        assertFalse(PasswordUtils.checkPassword("", hash));
    }

    @Test
    public void hashPassword_hashLengthIs60() {
        String hash = PasswordUtils.hashPassword("testPassword");
        assertEquals("Bcrypt hash should be 60 characters", 60, hash.length());
    }
}
