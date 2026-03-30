package com.example.cs360weighttracker.data;

/**
 * Repository that mediates between {@link DatabaseHelper} and the
 * user-related ViewModels.
 *
 * <p>This thin wrapper currently delegates directly to
 * {@code DatabaseHelper}. It exists to decouple ViewModels from the
 * concrete data source, making future changes (e.g., caching, async
 * operations, or swapping to Room) easier.</p>
 *
 * @see DatabaseHelper
 * @see com.example.cs360weighttracker.features.login.LoginViewModel
 * @see com.example.cs360weighttracker.features.login.CreateAccountViewModel
 * @see com.example.cs360weighttracker.features.profile.ProfileViewModel
 */
public class UserRepository {

    private final DatabaseHelper dbHelper;

    /**
     * @param dbHelper the database helper instance to delegate to
     */
    public UserRepository(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    /**
     * Authenticates credentials and returns the user ID.
     *
     * @param username the login name
     * @param password the password
     * @return user ID on success, or {@code -1} on failure
     * @see DatabaseHelper#loginUser(String, String)
     */
    public int loginUser(String username, String password) {
        return dbHelper.loginUser(username, password);
    }

    /**
     * Creates a new user account.
     *
     * @param username    unique login name
     * @param password    password (stored as plain text)
     * @param goalWeight  target weight in pounds
     * @param phoneNumber optional phone number for SMS; may be {@code null}
     * @return {@code true} if created successfully
     * @see DatabaseHelper#createUser(String, String, double, String)
     */
    public boolean createUser(String username, String password, double goalWeight, String phoneNumber) {
        return dbHelper.createUser(username, password, goalWeight, phoneNumber);
    }

    /**
     * @param userId user's primary-key ID
     * @return goal weight in pounds, or {@code -1} if user not found
     * @see DatabaseHelper#getGoalWeight(int)
     */
    public double getGoalWeight(int userId) {
        return dbHelper.getGoalWeight(userId);
    }

    /**
     * @param userId user's primary-key ID
     * @return phone number or empty string
     * @see DatabaseHelper#getPhoneNumber(int)
     */
    public String getPhoneNumber(int userId) {
        return dbHelper.getPhoneNumber(userId);
    }

    /**
     * @param userId user's primary-key ID
     * @return {@code true} if the goal-reached SMS was already sent
     * @see DatabaseHelper#isGoalSmsSent(int)
     */
    public boolean isGoalSmsSent(int userId) {
        return dbHelper.isGoalSmsSent(userId);
    }

    /**
     * Marks the goal-reached SMS as sent.
     *
     * @param userId user's primary-key ID
     * @see DatabaseHelper#setGoalSmsSent(int)
     */
    public void setGoalSmsSent(int userId) {
        dbHelper.setGoalSmsSent(userId);
    }

    /**
     * Loads the user's profile data.
     *
     * @param userId user's primary-key ID
     * @return a {@link UserProfile}, or {@code null} if user not found
     * @see DatabaseHelper#getUserProfile(int)
     */
    public UserProfile getUserProfile(int userId) {
        return dbHelper.getUserProfile(userId);
    }

    /**
     * Updates the user's goal weight and phone number.
     *
     * <p>Resets the {@code goal_reached_sent} flag if the goal weight changed.</p>
     *
     * @param userId      user's primary-key ID
     * @param goalWeight  new target weight in pounds
     * @param phoneNumber new phone number (may be {@code null} or empty)
     * @return {@code true} if the row was updated
     * @see DatabaseHelper#updateUser(int, double, String)
     */
    public boolean updateUser(int userId, double goalWeight, String phoneNumber) {
        return dbHelper.updateUser(userId, goalWeight, phoneNumber);
    }
}
