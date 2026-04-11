package com.example.cs360weighttracker.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import androidx.sqlite.SQLiteConnection;
import androidx.sqlite.SQLiteStatement;
import androidx.sqlite.driver.bundled.BundledSQLiteDriver;

import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper implements Closeable {

    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "weight_tracker.db";
    private static final int DATABASE_VERSION = 7;

    // Table names
    private static final String TABLE_USERS = "users";
    private static final String TABLE_WEIGHTS = "weights";

    private final SQLiteConnection connection;

    // Constructor
    public DatabaseHelper(Context context) {
        File dbFile = context.getDatabasePath(DATABASE_NAME);
        File dbDir = dbFile.getParentFile();
        if (dbDir != null && !dbDir.exists()) {
            dbDir.mkdirs();
        }
        BundledSQLiteDriver driver = new BundledSQLiteDriver();
        connection = driver.open(dbFile.getAbsolutePath());
        initializeDatabase();
    }

    private void initializeDatabase() {
        execSQL("PRAGMA journal_mode=WAL");
        int currentVersion = getSchemaVersion();

        if (currentVersion > DATABASE_VERSION) {
            throw new IllegalStateException(
                "Database version " + currentVersion + " is newer than supported version " + DATABASE_VERSION);
        }

        if (currentVersion == DATABASE_VERSION) {
            return;
        }

        execSQL("BEGIN TRANSACTION");
        boolean success = false;
        try {
            if (currentVersion == 0) {
                onCreate();
            } else {
                onUpgrade(currentVersion, DATABASE_VERSION);
            }
            setSchemaVersion(DATABASE_VERSION);
            success = true;
        } finally {
            execSQL(success ? "COMMIT" : "ROLLBACK");
        }
    }

    private void execSQL(String sql) {
        SQLiteStatement stmt = connection.prepare(sql);
        try {
            stmt.step();
        } finally {
            stmt.close();
        }
    }

    private int getSchemaVersion() {
        SQLiteStatement stmt = connection.prepare("PRAGMA user_version");
        try {
            if (stmt.step()) {
                return (int) stmt.getLong(0);
            }
        } finally {
            stmt.close();
        }
        return 0;
    }

    private void setSchemaVersion(int version) {
        execSQL("PRAGMA user_version = " + version);
    }

    private int getChanges() {
        SQLiteStatement stmt = connection.prepare("SELECT changes()");
        try {
            stmt.step();
            return (int) stmt.getLong(0);
        } finally {
            stmt.close();
        }
    }

    private void createFts5Table() {
        execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS weights_fts USING fts5("
            + "notes, content='" + TABLE_WEIGHTS + "', content_rowid='id')");
    }

    private void createFts5Triggers() {
        execSQL("CREATE TRIGGER IF NOT EXISTS weights_ai AFTER INSERT ON " + TABLE_WEIGHTS
            + " BEGIN INSERT INTO weights_fts(rowid, notes) VALUES (new.id, new.notes); END;");

        execSQL("CREATE TRIGGER IF NOT EXISTS weights_ad AFTER DELETE ON " + TABLE_WEIGHTS
            + " BEGIN INSERT INTO weights_fts(weights_fts, rowid, notes) "
            + "VALUES('delete', old.id, old.notes); END;");

        execSQL("CREATE TRIGGER IF NOT EXISTS weights_au AFTER UPDATE ON " + TABLE_WEIGHTS
            + " BEGIN INSERT INTO weights_fts(weights_fts, rowid, notes) "
            + "VALUES('delete', old.id, old.notes); "
            + "INSERT INTO weights_fts(rowid, notes) VALUES (new.id, new.notes); END;");
    }

    // onCreate is called when the database is first created (user_version == 0)
    private void onCreate() {
        // Create Users Table
        execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "username TEXT UNIQUE, "
            + "password TEXT, "
            + "goal_weight REAL, "
            + "phone_number TEXT, "
            + "goal_reached_sent INTEGER DEFAULT 0)");

        // Create Weights Table (with notes column)
        execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_WEIGHTS + " ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "userId INTEGER, "
            + "date TEXT, "
            + "weight REAL, "
            + "notes TEXT DEFAULT '')");

        // Compound index for efficient user+date lookups
        execSQL("CREATE INDEX IF NOT EXISTS idx_weights_user_date ON "
            + TABLE_WEIGHTS + "(userId, date)");

        // FTS5 virtual table for full-text search on weight notes
        createFts5Table();

        // Triggers to keep FTS5 in sync
        createFts5Triggers();
    }

    // onUpgrade is called when user_version < DATABASE_VERSION
    private void onUpgrade(int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // Legacy path — drop and recreate
            execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
            execSQL("DROP TABLE IF EXISTS " + TABLE_WEIGHTS);
            onCreate();
            return;
        }
        if (oldVersion < 4) {
            execSQL("CREATE INDEX IF NOT EXISTS idx_weights_user_date ON "
                + TABLE_WEIGHTS + "(userId, date)");
        }
        if (oldVersion < 5) {
            // Add notes column to weights table if missing
            try {
                execSQL("ALTER TABLE " + TABLE_WEIGHTS + " ADD COLUMN notes TEXT DEFAULT ''");
            } catch (Exception ignored) {
            }

            // Create FTS5 table and triggers if not present
            createFts5Table();
            createFts5Triggers();

            // Populate FTS table from existing content
            try {
                execSQL("INSERT INTO weights_fts(rowid, notes) SELECT id, notes FROM " + TABLE_WEIGHTS);
            } catch (Exception ignored) {
            }
        }
        if (oldVersion < 6) {
            // Hash all existing plain-text passwords with bcrypt (transactional)
            execSQL("BEGIN TRANSACTION");
            try {
                // Read all users first to avoid modifying while iterating
                List<int[]> ids = new ArrayList<>();
                List<String> passwords = new ArrayList<>();
                SQLiteStatement readStmt = connection.prepare(
                    "SELECT id, password FROM " + TABLE_USERS);
                try {
                    while (readStmt.step()) {
                        ids.add(new int[]{(int) readStmt.getLong(0)});
                        passwords.add(readStmt.isNull(1) ? null : readStmt.getText(1));
                    }
                } finally {
                    readStmt.close();
                }

                for (int i = 0; i < ids.size(); i++) {
                    int id = ids.get(i)[0];
                    String plainPassword = passwords.get(i);
                    if (plainPassword != null && !plainPassword.startsWith("$2a$")) {
                        String hashedPassword = PasswordUtils.hashPassword(plainPassword);
                        SQLiteStatement updateStmt = connection.prepare(
                            "UPDATE " + TABLE_USERS + " SET password=? WHERE id=?");
                        try {
                            updateStmt.bindText(1, hashedPassword);
                            updateStmt.bindLong(2, id);
                            updateStmt.step();
                        } finally {
                            updateStmt.close();
                        }
                    }
                }
                execSQL("COMMIT");
            } catch (Exception e) {
                try { execSQL("ROLLBACK"); } catch (Exception ignored) {}
            }
        }
        if (oldVersion < 7) {
            // Ensure FTS5 table and triggers exist — they may have been
            // missing on devices where the old framework SQLite lacked FTS5
            createFts5Table();
            createFts5Triggers();

            // Rebuild the FTS index from existing data
            try {
                execSQL("INSERT INTO weights_fts(weights_fts) VALUES('rebuild')");
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void close() {
        connection.close();
    }

    // --- User Logic ---

    public boolean createUser(String username, String password, double goalWeight, String phoneNumber) {
        SQLiteStatement stmt = connection.prepare(
            "INSERT INTO " + TABLE_USERS
                + " (username, password, goal_weight, phone_number) VALUES (?, ?, ?, ?)");
        try {
            stmt.bindText(1, username);
            stmt.bindText(2, PasswordUtils.hashPassword(password));
            stmt.bindDouble(3, goalWeight);
            if (phoneNumber != null) {
                stmt.bindText(4, phoneNumber);
            } else {
                stmt.bindNull(4);
            }
            stmt.step();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            stmt.close();
        }
    }

    public int loginUser(String username, String password) {
        SQLiteStatement stmt = connection.prepare(
            "SELECT id, password FROM " + TABLE_USERS + " WHERE username=?");
        try {
            stmt.bindText(1, username);
            int userId = -1;
            if (stmt.step()) {
                String storedHash = stmt.isNull(1) ? null : stmt.getText(1);
                if (storedHash != null && storedHash.startsWith("$2a$")) {
                    // Bcrypt path
                    if (PasswordUtils.checkPassword(password, storedHash)) {
                        userId = (int) stmt.getLong(0);
                    }
                } else {
                    // Legacy plain-text fallback — hash and upgrade on success
                    if (storedHash != null && storedHash.equals(password)) {
                        userId = (int) stmt.getLong(0);
                        SQLiteStatement update = connection.prepare(
                            "UPDATE " + TABLE_USERS + " SET password=? WHERE id=?");
                        try {
                            update.bindText(1, PasswordUtils.hashPassword(password));
                            update.bindLong(2, userId);
                            update.step();
                        } finally {
                            update.close();
                        }
                    }
                }
            }
            return userId;
        } finally {
            stmt.close();
        }
    }

    public String getPhoneNumber(int userId) {
        SQLiteStatement stmt = connection.prepare(
            "SELECT phone_number FROM " + TABLE_USERS + " WHERE id=?");
        try {
            stmt.bindLong(1, userId);
            String number = "";
            if (stmt.step()) {
                number = stmt.isNull(0) ? "" : stmt.getText(0);
            }
            return number;
        } finally {
            stmt.close();
        }
    }

    public boolean isGoalSmsSent(int userId) {
        SQLiteStatement stmt = connection.prepare(
            "SELECT goal_reached_sent FROM " + TABLE_USERS + " WHERE id=?");
        try {
            stmt.bindLong(1, userId);
            boolean sent = false;
            if (stmt.step()) {
                sent = stmt.getLong(0) == 1;
            }
            return sent;
        } finally {
            stmt.close();
        }
    }

    public void setGoalSmsSent(int userId) {
        SQLiteStatement stmt = connection.prepare(
            "UPDATE " + TABLE_USERS + " SET goal_reached_sent=1 WHERE id=?");
        try {
            stmt.bindLong(1, userId);
            stmt.step();
        } finally {
            stmt.close();
        }
    }

    public UserProfile getUserProfile(int userId) {
        SQLiteStatement stmt = connection.prepare(
            "SELECT username, goal_weight, phone_number FROM " + TABLE_USERS + " WHERE id=?");
        try {
            stmt.bindLong(1, userId);
            UserProfile profile = null;
            if (stmt.step()) {
                String username = stmt.getText(0);
                double goalWeight = stmt.getDouble(1);
                String phone = stmt.isNull(2) ? null : stmt.getText(2);
                profile = new UserProfile(username, goalWeight, phone);
            }
            return profile;
        } finally {
            stmt.close();
        }
    }

    public boolean updateUser(int userId, double goalWeight, String phoneNumber) {
        // Only reset SMS flag when goal weight actually changes
        double currentGoal = getGoalWeight(userId);

        String sql;
        boolean resetFlag = Double.compare(currentGoal, goalWeight) != 0;
        if (resetFlag) {
            sql = "UPDATE " + TABLE_USERS
                + " SET goal_weight=?, phone_number=?, goal_reached_sent=0 WHERE id=?";
        } else {
            sql = "UPDATE " + TABLE_USERS
                + " SET goal_weight=?, phone_number=? WHERE id=?";
        }

        SQLiteStatement stmt = connection.prepare(sql);
        try {
            stmt.bindDouble(1, goalWeight);
            if (phoneNumber != null) {
                stmt.bindText(2, phoneNumber);
            } else {
                stmt.bindNull(2);
            }
            stmt.bindLong(3, userId);
            stmt.step();
            return getChanges() > 0;
        } finally {
            stmt.close();
        }
    }

    // --- Weight Logic ---

    public boolean addWeight(int userId, String date, double weight) {
        return addWeight(userId, date, weight, "");
    }

    public boolean addWeight(int userId, String date, double weight, String notes) {
        SQLiteStatement stmt = connection.prepare(
            "INSERT INTO " + TABLE_WEIGHTS
                + " (userId, date, weight, notes) VALUES (?, ?, ?, ?)");
        try {
            stmt.bindLong(1, userId);
            stmt.bindText(2, date);
            stmt.bindDouble(3, weight);
            stmt.bindText(4, notes != null ? notes : "");
            stmt.step();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            stmt.close();
        }
    }

    public List<WeightEntry> getWeights(int userId) {
        List<WeightEntry> list = new ArrayList<>();
        SQLiteStatement stmt = connection.prepare(
            "SELECT id, date, weight, notes FROM " + TABLE_WEIGHTS
                + " WHERE userId=? ORDER BY date DESC");
        try {
            stmt.bindLong(1, userId);
            while (stmt.step()) {
                int id = (int) stmt.getLong(0);
                String date = stmt.getText(1);
                double weight = stmt.getDouble(2);
                String notes = stmt.isNull(3) ? "" : stmt.getText(3);
                list.add(new WeightEntry(id, date, weight, notes));
            }
        } finally {
            stmt.close();
        }
        return list;
    }

    public List<WeightEntry> getWeightsInRange(int userId, String startDate, String endDate) {
        List<WeightEntry> list = new ArrayList<>();
        SQLiteStatement stmt = connection.prepare(
            "SELECT id, date, weight, notes FROM " + TABLE_WEIGHTS
                + " WHERE userId=? AND date BETWEEN ? AND ? ORDER BY date DESC");
        try {
            stmt.bindLong(1, userId);
            stmt.bindText(2, startDate);
            stmt.bindText(3, endDate);
            while (stmt.step()) {
                int id = (int) stmt.getLong(0);
                String date = stmt.getText(1);
                double weight = stmt.getDouble(2);
                String notes = stmt.isNull(3) ? "" : stmt.getText(3);
                list.add(new WeightEntry(id, date, weight, notes));
            }
        } finally {
            stmt.close();
        }
        return list;
    }

    public List<WeightEntry> searchWeightNotes(int userId, String query) {
        List<WeightEntry> list = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return list;
        }

        SQLiteStatement stmt = null;
        try {
            stmt = connection.prepare(
                "SELECT w.id, w.date, w.weight, w.notes FROM " + TABLE_WEIGHTS + " w "
                    + "JOIN weights_fts f ON f.rowid = w.id "
                    + "WHERE f.notes MATCH ? AND w.userId=? ORDER BY w.date DESC");
            stmt.bindText(1, query);
            stmt.bindLong(2, userId);
            while (stmt.step()) {
                addWeightEntryFromStatement(list, stmt);
            }
            return list;
        } catch (RuntimeException e) {
            Log.w(TAG, "FTS search failed, falling back to LIKE search", e);
            return searchWeightNotesFallback(userId, query);
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
    }

    private List<WeightEntry> searchWeightNotesFallback(int userId, String query) {
        List<WeightEntry> list = new ArrayList<>();
        SQLiteStatement stmt = connection.prepare(
            "SELECT id, date, weight, notes FROM " + TABLE_WEIGHTS
                + " WHERE userId=? AND notes LIKE ? ESCAPE '\\' ORDER BY date DESC");
        try {
            stmt.bindLong(1, userId);
            stmt.bindText(2, "%" + escapeLikePattern(query.trim()) + "%");
            while (stmt.step()) {
                addWeightEntryFromStatement(list, stmt);
            }
        } finally {
            stmt.close();
        }
        return list;
    }

    private void addWeightEntryFromStatement(List<WeightEntry> list, SQLiteStatement stmt) {
        int id = (int) stmt.getLong(0);
        String date = stmt.getText(1);
        double weight = stmt.getDouble(2);
        String notes = stmt.isNull(3) ? "" : stmt.getText(3);
        list.add(new WeightEntry(id, date, weight, notes));
    }

    private String escapeLikePattern(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }

    public List<TimePeriodAverage> getWeeklyAverages(int userId) {
        List<TimePeriodAverage> list = new ArrayList<>();
        SQLiteStatement stmt = connection.prepare(
            "SELECT strftime('%Y-W%W', date) AS period, AVG(weight) as average, COUNT(*) as entryCount "
                + "FROM " + TABLE_WEIGHTS + " WHERE userId=? GROUP BY period ORDER BY period DESC");
        try {
            stmt.bindLong(1, userId);
            while (stmt.step()) {
                String period = stmt.getText(0);
                double average = stmt.getDouble(1);
                int count = (int) stmt.getLong(2);
                list.add(new TimePeriodAverage(period, average, count));
            }
        } finally {
            stmt.close();
        }
        return list;
    }

    public List<TimePeriodAverage> getMonthlyAverages(int userId) {
        List<TimePeriodAverage> list = new ArrayList<>();
        SQLiteStatement stmt = connection.prepare(
            "SELECT strftime('%Y-%m', date) AS period, AVG(weight) as average, COUNT(*) as entryCount "
                + "FROM " + TABLE_WEIGHTS + " WHERE userId=? GROUP BY period ORDER BY period DESC");
        try {
            stmt.bindLong(1, userId);
            while (stmt.step()) {
                String period = stmt.getText(0);
                double average = stmt.getDouble(1);
                int count = (int) stmt.getLong(2);
                list.add(new TimePeriodAverage(period, average, count));
            }
        } finally {
            stmt.close();
        }
        return list;
    }

    public boolean updateWeight(int weightId, String date, double weight) {
        SQLiteStatement stmt = connection.prepare(
            "UPDATE " + TABLE_WEIGHTS + " SET date=?, weight=? WHERE id=?");
        try {
            stmt.bindText(1, date);
            stmt.bindDouble(2, weight);
            stmt.bindLong(3, weightId);
            stmt.step();
            return getChanges() > 0;
        } finally {
            stmt.close();
        }
    }

    public boolean updateWeight(int weightId, String date, double weight, String notes) {
        SQLiteStatement stmt = connection.prepare(
            "UPDATE " + TABLE_WEIGHTS + " SET date=?, weight=?, notes=? WHERE id=?");
        try {
            stmt.bindText(1, date);
            stmt.bindDouble(2, weight);
            stmt.bindText(3, notes != null ? notes : "");
            stmt.bindLong(4, weightId);
            stmt.step();
            return getChanges() > 0;
        } finally {
            stmt.close();
        }
    }

    public boolean updateWeightNotes(int weightId, String notes) {
        SQLiteStatement stmt = connection.prepare(
            "UPDATE " + TABLE_WEIGHTS + " SET notes=? WHERE id=?");
        try {
            stmt.bindText(1, notes != null ? notes : "");
            stmt.bindLong(2, weightId);
            stmt.step();
            return getChanges() > 0;
        } finally {
            stmt.close();
        }
    }

    public int bulkAddWeights(int userId, java.util.List<String[]> rows) {
        int imported = 0;
        SQLiteStatement stmt = connection.prepare(
            "INSERT INTO " + TABLE_WEIGHTS
                + " (userId, date, weight, notes) VALUES (?, ?, ?, ?)");
        try {
            execSQL("BEGIN TRANSACTION");
            for (String[] row : rows) {
                // expected: {date, weight, notes}
                if (row == null || row.length < 2) continue;
                String date = row[0];
                double weight;
                try {
                    weight = Double.parseDouble(row[1]);
                } catch (NumberFormatException ex) {
                    continue;
                }
                String notes = row.length > 2 && row[2] != null ? row[2] : "";

                stmt.bindLong(1, userId);
                stmt.bindText(2, date);
                stmt.bindDouble(3, weight);
                stmt.bindText(4, notes);
                try {
                    stmt.step();
                    imported++;
                } catch (Exception e) {
                    // skip failed individual inserts
                }
                stmt.reset();
            }
            execSQL("COMMIT");
        } catch (Exception e) {
            try { execSQL("ROLLBACK"); } catch (Exception ignored) {}
        } finally {
            stmt.close();
        }
        return imported;
    }

    public boolean deleteWeight(int weightId) {
        SQLiteStatement stmt = connection.prepare(
            "DELETE FROM " + TABLE_WEIGHTS + " WHERE id=?");
        try {
            stmt.bindLong(1, weightId);
            stmt.step();
            return getChanges() > 0;
        } finally {
            stmt.close();
        }
    }

    public double getGoalWeight(int userId) {
        SQLiteStatement stmt = connection.prepare(
            "SELECT goal_weight FROM " + TABLE_USERS + " WHERE id=?");
        try {
            stmt.bindLong(1, userId);
            double goalWeight = -1;
            if (stmt.step()) {
                goalWeight = stmt.getDouble(0);
            }
            return goalWeight;
        } finally {
            stmt.close();
        }
    }

    // --- Package-private test helpers ---
    @VisibleForTesting
    void deleteUserByUsername(String username) {
        SQLiteStatement stmt = connection.prepare(
            "DELETE FROM " + TABLE_USERS + " WHERE username=?");
        try {
            stmt.bindText(1, username);
            stmt.step();
        } finally {
            stmt.close();
        }
    }

    @VisibleForTesting
    void deleteWeightsByUserId(int userId) {
        SQLiteStatement stmt = connection.prepare(
            "DELETE FROM " + TABLE_WEIGHTS + " WHERE userId=?");
        try {
            stmt.bindLong(1, userId);
            stmt.step();
        } finally {
            stmt.close();
        }
    }

    @VisibleForTesting
    void deleteUserById(int userId) {
        SQLiteStatement stmt = connection.prepare(
            "DELETE FROM " + TABLE_USERS + " WHERE id=?");
        try {
            stmt.bindLong(1, userId);
            stmt.step();
        } finally {
            stmt.close();
        }
    }

    @VisibleForTesting
    String getStoredPasswordHash(String username) {
        SQLiteStatement stmt = connection.prepare(
            "SELECT password FROM " + TABLE_USERS + " WHERE username=?");
        try {
            stmt.bindText(1, username);
            if (stmt.step()) {
                return stmt.isNull(0) ? null : stmt.getText(0);
            }
        } finally {
            stmt.close();
        }
        return null;
    }

    @VisibleForTesting
    boolean insertRawUser(String username, String rawPassword, double goalWeight) {
        SQLiteStatement stmt = connection.prepare(
            "INSERT INTO " + TABLE_USERS
                + " (username, password, goal_weight) VALUES (?, ?, ?)");
        try {
            stmt.bindText(1, username);
            stmt.bindText(2, rawPassword);
            stmt.bindDouble(3, goalWeight);
            stmt.step();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            stmt.close();
        }
    }
}


