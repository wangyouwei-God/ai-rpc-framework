package com.aicore.rpc.timeout;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Latency statistics collector using a fixed-size circular buffer.
 * Calculates percentile latencies (P50, P95, P99) for adaptive timeout.
 */
public class LatencyStatistics {

    private final long[] samples;
    private final int capacity;
    private final AtomicInteger count;
    private final AtomicInteger index;
    private final ReentrantLock sortLock;

    public LatencyStatistics(int capacity) {
        this.capacity = capacity;
        this.samples = new long[capacity];
        this.count = new AtomicInteger(0);
        this.index = new AtomicInteger(0);
        this.sortLock = new ReentrantLock();
    }

    /**
     * Record a latency sample in milliseconds.
     */
    public void record(long latencyMs) {
        int idx = index.getAndIncrement() % capacity;
        samples[idx] = latencyMs;
        if (count.get() < capacity) {
            count.incrementAndGet();
        }
    }

    /**
     * Get the number of recorded samples.
     */
    public int getSampleCount() {
        return count.get();
    }

    /**
     * Calculate percentile latency.
     * 
     * @param percentile value between 0 and 100 (e.g., 99 for P99)
     * @return the percentile latency in milliseconds, or -1 if insufficient data
     */
    public long getPercentile(double percentile) {
        int currentCount = count.get();
        if (currentCount == 0) {
            return -1;
        }

        sortLock.lock();
        try {
            // Copy samples to avoid concurrent modification
            long[] copy = new long[currentCount];
            for (int i = 0; i < currentCount; i++) {
                copy[i] = samples[i];
            }

            // Sort for percentile calculation
            Arrays.sort(copy);

            // Calculate percentile index
            int idx = (int) Math.ceil((percentile / 100.0) * currentCount) - 1;
            idx = Math.max(0, Math.min(idx, currentCount - 1));

            return copy[idx];
        } finally {
            sortLock.unlock();
        }
    }

    /**
     * Get P50 (median) latency.
     */
    public long getP50() {
        return getPercentile(50);
    }

    /**
     * Get P95 latency.
     */
    public long getP95() {
        return getPercentile(95);
    }

    /**
     * Get P99 latency.
     */
    public long getP99() {
        return getPercentile(99);
    }

    /**
     * Get average latency.
     */
    public double getAverage() {
        int currentCount = count.get();
        if (currentCount == 0) {
            return -1;
        }

        long sum = 0;
        for (int i = 0; i < currentCount; i++) {
            sum += samples[i];
        }
        return (double) sum / currentCount;
    }

    /**
     * Get minimum latency.
     */
    public long getMin() {
        int currentCount = count.get();
        if (currentCount == 0) {
            return -1;
        }

        long min = Long.MAX_VALUE;
        for (int i = 0; i < currentCount; i++) {
            min = Math.min(min, samples[i]);
        }
        return min;
    }

    /**
     * Get maximum latency.
     */
    public long getMax() {
        int currentCount = count.get();
        if (currentCount == 0) {
            return -1;
        }

        long max = 0;
        for (int i = 0; i < currentCount; i++) {
            max = Math.max(max, samples[i]);
        }
        return max;
    }

    /**
     * Reset all statistics.
     */
    public void reset() {
        count.set(0);
        index.set(0);
        Arrays.fill(samples, 0);
    }

    @Override
    public String toString() {
        return String.format("LatencyStats[samples=%d, avg=%.1fms, p50=%dms, p95=%dms, p99=%dms]",
                getSampleCount(), getAverage(), getP50(), getP95(), getP99());
    }
}
