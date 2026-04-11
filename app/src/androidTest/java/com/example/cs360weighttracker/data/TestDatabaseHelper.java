package com.example.cs360weighttracker.data;

import android.content.Context;

/**
 * Test-only wrapper that exposes package-private DatabaseHelper test helpers
 * to instrumented tests in other packages.
 */
public class TestDatabaseHelper extends DatabaseHelper {

    public TestDatabaseHelper(Context context) {
        super(context);
    }

    public void testDeleteUserByUsername(String username) {
        deleteUserByUsername(username);
    }

    public void testDeleteWeightsByUserId(int userId) {
        deleteWeightsByUserId(userId);
    }

    public void testDeleteUserById(int userId) {
        deleteUserById(userId);
    }
}
