package com.example.cs360weighttracker.features.weight;

import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.cs360weighttracker.R;
import com.example.cs360weighttracker.data.DatabaseHelper;
import com.example.cs360weighttracker.data.WeightEntry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

@RunWith(AndroidJUnit4.class)
public class AddWeightActivityTest {

    private DatabaseHelper dbHelper;
    private int testUserId;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        dbHelper = new DatabaseHelper(context);
        // Clean up any leftovers from previous test runs
        dbHelper.deleteUserByUsername("testuser");
        // Create test user with goal 150.0, no phone
        dbHelper.createUser("testuser", "testpass", 150.0, null);
        testUserId = dbHelper.loginUser("testuser", "testpass");
        assertTrue("Test user must be created", testUserId != -1);
    }

    @After
    public void tearDown() {
        dbHelper.deleteWeightsByUserId(testUserId);
        dbHelper.deleteUserById(testUserId);
    }

    private void waitForDestroy(ActivityScenario<?> scenario) throws InterruptedException {
        // Poll for DESTROYED state with timeout (emulator can be slow)
        long deadline = System.currentTimeMillis() + 5000;
        while (scenario.getState() != Lifecycle.State.DESTROYED
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }
        assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
    }

    private Intent createIntent(int userId) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, AddWeightActivity.class);
        intent.putExtra("userId", userId);
        return intent;
    }

    // --- Happy path ---

    @Test
    public void onCreate_validUserId_displaysAllFormElements() {
        try (ActivityScenario<AddWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.etDate)).check(matches(isDisplayed()));
            onView(withId(R.id.etWeight)).check(matches(isDisplayed()));
            onView(withId(R.id.btnSaveWeight)).check(matches(isDisplayed()));
            onView(withId(R.id.btnCancel)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void onCreate_validUserId_defaultDateIsToday() {
        Calendar calendar = Calendar.getInstance();
        String expected = String.format(Locale.US, "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));

        try (ActivityScenario<AddWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.etDate)).check(matches(withText(expected)));
        }
    }

    @Test
    public void btnSave_validInput_finishesActivity() throws InterruptedException {
        try (ActivityScenario<AddWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.etWeight))
                    .perform(typeText("165.5"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveWeight)).perform(click());

            waitForDestroy(scenario);
        }
    }

    @Test
    public void btnSave_validInput_insertsWeightInDb() {
        try (ActivityScenario<AddWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.etWeight))
                    .perform(typeText("165.5"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveWeight)).perform(click());

            // Verify weight was persisted
            List<WeightEntry> entries = dbHelper.getWeights(testUserId);
            assertFalse("Weight entry should exist", entries.isEmpty());
            assertEquals(165.5, entries.get(0).weight, 0.01);
        }
    }

    // --- Failure cases ---

    @Test
    public void btnSave_emptyWeight_doesNotInsertWeight() {
        try (ActivityScenario<AddWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            // Date is pre-filled; weight is left empty
            onView(withId(R.id.btnSaveWeight)).perform(click());

            // No weight should have been inserted
            List<WeightEntry> entries = dbHelper.getWeights(testUserId);
            assertTrue("No weight entry should exist", entries.isEmpty());
        }
    }

    @Test
    public void btnSave_emptyWeight_doesNotFinishActivity() {
        try (ActivityScenario<AddWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.btnSaveWeight)).perform(click());

            assertNotEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    @Test
    public void onCreate_invalidUserId_finishesActivity() throws InterruptedException {
        try (ActivityScenario<AddWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(-1))) {
            waitForDestroy(scenario);
        }
    }

    // --- Edge cases ---

    @Test
    public void btnCancel_clicked_finishesActivity() throws InterruptedException {
        try (ActivityScenario<AddWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.btnCancel)).perform(click());

            waitForDestroy(scenario);
        }
    }

    @Test
    public void btnSave_weightBelowGoal_savesAndFinishes() throws InterruptedException {
        // User goal is 150.0, enter weight below goal → goal reached, save succeeds
        try (ActivityScenario<AddWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.etWeight))
                    .perform(typeText("149.0"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveWeight)).perform(click());

            // Weight should be persisted
            List<WeightEntry> entries = dbHelper.getWeights(testUserId);
            assertFalse("Weight entry should exist", entries.isEmpty());
            assertEquals(149.0, entries.get(0).weight, 0.01);

            // Activity should finish after successful save
            waitForDestroy(scenario);
        }
    }

    @Test
    public void btnSave_weightBelowGoal_noPhone_marksGoalSent() throws InterruptedException {
        // User has no phone and goal 150.0; weight 149.0 triggers goal, flag should be set
        try (ActivityScenario<AddWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.etWeight))
                    .perform(typeText("149.0"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveWeight)).perform(click());

            waitForDestroy(scenario);

            // goal_reached_sent flag should now be set in the DB
            assertTrue("Goal sent flag should be set", dbHelper.isGoalSmsSent(testUserId));
        }
    }

    @Test
    public void btnSave_weightAboveGoal_doesNotMarkGoalSent() throws InterruptedException {
        // Weight 165.5 > goal 150.0 → goal not reached, flag stays unset
        try (ActivityScenario<AddWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.etWeight))
                    .perform(typeText("165.5"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveWeight)).perform(click());

            waitForDestroy(scenario);

            assertFalse("Goal sent flag should not be set", dbHelper.isGoalSmsSent(testUserId));
        }
    }

    @Test
    public void btnSave_weightAboveGoal_doesNotTriggerGoalEvent() throws InterruptedException {
        // Weight 165.5 > goal 150.0 → no congrats toast, just success
        try (ActivityScenario<AddWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.etWeight))
                    .perform(typeText("165.5"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveWeight)).perform(click());

            // Activity should finish (weight saved successfully)
            waitForDestroy(scenario);
        }
    }

    // --- DatePicker tests ---

    @Test
    public void etDate_clicked_opensDatePicker() throws InterruptedException {
        try (ActivityScenario<AddWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.etDate)).perform(click());
            Thread.sleep(500);

            // DatePickerDialog should be visible
            onView(withClassName(equalTo("android.widget.DatePicker")))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void etDate_datePickerConfirmed_retainsTodayDate() throws InterruptedException {
        Calendar calendar = Calendar.getInstance();
        String expected = String.format(Locale.US, "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));

        try (ActivityScenario<AddWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            // Open the picker and confirm without changing date
            onView(withId(R.id.etDate)).perform(click());
            Thread.sleep(500);
            onView(withId(android.R.id.button1)).perform(click());

            // Field should still show today's date
            onView(withId(R.id.etDate)).check(matches(withText(expected)));
        }
    }

    @Test
    public void etDate_datePickerCancelled_retainsOriginalDate() throws InterruptedException {
        Calendar calendar = Calendar.getInstance();
        String expected = String.format(Locale.US, "%04d-%02d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH));

        try (ActivityScenario<AddWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.etDate)).perform(click());
            Thread.sleep(500);
            onView(withId(android.R.id.button2)).perform(click());

            // Field should still show original date
            onView(withId(R.id.etDate)).check(matches(withText(expected)));
        }
    }
}
