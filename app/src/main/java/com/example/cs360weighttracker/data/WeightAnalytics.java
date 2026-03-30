package com.example.cs360weighttracker.data;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WeightAnalytics {

    /**
     * Sliding-window moving average over the most recent entries.
     * Entries must be sorted ascending by date before calling.
     *
     * @param entries    sorted list of weight entries (ascending by date)
     * @param windowSize number of entries in each window (e.g. 7)
     * @return list of averaged values; size = entries.size() - windowSize + 1.
     *         Empty list if fewer entries than windowSize.
     */
    public static List<Double> calculateMovingAverage(List<WeightEntry> entries, int windowSize) {
        List<Double> result = new ArrayList<>();
        if (entries == null || entries.size() < windowSize || windowSize <= 0) {
            return result;
        }

        double windowSum = 0;
        for (int i = 0; i < windowSize; i++) {
            windowSum += entries.get(i).weight;
        }
        result.add(windowSum / windowSize);

        for (int i = windowSize; i < entries.size(); i++) {
            windowSum += entries.get(i).weight - entries.get(i - windowSize).weight;
            result.add(windowSum / windowSize);
        }
        return result;
    }

    /**
     * Linear regression slope over all entries, expressed as lbs per week.
     * Uses least-squares regression on (dayIndex, weight) pairs.
     * Entries must be sorted ascending by date.
     *
     * @param entries sorted list of weight entries (ascending by date)
     * @return rate of change in lbs/week, or 0.0 if fewer than 2 entries
     */
    public static double calculateRateOfChange(List<WeightEntry> entries) {
        if (entries == null || entries.size() < 2) {
            return 0.0;
        }

        LocalDate startDate = LocalDate.parse(entries.get(0).date);
        int n = entries.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

        for (int i = 0; i < n; i++) {
            double x = ChronoUnit.DAYS.between(startDate, LocalDate.parse(entries.get(i).date));
            double y = entries.get(i).weight;
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double denominator = n * sumX2 - sumX * sumX;
        if (denominator == 0) {
            return 0.0;
        }

        double slopePerDay = (n * sumXY - sumX * sumY) / denominator;
        return slopePerDay * 7.0; // convert to lbs/week
    }

    /**
     * Finds the minimum weight entry via single-pass scan.
     *
     * @param entries list of weight entries (any order)
     * @return entry with the lowest weight, or null if list is empty/null
     */
    public static WeightEntry findMin(List<WeightEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        WeightEntry min = entries.get(0);
        for (int i = 1; i < entries.size(); i++) {
            if (entries.get(i).weight < min.weight) {
                min = entries.get(i);
            }
        }
        return min;
    }

    /**
     * Finds the maximum weight entry via single-pass scan.
     *
     * @param entries list of weight entries (any order)
     * @return entry with the highest weight, or null if list is empty/null
     */
    public static WeightEntry findMax(List<WeightEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        WeightEntry max = entries.get(0);
        for (int i = 1; i < entries.size(); i++) {
            if (entries.get(i).weight > max.weight) {
                max = entries.get(i);
            }
        }
        return max;
    }

    /**
     * Calculates the arithmetic mean of all weight values.
     *
     * @param entries list of weight entries (any order)
     * @return average weight, or 0.0 if list is empty/null
     */
    public static double calculateAverage(List<WeightEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return 0.0;
        }
        double sum = 0;
        for (WeightEntry entry : entries) {
            sum += entry.weight;
        }
        return sum / entries.size();
    }

    /**
     * Projects the date the user will reach their goal weight based on
     * linear regression trend. Entries must be sorted ascending by date.
     *
     * @param entries    sorted list of weight entries (ascending by date)
     * @param goalWeight the target weight in lbs
     * @return projected date string (YYYY-MM-DD), or null if:
     *         - fewer than 2 entries
     *         - goal already reached (latest weight <= goal)
     *         - trend is flat or moving away from goal
     */
    public static String projectGoalDate(List<WeightEntry> entries, double goalWeight) {
        if (entries == null || entries.size() < 2) {
            return null;
        }

        double latestWeight = entries.get(entries.size() - 1).weight;
        if (latestWeight <= goalWeight) {
            return null; // already at or below goal
        }

        double ratePerWeek = calculateRateOfChange(entries);
        if (ratePerWeek >= 0) {
            return null; // not losing weight — can't project
        }

        double ratePerDay = ratePerWeek / 7.0;
        double weightToLose = latestWeight - goalWeight;
        long daysToGoal = (long) Math.ceil(weightToLose / Math.abs(ratePerDay));

        LocalDate latestDate = LocalDate.parse(entries.get(entries.size() - 1).date);
        LocalDate projectedDate = latestDate.plusDays(daysToGoal);
        return projectedDate.toString();
    }

    /**
     * Counts consecutive days with a weight entry, ending at the most
     * recent entry. Entries must be sorted ascending by date.
     *
     * @param entries sorted list of weight entries (ascending by date)
     * @return streak count (at least 1 if entries exist), or 0 if empty/null
     */
    public static int calculateStreak(List<WeightEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return 0;
        }
        if (entries.size() == 1) {
            return 1;
        }

        int streak = 1;
        for (int i = entries.size() - 1; i > 0; i--) {
            try {
                LocalDate current = LocalDate.parse(entries.get(i).date);
                LocalDate previous = LocalDate.parse(entries.get(i - 1).date);
                if (ChronoUnit.DAYS.between(previous, current) == 1) {
                    streak++;
                } else {
                    break;
                }
            } catch (DateTimeParseException e) {
                break;
            }
        }
        return streak;
    }

}
