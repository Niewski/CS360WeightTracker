package com.example.cs360weighttracker.features.profile;

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
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ProfileActivityTest {

    private DatabaseHelper dbHelper;
    private int testUserId;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("users", "username=?", new String[]{"testuser"});
        dbHelper.createUser("testuser", "testpass", 150.0, "5551234567");
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
        Intent intent = new Intent(context, ProfileActivity.class);
        intent.putExtra("userId", userId);
        return intent;
    }

    // --- Happy path ---

    @Test
    public void onCreate_validUserId_displaysAllFormElements() {
        try (ActivityScenario<ProfileActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.etUsername)).check(matches(isDisplayed()));
            onView(withId(R.id.etGoalWeight)).check(matches(isDisplayed()));
            onView(withId(R.id.etPhoneNumber)).check(matches(isDisplayed()));
            onView(withId(R.id.btnSaveProfile)).check(matches(isDisplayed()));
            onView(withId(R.id.btnCancel)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void onCreate_validUserId_populatesUsername() {
        try (ActivityScenario<ProfileActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.etUsername)).check(matches(withText("testuser")));
        }
    }

    @Test
    public void onCreate_validUserId_populatesGoalWeight() {
        try (ActivityScenario<ProfileActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.etGoalWeight)).check(matches(withText(containsString("150"))));
        }
    }

    @Test
    public void onCreate_validUserId_populatesPhoneNumber() {
        try (ActivityScenario<ProfileActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.etPhoneNumber)).check(matches(withText("5551234567")));
        }
    }

    @Test
    public void btnSave_validGoalWeight_finishesActivity() throws InterruptedException {
        try (ActivityScenario<ProfileActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.etGoalWeight))
                    .perform(clearText(), typeText("160.0"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveProfile)).perform(click());

            waitForDestroy(scenario);
        }
    }

    @Test
    public void btnSave_validGoalWeight_persistsChange() throws InterruptedException {
        try (ActivityScenario<ProfileActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.etGoalWeight))
                    .perform(clearText(), typeText("175.5"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveProfile)).perform(click());

            Thread.sleep(1000);
        }

        assertEquals(175.5, dbHelper.getGoalWeight(testUserId), 0.01);
    }

    // --- Failure cases ---

    @Test
    public void onCreate_invalidUserId_finishesActivity() throws InterruptedException {
        try (ActivityScenario<ProfileActivity> scenario =
                     ActivityScenario.launch(createIntent(-1))) {
            waitForDestroy(scenario);
        }
    }

    @Test
    public void btnSave_emptyGoalWeight_doesNotFinish() throws InterruptedException {
        try (ActivityScenario<ProfileActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.etGoalWeight))
                    .perform(clearText(), closeSoftKeyboard());
            onView(withId(R.id.btnSaveProfile)).perform(click());

            Thread.sleep(500);
            assertNotEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    @Test
    public void btnSave_emptyGoalWeight_doesNotChangeGoal() throws InterruptedException {
        try (ActivityScenario<ProfileActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.etGoalWeight))
                    .perform(clearText(), closeSoftKeyboard());
            onView(withId(R.id.btnSaveProfile)).perform(click());

            Thread.sleep(500);
        }

        // Goal should remain unchanged
        assertEquals(150.0, dbHelper.getGoalWeight(testUserId), 0.01);
    }

    // --- Edge cases ---

    @Test
    public void btnCancel_clicked_finishesActivity() throws InterruptedException {
        try (ActivityScenario<ProfileActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.btnCancel)).perform(click());

            waitForDestroy(scenario);
        }
    }

    @Test
    public void btnCancel_afterEditing_doesNotPersistChange() throws InterruptedException {
        try (ActivityScenario<ProfileActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.etGoalWeight))
                    .perform(clearText(), typeText("999.0"), closeSoftKeyboard());
            onView(withId(R.id.btnCancel)).perform(click());

            Thread.sleep(2000);
        }

        // Goal should remain unchanged
        assertEquals(150.0, dbHelper.getGoalWeight(testUserId), 0.01);
    }

    @Test
    public void btnSave_updatesPhoneNumber_persistsChange() throws InterruptedException {
        try (ActivityScenario<ProfileActivity> scenario =
                     ActivityScenario.launch(createIntent(testUserId))) {
            onView(withId(R.id.etPhoneNumber))
                    .perform(clearText(), typeText("5559999999"), closeSoftKeyboard());
            onView(withId(R.id.btnSaveProfile)).perform(click());

            Thread.sleep(1000);
        }

        assertEquals("5559999999", dbHelper.getPhoneNumber(testUserId));
    }
}
