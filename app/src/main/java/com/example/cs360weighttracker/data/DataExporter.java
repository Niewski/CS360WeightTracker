package com.example.cs360weighttracker.data;

import android.database.Cursor;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class DataExporter {

    public static void exportToCsv(int userId, OutputStream outputStream,
                                   DatabaseHelper dbHelper) throws IOException {
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            // Header
            writer.println("date,weight,notes");

            Cursor cursor = dbHelper.getWeights(userId);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                    double weight = cursor.getDouble(cursor.getColumnIndexOrThrow("weight"));
                    String notes = cursor.getString(cursor.getColumnIndexOrThrow("notes"));
                    if (notes == null) notes = "";

                    // Escape quotes by doubling and wrap field in quotes when necessary
                    String escapedNotes = notes.replace("\"", "\"\"");
                    boolean needsQuotes = escapedNotes.contains(",") || escapedNotes.contains("\n") || escapedNotes.contains("\r") || escapedNotes.contains("\"");
                    if (needsQuotes) {
                        escapedNotes = "\"" + escapedNotes + "\"";
                    }

                    String line = String.format(Locale.US, "%s,%.1f,%s", date, weight, escapedNotes);
                    writer.println(line);
                }
                cursor.close();
            }
            writer.flush();
        }
    }
}
