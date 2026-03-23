package com.example.cs360weighttracker.data;

public class UserProfile {
    public final String username;
    public final double goalWeight;
    public final String phoneNumber;

    public UserProfile(String username, double goalWeight, String phoneNumber) {
        this.username = username;
        this.goalWeight = goalWeight;
        this.phoneNumber = phoneNumber;
    }
}
