package com.example.cs360weighttracker.features.weight;

import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.cs360weighttracker.R;
import com.example.cs360weighttracker.data.DatabaseHelper;
import com.example.cs360weighttracker.data.TestDatabaseHelper;
import com.example.cs360weighttracker.data.WeightEntry;

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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class WeightHistoryActivityTest {

    private TestDatabaseHelper dbHelper;
    private int testUserId;
    private static final String TEST_DATE = "2026-01-15";
    private static final double TEST_WEIGHT = 165.5;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        dbHelper = new TestDatabaseHelper(context);

        // Clean up leftovers
        dbHelper.testDeleteUserByUsername("testuser");

        // Create test user
        dbHelper.createUser("testuser", "testpass", 150.0, null);
        testUserId = dbHelper.loginUser("testuser", "testpass");
        assertTrue("Test user must be created", testUserId != -1);
    }

    @After
    public void tearDown() {
        if (dbHelper != null) {
            try {
                dbHelper.testDeleteWeightsByUserId(testUserId);
                dbHelper.testDeleteUserById(testUserId);
            } finally {
                dbHelper.close();
            }
        }
    }

    private void waitForDestroy(ActivityScenario<?> scenario) throws InterruptedException {
        Thread.sleep(1000);
        assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
    }

    private Intent createIntent(int userId) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, WeightHistoryActivity.class);
        intent.putExtra("userId", userId);
        return intent;
    }

    // --- Happy path ---

    @Test
    public void onCreate_validUserId_displaysAllElements() {
        try (ActivityScenario<WeightHistoryActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.rvWeightEntries)).check(matches(isDisplayed()));
            onView(withId(R.id.fabAddWeight)).check(matches(isDisplayed()));
            onView(withId(R.id.btnProfile)).check(matches(isDisplayed()));
            onView(withId(R.id.tvPageTitle)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void onCreate_withWeightEntry_displaysEntryInList() {
        dbHelper.addWeight(testUserId, TEST_DATE, TEST_WEIGHT);

        try (ActivityScenario<WeightHistoryActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withText(TEST_DATE)).check(matches(isDisplayed()));
            onView(withText("165.5 lbs")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void onCreate_withMultipleEntries_displaysAll() {
        dbHelper.addWeight(testUserId, "2026-01-15", 165.5);
        dbHelper.addWeight(testUserId, "2026-01-16", 164.0);

        try (ActivityScenario<WeightHistoryActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withText("2026-01-15")).check(matches(isDisplayed()));
            onView(withText("165.5 lbs")).check(matches(isDisplayed()));
            onView(withText("2026-01-16")).check(matches(isDisplayed()));
            onView(withText("164.0 lbs")).check(matches(isDisplayed()));
        }
    }

    // --- Failure cases ---

    @Test
    public void onCreate_invalidUserId_finishesActivity() throws InterruptedException {
        try (ActivityScenario<WeightHistoryActivity> scenario =
                     ActivityScenario.launch(createIntent(-1))) {
            waitForDestroy(scenario);
        }
    }

    @Test
    public void onCreate_missingUserIdExtra_finishesActivity() throws InterruptedException {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, WeightHistoryActivity.class);
        // No userId extra — defaults to -1

        try (ActivityScenario<WeightHistoryActivity> scenario =
                     ActivityScenario.launch(intent)) {
            waitForDestroy(scenario);
        }
    }

    // --- Edge cases ---

    @Test
    public void onCreate_noWeightEntries_displaysEmptyList() {
        try (ActivityScenario<WeightHistoryActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            // RecyclerView should be visible but empty
            onView(withId(R.id.rvWeightEntries)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void onResume_afterAddingEntry_refreshesList() {
        try (ActivityScenario<WeightHistoryActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            // List starts empty; add an entry directly to DB
            dbHelper.addWeight(testUserId, TEST_DATE, TEST_WEIGHT);

            // Simulate returning to activity (triggers onResume → loadWeights)
            scenario.onActivity(activity -> {
                // Force onResume reload
            });
            scenario.moveToState(Lifecycle.State.STARTED);
            scenario.moveToState(Lifecycle.State.RESUMED);

            onView(withText(TEST_DATE)).check(matches(isDisplayed()));
            onView(withText("165.5 lbs")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void deleteButton_clicked_removesEntryFromDb() {
        dbHelper.addWeight(testUserId, TEST_DATE, TEST_WEIGHT);

        try (ActivityScenario<WeightHistoryActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            // Click the delete button on the first item
            onView(withId(R.id.btnDelete)).perform(click());

            // Verify entry was removed from database
            List<WeightEntry> entries = dbHelper.getWeights(testUserId);
            assertEquals("Weight entry should be deleted", 0, entries.size());
        }
    }

    @Test
    public void fabAddWeight_clicked_doesNotFinishActivity() {
        try (ActivityScenario<WeightHistoryActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.fabAddWeight)).perform(click());

            // WeightHistoryActivity should still be alive (AddWeightActivity launched on top)
            assertNotEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    @Test
    public void btnProfile_clicked_doesNotFinishActivity() {
        try (ActivityScenario<WeightHistoryActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.btnProfile)).perform(click());

            // WeightHistoryActivity should still be alive (ProfileActivity launched on top)
            assertNotEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }
}
