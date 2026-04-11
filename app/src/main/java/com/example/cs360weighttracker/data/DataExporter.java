package com.example.cs360weighttracker.data;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Exports a user's weight entries to CSV format.
 *
 * <p>Output columns: {@code date,weight,notes}. Fields containing
 * commas or quotes are properly escaped per RFC 4180.</p>
 */
public class DataExporter {

    /**
     * Writes all weight entries for the given user as CSV rows to the
     * supplied output stream.
     *
     * @param userId       the user whose entries to export
     * @param outputStream destination stream (UTF-8)
     * @param dbHelper     database helper for querying entries
     * @throws IOException if writing to the stream fails
     */
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

    /**
     * Escapes a notes string for safe CSV output.
     *
     * <p>Newlines are replaced with spaces, quotes are doubled, and
     * the field is wrapped in quotes when it contains commas or
     * quotes.</p>
     *
     * @param notes raw notes text
     * @return CSV-safe string
     */
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
