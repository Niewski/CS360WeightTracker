package com.example.cs360weighttracker.features.weight;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;

import com.example.cs360weighttracker.R;
import com.example.cs360weighttracker.data.DatabaseHelper;
import com.example.cs360weighttracker.data.WeightEntry;
import com.example.cs360weighttracker.data.WeightRepository;

import java.util.List;
import java.util.Locale;

/**
 * Dashboard screen displaying computed analytics for the user's
 * weight history (current weight, rate of change, min/max, streaks,
 * projected goal date, and 7-day moving averages).
 *
 * <p>Delegates computation to {@link AnalyticsViewModel} and
 * observes the result via {@link LiveData}.</p>
 */
public class AnalyticsActivity extends AppCompatActivity {

    private int userId;
    private TextView tvCurrentWeight;
    private TextView tvGoalWeight;
    private TextView tvTotalChange;
    private TextView tvRate;
    private TextView tvMinWeight;
    private TextView tvMinDate;
    private TextView tvMaxWeight;
    private TextView tvMaxDate;
    private TextView tvAverageWeight;
    private TextView tvStreak;
    private TextView tvLongestStreak;
    private TextView tvProjectedGoal;
    private LinearLayout llMovingAverages;

    /**
     * Wires UI views, initializes the ViewModel, and observes
     * analytics results.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analytics);

        userId = getIntent().getIntExtra("userId", -1);
        if (userId == -1) {
            Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvCurrentWeight = findViewById(R.id.tvCurrentWeight);
        tvGoalWeight = findViewById(R.id.tvGoalWeight);
        tvTotalChange = findViewById(R.id.tvTotalChange);
        tvRate = findViewById(R.id.tvRate);
        tvMinWeight = findViewById(R.id.tvMinWeight);
        tvMinDate = findViewById(R.id.tvMinDate);
        tvMaxWeight = findViewById(R.id.tvMaxWeight);
        tvMaxDate = findViewById(R.id.tvMaxDate);
        tvAverageWeight = findViewById(R.id.tvAverageWeight);
        tvStreak = findViewById(R.id.tvStreak);
        tvLongestStreak = findViewById(R.id.tvLongestStreak);
        tvProjectedGoal = findViewById(R.id.tvProjectedGoal);
        llMovingAverages = findViewById(R.id.llMovingAverages);
        Button btnBack = findViewById(R.id.btnBack);

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        WeightRepository repository = new WeightRepository(dbHelper);
        double goalWeight = dbHelper.getGoalWeight(userId);

        AnalyticsViewModel viewModel = new ViewModelProvider(this).get(AnalyticsViewModel.class);
        viewModel.init(repository, userId, goalWeight);

        btnBack.setOnClickListener(v -> finish());

        viewModel.getAnalyticsResult().observe(this, this::displayAnalytics);
    }

    /**
     * Populates all analytics fields from the computed result,
     * handling the "no data" case.
     */
    private void displayAnalytics(AnalyticsViewModel.AnalyticsResult result) {
        // Goal weight
        tvGoalWeight.setText(String.format(Locale.US, "%.1f lbs", result.goalWeight));

        if (!result.hasData) {
            tvCurrentWeight.setText(getString(R.string.na_label));
            tvTotalChange.setText(getString(R.string.na_label));
            tvRate.setText(getString(R.string.na_label));
            tvMinWeight.setText(getString(R.string.na_label));
            tvMinDate.setText("");
            tvMaxWeight.setText(getString(R.string.na_label));
            tvMaxDate.setText("");
            tvAverageWeight.setText(getString(R.string.na_label));
            tvStreak.setText(getString(R.string.na_label));
            tvLongestStreak.setText(getString(R.string.na_label));
            tvProjectedGoal.setText(getString(R.string.no_entries));
            llMovingAverages.removeAllViews();
            return;
        }

        // Current weight
        tvCurrentWeight.setText(String.format(Locale.US, "%.1f lbs", result.currentWeight));

        // Total change
        tvTotalChange.setText(String.format(Locale.US, "%+.1f lbs", result.totalChange));

        // Rate of change
        tvRate.setText(String.format(Locale.US, "%+.1f", result.rateOfChange));

        // Min
        if (result.minEntry != null) {
            tvMinWeight.setText(String.format(Locale.US, "%.1f lbs", result.minEntry.weight));
            tvMinDate.setText(result.minEntry.date);
        }

        // Max
        if (result.maxEntry != null) {
            tvMaxWeight.setText(String.format(Locale.US, "%.1f lbs", result.maxEntry.weight));
            tvMaxDate.setText(result.maxEntry.date);
        }

        // Average
        tvAverageWeight.setText(String.format(Locale.US, "%.1f lbs", result.average));

        // Streak
        String currentStreakText = getResources().getQuantityString(
            R.plurals.days, result.streak, result.streak);
        tvStreak.setText(currentStreakText);

        // Longest streak
        String longestStreakText = getResources().getQuantityString(
            R.plurals.days, result.longestStreak, result.longestStreak);
        tvLongestStreak.setText(longestStreakText);

        // Projected goal date
        if (result.projectedGoalDate != null) {
            tvProjectedGoal.setText(result.projectedGoalDate);
        } else if (result.goalReached) {
            tvProjectedGoal.setText(getString(R.string.goal_reached));
        } else {
            tvProjectedGoal.setText(getString(R.string.not_enough_data));
        }

        // 7-day moving average
        displayMovingAverages(result.movingAverages, result.entries);
    }

    /**
     * Renders the most recent 7-day moving average values as a
     * scrollable list of cards.
     */
    private void displayMovingAverages(List<Double> movingAverages, List<WeightEntry> entries) {
        float density = getResources().getDisplayMetrics().density;
        llMovingAverages.removeAllViews();

        if (movingAverages.isEmpty()) {
            TextView noData = new TextView(this);
            noData.setText(getString(R.string.not_enough_data));
            noData.setGravity(Gravity.CENTER);
            noData.setPadding(0, (int)(16 * density), 0, 0);
            llMovingAverages.addView(noData);
        } else {
            int start = Math.max(0, movingAverages.size() - 7);
            for (int i = start; i < movingAverages.size(); i++) {
                // MA at index i corresponds to entries[i + windowSize - 1]
                int entryIndex = i + 6; // windowSize - 1 = 6
                String date = entries.get(entryIndex).date;

                CardView card = new CardView(this);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(0, (int)(8 * density), 0, (int)(8 * density));
                card.setLayoutParams(cardParams);
                card.setCardElevation(4 * density);
                card.setRadius(8 * density);
                card.setContentPadding(
                        (int)(16 * density), (int)(12 * density),
                        (int)(16 * density), (int)(12 * density));

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);

                TextView tvDate = new TextView(this);
                tvDate.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                tvDate.setText(date);

                TextView tvAvg = new TextView(this);
                tvAvg.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                tvAvg.setText(String.format(Locale.US, "%.1f lbs", movingAverages.get(i)));
                tvAvg.setGravity(Gravity.END);

                row.addView(tvDate);
                row.addView(tvAvg);
                card.addView(row);
                llMovingAverages.addView(card);
            }
        }
    }
}
