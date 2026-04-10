package com.example.cs360weighttracker.data;

import android.content.Context;
import android.database.Cursor;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DatabaseHelperAdvancedTest {

    private DatabaseHelper dbHelper;
    private int userId;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase("weight_tracker.db");
        dbHelper = new DatabaseHelper(context);
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

    // --- addWeight with notes ---

    @Test
    public void addWeight_withNotes_returnsTrue() {
        assertTrue(dbHelper.addWeight(userId, "2026-01-15", 165.5, "Feeling good"));
    }

    @Test
    public void addWeight_withoutNotes_returnsTrue() {
        assertTrue(dbHelper.addWeight(userId, "2026-01-15", 165.5));
    }

    @Test
    public void addWeight_withNotes_notesRetrievable() {
        dbHelper.addWeight(userId, "2026-01-15", 165.5, "Morning weigh-in");
        Cursor cursor = dbHelper.getWeights(userId);
        assertTrue(cursor.moveToFirst());
        String notes = cursor.getString(cursor.getColumnIndexOrThrow("notes"));
        assertEquals("Morning weigh-in", notes);
        cursor.close();
    }

    @Test
    public void addWeight_withoutNotes_defaultsToEmpty() {
        dbHelper.addWeight(userId, "2026-01-15", 165.5);
        Cursor cursor = dbHelper.getWeights(userId);
        assertTrue(cursor.moveToFirst());
        String notes = cursor.getString(cursor.getColumnIndexOrThrow("notes"));
        assertEquals("", notes);
        cursor.close();
    }

    // --- updateWeightNotes ---

    @Test
    public void updateWeightNotes_existingEntry_returnsTrue() {
        dbHelper.addWeight(userId, "2026-01-15", 165.5, "");
        Cursor cursor = dbHelper.getWeights(userId);
        assertTrue(cursor.moveToFirst());
        int weightId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        cursor.close();

        assertTrue(dbHelper.updateWeightNotes(weightId, "Updated notes"));

        cursor = dbHelper.getWeights(userId);
        assertTrue(cursor.moveToFirst());
        assertEquals("Updated notes", cursor.getString(cursor.getColumnIndexOrThrow("notes")));
        cursor.close();
    }

    // --- updateWeight with notes ---

    @Test
    public void updateWeight_withNotes_updatesAllFields() {
        dbHelper.addWeight(userId, "2026-01-15", 165.5, "Original");
        Cursor cursor = dbHelper.getWeights(userId);
        assertTrue(cursor.moveToFirst());
        int weightId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        cursor.close();

        assertTrue(dbHelper.updateWeight(weightId, "2026-01-16", 164.0, "Edited"));

        cursor = dbHelper.getWeights(userId);
        assertTrue(cursor.moveToFirst());
        assertEquals("2026-01-16", cursor.getString(cursor.getColumnIndexOrThrow("date")));
        assertEquals(164.0, cursor.getDouble(cursor.getColumnIndexOrThrow("weight")), 0.01);
        assertEquals("Edited", cursor.getString(cursor.getColumnIndexOrThrow("notes")));
        cursor.close();
    }

    @Test
    public void updateWeight_withoutNotes_preservesExistingNotes() {
        dbHelper.addWeight(userId, "2026-01-15", 165.5, "Keep me");
        Cursor cursor = dbHelper.getWeights(userId);
        assertTrue(cursor.moveToFirst());
        int weightId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        cursor.close();

        // Use the 3-param updateWeight (no notes parameter)
        assertTrue(dbHelper.updateWeight(weightId, "2026-01-16", 164.0));

        cursor = dbHelper.getWeights(userId);
        assertTrue(cursor.moveToFirst());
        assertEquals("Keep me", cursor.getString(cursor.getColumnIndexOrThrow("notes")));
        cursor.close();
    }

    // --- getWeightsInRange ---

    @Test
    public void getWeightsInRange_validRange_returnsFilteredEntries() {
        dbHelper.addWeight(userId, "2026-01-01", 170.0);
        dbHelper.addWeight(userId, "2026-01-15", 165.0);
        dbHelper.addWeight(userId, "2026-02-01", 160.0);

        Cursor cursor = dbHelper.getWeightsInRange(userId, "2026-01-01", "2026-01-31");
        assertEquals(2, cursor.getCount());
        cursor.close();
    }

    @Test
    public void getWeightsInRange_noMatches_returnsEmpty() {
        dbHelper.addWeight(userId, "2026-01-15", 165.0);

        Cursor cursor = dbHelper.getWeightsInRange(userId, "2026-03-01", "2026-03-31");
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    @Test
    public void getWeightsInRange_boundaryInclusive_includesEdgeDates() {
        dbHelper.addWeight(userId, "2026-01-01", 170.0);
        dbHelper.addWeight(userId, "2026-01-31", 165.0);

        Cursor cursor = dbHelper.getWeightsInRange(userId, "2026-01-01", "2026-01-31");
        assertEquals(2, cursor.getCount());
        cursor.close();
    }

    // --- FTS5 Search ---

    @Test
    public void searchWeightNotes_matchingQuery_returnsResults() {
        dbHelper.addWeight(userId, "2026-01-15", 165.5, "Morning weigh-in");
        dbHelper.addWeight(userId, "2026-01-16", 164.0, "After workout");

        Cursor cursor = dbHelper.searchWeightNotes(userId, "morning");
        assertEquals(1, cursor.getCount());
        cursor.close();
    }

    @Test
    public void searchWeightNotes_noMatch_returnsEmpty() {
        dbHelper.addWeight(userId, "2026-01-15", 165.5, "Morning weigh-in");

        Cursor cursor = dbHelper.searchWeightNotes(userId, "evening");
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    @Test
    public void searchWeightNotes_emptyNotes_notMatched() {
        dbHelper.addWeight(userId, "2026-01-15", 165.5, "");
        dbHelper.addWeight(userId, "2026-01-16", 164.0, "Has notes");

        Cursor cursor = dbHelper.searchWeightNotes(userId, "notes");
        assertEquals(1, cursor.getCount());
        cursor.close();
    }

    @Test
    public void fts5Sync_afterDelete_removedFromSearch() {
        dbHelper.addWeight(userId, "2026-01-15", 165.5, "Delete me test");
        Cursor cursor = dbHelper.getWeights(userId);
        assertTrue(cursor.moveToFirst());
        int weightId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        cursor.close();

        dbHelper.deleteWeight(weightId);

        cursor = dbHelper.searchWeightNotes(userId, "Delete me test");
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    @Test
    public void fts5Sync_afterUpdate_searchFindsNewText() {
        dbHelper.addWeight(userId, "2026-01-15", 165.5, "Original note");
        Cursor cursor = dbHelper.getWeights(userId);
        assertTrue(cursor.moveToFirst());
        int weightId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        cursor.close();

        dbHelper.updateWeightNotes(weightId, "Completely different text");

        cursor = dbHelper.searchWeightNotes(userId, "different");
        assertEquals(1, cursor.getCount());
        cursor.close();

        // Old text no longer matches
        cursor = dbHelper.searchWeightNotes(userId, "Original");
        assertEquals(0, cursor.getCount());
        cursor.close();
    }

    // --- Weekly Averages ---

    @Test
    public void getWeeklyAverages_multipleWeeks_returnsGrouped() {
        dbHelper.addWeight(userId, "2026-01-06", 170.0);
        dbHelper.addWeight(userId, "2026-01-07", 168.0);
        dbHelper.addWeight(userId, "2026-01-13", 165.0);

        Cursor cursor = dbHelper.getWeeklyAverages(userId);
        assertTrue(cursor.getCount() >= 2);
        cursor.close();
    }

    @Test
    public void getWeeklyAverages_sameWeek_averagesCorrectly() {
        dbHelper.addWeight(userId, "2026-01-06", 170.0);
        dbHelper.addWeight(userId, "2026-01-07", 168.0);

        Cursor cursor = dbHelper.getWeeklyAverages(userId);
        assertTrue(cursor.moveToFirst());
        double avg = cursor.getDouble(cursor.getColumnIndexOrThrow("avg_weight"));
        assertEquals(169.0, avg, 0.01);
        cursor.close();
    }

    // --- Monthly Averages ---

    @Test
    public void getMonthlyAverages_multipleMonths_returnsGrouped() {
        dbHelper.addWeight(userId, "2026-01-15", 170.0);
        dbHelper.addWeight(userId, "2026-01-20", 168.0);
        dbHelper.addWeight(userId, "2026-02-10", 165.0);

        Cursor cursor = dbHelper.getMonthlyAverages(userId);
        assertEquals(2, cursor.getCount());
        cursor.close();
    }

    @Test
    public void getMonthlyAverages_sameMonth_averagesCorrectly() {
        dbHelper.addWeight(userId, "2026-01-15", 170.0);
        dbHelper.addWeight(userId, "2026-01-20", 166.0);

        Cursor cursor = dbHelper.getMonthlyAverages(userId);
        assertTrue(cursor.moveToFirst());
        double avg = cursor.getDouble(cursor.getColumnIndexOrThrow("avg_weight"));
        assertEquals(168.0, avg, 0.01);
        cursor.close();
    }

    @Test
    public void getMonthlyAverages_noEntries_returnsEmpty() {
        Cursor cursor = dbHelper.getMonthlyAverages(userId);
        assertEquals(0, cursor.getCount());
        cursor.close();
    }
}
