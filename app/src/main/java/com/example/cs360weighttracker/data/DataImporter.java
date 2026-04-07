package com.example.cs360weighttracker.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DataImporter {

    public static int importFromCsv(int userId, InputStream inputStream,
                                    DatabaseHelper dbHelper) throws IOException {
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        // Skip header line
        String headerLine = reader.readLine();
        if (headerLine == null) {
            return 0;
        }

        List<String[]> validRows = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            String[] parts = parseCsvLine(line);
            if (parts.length < 2) continue;

            String date = parts[0].trim();
            // Simple date validation YYYY-MM-DD
            if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) continue;

            String weightStr = parts[1].trim();
            try {
                Double.parseDouble(weightStr);
            } catch (NumberFormatException ex) {
                continue;
            }

            String notes = "";
            if (parts.length > 2) {
                notes = parts[2];
            }

            validRows.add(new String[]{date, weightStr, notes});
        }

        if (validRows.isEmpty()) return 0;
        return dbHelper.bulkAddWeights(userId, validRows);
    }

    static String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    // Lookahead for escaped quote
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++; // skip the escaped quote
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
