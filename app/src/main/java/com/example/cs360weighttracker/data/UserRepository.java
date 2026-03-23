package com.example.cs360weighttracker.data;

public class UserRepository {

    private final DatabaseHelper dbHelper;

    public UserRepository(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public int loginUser(String username, String password) {
        return dbHelper.loginUser(username, password);
    }

    public boolean createUser(String username, String password, double goalWeight, String phoneNumber) {
        return dbHelper.createUser(username, password, goalWeight, phoneNumber);
    }

    public double getGoalWeight(int userId) {
        return dbHelper.getGoalWeight(userId);
    }

    public String getPhoneNumber(int userId) {
        return dbHelper.getPhoneNumber(userId);
    }

    public boolean isGoalSmsSent(int userId) {
        return dbHelper.isGoalSmsSent(userId);
    }

    public void setGoalSmsSent(int userId) {
        dbHelper.setGoalSmsSent(userId);
    }

    public UserProfile getUserProfile(int userId) {
        return dbHelper.getUserProfile(userId);
    }

    public boolean updateUser(int userId, double goalWeight, String phoneNumber) {
        return dbHelper.updateUser(userId, goalWeight, phoneNumber);
    }
}
