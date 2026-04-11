package com.example.cs360weighttracker.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Repository that mediates between {@link DatabaseHelper} and the
 * weight-related ViewModels.
 *
 * <p>With the BundledSQLiteDriver migration, DatabaseHelper now returns
 * {@code List} types directly, so this repository mostly delegates
 * calls through. It still provides the ascending-sort variant and
 * shields ViewModels from the data-layer implementation.</p>
 */
public class WeightRepository {

    private final DatabaseHelper dbHelper;

    public WeightRepository(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public List<WeightEntry> getWeights(int userId) {
        return dbHelper.getWeights(userId);
    }

    public List<WeightEntry> getWeightsAscending(int userId) {
        List<WeightEntry> list = new ArrayList<>(dbHelper.getWeights(userId));
        Collections.sort(list);
        return list;
    }

    public List<WeightEntry> getWeightsInRange(int userId, String startDate, String endDate) {
        return dbHelper.getWeightsInRange(userId, startDate, endDate);
    }

    public List<WeightEntry> searchWeightNotes(int userId, String query) {
        return dbHelper.searchWeightNotes(userId, query);
    }

    public List<TimePeriodAverage> getWeeklyAverages(int userId) {
        return dbHelper.getWeeklyAverages(userId);
    }

    public List<TimePeriodAverage> getMonthlyAverages(int userId) {
        return dbHelper.getMonthlyAverages(userId);
    }

    public boolean addWeight(int userId, String date, double weight) {
        return dbHelper.addWeight(userId, date, weight);
    }

    public boolean addWeight(int userId, String date, double weight, String notes) {
        return dbHelper.addWeight(userId, date, weight, notes);
    }

    public boolean updateWeight(int weightId, String date, double weight) {
        return dbHelper.updateWeight(weightId, date, weight);
    }

    public boolean updateWeight(int weightId, String date, double weight, String notes) {
        return dbHelper.updateWeight(weightId, date, weight, notes);
    }

    public boolean updateWeightNotes(int weightId, String notes) {
        return dbHelper.updateWeightNotes(weightId, notes);
    }

    public boolean deleteWeight(int weightId) {
        return dbHelper.deleteWeight(weightId);
    }
}
