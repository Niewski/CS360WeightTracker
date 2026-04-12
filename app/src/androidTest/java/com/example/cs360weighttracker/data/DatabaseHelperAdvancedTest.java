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
        List<WeightEntry> weights = dbHelper.getWeights(userId);
        assertFalse(weights.isEmpty());
        assertEquals("Morning weigh-in", weights.get(0).notes);
    }

    @Test
    public void addWeight_withoutNotes_defaultsToEmpty() {
        dbHelper.addWeight(userId, "2026-01-15", 165.5);
        List<WeightEntry> weights = dbHelper.getWeights(userId);
        assertFalse(weights.isEmpty());
        assertEquals("", weights.get(0).notes);
    }

    // --- updateWeightNotes ---

    @Test
    public void updateWeightNotes_existingEntry_returnsTrue() {
        dbHelper.addWeight(userId, "2026-01-15", 165.5, "");
        List<WeightEntry> weights = dbHelper.getWeights(userId);
        assertFalse(weights.isEmpty());
        int weightId = weights.get(0).id;

        assertTrue(dbHelper.updateWeightNotes(weightId, "Updated notes"));

        weights = dbHelper.getWeights(userId);
        assertFalse(weights.isEmpty());
        assertEquals("Updated notes", weights.get(0).notes);
    }

    // --- updateWeight with notes ---

    @Test
    public void updateWeight_withNotes_updatesAllFields() {
        dbHelper.addWeight(userId, "2026-01-15", 165.5, "Original");
        List<WeightEntry> weights = dbHelper.getWeights(userId);
        assertFalse(weights.isEmpty());
        int weightId = weights.get(0).id;

        assertTrue(dbHelper.updateWeight(weightId, "2026-01-16", 164.0, "Edited"));

        weights = dbHelper.getWeights(userId);
        assertFalse(weights.isEmpty());
        assertEquals("2026-01-16", weights.get(0).date);
        assertEquals(164.0, weights.get(0).weight, 0.01);
        assertEquals("Edited", weights.get(0).notes);
    }

    @Test
    public void updateWeight_withoutNotes_preservesExistingNotes() {
        dbHelper.addWeight(userId, "2026-01-15", 165.5, "Keep me");
        List<WeightEntry> weights = dbHelper.getWeights(userId);
        assertFalse(weights.isEmpty());
        int weightId = weights.get(0).id;

        // Use the 3-param updateWeight (no notes parameter)
        assertTrue(dbHelper.updateWeight(weightId, "2026-01-16", 164.0));

        weights = dbHelper.getWeights(userId);
        assertFalse(weights.isEmpty());
        assertEquals("Keep me", weights.get(0).notes);
    }

    // --- getWeightsInRange ---

    @Test
    public void getWeightsInRange_validRange_returnsFilteredEntries() {
        dbHelper.addWeight(userId, "2026-01-01", 170.0);
        dbHelper.addWeight(userId, "2026-01-15", 165.0);
        dbHelper.addWeight(userId, "2026-02-01", 160.0);

        List<WeightEntry> weights = dbHelper.getWeightsInRange(userId, "2026-01-01", "2026-01-31");
        assertEquals(2, weights.size());
    }

    @Test
    public void getWeightsInRange_noMatches_returnsEmpty() {
        dbHelper.addWeight(userId, "2026-01-15", 165.0);

        List<WeightEntry> weights = dbHelper.getWeightsInRange(userId, "2026-03-01", "2026-03-31");
        assertEquals(0, weights.size());
    }

    @Test
    public void getWeightsInRange_boundaryInclusive_includesEdgeDates() {
        dbHelper.addWeight(userId, "2026-01-01", 170.0);
        dbHelper.addWeight(userId, "2026-01-31", 165.0);

        List<WeightEntry> weights = dbHelper.getWeightsInRange(userId, "2026-01-01", "2026-01-31");
        assertEquals(2, weights.size());
    }

    // --- FTS5 Search ---

    @Test
    public void searchWeightNotes_matchingQuery_returnsResults() {
        dbHelper.addWeight(userId, "2026-01-15", 165.5, "Morning weigh-in");
        dbHelper.addWeight(userId, "2026-01-16", 164.0, "After workout");

        List<WeightEntry> results = dbHelper.searchWeightNotes(userId, "morning");
        assertEquals(1, results.size());
    }

    @Test
    public void searchWeightNotes_noMatch_returnsEmpty() {
        dbHelper.addWeight(userId, "2026-01-15", 165.5, "Morning weigh-in");

        List<WeightEntry> results = dbHelper.searchWeightNotes(userId, "evening");
        assertEquals(0, results.size());
    }

    @Test
    public void searchWeightNotes_emptyNotes_notMatched() {
        dbHelper.addWeight(userId, "2026-01-15", 165.5, "");
        dbHelper.addWeight(userId, "2026-01-16", 164.0, "Has notes");

        List<WeightEntry> results = dbHelper.searchWeightNotes(userId, "notes");
        assertEquals(1, results.size());
    }

    @Test
    public void fts5Sync_afterDelete_removedFromSearch() {
        dbHelper.addWeight(userId, "2026-01-15", 165.5, "Delete me test");
        List<WeightEntry> weights = dbHelper.getWeights(userId);
        assertFalse(weights.isEmpty());
        int weightId = weights.get(0).id;

        dbHelper.deleteWeight(weightId);

        List<WeightEntry> results = dbHelper.searchWeightNotes(userId, "Delete me test");
        assertEquals(0, results.size());
    }

    @Test
    public void fts5Sync_afterUpdate_searchFindsNewText() {
        dbHelper.addWeight(userId, "2026-01-15", 165.5, "Original note");
        List<WeightEntry> weights = dbHelper.getWeights(userId);
        assertFalse(weights.isEmpty());
        int weightId = weights.get(0).id;

        dbHelper.updateWeightNotes(weightId, "Completely different text");

        List<WeightEntry> results = dbHelper.searchWeightNotes(userId, "different");
        assertEquals(1, results.size());

        // Old text no longer matches
        results = dbHelper.searchWeightNotes(userId, "Original");
        assertEquals(0, results.size());
    }

    // --- Weekly Averages ---

    @Test
    public void getWeeklyAverages_multipleWeeks_returnsGrouped() {
        dbHelper.addWeight(userId, "2026-01-06", 170.0);
        dbHelper.addWeight(userId, "2026-01-07", 168.0);
        dbHelper.addWeight(userId, "2026-01-13", 165.0);

        List<TimePeriodAverage> averages = dbHelper.getWeeklyAverages(userId);
        assertTrue(averages.size() >= 2);
    }

    @Test
    public void getWeeklyAverages_sameWeek_averagesCorrectly() {
        dbHelper.addWeight(userId, "2026-01-06", 170.0);
        dbHelper.addWeight(userId, "2026-01-07", 168.0);

        List<TimePeriodAverage> averages = dbHelper.getWeeklyAverages(userId);
        assertFalse(averages.isEmpty());
        assertEquals(169.0, averages.get(0).average, 0.01);
    }

    // --- Monthly Averages ---

    @Test
    public void getMonthlyAverages_multipleMonths_returnsGrouped() {
        dbHelper.addWeight(userId, "2026-01-15", 170.0);
        dbHelper.addWeight(userId, "2026-01-20", 168.0);
        dbHelper.addWeight(userId, "2026-02-10", 165.0);

        List<TimePeriodAverage> averages = dbHelper.getMonthlyAverages(userId);
        assertEquals(2, averages.size());
    }

    @Test
    public void getMonthlyAverages_sameMonth_averagesCorrectly() {
        dbHelper.addWeight(userId, "2026-01-15", 170.0);
        dbHelper.addWeight(userId, "2026-01-20", 166.0);

        List<TimePeriodAverage> averages = dbHelper.getMonthlyAverages(userId);
        assertFalse(averages.isEmpty());
        assertEquals(168.0, averages.get(0).average, 0.01);
    }

    @Test
    public void getMonthlyAverages_noEntries_returnsEmpty() {
        List<TimePeriodAverage> averages = dbHelper.getMonthlyAverages(userId);
        assertEquals(0, averages.size());
    }
}
