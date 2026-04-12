package com.example.cs360weighttracker.data;

/**
 * Immutable snapshot of a user's profile fields.
 *
 * <p>Returned by {@link DatabaseHelper#getUserProfile(int)} and
 * consumed by the profile UI.</p>
 */
public class UserProfile {
    /** Display username (unique). */
    public final String username;
    /** Target goal weight in lbs. */
    public final double goalWeight;
    /** Optional phone number for SMS notifications (may be {@code null}). */
    public final String phoneNumber;

    /**
     * @param username    display name
     * @param goalWeight  goal weight in lbs
     * @param phoneNumber phone number or {@code null}
     */
    public UserProfile(String username, double goalWeight, String phoneNumber) {
        this.username = username;
        this.goalWeight = goalWeight;
        this.phoneNumber = phoneNumber;
    }
}
