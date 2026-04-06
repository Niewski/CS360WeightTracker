package com.example.cs360weighttracker.data;

public class TimePeriodAverage {
    public final String period;
    public final double average;
    public final int entryCount;

    public TimePeriodAverage(String period, double average, int entryCount) {
        this.period = period;
        this.average = average;
        this.entryCount = entryCount;
    }
}
