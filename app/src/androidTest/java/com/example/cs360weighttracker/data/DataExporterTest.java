package com.example.cs360weighttracker.data;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class DataExporterTest {

    private DatabaseHelper dbHelper;
    private int userId;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase("weight_tracker.db");
        dbHelper = new DatabaseHelper(context);
        dbHelper.createUser("exporttest", "pass123", 150.0, null);
        userId = dbHelper.loginUser("exporttest", "pass123");
    }

    @After
    public void tearDown() {
        dbHelper.close();
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase("weight_tracker.db");
    }

    @Test
    public void exportToCsv_noEntries_writesHeaderOnly() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataExporter.exportToCsv(userId, out, dbHelper);
        String csv = out.toString(StandardCharsets.UTF_8.name());

        assertEquals("date,weight,notes\r\n", csv.replace("\n", "\r\n").replaceAll("\\r\\r\\n", "\r\n"));
        assertTrue(csv.trim().equals("date,weight,notes"));
    }

    @Test
    public void exportToCsv_singleEntry_writesCorrectRow() throws IOException {
        dbHelper.addWeight(userId, "2026-03-15", 172.5, "morning");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataExporter.exportToCsv(userId, out, dbHelper);
        String csv = out.toString(StandardCharsets.UTF_8.name()).trim();

        String[] lines = csv.split("\\r?\\n");
        assertEquals(2, lines.length);
        assertEquals("date,weight,notes", lines[0]);
        assertEquals("2026-03-15,172.5,morning", lines[1]);
    }

    @Test
    public void exportToCsv_multipleEntries_writesAllRows() throws IOException {
        dbHelper.addWeight(userId, "2026-03-15", 172.5, "");
        dbHelper.addWeight(userId, "2026-03-16", 171.0, "");
        dbHelper.addWeight(userId, "2026-03-17", 170.0, "");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataExporter.exportToCsv(userId, out, dbHelper);
        String csv = out.toString(StandardCharsets.UTF_8.name()).trim();

        String[] lines = csv.split("\\r?\\n");
        assertEquals(4, lines.length); // header + 3 rows
    }

    @Test
    public void exportToCsv_notesWithComma_wrapsInQuotes() throws IOException {
        dbHelper.addWeight(userId, "2026-03-15", 172.5, "ate breakfast, then weighed");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataExporter.exportToCsv(userId, out, dbHelper);
        String csv = out.toString(StandardCharsets.UTF_8.name()).trim();

        String[] lines = csv.split("\\r?\\n");
        assertEquals(2, lines.length);
        assertTrue(lines[1].contains("\"ate breakfast, then weighed\""));
    }

    @Test
    public void exportToCsv_notesWithQuotes_escapesDoubleQuotes() throws IOException {
        dbHelper.addWeight(userId, "2026-03-15", 172.5, "said \"hello\"");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataExporter.exportToCsv(userId, out, dbHelper);
        String csv = out.toString(StandardCharsets.UTF_8.name()).trim();

        String[] lines = csv.split("\\r?\\n");
        assertEquals(2, lines.length);
        assertTrue(lines[1].contains("\"said \"\"hello\"\"\""));
    }

    @Test
    public void exportToCsv_nullNotes_writesEmptyNotesField() throws IOException {
        dbHelper.addWeight(userId, "2026-03-15", 172.5);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataExporter.exportToCsv(userId, out, dbHelper);
        String csv = out.toString(StandardCharsets.UTF_8.name()).trim();

        String[] lines = csv.split("\\r?\\n");
        assertEquals(2, lines.length);
        assertEquals("2026-03-15,172.5,", lines[1]);
    }
}
