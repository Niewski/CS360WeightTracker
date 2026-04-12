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
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class EditWeightActivityTest {

    private TestDatabaseHelper dbHelper;
    private int testUserId;
    private int testWeightId;
    private static final String TEST_DATE = "2026-01-15";
    private static final double TEST_WEIGHT = 165.5;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        dbHelper = new TestDatabaseHelper(context);

        // Clean up leftovers
        dbHelper.testDeleteUserByUsername("testuser");

        // Create test user and weight entry
        dbHelper.createUser("testuser", "testpass", 150.0, null);
        testUserId = dbHelper.loginUser("testuser", "testpass");
        assertTrue("Test user must be created", testUserId != -1);

        dbHelper.addWeight(testUserId, TEST_DATE, TEST_WEIGHT);

        // Retrieve the weight entry ID
        List<WeightEntry> entries = dbHelper.getWeights(testUserId);
        assertTrue("Test weight must exist", !entries.isEmpty());
        testWeightId = entries.get(0).id;
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
        // Poll for DESTROYED state with timeout (emulator can be slow)
        long deadline = System.currentTimeMillis() + 5000;
        while (scenario.getState() != Lifecycle.State.DESTROYED
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }
        assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
    }

    private Intent createIntent(int userId, int weightId, String date, double weight) {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent intent = new Intent(context, EditWeightActivity.class);
        intent.putExtra("userId", userId);
        intent.putExtra("weightId", weightId);
        intent.putExtra("date", date);
        intent.putExtra("weight", weight);
        return intent;
    }

    // --- Happy path ---

    @Test
    public void onCreate_validExtras_displaysAllFormElements() {
        try (ActivityScenario<EditWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId, testWeightId, TEST_DATE, TEST_WEIGHT))) {
            onView(withId(R.id.etDate)).check(matches(isDisplayed()));
            onView(withId(R.id.etWeight)).check(matches(isDisplayed()));
            onView(withId(R.id.btnUpdateWeight)).check(matches(isDisplayed()));
            onView(withId(R.id.btnCancel)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void onCreate_validExtras_prePopulatesDate() {
        try (ActivityScenario<EditWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId, testWeightId, TEST_DATE, TEST_WEIGHT))) {
            onView(withId(R.id.etDate)).check(matches(withText(TEST_DATE)));
        }
    }

    @Test
    public void onCreate_validExtras_prePopulatesWeight() {
        try (ActivityScenario<EditWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId, testWeightId, TEST_DATE, TEST_WEIGHT))) {
            onView(withId(R.id.etWeight)).check(matches(withText("165.5")));
        }
    }

    @Test
    public void btnUpdate_validInput_finishesActivity() throws InterruptedException {
        try (ActivityScenario<EditWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId, testWeightId, TEST_DATE, TEST_WEIGHT))) {
            onView(withId(R.id.etWeight))
                    .perform(clearText(), typeText("170.0"), closeSoftKeyboard());
            onView(withId(R.id.btnUpdateWeight)).perform(click());

            waitForDestroy(scenario);
        }
    }

    @Test
    public void btnUpdate_validInput_updatesWeightInDb() {
        try (ActivityScenario<EditWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId, testWeightId, TEST_DATE, TEST_WEIGHT))) {
            onView(withId(R.id.etWeight))
                    .perform(clearText(), typeText("170.0"), closeSoftKeyboard());
            onView(withId(R.id.btnUpdateWeight)).perform(click());

            // Verify the weight was updated in the database
            List<WeightEntry> entries = dbHelper.getWeights(testUserId);
            assertTrue("Weight entry should exist", !entries.isEmpty());
            assertEquals(170.0, entries.get(0).weight, 0.01);
        }
    }

    // --- Failure cases ---

    @Test
    public void btnUpdate_emptyWeight_doesNotFinishActivity() {
        try (ActivityScenario<EditWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId, testWeightId, TEST_DATE, TEST_WEIGHT))) {
            onView(withId(R.id.etWeight)).perform(clearText(), closeSoftKeyboard());
            onView(withId(R.id.btnUpdateWeight)).perform(click());

            assertNotEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    @Test
    public void btnUpdate_emptyWeight_doesNotUpdateDb() {
        try (ActivityScenario<EditWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId, testWeightId, TEST_DATE, TEST_WEIGHT))) {
            onView(withId(R.id.etWeight)).perform(clearText(), closeSoftKeyboard());
            onView(withId(R.id.btnUpdateWeight)).perform(click());

            // Original weight should remain unchanged
            List<WeightEntry> entries = dbHelper.getWeights(testUserId);
            assertTrue("Weight entry should exist", !entries.isEmpty());
            assertEquals(TEST_WEIGHT, entries.get(0).weight, 0.01);
        }
    }

    @Test
    public void onCreate_invalidUserId_finishesActivity() throws InterruptedException {
        try (ActivityScenario<EditWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(-1, testWeightId, TEST_DATE, TEST_WEIGHT))) {
            waitForDestroy(scenario);
        }
    }

    @Test
    public void onCreate_invalidWeightId_finishesActivity() throws InterruptedException {
        try (ActivityScenario<EditWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId, -1, TEST_DATE, TEST_WEIGHT))) {
            waitForDestroy(scenario);
        }
    }

    // --- Edge cases ---

    @Test
    public void btnCancel_clicked_finishesActivity() throws InterruptedException {
        try (ActivityScenario<EditWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId, testWeightId, TEST_DATE, TEST_WEIGHT))) {
            onView(withId(R.id.btnCancel)).perform(click());

            waitForDestroy(scenario);
        }
    }

    @Test
    public void btnCancel_clicked_doesNotUpdateDb() {
        try (ActivityScenario<EditWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId, testWeightId, TEST_DATE, TEST_WEIGHT))) {
            // Change the weight field but cancel
            onView(withId(R.id.etWeight))
                    .perform(clearText(), typeText("999.0"), closeSoftKeyboard());
            onView(withId(R.id.btnCancel)).perform(click());

            // Original weight should remain
            List<WeightEntry> entries = dbHelper.getWeights(testUserId);
            assertTrue("Weight entry should exist", !entries.isEmpty());
            assertEquals(TEST_WEIGHT, entries.get(0).weight, 0.01);
        }
    }

    // --- DatePicker tests ---

    @Test
    public void etDate_clicked_opensDatePicker() throws InterruptedException {
        try (ActivityScenario<EditWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId, testWeightId, TEST_DATE, TEST_WEIGHT))) {
            onView(withId(R.id.etDate)).perform(click());
            Thread.sleep(500);

            // DatePickerDialog should be visible
            onView(withClassName(equalTo("android.widget.DatePicker")))
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void etDate_datePickerConfirmed_retainsExistingDate() throws InterruptedException {
        try (ActivityScenario<EditWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId, testWeightId, TEST_DATE, TEST_WEIGHT))) {
            // Open the picker and confirm without changing date
            onView(withId(R.id.etDate)).perform(click());
            Thread.sleep(500);
            onView(withId(android.R.id.button1)).perform(click());

            // Field should still show the pre-populated date
            onView(withId(R.id.etDate)).check(matches(withText(TEST_DATE)));
        }
    }

    @Test
    public void etDate_datePickerCancelled_retainsExistingDate() throws InterruptedException {
        try (ActivityScenario<EditWeightActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId, testWeightId, TEST_DATE, TEST_WEIGHT))) {
            onView(withId(R.id.etDate)).perform(click());
            Thread.sleep(500);
            onView(withId(android.R.id.button2)).perform(click());

            // Field should still show the original date after cancel
            onView(withId(R.id.etDate)).check(matches(withText(TEST_DATE)));
        }
    }
}
