package com.example.cs360weighttracker.features.weight;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.cs360weighttracker.data.WeightAnalytics;
import com.example.cs360weighttracker.data.WeightEntry;
import com.example.cs360weighttracker.data.WeightRepository;

import java.util.Collections;
import java.util.List;

/**
 * ViewModel for the analytics dashboard screen.
 *
 * <p>Fetches weight entries from the repository and computes
 * statistics (rate of change, min/max, streaks, moving averages,
 * projected goal date) via {@link WeightAnalytics}. Results are
 * published through {@link LiveData} as an {@link AnalyticsResult}.</p>
 */
public class AnalyticsViewModel extends ViewModel {

    private WeightRepository repository;
    private int userId = -1;
    private double goalWeight = -1;

    private final MutableLiveData<AnalyticsResult> analyticsResult = new MutableLiveData<>();

    /**
     * One-time initializer — supplies the repository, user ID, and
     * goal weight, then triggers the initial analytics computation.
     */
    public void init(WeightRepository repository, int userId, double goalWeight) {
        if (this.repository != null) return;
        this.repository = repository;
        this.userId = userId;
        this.goalWeight = goalWeight;
        loadAnalytics();
    }

    /**
     * Observable analytics computation result.
     */
    public LiveData<AnalyticsResult> getAnalyticsResult() {
        return analyticsResult;
    }

    /**
     * Recomputes all analytics from the current repository data and
     * publishes a new {@link AnalyticsResult}.
     */
    public void loadAnalytics() {
        if (repository == null || userId == -1) return;

        List<WeightEntry> entries = repository.getWeightsAscending(userId);
        AnalyticsResult result = new AnalyticsResult();
        result.goalWeight = goalWeight;
        result.entries = entries;

        if (entries.isEmpty()) {
            result.hasData = false;
            analyticsResult.setValue(result);
            return;
        }

        result.hasData = true;
        result.currentWeight = entries.get(entries.size() - 1).weight;
        result.totalChange = result.currentWeight - entries.get(0).weight;
        result.rateOfChange = WeightAnalytics.calculateRateOfChange(entries);
        result.minEntry = WeightAnalytics.findMin(entries);
        result.maxEntry = WeightAnalytics.findMax(entries);
        result.average = WeightAnalytics.calculateAverage(entries);
        result.streak = WeightAnalytics.calculateStreak(entries);
        result.longestStreak = WeightAnalytics.calculateLongestStreak(entries);
        result.projectedGoalDate = WeightAnalytics.projectGoalDate(entries, goalWeight);
        result.goalReached = result.currentWeight <= goalWeight;
        result.movingAverages = WeightAnalytics.calculateMovingAverage(entries, 7);

        analyticsResult.setValue(result);
    }

    /**
     * Container for all computed analytics fields consumed by the
     * Activity UI.
     */
    public static class AnalyticsResult {
        public boolean hasData;
        public double goalWeight;
        public double currentWeight;
        public double totalChange;
        public double rateOfChange;
        public WeightEntry minEntry;
        public WeightEntry maxEntry;
        public double average;
        public int streak;
        public int longestStreak;
        public String projectedGoalDate;
        public boolean goalReached;
        public List<Double> movingAverages = Collections.emptyList();
        public List<WeightEntry> entries = Collections.emptyList();
    }
}
