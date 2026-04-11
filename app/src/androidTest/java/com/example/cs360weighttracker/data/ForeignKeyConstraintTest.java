package com.example.cs360weighttracker.data;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ForeignKeyConstraintTest {

    private TestDatabaseHelper dbHelper;
    private int userId;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase("weight_tracker.db");
        dbHelper = new TestDatabaseHelper(context);
        dbHelper.createUser("testuser", "testpass", 150.0, null);
        userId = dbHelper.loginUser("testuser", "testpass");
        assertTrue("Test user should be created", userId > 0);
    }

    @After
    public void tearDown() {
        dbHelper.close();
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase("weight_tracker.db");
    }

    @Test
    public void addWeight_nonExistentUser_returnsFalse() {
        int fakeUserId = 99999;
        assertFalse(dbHelper.addWeight(fakeUserId, "2026-01-15", 165.5));
    }

    @Test
    public void deleteUser_cascadesWeightDeletion() {
        // Add weights for the user
        assertTrue(dbHelper.addWeight(userId, "2026-01-15", 165.5));
        assertTrue(dbHelper.addWeight(userId, "2026-01-16", 164.0));

        List<WeightEntry> before = dbHelper.getWeights(userId);
        assertEquals(2, before.size());

        // Delete the user — weights should cascade-delete
        dbHelper.testDeleteUserById(userId);

        List<WeightEntry> after = dbHelper.getWeights(userId);
        assertEquals(0, after.size());
    }

    @Test
    public void deleteUser_doesNotAffectOtherUsersWeights() {
        // Create a second user
        dbHelper.createUser("otheruser", "otherpass", 140.0, null);
        int otherUserId = dbHelper.loginUser("otheruser", "otherpass");
        assertTrue(otherUserId > 0);

        // Add weights for both users
        assertTrue(dbHelper.addWeight(userId, "2026-01-15", 165.5));
        assertTrue(dbHelper.addWeight(otherUserId, "2026-01-15", 130.0));

        // Delete the first user
        dbHelper.testDeleteUserById(userId);

        // Second user's weights are untouched
        List<WeightEntry> otherWeights = dbHelper.getWeights(otherUserId);
        assertEquals(1, otherWeights.size());
        assertEquals(130.0, otherWeights.get(0).weight, 0.01);
    }

    @Test
    public void addWeight_validUser_succeeds() {
        assertTrue(dbHelper.addWeight(userId, "2026-01-15", 165.5));
        List<WeightEntry> weights = dbHelper.getWeights(userId);
        assertEquals(1, weights.size());
    }
}
