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

    /**
     * @param dbHelper database helper instance
     */
    public WeightRepository(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Returns all weight entries for a user, newest first.
     */
    public List<WeightEntry> getWeights(int userId) {
        return dbHelper.getWeights(userId);
    }

    /**
     * Returns all weight entries for a user sorted oldest-first
     * (natural {@link WeightEntry} order).
     */
    public List<WeightEntry> getWeightsAscending(int userId) {
        List<WeightEntry> list = new ArrayList<>(dbHelper.getWeights(userId));
        Collections.sort(list);
        return list;
    }

    /**
     * Returns weight entries between {@code startDate} and
     * {@code endDate} inclusive, newest first.
     */
    public List<WeightEntry> getWeightsInRange(int userId, String startDate, String endDate) {
        return dbHelper.getWeightsInRange(userId, startDate, endDate);
    }

    /**
     * Full-text search on weight entry notes.
     */
    public List<WeightEntry> searchWeightNotes(int userId, String query) {
        return dbHelper.searchWeightNotes(userId, query);
    }

    /**
     * Returns weekly averages for a user, newest week first.
     */
    public List<TimePeriodAverage> getWeeklyAverages(int userId) {
        return dbHelper.getWeeklyAverages(userId);
    }

    /**
     * Returns monthly averages for a user, newest month first.
     */
    public List<TimePeriodAverage> getMonthlyAverages(int userId) {
        return dbHelper.getMonthlyAverages(userId);
    }

    /**
     * Inserts a new weight entry without notes.
     *
     * @return {@code true} if the row was inserted
     */
    public boolean addWeight(int userId, String date, double weight) {
        return dbHelper.addWeight(userId, date, weight);
    }

    /**
     * Inserts a new weight entry with notes.
     *
     * @return {@code true} if the row was inserted
     */
    public boolean addWeight(int userId, String date, double weight, String notes) {
        return dbHelper.addWeight(userId, date, weight, notes);
    }

    /**
     * Updates the date and weight of an existing entry.
     *
     * @return {@code true} if a row was affected
     */
    public boolean updateWeight(int weightId, String date, double weight) {
        return dbHelper.updateWeight(weightId, date, weight);
    }

    /**
     * Updates the date, weight, and notes of an existing entry.
     *
     * @return {@code true} if a row was affected
     */
    public boolean updateWeight(int weightId, String date, double weight, String notes) {
        return dbHelper.updateWeight(weightId, date, weight, notes);
    }

    /**
     * Updates only the notes field of an existing entry.
     *
     * @return {@code true} if a row was affected
     */
    public boolean updateWeightNotes(int weightId, String notes) {
        return dbHelper.updateWeightNotes(weightId, notes);
    }

    /**
     * Deletes a weight entry by its row ID.
     *
     * @return {@code true} if a row was deleted
     */
    public boolean deleteWeight(int weightId) {
        return dbHelper.deleteWeight(weightId);
    }
}
