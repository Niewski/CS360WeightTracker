package com.example.cs360weighttracker.data;

import java.util.Objects;

/**
 * Mutable data holder for a single weight-log row.
 *
 * <p>Natural ordering is by {@link #date} ascending, with ties
 * broken by {@link #id}. Equality is based on {@code id} alone.</p>
 */
public class WeightEntry implements Comparable<WeightEntry> {
    public int id;
    public String date;
    public double weight;
    public String notes;

    /**
     * Convenience constructor without notes (defaults to empty).
     */
    public WeightEntry(int id, String date, double weight) {
        this(id, date, weight, "");
    }

    /**
     * Full constructor.
     *
     * @param id     row ID from the weights table
     * @param date   date string in {@code YYYY-MM-DD} format
     * @param weight weight in lbs
     * @param notes  optional user notes (null is normalized to empty)
     */
    public WeightEntry(int id, String date, double weight, String notes) {
        this.id = id;
        this.date = date;
        this.weight = weight;
        this.notes = notes != null ? notes : "";
    }

    @Override
    public int compareTo(WeightEntry other) {
        int dateComparison = this.date.compareTo(other.date);
        if (dateComparison != 0) {
            return dateComparison;
        }
        return Integer.compare(this.id, other.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WeightEntry that = (WeightEntry) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
