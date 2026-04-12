package com.example.cs360weighttracker.data;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class DataImporterTest {

    // --- parseCsvLine ---

    @Test
    public void parseCsvLine_simpleFields_returnsCorrectParts() {
        String[] result = DataImporter.parseCsvLine("2026-01-15,170.5,morning weigh-in");
        assertEquals(3, result.length);
        assertEquals("2026-01-15", result[0]);
        assertEquals("170.5", result[1]);
        assertEquals("morning weigh-in", result[2]);
    }

    @Test
    public void parseCsvLine_twoFieldsNoNotes_returnsTwoParts() {
        String[] result = DataImporter.parseCsvLine("2026-01-15,170.5");
        assertEquals(2, result.length);
        assertEquals("2026-01-15", result[0]);
        assertEquals("170.5", result[1]);
    }

    @Test
    public void parseCsvLine_emptyNotes_returnsEmptyThirdField() {
        String[] result = DataImporter.parseCsvLine("2026-01-15,170.5,");
        assertEquals(3, result.length);
        assertEquals("", result[2]);
    }

    @Test
    public void parseCsvLine_quotedFieldWithComma_parsesCorrectly() {
        String[] result = DataImporter.parseCsvLine("2026-01-15,170.5,\"ate breakfast, then weighed\"");
        assertEquals(3, result.length);
        assertEquals("ate breakfast, then weighed", result[2]);
    }

    @Test
    public void parseCsvLine_quotedFieldWithEscapedQuotes_parsesCorrectly() {
        String[] result = DataImporter.parseCsvLine("2026-01-15,170.5,\"said \"\"hello\"\"\"");
        assertEquals(3, result.length);
        assertEquals("said \"hello\"", result[2]);
    }

    @Test
    public void parseCsvLine_quotedFieldWithNewline_parsesCorrectly() {
        String[] result = DataImporter.parseCsvLine("2026-01-15,170.5,\"line1\nline2\"");
        assertEquals(3, result.length);
        assertEquals("line1\nline2", result[2]);
    }

    @Test
    public void parseCsvLine_emptyString_returnsSingleEmptyField() {
        String[] result = DataImporter.parseCsvLine("");
        assertEquals(1, result.length);
        assertEquals("", result[0]);
    }

    @Test
    public void parseCsvLine_onlyCommas_returnsEmptyFields() {
        String[] result = DataImporter.parseCsvLine(",,");
        assertEquals(3, result.length);
        assertEquals("", result[0]);
        assertEquals("", result[1]);
        assertEquals("", result[2]);
    }
}
