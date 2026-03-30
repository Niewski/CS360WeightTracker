package com.example.cs360weighttracker.data;

import java.util.Objects;

public class WeightEntry implements Comparable<WeightEntry> {
    public int id;
    public String date;
    public double weight;

    public WeightEntry(int id, String date, double weight) {
        this.id = id;
        this.date = date;
        this.weight = weight;
    }

    @Override
    public int compareTo(WeightEntry other) {
        return this.date.compareTo(other.date);
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
