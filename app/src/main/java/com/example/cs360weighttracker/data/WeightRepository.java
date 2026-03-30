package com.example.cs360weighttracker.data;

import android.database.Cursor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Repository that mediates between {@link DatabaseHelper} and the
 * weight-related ViewModels.
 *
 * <p>Converts raw {@link Cursor} results into {@code List<WeightEntry>}
 * and handles cursor lifecycle internally, so callers never manage
 * cursors directly.</p>
 *
 * @see DatabaseHelper
 * @see WeightEntry
 * @see com.example.cs360weighttracker.features.weight.WeightHistoryViewModel
 * @see com.example.cs360weighttracker.features.weight.AddWeightViewModel
 * @see com.example.cs360weighttracker.features.weight.EditWeightViewModel
 */
public class WeightRepository {

    private final DatabaseHelper dbHelper;

    /**
     * @param dbHelper the database helper instance to delegate to
     */
    public WeightRepository(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Retrieves all weight entries for the specified user, ordered by date
     * descending (most recent first).
     *
     * <p>Iterates the database cursor, converts each row to a
     * {@link WeightEntry}, then closes the cursor before returning.</p>
     *
     * @param userId the owning user's primary-key ID
     * @return a list of weight entries; empty list if none exist
     */
    public List<WeightEntry> getWeights(int userId) {
        List<WeightEntry> list = new ArrayList<>();
        Cursor cursor = dbHelper.getWeights(userId);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                double weight = cursor.getDouble(cursor.getColumnIndexOrThrow("weight"));
                list.add(new WeightEntry(id, date, weight));
            }
            cursor.close();
        }
        return list;
    }

    /**
     * Inserts a new weight entry.
     *
     * @param userId the owning user's primary-key ID
     * @param date   the entry date in {@code YYYY-MM-DD} format
     * @param weight the weight value in pounds
     * @return {@code true} if the row was inserted successfully
     * @see DatabaseHelper#addWeight(int, String, double)
     */
    public boolean addWeight(int userId, String date, double weight) {
        return dbHelper.addWeight(userId, date, weight);
    }

    /**
     * Updates an existing weight entry's date and weight.
     *
     * @param weightId the primary-key ID of the weight row
     * @param date     the new date in {@code YYYY-MM-DD} format
     * @param weight   the new weight value in pounds
     * @return {@code true} if the row was updated
     * @see DatabaseHelper#updateWeight(int, String, double)
     */
    public boolean updateWeight(int weightId, String date, double weight) {
        return dbHelper.updateWeight(weightId, date, weight);
    }

    /**
     * Deletes a weight entry by its primary key.
     *
     * @param weightId the primary-key ID of the weight row to delete
     * @return {@code true} if a row was deleted
     * @see DatabaseHelper#deleteWeight(int)
     */
    public boolean deleteWeight(int weightId) {
        return dbHelper.deleteWeight(weightId);
    }

    /**
     * Retrieves all weight entries for the specified user, ordered by date
     * ascending (oldest first) — suitable for analytics calculations.
     *
     * @param userId the owning user's primary-key ID
     * @return a list of weight entries sorted ascending by date
     */
    public List<WeightEntry> getWeightsAscending(int userId) {
        List<WeightEntry> list = getWeights(userId);
        Collections.sort(list);
        return list;
    }
}
