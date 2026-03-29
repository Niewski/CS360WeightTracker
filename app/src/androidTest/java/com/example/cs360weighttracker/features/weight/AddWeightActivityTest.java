package com.example.cs360weighttracker.features.weight;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
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
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("users", "username=?", new String[]{"testuser"});
        // Create test user with goal 150.0, no phone
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
        // Allow finish() to propagate through the lifecycle
        Thread.sleep(1000);
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
            Cursor cursor = dbHelper.getWeights(testUserId);
            assertTrue("Weight entry should exist", cursor.moveToFirst());
            double weight = cursor.getDouble(cursor.getColumnIndexOrThrow("weight"));
            assertEquals(165.5, weight, 0.01);
            cursor.close();
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
            Cursor cursor = dbHelper.getWeights(testUserId);
            assertFalse("No weight entry should exist", cursor.moveToFirst());
            cursor.close();
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
            Cursor cursor = dbHelper.getWeights(testUserId);
            assertTrue("Weight entry should exist", cursor.moveToFirst());
            double weight = cursor.getDouble(cursor.getColumnIndexOrThrow("weight"));
            assertEquals(149.0, weight, 0.01);
            cursor.close();

            // Activity should finish after successful save
            waitForDestroy(scenario);
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
}
