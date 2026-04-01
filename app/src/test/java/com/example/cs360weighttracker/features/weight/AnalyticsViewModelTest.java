package com.example.cs360weighttracker.features.weight;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.example.cs360weighttracker.data.WeightEntry;
import com.example.cs360weighttracker.data.WeightRepository;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AnalyticsViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private AnalyticsViewModel viewModel;
    private FakeWeightRepository fakeRepo;
    private static final int TEST_USER_ID = 1;
    private static final double TEST_GOAL_WEIGHT = 150.0;

    @Before
    public void setUp() {
        viewModel = new AnalyticsViewModel();
        fakeRepo = new FakeWeightRepository();
    }

    // --- init: happy path ---

    @Test
    public void init_validArgs_loadsAnalytics() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 165.5));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertTrue(result.hasData);
    }

    @Test
    public void init_calledTwice_doesNotReinit() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 165.5));
        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        FakeWeightRepository secondRepo = new FakeWeightRepository();
        secondRepo.weightEntries.add(new WeightEntry(99, "2026-06-01", 200.0));
        viewModel.init(secondRepo, 99, 180.0);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertEquals(165.5, result.currentWeight, 0.01);
    }

    // --- init: no data ---

    @Test
    public void init_noEntries_hasDataFalse() {
        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertFalse(result.hasData);
    }

    @Test
    public void init_noEntries_goalWeightStillSet() {
        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertEquals(TEST_GOAL_WEIGHT, result.goalWeight, 0.01);
    }

    // --- currentWeight ---

    @Test
    public void loadAnalytics_singleEntry_currentWeightIsEntry() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 165.5));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertEquals(165.5, result.currentWeight, 0.01);
    }

    @Test
    public void loadAnalytics_multipleEntries_currentWeightIsLast() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 170.0));
        fakeRepo.weightEntries.add(new WeightEntry(2, "2026-01-16", 168.0));
        fakeRepo.weightEntries.add(new WeightEntry(3, "2026-01-17", 163.2));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertEquals(163.2, result.currentWeight, 0.01);
    }

    // --- totalChange ---

    @Test
    public void loadAnalytics_weightLost_totalChangeNegative() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 170.0));
        fakeRepo.weightEntries.add(new WeightEntry(2, "2026-01-16", 165.0));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertEquals(-5.0, result.totalChange, 0.01);
    }

    @Test
    public void loadAnalytics_weightGained_totalChangePositive() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 160.0));
        fakeRepo.weightEntries.add(new WeightEntry(2, "2026-01-16", 165.0));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertEquals(5.0, result.totalChange, 0.01);
    }

    @Test
    public void loadAnalytics_singleEntry_totalChangeZero() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 165.5));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertEquals(0.0, result.totalChange, 0.01);
    }

    // --- min / max ---

    @Test
    public void loadAnalytics_multipleEntries_findsCorrectMin() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 170.0));
        fakeRepo.weightEntries.add(new WeightEntry(2, "2026-01-16", 160.0));
        fakeRepo.weightEntries.add(new WeightEntry(3, "2026-01-17", 165.0));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertNotNull(result.minEntry);
        assertEquals(160.0, result.minEntry.weight, 0.01);
        assertEquals("2026-01-16", result.minEntry.date);
    }

    @Test
    public void loadAnalytics_multipleEntries_findsCorrectMax() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 170.0));
        fakeRepo.weightEntries.add(new WeightEntry(2, "2026-01-16", 160.0));
        fakeRepo.weightEntries.add(new WeightEntry(3, "2026-01-17", 165.0));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertNotNull(result.maxEntry);
        assertEquals(170.0, result.maxEntry.weight, 0.01);
        assertEquals("2026-01-15", result.maxEntry.date);
    }

    // --- average ---

    @Test
    public void loadAnalytics_multipleEntries_calculatesAverage() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 170.0));
        fakeRepo.weightEntries.add(new WeightEntry(2, "2026-01-16", 160.0));
        fakeRepo.weightEntries.add(new WeightEntry(3, "2026-01-17", 165.0));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertEquals(165.0, result.average, 0.01);
    }

    // --- streak ---

    @Test
    public void loadAnalytics_consecutiveDays_calculatesStreak() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 170.0));
        fakeRepo.weightEntries.add(new WeightEntry(2, "2026-01-16", 169.0));
        fakeRepo.weightEntries.add(new WeightEntry(3, "2026-01-17", 168.0));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertEquals(3, result.streak);
    }

    @Test
    public void loadAnalytics_gapInDays_streakResets() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-10", 170.0));
        fakeRepo.weightEntries.add(new WeightEntry(2, "2026-01-15", 169.0));
        fakeRepo.weightEntries.add(new WeightEntry(3, "2026-01-16", 168.0));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertEquals(2, result.streak);
    }

    @Test
    public void loadAnalytics_singleEntry_streakIsOne() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 165.5));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertEquals(1, result.streak);
    }

    // --- longestStreak ---

    @Test
    public void loadAnalytics_allConsecutive_longestStreakEqualsCurrentStreak() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 170.0));
        fakeRepo.weightEntries.add(new WeightEntry(2, "2026-01-16", 169.0));
        fakeRepo.weightEntries.add(new WeightEntry(3, "2026-01-17", 168.0));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertEquals(3, result.longestStreak);
        assertEquals(result.streak, result.longestStreak);
    }

    @Test
    public void loadAnalytics_longestStreakInPast_longestStreakGreaterThanCurrent() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-01", 172.0));
        fakeRepo.weightEntries.add(new WeightEntry(2, "2026-01-02", 171.0));
        fakeRepo.weightEntries.add(new WeightEntry(3, "2026-01-03", 170.0));
        fakeRepo.weightEntries.add(new WeightEntry(4, "2026-01-03", 170.0));
        fakeRepo.weightEntries.add(new WeightEntry(5, "2026-01-10", 169.0));
        fakeRepo.weightEntries.add(new WeightEntry(6, "2026-01-11", 168.0));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertEquals(2, result.streak);         // current: Jan 10-11
        assertEquals(3, result.longestStreak);  // longest: Jan 1-2-3
    }

    @Test
    public void loadAnalytics_singleEntry_longestStreakIsOne() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 165.5));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertEquals(1, result.longestStreak);
    }

    // --- goalReached ---

    @Test
    public void loadAnalytics_currentBelowGoal_goalReachedTrue() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 170.0));
        fakeRepo.weightEntries.add(new WeightEntry(2, "2026-01-16", 148.0));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertTrue(result.goalReached);
    }

    @Test
    public void loadAnalytics_currentEqualsGoal_goalReachedTrue() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 170.0));
        fakeRepo.weightEntries.add(new WeightEntry(2, "2026-01-16", 150.0));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertTrue(result.goalReached);
    }

    @Test
    public void loadAnalytics_currentAboveGoal_goalReachedFalse() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 165.5));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertFalse(result.goalReached);
    }

    // --- projectedGoalDate ---

    @Test
    public void loadAnalytics_losingWeight_projectsGoalDate() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-01", 170.0));
        fakeRepo.weightEntries.add(new WeightEntry(2, "2026-01-08", 169.0));
        fakeRepo.weightEntries.add(new WeightEntry(3, "2026-01-15", 168.0));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertNotNull(result.projectedGoalDate);
    }

    @Test
    public void loadAnalytics_goalAlreadyReached_projectedGoalDateNull() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 160.0));
        fakeRepo.weightEntries.add(new WeightEntry(2, "2026-01-16", 148.0));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertNull(result.projectedGoalDate);
    }

    @Test
    public void loadAnalytics_gainingWeight_projectedGoalDateNull() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 160.0));
        fakeRepo.weightEntries.add(new WeightEntry(2, "2026-01-16", 165.0));
        fakeRepo.weightEntries.add(new WeightEntry(3, "2026-01-17", 170.0));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertNull(result.projectedGoalDate);
    }

    // --- movingAverages ---

    @Test
    public void loadAnalytics_fewerThanWindowEntries_movingAveragesEmpty() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 170.0));
        fakeRepo.weightEntries.add(new WeightEntry(2, "2026-01-16", 169.0));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertTrue(result.movingAverages.isEmpty());
    }

    @Test
    public void loadAnalytics_exactlySevenEntries_movingAveragesHasOne() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-01", 170.0));
        fakeRepo.weightEntries.add(new WeightEntry(2, "2026-01-02", 169.0));
        fakeRepo.weightEntries.add(new WeightEntry(3, "2026-01-03", 168.0));
        fakeRepo.weightEntries.add(new WeightEntry(4, "2026-01-04", 167.0));
        fakeRepo.weightEntries.add(new WeightEntry(5, "2026-01-05", 166.0));
        fakeRepo.weightEntries.add(new WeightEntry(6, "2026-01-06", 165.0));
        fakeRepo.weightEntries.add(new WeightEntry(7, "2026-01-07", 164.0));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertEquals(1, result.movingAverages.size());
        assertEquals(167.0, result.movingAverages.get(0), 0.01);
    }

    // --- loadAnalytics: reload ---

    @Test
    public void loadAnalytics_afterNewEntry_updatesResult() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 165.5));
        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        assertEquals(165.5, viewModel.getAnalyticsResult().getValue().currentWeight, 0.01);

        fakeRepo.weightEntries.add(new WeightEntry(2, "2026-01-16", 164.0));
        viewModel.loadAnalytics();

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertEquals(164.0, result.currentWeight, 0.01);
    }

    @Test
    public void loadAnalytics_beforeInit_doesNotCrash() {
        viewModel.loadAnalytics();

        // Should not have set any result
        assertNull(viewModel.getAnalyticsResult().getValue());
    }

    // --- entries list ---

    @Test
    public void loadAnalytics_entriesPassedToResult() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 170.0));
        fakeRepo.weightEntries.add(new WeightEntry(2, "2026-01-16", 168.0));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertEquals(2, result.entries.size());
    }

    // --- rateOfChange ---

    @Test
    public void loadAnalytics_singleEntry_rateOfChangeZero() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-15", 165.5));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertEquals(0.0, result.rateOfChange, 0.01);
    }

    @Test
    public void loadAnalytics_losingWeight_rateOfChangeNegative() {
        fakeRepo.weightEntries.add(new WeightEntry(1, "2026-01-01", 170.0));
        fakeRepo.weightEntries.add(new WeightEntry(2, "2026-01-08", 169.0));

        viewModel.init(fakeRepo, TEST_USER_ID, TEST_GOAL_WEIGHT);

        AnalyticsViewModel.AnalyticsResult result = viewModel.getAnalyticsResult().getValue();
        assertNotNull(result);
        assertTrue(result.rateOfChange < 0);
    }

    // --- Fake repository ---

    private static class FakeWeightRepository extends WeightRepository {
        final List<WeightEntry> weightEntries = new ArrayList<>();

        FakeWeightRepository() {
            super(null);
        }

        @Override
        public List<WeightEntry> getWeightsAscending(int userId) {
            List<WeightEntry> sorted = new ArrayList<>(weightEntries);
            Collections.sort(sorted);
            return sorted;
        }
    }
}
