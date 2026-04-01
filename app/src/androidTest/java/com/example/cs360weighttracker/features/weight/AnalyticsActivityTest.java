package com.example.cs360weighttracker.features.weight;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.cs360weighttracker.R;
import com.example.cs360weighttracker.data.DatabaseHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class AnalyticsActivityTest {

    private DatabaseHelper dbHelper;
    private int testUserId;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        dbHelper = new DatabaseHelper(context);

        // Clean up leftovers
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("users", "username=?", new String[]{"testuser"});

        // Create test user with goal 150.0
        dbHelper.createUser("testuser", "testpass", 150.0, null);
        testUserId = dbHelper.loginUser("testuser", "testpass");
        assertTrue("Test user must be created", testUserId != -1);
    }

    @After
    public void tearDown() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("weights", "userId=?", new String[]{String.valueOf(testUserId)});
        db.delete("users", "id=?", new String[]{String.valueOf(testUserId)});
    }

    private void waitForDestroy(ActivityScenario<?> scenario) throws InterruptedException {
        Thread.sleep(1000);
        assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
    }

    private Intent createIntent(int userId) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, AnalyticsActivity.class);
        intent.putExtra("userId", userId);
        return intent;
    }

    // --- Happy path ---

    @Test
    public void onCreate_validUserId_displaysAllElements() {
        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.tvAnalyticsTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.tvCurrentWeight)).check(matches(isDisplayed()));
            onView(withId(R.id.tvGoalWeight)).check(matches(isDisplayed()));
            onView(withId(R.id.tvTotalChange)).check(matches(isDisplayed()));
            onView(withId(R.id.tvRate)).check(matches(isDisplayed()));
            onView(withId(R.id.tvMinWeight)).check(matches(isDisplayed()));
            onView(withId(R.id.tvMaxWeight)).check(matches(isDisplayed()));
            onView(withId(R.id.tvAverageWeight)).check(matches(isDisplayed()));
            onView(withId(R.id.tvStreak)).check(matches(isDisplayed()));
            onView(withId(R.id.tvLongestStreak)).check(matches(isDisplayed()));
            onView(withId(R.id.tvProjectedGoal)).check(matches(isDisplayed()));
            onView(withId(R.id.btnBack)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void onCreate_validUserId_displaysGoalWeight() {
        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.tvGoalWeight)).check(matches(withText("150.0 lbs")));
        }
    }

    @Test
    public void onCreate_noEntries_displaysNAForStats() {
        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.tvCurrentWeight)).check(matches(withText("N/A")));
            onView(withId(R.id.tvTotalChange)).check(matches(withText("N/A")));
            onView(withId(R.id.tvRate)).check(matches(withText("N/A")));
            onView(withId(R.id.tvMinWeight)).check(matches(withText("N/A")));
            onView(withId(R.id.tvMaxWeight)).check(matches(withText("N/A")));
            onView(withId(R.id.tvAverageWeight)).check(matches(withText("N/A")));
            onView(withId(R.id.tvStreak)).check(matches(withText("N/A")));
            onView(withId(R.id.tvLongestStreak)).check(matches(withText("N/A")));
        }
    }

    @Test
    public void onCreate_noEntries_displaysNoEntriesMessage() {
        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.tvProjectedGoal)).check(matches(withText("No weight entries yet")));
        }
    }

    @Test
    public void onCreate_withOneEntry_displaysCurrentWeight() {
        dbHelper.addWeight(testUserId, "2026-01-15", 165.5);

        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.tvCurrentWeight)).check(matches(withText("165.5 lbs")));
        }
    }

    @Test
    public void onCreate_withOneEntry_displaysTotalChangeZero() {
        dbHelper.addWeight(testUserId, "2026-01-15", 165.5);

        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.tvTotalChange)).check(matches(withText("+0.0 lbs")));
        }
    }

    @Test
    public void onCreate_withMultipleEntries_displaysCorrectCurrentWeight() {
        dbHelper.addWeight(testUserId, "2026-01-15", 170.0);
        dbHelper.addWeight(testUserId, "2026-01-16", 168.0);
        dbHelper.addWeight(testUserId, "2026-01-17", 163.2);

        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.tvCurrentWeight)).check(matches(withText("163.2 lbs")));
        }
    }

    @Test
    public void onCreate_withMultipleEntries_displaysTotalChange() {
        dbHelper.addWeight(testUserId, "2026-01-15", 170.0);
        dbHelper.addWeight(testUserId, "2026-01-16", 165.0);

        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.tvTotalChange)).check(matches(withText("-5.0 lbs")));
        }
    }

    @Test
    public void onCreate_withMultipleEntries_displaysMinWeight() {
        dbHelper.addWeight(testUserId, "2026-01-15", 170.0);
        dbHelper.addWeight(testUserId, "2026-01-16", 160.0);
        dbHelper.addWeight(testUserId, "2026-01-17", 165.0);

        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.tvMinWeight)).check(matches(withText("160.0 lbs")));
            onView(withId(R.id.tvMinDate)).check(matches(withText("2026-01-16")));
        }
    }

    @Test
    public void onCreate_withMultipleEntries_displaysMaxWeight() {
        dbHelper.addWeight(testUserId, "2026-01-15", 170.0);
        dbHelper.addWeight(testUserId, "2026-01-16", 160.0);
        dbHelper.addWeight(testUserId, "2026-01-17", 165.0);

        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.tvMaxWeight)).check(matches(withText("170.0 lbs")));
            onView(withId(R.id.tvMaxDate)).check(matches(withText("2026-01-15")));
        }
    }

    @Test
    public void onCreate_withMultipleEntries_displaysAverage() {
        dbHelper.addWeight(testUserId, "2026-01-15", 170.0);
        dbHelper.addWeight(testUserId, "2026-01-16", 160.0);
        dbHelper.addWeight(testUserId, "2026-01-17", 165.0);

        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.tvAverageWeight)).check(matches(withText("165.0 lbs")));
        }
    }

    @Test
    public void onCreate_consecutiveDays_displaysStreak() {
        dbHelper.addWeight(testUserId, "2026-01-15", 170.0);
        dbHelper.addWeight(testUserId, "2026-01-16", 169.0);
        dbHelper.addWeight(testUserId, "2026-01-17", 168.0);

        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.tvStreak)).check(matches(withText("3 days")));
        }
    }

    @Test
    public void onCreate_consecutiveDays_displaysLongestStreak() {
        dbHelper.addWeight(testUserId, "2026-01-15", 170.0);
        dbHelper.addWeight(testUserId, "2026-01-16", 169.0);
        dbHelper.addWeight(testUserId, "2026-01-17", 168.0);

        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.tvLongestStreak)).check(matches(withText("3 days")));
        }
    }

    @Test
    public void onCreate_weightAtGoal_displaysGoalReached() {
        dbHelper.addWeight(testUserId, "2026-01-15", 170.0);
        dbHelper.addWeight(testUserId, "2026-01-16", 150.0);

        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.tvProjectedGoal)).check(matches(withText("Goal reached!")));
        }
    }

    @Test
    public void onCreate_weightBelowGoal_displaysGoalReached() {
        dbHelper.addWeight(testUserId, "2026-01-15", 170.0);
        dbHelper.addWeight(testUserId, "2026-01-16", 145.0);

        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.tvProjectedGoal)).check(matches(withText("Goal reached!")));
        }
    }

    @Test
    public void onCreate_losingWeight_displaysProjectedDate() {
        dbHelper.addWeight(testUserId, "2026-01-01", 170.0);
        dbHelper.addWeight(testUserId, "2026-01-08", 169.0);
        dbHelper.addWeight(testUserId, "2026-01-15", 168.0);

        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            // Projected date should be a YYYY-MM-DD string
            onView(withId(R.id.tvProjectedGoal)).check(matches(withText(containsString("2026-"))));
        }
    }

    @Test
    public void onCreate_gainingWeight_displaysNotEnoughData() {
        dbHelper.addWeight(testUserId, "2026-01-15", 160.0);
        dbHelper.addWeight(testUserId, "2026-01-16", 165.0);
        dbHelper.addWeight(testUserId, "2026-01-17", 170.0);

        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.tvProjectedGoal)).check(matches(withText("Not enough data")));
        }
    }

    // --- Back button ---

    @Test
    public void btnBack_click_finishesActivity() throws InterruptedException {
        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.btnBack)).perform(click());
            waitForDestroy(scenario);
        }
    }

    // --- Failure cases ---

    @Test
    public void onCreate_invalidUserId_finishesActivity() throws InterruptedException {
        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(-1))) {
            waitForDestroy(scenario);
        }
    }

    // --- Edge cases ---

    @Test
    public void onCreate_singleEntry_streakIsOneDay() {
        dbHelper.addWeight(testUserId, "2026-01-15", 165.5);

        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.tvStreak)).check(matches(withText("1 days")));
            onView(withId(R.id.tvLongestStreak)).check(matches(withText("1 days")));
        }
    }

    @Test
    public void onCreate_gapInDays_currentStreakResets() {
        dbHelper.addWeight(testUserId, "2026-01-10", 170.0);
        dbHelper.addWeight(testUserId, "2026-01-11", 169.0);
        dbHelper.addWeight(testUserId, "2026-01-16", 168.0);

        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.tvStreak)).check(matches(withText("1 days")));
            onView(withId(R.id.tvLongestStreak)).check(matches(withText("2 days")));
        }
    }

    @Test
    public void onCreate_singleEntry_rateDisplaysZero() {
        dbHelper.addWeight(testUserId, "2026-01-15", 165.5);

        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.tvRate)).check(matches(withText("+0.0")));
        }
    }

    @Test
    public void onCreate_singleEntry_notEnoughDataForProjection() {
        dbHelper.addWeight(testUserId, "2026-01-15", 165.5);

        try (ActivityScenario<AnalyticsActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.tvProjectedGoal)).check(matches(withText("Not enough data")));
        }
    }
}
