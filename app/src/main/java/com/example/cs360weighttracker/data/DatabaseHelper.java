package com.example.cs360weighttracker.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "weight_tracker.db";
    private static final int DATABASE_VERSION = 3;

    // Table names
    private static final String TABLE_USERS = "users";
    private static final String TABLE_WEIGHTS = "weights";

    // Constructor
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // onCreate is called only once when the database is first created
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Users Table
        String createUsersTable = "CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE, " +
                "password TEXT, " +
                "goal_weight REAL, " +
                "phone_number TEXT, " +
                "goal_reached_sent INTEGER DEFAULT 0)";
        db.execSQL(createUsersTable);

        // Create Weights Table
        String createWeightsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_WEIGHTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "userId INTEGER, " +
                "date TEXT, " +
                "weight REAL)";
        db.execSQL(createWeightsTable);
    }

    // onUpgrade is called when DATABASE_VERSION is incremented
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For simplicity, drop and recreate
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEIGHTS);
        onCreate(db);
    }

    // --- User Logic ---
    public boolean createUser(String username, String password, double goalWeight, String phoneNumber) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("password", password);
        values.put("goal_weight", goalWeight);
        values.put("phone_number", phoneNumber);
        long result = db.insert("users", null, values);
        return result != -1;
    }

    public int loginUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM users WHERE username=? AND password= ?",
                new String[]{username, password});
        int userId = -1;
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
        }
        cursor.close();
        return userId;
    }

    public String getPhoneNumber(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT phone_number FROM users WHERE id=?", new String[]{String.valueOf(userId)});
        String number = "";
        if (cursor.moveToFirst()) {
            number = cursor.getString(cursor.getColumnIndexOrThrow("phone_number"));
        }
        cursor.close();
        return number;
    }

    public boolean isGoalSmsSent(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT goal_reached_sent FROM users WHERE id=?",
                new String[]{String.valueOf(userId)});
        boolean sent = false;
        if (cursor.moveToFirst()) {
            sent = cursor.getInt(cursor.getColumnIndexOrThrow("goal_reached_sent")) == 1;
        }
        cursor.close();
        return sent;
    }

    public void setGoalSmsSent(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("goal_reached_sent", 1);
        db.update("users", values, "id=?", new String[]{String.valueOf(userId)});
    }

    public UserProfile getUserProfile(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT username, goal_weight, phone_number FROM users WHERE id=?",
                new String[]{String.valueOf(userId)});
        UserProfile profile = null;
        if (cursor.moveToFirst()) {
            String username = cursor.getString(cursor.getColumnIndexOrThrow("username"));
            double goalWeight = cursor.getDouble(cursor.getColumnIndexOrThrow("goal_weight"));
            String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone_number"));
            profile = new UserProfile(username, goalWeight, phone);
        }
        cursor.close();
        return profile;
    }

    public boolean updateUser(int userId, double goalWeight, String phoneNumber) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Only reset SMS flag when goal weight actually changes
        double currentGoal = getGoalWeight(userId);

        ContentValues values = new ContentValues();
        values.put("goal_weight", goalWeight);
        values.put("phone_number", phoneNumber);
        if (Double.compare(currentGoal, goalWeight) != 0) {
            values.put("goal_reached_sent", 0);
        }
        int rows = db.update("users", values, "id=?", new String[]{String.valueOf(userId)});
        return rows > 0;
    }

    // --- Weight Logic ---
    public boolean addWeight(int userId, String date, double weight) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("userId", userId);
        values.put("date", date);
        values.put("weight", weight);
        return db.insert(TABLE_WEIGHTS, null, values) != -1;
    }

    public Cursor getWeights(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM " + TABLE_WEIGHTS + " WHERE userId=? ORDER BY date DESC",
                new String[]{String.valueOf(userId)}
        );
    }

    public boolean updateWeight(int weightId, String date, double weight) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("date", date);
        values.put("weight", weight);
        int rows = db.update(TABLE_WEIGHTS, values, "id=?", new String[]{String.valueOf(weightId)});
        return rows > 0;
    }

    public boolean deleteWeight(int weightId) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_WEIGHTS, "id=?", new String[]{String.valueOf(weightId)}) > 0;
    }

    public double getGoalWeight(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT goal_weight FROM users WHERE id=?", new String[]{String.valueOf(userId)});
        double goalWeight = -1;
        if (cursor.moveToFirst()) {
            goalWeight = cursor.getDouble(cursor.getColumnIndexOrThrow("goal_weight"));
        }
        cursor.close();
        return goalWeight;
    }
}


