package com.example.cs360weighttracker.data;

public class WeightEntry {
    public int id;
    public String date;
    public double weight;

    public WeightEntry(int id, String date, double weight) {
        this.id = id;
        this.date = date;
        this.weight = weight;
    }
}
