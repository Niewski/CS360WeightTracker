package com.example.cs360weighttracker.data;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class DataImporterTest {

    private DatabaseHelper dbHelper;
    private int userId;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase("weight_tracker.db");
        dbHelper = new DatabaseHelper(context);
        dbHelper.createUser("importtest", "pass123", 150.0, null);
        userId = dbHelper.loginUser("importtest", "pass123");
    }

    @After
    public void tearDown() {
        dbHelper.close();
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase("weight_tracker.db");
    }

    private InputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void importFromCsv_validRows_importsAll() throws IOException {
        String csv = "date,weight,notes\n"
                + "2026-03-15,172.5,morning\n"
                + "2026-03-16,171.0,evening\n";

        int count = DataImporter.importFromCsv(userId, toStream(csv), dbHelper);
        assertEquals(2, count);
    }

    @Test
    public void importFromCsv_emptyFile_returnsZero() throws IOException {
        int count = DataImporter.importFromCsv(userId, toStream(""), dbHelper);
        assertEquals(0, count);
    }

    @Test
    public void importFromCsv_headerOnly_returnsZero() throws IOException {
        int count = DataImporter.importFromCsv(userId, toStream("date,weight,notes\n"), dbHelper);
        assertEquals(0, count);
    }

    @Test
    public void importFromCsv_invalidDate_skipsRow() throws IOException {
        String csv = "date,weight,notes\n"
                + "03-15-2026,172.5,bad date\n"
                + "2026-03-16,171.0,good date\n";

        int count = DataImporter.importFromCsv(userId, toStream(csv), dbHelper);
        assertEquals(1, count);
    }

    @Test
    public void importFromCsv_invalidWeight_skipsRow() throws IOException {
        String csv = "date,weight,notes\n"
                + "2026-03-15,abc,bad weight\n"
                + "2026-03-16,171.0,good weight\n";

        int count = DataImporter.importFromCsv(userId, toStream(csv), dbHelper);
        assertEquals(1, count);
    }

    @Test
    public void importFromCsv_missingColumns_skipsRow() throws IOException {
        String csv = "date,weight,notes\n"
                + "2026-03-15\n"
                + "2026-03-16,171.0,valid\n";

        int count = DataImporter.importFromCsv(userId, toStream(csv), dbHelper);
        assertEquals(1, count);
    }

    @Test
    public void importFromCsv_blankLines_skipped() throws IOException {
        String csv = "date,weight,notes\n"
                + "\n"
                + "2026-03-15,172.5,morning\n"
                + "\n";

        int count = DataImporter.importFromCsv(userId, toStream(csv), dbHelper);
        assertEquals(1, count);
    }

    @Test
    public void importFromCsv_quotedNotes_parsesCorrectly() throws IOException {
        String csv = "date,weight,notes\n"
                + "2026-03-15,172.5,\"ate breakfast, then weighed\"\n";

        int count = DataImporter.importFromCsv(userId, toStream(csv), dbHelper);
        assertEquals(1, count);
    }

    @Test
    public void importFromCsv_roundTrip_preservesData() throws IOException {
        // Add data, export, clear, import, verify count matches
        dbHelper.addWeight(userId, "2026-03-15", 172.5, "morning");
        dbHelper.addWeight(userId, "2026-03-16", 171.0, "evening");

        ByteArrayOutputStream exportOut = new ByteArrayOutputStream();
        DataExporter.exportToCsv(userId, exportOut, dbHelper);
        String csv = exportOut.toString(StandardCharsets.UTF_8.name());

        // Create a second user to import into (avoids duplicates)
        dbHelper.createUser("importtest2", "pass123", 150.0, null);
        int userId2 = dbHelper.loginUser("importtest2", "pass123");

        int count = DataImporter.importFromCsv(userId2, toStream(csv), dbHelper);
        assertEquals(2, count);
    }
}
