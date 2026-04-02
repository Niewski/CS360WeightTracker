package com.example.cs360weighttracker.data;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WeightAnalyticsTest {

    // --- calculateMovingAverage ---

    @Test
    public void calculateMovingAverage_exactWindowSize_returnsSingleValue() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "2026-01-01", 170.0),
                new WeightEntry(2, "2026-01-02", 169.0),
                new WeightEntry(3, "2026-01-03", 168.0)
        );

        List<Double> result = WeightAnalytics.calculateMovingAverage(entries, 3);

        assertEquals(1, result.size());
        assertEquals(169.0, result.get(0), 0.01);
    }

    @Test
    public void calculateMovingAverage_moreThanWindowSize_returnsMultipleValues() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "2026-01-01", 170.0),
                new WeightEntry(2, "2026-01-02", 168.0),
                new WeightEntry(3, "2026-01-03", 166.0),
                new WeightEntry(4, "2026-01-04", 164.0),
                new WeightEntry(5, "2026-01-05", 162.0)
        );

        List<Double> result = WeightAnalytics.calculateMovingAverage(entries, 3);

        assertEquals(3, result.size());
        assertEquals(168.0, result.get(0), 0.01); // (170+168+166)/3
        assertEquals(166.0, result.get(1), 0.01); // (168+166+164)/3
        assertEquals(164.0, result.get(2), 0.01); // (166+164+162)/3
    }

    @Test
    public void calculateMovingAverage_fewerEntriesThanWindow_returnsEmptyList() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "2026-01-01", 170.0),
                new WeightEntry(2, "2026-01-02", 169.0)
        );

        List<Double> result = WeightAnalytics.calculateMovingAverage(entries, 7);

        assertTrue(result.isEmpty());
    }

    @Test
    public void calculateMovingAverage_nullEntries_returnsEmptyList() {
        List<Double> result = WeightAnalytics.calculateMovingAverage(null, 7);
        assertTrue(result.isEmpty());
    }

    @Test
    public void calculateMovingAverage_zeroWindowSize_returnsEmptyList() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "2026-01-01", 170.0)
        );

        List<Double> result = WeightAnalytics.calculateMovingAverage(entries, 0);

        assertTrue(result.isEmpty());
    }

    // --- calculateRateOfChange ---

    @Test
    public void calculateRateOfChange_decreasingWeight_returnsNegative() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "2026-01-01", 170.0),
                new WeightEntry(2, "2026-01-08", 168.0)
        );

        double rate = WeightAnalytics.calculateRateOfChange(entries);

        // Lost 2 lbs in 7 days = -2.0 lbs/week
        assertEquals(-2.0, rate, 0.01);
    }

    @Test
    public void calculateRateOfChange_increasingWeight_returnsPositive() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "2026-01-01", 160.0),
                new WeightEntry(2, "2026-01-08", 162.0)
        );

        double rate = WeightAnalytics.calculateRateOfChange(entries);

        assertEquals(2.0, rate, 0.01);
    }

    @Test
    public void calculateRateOfChange_flatWeight_returnsZero() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "2026-01-01", 165.0),
                new WeightEntry(2, "2026-01-08", 165.0)
        );

        double rate = WeightAnalytics.calculateRateOfChange(entries);

        assertEquals(0.0, rate, 0.01);
    }

    @Test
    public void calculateRateOfChange_singleEntry_returnsZero() {
        List<WeightEntry> entries = Collections.singletonList(
                new WeightEntry(1, "2026-01-01", 170.0)
        );

        double rate = WeightAnalytics.calculateRateOfChange(entries);

        assertEquals(0.0, rate, 0.01);
    }

    @Test
    public void calculateRateOfChange_nullEntries_returnsZero() {
        assertEquals(0.0, WeightAnalytics.calculateRateOfChange(null), 0.01);
    }

    @Test
    public void calculateRateOfChange_malformedStartDate_returnsZero() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "bad-date", 170.0),
                new WeightEntry(2, "2026-01-08", 168.0)
        );

        assertEquals(0.0, WeightAnalytics.calculateRateOfChange(entries), 0.01);
    }

    @Test
    public void calculateRateOfChange_malformedLaterDate_returnsZero() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "2026-01-01", 170.0),
                new WeightEntry(2, "bad-date", 168.0)
        );

        assertEquals(0.0, WeightAnalytics.calculateRateOfChange(entries), 0.01);
    }

    // --- findMin / findMax ---

    @Test
    public void findMin_multipleEntries_returnsLowest() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "2026-01-01", 170.0),
                new WeightEntry(2, "2026-01-02", 155.0),
                new WeightEntry(3, "2026-01-03", 165.0)
        );

        WeightEntry min = WeightAnalytics.findMin(entries);

        assertNotNull(min);
        assertEquals(155.0, min.weight, 0.01);
        assertEquals("2026-01-02", min.date);
    }

    @Test
    public void findMax_multipleEntries_returnsHighest() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "2026-01-01", 170.0),
                new WeightEntry(2, "2026-01-02", 155.0),
                new WeightEntry(3, "2026-01-03", 165.0)
        );

        WeightEntry max = WeightAnalytics.findMax(entries);

        assertNotNull(max);
        assertEquals(170.0, max.weight, 0.01);
        assertEquals("2026-01-01", max.date);
    }

    @Test
    public void findMin_emptyList_returnsNull() {
        assertNull(WeightAnalytics.findMin(new ArrayList<>()));
    }

    @Test
    public void findMax_nullList_returnsNull() {
        assertNull(WeightAnalytics.findMax(null));
    }

    @Test
    public void findMin_singleEntry_returnsThatEntry() {
        List<WeightEntry> entries = Collections.singletonList(
                new WeightEntry(1, "2026-01-01", 170.0)
        );

        WeightEntry min = WeightAnalytics.findMin(entries);
        assertNotNull(min);
        assertEquals(170.0, min.weight, 0.01);
    }

    // --- calculateAverage ---

    @Test
    public void calculateAverage_multipleEntries_returnsCorrectMean() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "2026-01-01", 160.0),
                new WeightEntry(2, "2026-01-02", 170.0),
                new WeightEntry(3, "2026-01-03", 180.0)
        );

        double avg = WeightAnalytics.calculateAverage(entries);

        assertEquals(170.0, avg, 0.01);
    }

    @Test
    public void calculateAverage_emptyList_returnsZero() {
        assertEquals(0.0, WeightAnalytics.calculateAverage(new ArrayList<>()), 0.01);
    }

    @Test
    public void calculateAverage_nullList_returnsZero() {
        assertEquals(0.0, WeightAnalytics.calculateAverage(null), 0.01);
    }

    // --- projectGoalDate ---

    @Test
    public void projectGoalDate_losingWeight_returnsDate() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "2026-01-01", 170.0),
                new WeightEntry(2, "2026-01-08", 168.0),
                new WeightEntry(3, "2026-01-15", 166.0)
        );

        String projected = WeightAnalytics.projectGoalDate(entries, 160.0);

        assertNotNull(projected);
        // Losing ~2 lbs/week, 6 lbs to go => ~3 weeks => around 2026-02-05
        assertEquals("2026-02-05", projected);
    }

    @Test
    public void projectGoalDate_alreadyAtGoal_returnsNull() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "2026-01-01", 170.0),
                new WeightEntry(2, "2026-01-08", 160.0)
        );

        String projected = WeightAnalytics.projectGoalDate(entries, 160.0);

        assertNull(projected);
    }

    @Test
    public void projectGoalDate_gainingWeight_returnsNull() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "2026-01-01", 165.0),
                new WeightEntry(2, "2026-01-08", 168.0)
        );

        String projected = WeightAnalytics.projectGoalDate(entries, 160.0);

        assertNull(projected);
    }

    @Test
    public void projectGoalDate_singleEntry_returnsNull() {
        List<WeightEntry> entries = Collections.singletonList(
                new WeightEntry(1, "2026-01-01", 170.0)
        );

        assertNull(WeightAnalytics.projectGoalDate(entries, 160.0));
    }

    @Test
    public void projectGoalDate_nullEntries_returnsNull() {
        assertNull(WeightAnalytics.projectGoalDate(null, 160.0));
    }

    @Test
    public void projectGoalDate_malformedDate_returnsNull() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "2026-01-01", 170.0),
                new WeightEntry(2, "bad-date", 165.0)
        );

        assertNull(WeightAnalytics.projectGoalDate(entries, 160.0));
    }

    @Test
    public void projectGoalDate_nearZeroSlope_returnsNull() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "2026-01-01", 170.0),
                new WeightEntry(2, "2026-01-08", 169.95)
        );

        // Rate is ~-0.05 lbs/week = ~-0.007 lbs/day, below 0.01 threshold
        assertNull(WeightAnalytics.projectGoalDate(entries, 160.0));
    }

    // --- calculateStreak ---

    @Test
    public void calculateStreak_consecutiveDays_returnsCorrectCount() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "2026-01-13", 170.0),
                new WeightEntry(2, "2026-01-14", 169.0),
                new WeightEntry(3, "2026-01-15", 168.0)
        );

        assertEquals(3, WeightAnalytics.calculateStreak(entries));
    }

    @Test
    public void calculateStreak_gapInMiddle_countsFromEnd() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "2026-01-10", 172.0),
                new WeightEntry(2, "2026-01-11", 171.0),
                new WeightEntry(3, "2026-01-14", 169.0),
                new WeightEntry(4, "2026-01-15", 168.0)
        );

        assertEquals(2, WeightAnalytics.calculateStreak(entries));
    }

    @Test
    public void calculateStreak_singleEntry_returnsOne() {
        List<WeightEntry> entries = Collections.singletonList(
                new WeightEntry(1, "2026-01-15", 170.0)
        );

        assertEquals(1, WeightAnalytics.calculateStreak(entries));
    }

    @Test
    public void calculateStreak_emptyList_returnsZero() {
        assertEquals(0, WeightAnalytics.calculateStreak(new ArrayList<>()));
    }

    @Test
    public void calculateStreak_nullList_returnsZero() {
        assertEquals(0, WeightAnalytics.calculateStreak(null));
    }

    // --- calculateLongestStreak ---

    @Test
    public void calculateLongestStreak_allConsecutive_returnsTotal() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "2026-01-13", 170.0),
                new WeightEntry(2, "2026-01-14", 169.0),
                new WeightEntry(3, "2026-01-15", 168.0)
        );

        assertEquals(3, WeightAnalytics.calculateLongestStreak(entries));
    }

    @Test
    public void calculateLongestStreak_longestInMiddle_returnsLongest() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "2026-01-01", 172.0),
                new WeightEntry(2, "2026-01-05", 171.0),
                new WeightEntry(3, "2026-01-06", 170.0),
                new WeightEntry(4, "2026-01-07", 169.0),
                new WeightEntry(5, "2026-01-10", 168.0),
                new WeightEntry(6, "2026-01-11", 167.0)
        );

        // Longest run is Jan 5-6-7 = 3 days
        assertEquals(3, WeightAnalytics.calculateLongestStreak(entries));
    }

    @Test
    public void calculateLongestStreak_longestAtEnd_returnsLongest() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "2026-01-01", 172.0),
                new WeightEntry(2, "2026-01-02", 171.0),
                new WeightEntry(3, "2026-01-10", 170.0),
                new WeightEntry(4, "2026-01-11", 169.0),
                new WeightEntry(5, "2026-01-12", 168.0)
        );

        // Longest run is Jan 10-11-12 = 3 days
        assertEquals(3, WeightAnalytics.calculateLongestStreak(entries));
    }

    @Test
    public void calculateLongestStreak_noConsecutive_returnsOne() {
        List<WeightEntry> entries = Arrays.asList(
                new WeightEntry(1, "2026-01-01", 172.0),
                new WeightEntry(2, "2026-01-05", 171.0),
                new WeightEntry(3, "2026-01-10", 170.0)
        );

        assertEquals(1, WeightAnalytics.calculateLongestStreak(entries));
    }

    @Test
    public void calculateLongestStreak_singleEntry_returnsOne() {
        List<WeightEntry> entries = Collections.singletonList(
                new WeightEntry(1, "2026-01-15", 170.0)
        );

        assertEquals(1, WeightAnalytics.calculateLongestStreak(entries));
    }

    @Test
    public void calculateLongestStreak_emptyList_returnsZero() {
        assertEquals(0, WeightAnalytics.calculateLongestStreak(new ArrayList<>()));
    }

    @Test
    public void calculateLongestStreak_nullList_returnsZero() {
        assertEquals(0, WeightAnalytics.calculateLongestStreak(null));
    }

}
