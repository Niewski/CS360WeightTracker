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
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class EditWeightActivityTest {

    private DatabaseHelper dbHelper;
    private int testUserId;
    private int testWeightId;
    private static final String TEST_DATE = "2026-01-15";
    private static final double TEST_WEIGHT = 165.5;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        dbHelper = new DatabaseHelper(context);

        // Clean up leftovers
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("users", "username=?", new String[]{"testuser"});

        // Create test user and weight entry
        dbHelper.createUser("testuser", "testpass", 150.0, null);
        testUserId = dbHelper.loginUser("testuser", "testpass");
        assertTrue("Test user must be created", testUserId != -1);

        dbHelper.addWeight(testUserId, TEST_DATE, TEST_WEIGHT);

        // Retrieve the weight entry ID
        Cursor cursor = dbHelper.getWeights(testUserId);
        assertTrue("Test weight must exist", cursor.moveToFirst());
        testWeightId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        cursor.close();
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
            Cursor cursor = dbHelper.getWeights(testUserId);
            assertTrue("Weight entry should exist", cursor.moveToFirst());
            double weight = cursor.getDouble(cursor.getColumnIndexOrThrow("weight"));
            assertEquals(170.0, weight, 0.01);
            cursor.close();
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
            Cursor cursor = dbHelper.getWeights(testUserId);
            assertTrue("Weight entry should exist", cursor.moveToFirst());
            double weight = cursor.getDouble(cursor.getColumnIndexOrThrow("weight"));
            assertEquals(TEST_WEIGHT, weight, 0.01);
            cursor.close();
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
            Cursor cursor = dbHelper.getWeights(testUserId);
            assertTrue("Weight entry should exist", cursor.moveToFirst());
            double weight = cursor.getDouble(cursor.getColumnIndexOrThrow("weight"));
            assertEquals(TEST_WEIGHT, weight, 0.01);
            cursor.close();
        }
    }
}
