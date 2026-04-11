package com.example.cs360weighttracker.data;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DataExporter {

    public static void exportToCsv(int userId, OutputStream outputStream,
                                   DatabaseHelper dbHelper) throws IOException {
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            // Header
            writer.println("date,weight,notes");

            List<WeightEntry> entries = dbHelper.getWeights(userId);
            for (WeightEntry entry : entries) {
                String notes = entry.notes != null ? entry.notes : "";

                // Sanitize newlines so each CSV record stays on one line
                String escapedNotes = getEscapedNotes(notes);

                // Use Double.toString to preserve full precision
                String line = entry.date + "," + Double.toString(entry.weight) + "," + escapedNotes;
                writer.println(line);
            }
            writer.flush();
        }
    }

    @NonNull
    private static String getEscapedNotes(String notes) {
        String sanitizedNotes = notes.replace("\r\n", " ")
                                     .replace("\n", " ")
                                     .replace("\r", " ");

        // Escape quotes by doubling and wrap field in quotes when necessary
        String escapedNotes = sanitizedNotes.replace("\"", "\"\"");
        boolean needsQuotes = escapedNotes.contains(",") || escapedNotes.contains("\"");
        if (needsQuotes) {
            escapedNotes = "\"" + escapedNotes + "\"";
        }
        return escapedNotes;
    }
}
