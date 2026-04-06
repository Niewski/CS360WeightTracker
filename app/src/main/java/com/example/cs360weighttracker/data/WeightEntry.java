package com.example.cs360weighttracker.data;

import java.util.Objects;

public class WeightEntry implements Comparable<WeightEntry> {
    public int id;
    public String date;
    public double weight;
    public String notes;

    public WeightEntry(int id, String date, double weight) {
        this(id, date, weight, "");
    }

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
