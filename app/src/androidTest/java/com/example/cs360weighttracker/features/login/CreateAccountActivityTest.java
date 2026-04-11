package com.example.cs360weighttracker.features.login;

import android.content.Context;

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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class CreateAccountActivityTest {

    private DatabaseHelper dbHelper;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        dbHelper = new DatabaseHelper(context);
        // Clean up any leftovers from previous test runs
        dbHelper.deleteUserByUsername("testuser");
    }

    @After
    public void tearDown() {
        dbHelper.deleteUserByUsername("testuser");
    }

    private void waitForDestroy(ActivityScenario<?> scenario) throws InterruptedException {
        Thread.sleep(1000);
        assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
    }

    // --- Happy path ---

    @Test
    public void onCreate_displaysAllFormElements() {
        try (ActivityScenario<CreateAccountActivity> scenario =
                     ActivityScenario.launch(CreateAccountActivity.class)) {
            onView(withId(R.id.etUsername)).check(matches(isDisplayed()));
            onView(withId(R.id.etPassword)).check(matches(isDisplayed()));
            onView(withId(R.id.etGoalWeight)).check(matches(isDisplayed()));
            onView(withId(R.id.etPhoneNumber)).check(matches(isDisplayed()));
            onView(withId(R.id.btnCreateAccount)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void btnCreate_validInputNoPhone_createsAccountAndFinishes() throws InterruptedException {
        try (ActivityScenario<CreateAccountActivity> scenario =
                     ActivityScenario.launch(CreateAccountActivity.class)) {
            onView(withId(R.id.etUsername))
                    .perform(typeText("testuser"), closeSoftKeyboard());
            onView(withId(R.id.etPassword))
                    .perform(typeText("testpass"), closeSoftKeyboard());
            onView(withId(R.id.etGoalWeight))
                    .perform(typeText("150.0"), closeSoftKeyboard());
            onView(withId(R.id.btnCreateAccount)).perform(click());

            waitForDestroy(scenario);
        }

        // Verify user was actually persisted in the database
        int userId = dbHelper.loginUser("testuser", "testpass");
        assertTrue("User should exist in DB after account creation", userId != -1);
    }

    @Test
    public void btnCreate_validInputWithPhone_createsAccountAndFinishes() throws InterruptedException {
        try (ActivityScenario<CreateAccountActivity> scenario =
                     ActivityScenario.launch(CreateAccountActivity.class)) {
            onView(withId(R.id.etUsername))
                    .perform(typeText("testuser"), closeSoftKeyboard());
            onView(withId(R.id.etPassword))
                    .perform(typeText("testpass"), closeSoftKeyboard());
            onView(withId(R.id.etGoalWeight))
                    .perform(typeText("150.0"), closeSoftKeyboard());
            onView(withId(R.id.etPhoneNumber))
                    .perform(typeText("5551234567"), closeSoftKeyboard());
            onView(withId(R.id.btnCreateAccount)).perform(click());

            // SMS permission dialog may appear; activity finishes after handling it
            // Allow extra time for the permission flow
            Thread.sleep(2000);
        }

        int userId = dbHelper.loginUser("testuser", "testpass");
        assertTrue("User should exist in DB after account creation with phone", userId != -1);
    }

    @Test
    public void btnCreate_validInput_persistsGoalWeight() {
        try (ActivityScenario<CreateAccountActivity> scenario =
                     ActivityScenario.launch(CreateAccountActivity.class)) {
            onView(withId(R.id.etUsername))
                    .perform(typeText("testuser"), closeSoftKeyboard());
            onView(withId(R.id.etPassword))
                    .perform(typeText("testpass"), closeSoftKeyboard());
            onView(withId(R.id.etGoalWeight))
                    .perform(typeText("150.0"), closeSoftKeyboard());
            onView(withId(R.id.btnCreateAccount)).perform(click());

            // Allow the async observer callback to complete
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }

        int userId = dbHelper.loginUser("testuser", "testpass");
        assertTrue("User should exist", userId != -1);
        assertEquals(150.0, dbHelper.getGoalWeight(userId), 0.01);
    }

    // --- Failure cases ---

    @Test
    public void btnCreate_emptyUsername_doesNotFinish() {
        try (ActivityScenario<CreateAccountActivity> scenario =
                     ActivityScenario.launch(CreateAccountActivity.class)) {
            onView(withId(R.id.etPassword))
                    .perform(typeText("testpass"), closeSoftKeyboard());
            onView(withId(R.id.etGoalWeight))
                    .perform(typeText("150.0"), closeSoftKeyboard());
            onView(withId(R.id.btnCreateAccount)).perform(click());

            // Activity should stay open on validation failure
            assertNotEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    @Test
    public void btnCreate_emptyPassword_doesNotFinish() {
        try (ActivityScenario<CreateAccountActivity> scenario =
                     ActivityScenario.launch(CreateAccountActivity.class)) {
            onView(withId(R.id.etUsername))
                    .perform(typeText("testuser"), closeSoftKeyboard());
            onView(withId(R.id.etGoalWeight))
                    .perform(typeText("150.0"), closeSoftKeyboard());
            onView(withId(R.id.btnCreateAccount)).perform(click());

            assertNotEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    @Test
    public void btnCreate_emptyGoalWeight_doesNotFinish() {
        try (ActivityScenario<CreateAccountActivity> scenario =
                     ActivityScenario.launch(CreateAccountActivity.class)) {
            onView(withId(R.id.etUsername))
                    .perform(typeText("testuser"), closeSoftKeyboard());
            onView(withId(R.id.etPassword))
                    .perform(typeText("testpass"), closeSoftKeyboard());
            onView(withId(R.id.btnCreateAccount)).perform(click());

            assertNotEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    @Test
    public void btnCreate_allFieldsEmpty_doesNotFinish() {
        try (ActivityScenario<CreateAccountActivity> scenario =
                     ActivityScenario.launch(CreateAccountActivity.class)) {
            onView(withId(R.id.btnCreateAccount)).perform(click());

            assertNotEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    @Test
    public void btnCreate_emptyFields_doesNotCreateUser() {
        try (ActivityScenario<CreateAccountActivity> scenario =
                     ActivityScenario.launch(CreateAccountActivity.class)) {
            onView(withId(R.id.btnCreateAccount)).perform(click());

            // Allow callback to fire
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }

        int userId = dbHelper.loginUser("testuser", "testpass");
        assertEquals("No user should be created with empty fields", -1, userId);
    }

    @Test
    public void btnCreate_duplicateUsername_doesNotFinish() throws InterruptedException {
        // Pre-create the user
        dbHelper.createUser("testuser", "testpass", 150.0, null);

        try (ActivityScenario<CreateAccountActivity> scenario =
                     ActivityScenario.launch(CreateAccountActivity.class)) {
            onView(withId(R.id.etUsername))
                    .perform(typeText("testuser"), closeSoftKeyboard());
            onView(withId(R.id.etPassword))
                    .perform(typeText("testpass"), closeSoftKeyboard());
            onView(withId(R.id.etGoalWeight))
                    .perform(typeText("150.0"), closeSoftKeyboard());
            onView(withId(R.id.btnCreateAccount)).perform(click());

            // Allow observer to fire
            Thread.sleep(500);

            // Activity should remain open — duplicate username error
            assertNotEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    // --- Edge cases ---

    @Test
    public void btnCreate_invalidGoalWeight_doesNotFinish() {
        try (ActivityScenario<CreateAccountActivity> scenario =
                     ActivityScenario.launch(CreateAccountActivity.class)) {
            onView(withId(R.id.etUsername))
                    .perform(typeText("testuser"), closeSoftKeyboard());
            onView(withId(R.id.etPassword))
                    .perform(typeText("testpass"), closeSoftKeyboard());
            onView(withId(R.id.etGoalWeight))
                    .perform(typeText("abc"), closeSoftKeyboard());
            onView(withId(R.id.btnCreateAccount)).perform(click());

            assertNotEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    @Test
    public void btnCreate_invalidGoalWeight_doesNotCreateUser() {
        try (ActivityScenario<CreateAccountActivity> scenario =
                     ActivityScenario.launch(CreateAccountActivity.class)) {
            onView(withId(R.id.etUsername))
                    .perform(typeText("testuser"), closeSoftKeyboard());
            onView(withId(R.id.etPassword))
                    .perform(typeText("testpass"), closeSoftKeyboard());
            onView(withId(R.id.etGoalWeight))
                    .perform(typeText("abc"), closeSoftKeyboard());
            onView(withId(R.id.btnCreateAccount)).perform(click());

            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }

        int userId = dbHelper.loginUser("testuser", "testpass");
        assertEquals("No user should be created with invalid goal weight", -1, userId);
    }
}
