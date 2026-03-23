package com.example.cs360weighttracker.data;

import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

public class WeightRepository {

    private final DatabaseHelper dbHelper;

    public WeightRepository(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // --- Weight operations ---

    public List<WeightEntry> getWeights(int userId) {
        List<WeightEntry> list = new ArrayList<>();
        Cursor cursor = dbHelper.getWeights(userId);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                double weight = cursor.getDouble(cursor.getColumnIndexOrThrow("weight"));
                list.add(new WeightEntry(id, date, weight));
            }
            cursor.close();
        }
        return list;
    }

    public boolean addWeight(int userId, String date, double weight) {
        return dbHelper.addWeight(userId, date, weight);
    }

    public boolean updateWeight(int weightId, String date, double weight) {
        return dbHelper.updateWeight(weightId, date, weight);
    }

    public boolean deleteWeight(int weightId) {
        return dbHelper.deleteWeight(weightId);
    }
}
