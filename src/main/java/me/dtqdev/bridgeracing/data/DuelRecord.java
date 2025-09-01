package me.dtqdev.bridgeracing.data;

public class DuelRecord {
    private final double time;
    private final long timestamp;

    public DuelRecord(double time, long timestamp) {
        this.time = time;
        this.timestamp = timestamp;
    }

    public double getTime() {
        return time;
    }

    public long getTimestamp() {
        return timestamp;
    }
}