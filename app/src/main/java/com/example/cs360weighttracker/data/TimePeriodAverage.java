package com.example.cs360weighttracker.data;

/**
 * Immutable data holder representing the average weight for a
 * calendar period (e.g. a week or month).
 */
public class TimePeriodAverage {
    /** Period label such as {@code "2026-W15"} or {@code "2026-04"}. */
    public final String period;
    /** Average weight across entries in this period (lbs). */
    public final double average;
    /** Number of entries that contributed to the average. */
    public final int entryCount;

    /**
     * @param period     period label
     * @param average    average weight in lbs
     * @param entryCount number of entries in the period
     */
    public TimePeriodAverage(String period, double average, int entryCount) {
        this.period = period;
        this.average = average;
        this.entryCount = entryCount;
    }
}
