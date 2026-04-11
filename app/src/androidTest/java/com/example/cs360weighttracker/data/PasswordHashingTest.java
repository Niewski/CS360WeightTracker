package com.example.cs360weighttracker.data;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class PasswordHashingTest {

    private DatabaseHelper dbHelper;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase("weight_tracker.db");
        dbHelper = new DatabaseHelper(context);
    }

    @After
    public void tearDown() {
        dbHelper.close();
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase("weight_tracker.db");
    }

    // --- Password is stored hashed, not plain text ---

    @Test
    public void createUser_passwordStoredAsHash_notPlainText() {
        dbHelper.createUser("testuser", "mySecret123", 150.0, null);

        String storedPassword = dbHelper.getStoredPasswordHash("testuser");

        assertNotNull(storedPassword);
        assertNotEquals("Password must not be stored as plain text",
                "mySecret123", storedPassword);
        assertTrue("Stored password should be a bcrypt hash",
                storedPassword.startsWith("$2a$"));
    }

    @Test
    public void createUser_differentUsers_haveDifferentHashes() {
        dbHelper.createUser("user1", "samePassword", 150.0, null);
        dbHelper.createUser("user2", "samePassword", 150.0, null);

        String hash1 = dbHelper.getStoredPasswordHash("user1");
        String hash2 = dbHelper.getStoredPasswordHash("user2");

        assertNotNull(hash1);
        assertNotNull(hash2);
        assertNotEquals("Same password should produce different hashes due to random salt",
                hash1, hash2);
    }

    // --- Login with hashed passwords ---

    @Test
    public void loginUser_correctPassword_returnsUserId() {
        dbHelper.createUser("testuser", "testpass", 150.0, null);
        int userId = dbHelper.loginUser("testuser", "testpass");
        assertTrue("Login should succeed with correct password", userId > 0);
    }

    @Test
    public void loginUser_wrongPassword_returnsNegativeOne() {
        dbHelper.createUser("testuser", "testpass", 150.0, null);
        int userId = dbHelper.loginUser("testuser", "wrongpass");
        assertEquals("Login should fail with wrong password", -1, userId);
    }

    @Test
    public void loginUser_nonexistentUser_returnsNegativeOne() {
        int userId = dbHelper.loginUser("ghost", "password");
        assertEquals("Login should fail for nonexistent user", -1, userId);
    }

    @Test
    public void loginUser_caseSensitivePassword_failsOnWrongCase() {
        dbHelper.createUser("testuser", "MyPassword", 150.0, null);
        int userId = dbHelper.loginUser("testuser", "mypassword");
        assertEquals("Password check should be case-sensitive", -1, userId);
    }

    @Test
    public void loginUser_emptyPassword_failsAgainstNonEmptyPassword() {
        dbHelper.createUser("testuser", "realPassword", 150.0, null);
        int userId = dbHelper.loginUser("testuser", "");
        assertEquals("Empty password should not match", -1, userId);
    }

    @Test
    public void loginUser_multipleUsers_eachAuthenticatesIndependently() {
        dbHelper.createUser("alice", "alicePass", 140.0, null);
        dbHelper.createUser("bob", "bobPass", 180.0, null);

        int aliceId = dbHelper.loginUser("alice", "alicePass");
        int bobId = dbHelper.loginUser("bob", "bobPass");

        assertTrue("Alice should authenticate", aliceId > 0);
        assertTrue("Bob should authenticate", bobId > 0);
        assertNotEquals("Users should have different IDs", aliceId, bobId);

        // Cross-check: Alice's password doesn't work for Bob
        assertEquals(-1, dbHelper.loginUser("bob", "alicePass"));
        assertEquals(-1, dbHelper.loginUser("alice", "bobPass"));
    }

    // --- Legacy plain-text fallback ---

    @Test
    public void loginUser_legacyPlainTextPassword_authenticatesAndUpgrades() {
        // Simulate a pre-v6 user with plain-text password
        dbHelper.insertRawUser("legacyuser", "plainpass", 150.0);

        // Login should succeed via legacy fallback
        int userId = dbHelper.loginUser("legacyuser", "plainpass");
        assertTrue("Legacy plain-text login should succeed", userId > 0);

        // Password should now be upgraded to bcrypt
        String storedPassword = dbHelper.getStoredPasswordHash("legacyuser");
        assertNotNull(storedPassword);
        assertTrue("Password should be upgraded to bcrypt hash",
                storedPassword.startsWith("$2a$"));

        // Subsequent login should work via bcrypt path
        int userId2 = dbHelper.loginUser("legacyuser", "plainpass");
        assertEquals("Second login should still work", userId, userId2);
    }

    @Test
    public void loginUser_legacyPlainTextPassword_wrongPasswordFails() {
        // Simulate a pre-v6 user with plain-text password
        dbHelper.insertRawUser("legacyuser", "plainpass", 150.0);

        int userId = dbHelper.loginUser("legacyuser", "wrongpass");
        assertEquals("Wrong password should fail for legacy user", -1, userId);
    }

    @Test
    public void loginUser_corruptedHash_returnsNegativeOne() {
        // Simulate a corrupted hash in the database
        dbHelper.insertRawUser("corruptuser", "$2a$12$corrupted_not_valid_hash", 150.0);

        int userId = dbHelper.loginUser("corruptuser", "anypass");
        assertEquals("Corrupted hash should not crash, should return -1", -1, userId);
    }
}
