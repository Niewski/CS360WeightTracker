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
     * Creates a repository backed by the given helper.
     *
     * @param dbHelper the {@link DatabaseHelper} used for all database access
     */
    public WeightRepository(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // ── Read operations ─────────────────────────────────────────────────

    /**
     * Returns every weight entry for a user, ordered by date descending
     * (most recent first).
     *
     * @param userId the logged-in user's row ID
     * @return a list of entries; empty list if the user has none
     */
    public List<WeightEntry> getWeights(int userId) {
        List<WeightEntry> list = new ArrayList<>();
        Cursor cursor = dbHelper.getWeights(userId);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(entryFromCursor(cursor));
            }
            cursor.close();
        }
        return list;
    }

    /**
     * Returns every weight entry for a user, sorted by date ascending
     * (oldest first). Useful for charting trends over time.
     *
     * @param userId the logged-in user's row ID
     * @return a chronologically sorted list; empty list if the user has none
     */
    public List<WeightEntry> getWeightsAscending(int userId) {
        List<WeightEntry> list = getWeights(userId);
        Collections.sort(list);
        return list;
    }

    /**
     * Returns weight entries whose date falls within the inclusive range
     * [{@code startDate}, {@code endDate}].
     *
     * <p>Leverages the compound index on {@code (userId, date)} for
     * efficient filtering.</p>
     *
     * @param userId    the logged-in user's row ID
     * @param startDate lower bound in {@code YYYY-MM-DD} format
     * @param endDate   upper bound in {@code YYYY-MM-DD} format
     * @return matching entries ordered by date descending; empty list if none
     */
    public List<WeightEntry> getWeightsInRange(int userId, String startDate, String endDate) {
        List<WeightEntry> list = new ArrayList<>();
        Cursor cursor = dbHelper.getWeightsInRange(userId, startDate, endDate);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(entryFromCursor(cursor));
            }
            cursor.close();
        }
        return list;
    }

    /**
     * Performs an FTS5 full-text search against the {@code notes} column
     * and returns entries whose notes match the query.
     *
     * @param userId the logged-in user's row ID
     * @param query  the search term(s) to match via FTS5
     * @return matching entries ordered by date descending; empty list if none
     * @see DatabaseHelper#searchWeightNotes(int, String)
     */
    public List<WeightEntry> searchWeightNotes(int userId, String query) {
        List<WeightEntry> list = new ArrayList<>();
        Cursor cursor = dbHelper.searchWeightNotes(userId, query);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(entryFromCursor(cursor));
            }
            cursor.close();
        }
        return list;
    }

    // ── Aggregation operations ──────────────────────────────────────────

    /**
     * Returns average weight per ISO week ({@code YYYY-Www}) for the user,
     * ordered most recent week first.
     *
     * @param userId the logged-in user's row ID
     * @return list of {@link TimePeriodAverage} with period, average, and
     *         entry count per week
     */
    public List<TimePeriodAverage> getWeeklyAverages(int userId) {
        return aggregatesFromCursor(dbHelper.getWeeklyAverages(userId));
    }

    /**
     * Returns average weight per calendar month ({@code YYYY-MM}) for the
     * user, ordered most recent month first.
     *
     * @param userId the logged-in user's row ID
     * @return list of {@link TimePeriodAverage} with period, average, and
     *         entry count per month
     */
    public List<TimePeriodAverage> getMonthlyAverages(int userId) {
        return aggregatesFromCursor(dbHelper.getMonthlyAverages(userId));
    }

    // ── Write operations ────────────────────────────────────────────────

    /**
     * Inserts a new weight entry with no notes.
     *
     * @param userId the logged-in user's row ID
     * @param date   entry date in {@code YYYY-MM-DD} format
     * @param weight weight value in pounds
     * @return {@code true} if the row was inserted successfully
     */
    public boolean addWeight(int userId, String date, double weight) {
        return dbHelper.addWeight(userId, date, weight);
    }

    /**
     * Inserts a new weight entry with optional notes.
     *
     * @param userId the logged-in user's row ID
     * @param date   entry date in {@code YYYY-MM-DD} format
     * @param weight weight value in pounds
     * @param notes  free-text note (may be {@code null} or empty)
     * @return {@code true} if the row was inserted successfully
     */
    public boolean addWeight(int userId, String date, double weight, String notes) {
        return dbHelper.addWeight(userId, date, weight, notes);
    }

    /**
     * Updates the date and weight of an existing entry, leaving notes
     * unchanged.
     *
     * @param weightId primary key of the weight row to update
     * @param date     new date in {@code YYYY-MM-DD} format
     * @param weight   new weight value in pounds
     * @return {@code true} if exactly one row was updated
     */
    public boolean updateWeight(int weightId, String date, double weight) {
        return dbHelper.updateWeight(weightId, date, weight);
    }

    /**
     * Updates the date, weight, and notes of an existing entry.
     *
     * @param weightId primary key of the weight row to update
     * @param date     new date in {@code YYYY-MM-DD} format
     * @param weight   new weight value in pounds
     * @param notes    new note text (may be {@code null} or empty)
     * @return {@code true} if exactly one row was updated
     */
    public boolean updateWeight(int weightId, String date, double weight, String notes) {
        return dbHelper.updateWeight(weightId, date, weight, notes);
    }

    /**
     * Updates only the notes field of an existing weight entry;
     * date and weight remain unchanged.
     *
     * @param weightId primary key of the weight row to update
     * @param notes    new note text (may be {@code null} or empty)
     * @return {@code true} if exactly one row was updated
     */
    public boolean updateWeightNotes(int weightId, String notes) {
        return dbHelper.updateWeightNotes(weightId, notes);
    }

    /**
     * Deletes a single weight entry.
     *
     * @param weightId primary key of the weight row to delete
     * @return {@code true} if exactly one row was deleted
     */
    public boolean deleteWeight(int weightId) {
        return dbHelper.deleteWeight(weightId);
    }

    // ── Cursor mapping helpers ──────────────────────────────────────────

    /**
     * Maps the current cursor row to a {@link WeightEntry}, reading the
     * {@code id}, {@code date}, {@code weight}, and {@code notes} columns.
     *
     * @param cursor a cursor positioned at a valid row
     * @return a fully populated {@link WeightEntry}
     */
    private WeightEntry entryFromCursor(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
        double weight = cursor.getDouble(cursor.getColumnIndexOrThrow("weight"));
        String notes = cursor.getString(cursor.getColumnIndexOrThrow("notes"));
        return new WeightEntry(id, date, weight, notes);
    }

    /**
     * Iterates an aggregation cursor and collects each row into a
     * {@link TimePeriodAverage}. Closes the cursor when finished.
     *
     * @param cursor a cursor from a {@code GROUP BY} aggregation query,
     *               or {@code null}
     * @return list of averages; empty list if the cursor is null or empty
     */
    private List<TimePeriodAverage> aggregatesFromCursor(Cursor cursor) {
        List<TimePeriodAverage> list = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String period = cursor.getString(cursor.getColumnIndexOrThrow("period"));
                double average = cursor.getDouble(cursor.getColumnIndexOrThrow("average"));
                int count = cursor.getInt(cursor.getColumnIndexOrThrow("entryCount"));
                list.add(new TimePeriodAverage(period, average, count));
            }
            cursor.close();
        }
        return list;
    }
}
