package com.example.cs360weighttracker.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "weight_tracker.db";
    private static final int DATABASE_VERSION = 5;

    // Table names
    private static final String TABLE_USERS = "users";
    private static final String TABLE_WEIGHTS = "weights";

    private boolean fts5Available = true;

    // Constructor
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private void createFts5Table(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS weights_fts USING fts5("
                + "notes, content=" + TABLE_WEIGHTS + ", content_rowid=id)");
        } catch (Exception e) {
            Log.w(TAG, "FTS5 module not available, full-text search disabled", e);
            fts5Available = false;
        }
    }

    private void createFts5Triggers(SQLiteDatabase db) {
        if (!fts5Available) return;
        try {
            db.execSQL("CREATE TRIGGER IF NOT EXISTS weights_ai AFTER INSERT ON " + TABLE_WEIGHTS
                + " BEGIN INSERT INTO weights_fts(rowid, notes) VALUES (new.id, new.notes); END;");

            db.execSQL("CREATE TRIGGER IF NOT EXISTS weights_ad AFTER DELETE ON " + TABLE_WEIGHTS
                + " BEGIN INSERT INTO weights_fts(weights_fts, rowid, notes) "
                + "VALUES('delete', old.id, old.notes); END;");

            db.execSQL("CREATE TRIGGER IF NOT EXISTS weights_au AFTER UPDATE ON " + TABLE_WEIGHTS
                + " BEGIN INSERT INTO weights_fts(weights_fts, rowid, notes) "
                + "VALUES('delete', old.id, old.notes); "
                + "INSERT INTO weights_fts(rowid, notes) VALUES (new.id, new.notes); END;");
        } catch (Exception e) {
            Log.w(TAG, "Failed to create FTS5 triggers", e);
            fts5Available = false;
        }
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

        // Create Weights Table (with notes column)
        String createWeightsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_WEIGHTS + " (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "userId INTEGER, " +
            "date TEXT, " +
            "weight REAL, " +
            "notes TEXT DEFAULT '')";
        db.execSQL(createWeightsTable);

        // Compound index for efficient user+date lookups
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_weights_user_date ON "
            + TABLE_WEIGHTS + "(userId, date)");

        // FTS5 virtual table for full-text search on weight notes
        createFts5Table(db);

        // Triggers to keep FTS5 in sync
        createFts5Triggers(db);
    }

    // onUpgrade is called when DATABASE_VERSION is incremented
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // Legacy path — drop and recreate
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_WEIGHTS);
            onCreate(db);
            return;
        }
        if (oldVersion < 4) {
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_weights_user_date ON "
                + TABLE_WEIGHTS + "(userId, date)");
        }
        if (oldVersion < 5) {
            // Add notes column to weights table if missing
            try {
                db.execSQL("ALTER TABLE " + TABLE_WEIGHTS + " ADD COLUMN notes TEXT DEFAULT ''");
            } catch (Exception ignored) {
            }

            // Create FTS5 table and triggers if not present
            createFts5Table(db);
            createFts5Triggers(db);

            // Populate FTS table from existing content
            try {
                db.execSQL("INSERT INTO weights_fts(rowid, notes) SELECT id, notes FROM " + TABLE_WEIGHTS);
            } catch (Exception ignored) {
            }
        }
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
        return addWeight(userId, date, weight, "");
    }

    public boolean addWeight(int userId, String date, double weight, String notes) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("userId", userId);
        values.put("date", date);
        values.put("weight", weight);
        values.put("notes", notes != null ? notes : "");
        return db.insert(TABLE_WEIGHTS, null, values) != -1;
    }

    public Cursor getWeights(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(
                "SELECT * FROM " + TABLE_WEIGHTS + " WHERE userId=? ORDER BY date DESC",
                new String[]{String.valueOf(userId)}
        );
    }

    public Cursor getWeightsInRange(int userId, String startDate, String endDate) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(
            "SELECT * FROM " + TABLE_WEIGHTS + " WHERE userId=? AND date BETWEEN ? AND ? ORDER BY date DESC",
            new String[]{String.valueOf(userId), startDate, endDate}
        );
    }

    public Cursor searchWeightNotes(int userId, String query) {
        SQLiteDatabase db = getReadableDatabase();
        if (fts5Available) {
            return db.rawQuery(
                "SELECT w.* FROM " + TABLE_WEIGHTS + " w JOIN weights_fts f ON f.rowid = w.id "
                    + "WHERE f.notes MATCH ? AND w.userId=? ORDER BY w.date DESC",
                new String[]{query, String.valueOf(userId)}
            );
        }
        // Fallback to LIKE when FTS5 is not available
        return db.rawQuery(
            "SELECT * FROM " + TABLE_WEIGHTS + " WHERE userId=? AND notes LIKE ? ORDER BY date DESC",
            new String[]{String.valueOf(userId), "%" + query + "%"}
        );
    }

    public Cursor getWeeklyAverages(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(
            "SELECT strftime('%Y-W%W', date) AS period, AVG(weight) as average, COUNT(*) as entryCount "
                + "FROM " + TABLE_WEIGHTS + " WHERE userId=? GROUP BY period ORDER BY period DESC",
            new String[]{String.valueOf(userId)}
        );
    }

    public Cursor getMonthlyAverages(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        return db.rawQuery(
            "SELECT strftime('%Y-%m', date) AS period, AVG(weight) as average, COUNT(*) as entryCount "
                + "FROM " + TABLE_WEIGHTS + " WHERE userId=? GROUP BY period ORDER BY period DESC",
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

    public boolean updateWeight(int weightId, String date, double weight, String notes) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("date", date);
        values.put("weight", weight);
        values.put("notes", notes != null ? notes : "");
        int rows = db.update(TABLE_WEIGHTS, values, "id=?", new String[]{String.valueOf(weightId)});
        return rows > 0;
    }

    public boolean updateWeightNotes(int weightId, String notes) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("notes", notes != null ? notes : "");
        int rows = db.update(TABLE_WEIGHTS, values, "id=?", new String[]{String.valueOf(weightId)});
        return rows > 0;
    }

    public int bulkAddWeights(int userId, java.util.List<String[]> rows) {
        SQLiteDatabase db = getWritableDatabase();
        int imported = 0;
        db.beginTransaction();
        try {
            for (String[] row : rows) {
                // expected: {date, weight, notes}
                if (row == null || row.length < 2) continue;
                String date = row[0];
                String weightStr = row[1];
                String notes = row.length > 2 ? row[2] : "";
                double weight;
                try {
                    weight = Double.parseDouble(weightStr);
                } catch (NumberFormatException ex) {
                    continue;
                }
                ContentValues values = new ContentValues();
                values.put("userId", userId);
                values.put("date", date);
                values.put("weight", weight);
                values.put("notes", notes != null ? notes : "");
                long id = db.insert(TABLE_WEIGHTS, null, values);
                if (id != -1) imported++;
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return imported;
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


