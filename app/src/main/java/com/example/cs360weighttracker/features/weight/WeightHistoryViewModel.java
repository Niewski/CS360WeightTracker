package com.example.cs360weighttracker.features.weight;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cs360weighttracker.data.TimePeriodAverage;
import com.example.cs360weighttracker.data.WeightEntry;
import com.example.cs360weighttracker.data.WeightRepository;

import java.util.Collections;
import java.util.List;

/**
 * ViewModel for the weight-history screen. Exposes observable lists of
 * {@link WeightEntry} and {@link TimePeriodAverage} that the Activity
 * observes to populate RecyclerView adapters.
 *
 * <p>Supports three display modes controlled by {@link #setDisplayMode(int)}:
 * <ul>
 *   <li><b>0</b> — individual weight entries (default)</li>
 *   <li><b>1</b> — weekly averages</li>
 *   <li><b>2</b> — monthly averages</li>
 * </ul></p>
 *
 * @see WeightHistoryActivity
 * @see WeightRepository
 */
public class WeightHistoryViewModel extends ViewModel {

    private WeightRepository repository;
    private int userId = -1;

    /** Individual weight entries shown in display mode 0. */
    private final MutableLiveData<List<WeightEntry>> weights =
        new MutableLiveData<>(Collections.emptyList());

    /** Aggregated averages shown in display modes 1 (weekly) and 2 (monthly). */
    private final MutableLiveData<List<TimePeriodAverage>> summaryData =
        new MutableLiveData<>(Collections.emptyList());

    /** Current display mode: 0 = entries, 1 = weekly, 2 = monthly. */
    private final MutableLiveData<Integer> displayMode = new MutableLiveData<>(0);

    /**
     * One-time initialisation called from the Activity's {@code onCreate}.
     * Ignored on subsequent calls (e.g. after a configuration change) so
     * the repository and user ID are not reset.
     *
     * @param repository the {@link WeightRepository} for data access
     * @param userId     the logged-in user's row ID
     */
    public void init(WeightRepository repository, int userId) {
        if (this.repository != null) return;
        this.repository = repository;
        this.userId = userId;
        loadWeights();
    }

    // ── Observable getters ──────────────────────────────────────────────

    /**
     * Returns the observable list of individual weight entries.
     *
     * @return {@link LiveData} containing the current entries list
     */
    public LiveData<List<WeightEntry>> getWeights() {
        return weights;
    }

    /**
     * Returns the observable list of aggregated period averages
     * (weekly or monthly, depending on the current display mode).
     *
     * @return {@link LiveData} containing the current summary list
     */
    public LiveData<List<TimePeriodAverage>> getSummaryData() {
        return summaryData;
    }

    /**
     * Returns the observable display mode indicator.
     *
     * @return {@link LiveData} containing 0, 1, or 2
     */
    public LiveData<Integer> getDisplayMode() {
        return displayMode;
    }

    // ── Data loading ────────────────────────────────────────────────────

    /**
     * Reloads all individual weight entries from the repository and
     * publishes them to {@link #getWeights()}.
     */
    public void loadWeights() {
        if (repository != null && userId != -1) {
            weights.setValue(repository.getWeights(userId));
        }
    }

    /**
     * Switches the display mode and loads the appropriate data set.
     *
     * <p>Mode 0 reloads individual entries into {@link #getWeights()}.
     * Modes 1 and 2 populate {@link #getSummaryData()} with weekly or
     * monthly averages respectively.</p>
     *
     * @param mode 0 for entries, 1 for weekly averages, 2 for monthly
     */
    public void setDisplayMode(int mode) {
        displayMode.setValue(mode);
        if (mode == 0) {
            loadWeights();
        } else if (mode == 1) {
            summaryData.setValue(repository.getWeeklyAverages(userId));
        } else if (mode == 2) {
            summaryData.setValue(repository.getMonthlyAverages(userId));
        }
    }

    /**
     * Performs an FTS5 full-text search on weight notes and publishes
     * matching entries to {@link #getWeights()}. An empty or {@code null}
     * query reloads the full entry list instead.
     *
     * @param query the search term(s) to match against notes
     */
    public void searchNotes(String query) {
        if (repository == null || userId == -1) return;
        if (query == null || query.trim().isEmpty()) {
            loadWeights();
            return;
        }
        List<WeightEntry> results = repository.searchWeightNotes(userId, query);
        weights.setValue(results);
    }

    /**
     * Loads weight entries whose date falls within the inclusive range
     * [{@code startDate}, {@code endDate}] and publishes them to
     * {@link #getWeights()}.
     *
     * @param startDate lower bound in {@code YYYY-MM-DD} format
     * @param endDate   upper bound in {@code YYYY-MM-DD} format
     */
    public void loadWeightsInRange(String startDate, String endDate) {
        if (repository == null || userId == -1) return;
        List<WeightEntry> results = repository.getWeightsInRange(userId, startDate, endDate);
        weights.setValue(results);
    }

    // ── Write operations ────────────────────────────────────────────────

    /**
     * Deletes a weight entry and refreshes whichever data set is currently
     * active (entries or aggregated summaries).
     *
     * @param weightId primary key of the weight row to delete
     * @return {@code true} if the row was deleted successfully
     */
    public boolean deleteWeight(int weightId) {
        if (repository == null || userId == -1) {
            return false;
        }
        boolean deleted = repository.deleteWeight(weightId);
        if (deleted) {
            // refresh according to display mode
            Integer mode = displayMode.getValue();
            if (mode != null && mode > 0) {
                setDisplayMode(mode);
            } else {
                loadWeights();
            }
        }
        return deleted;
    }
}
