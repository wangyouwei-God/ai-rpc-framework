package com.aicore.rpc.circuitbreaker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sliding window metrics for tracking call outcomes.
 * Uses a count-based sliding window.
 */
public class SlidingWindowMetrics {

    private final int windowSize;
    private final AtomicInteger totalCalls;
    private final AtomicInteger failedCalls;
    private final AtomicInteger slowCalls;
    private final AtomicLong totalDuration;

    public SlidingWindowMetrics(int windowSize) {
        this.windowSize = windowSize;
        this.totalCalls = new AtomicInteger(0);
        this.failedCalls = new AtomicInteger(0);
        this.slowCalls = new AtomicInteger(0);
        this.totalDuration = new AtomicLong(0);
    }

    /**
     * Record a successful call.
     * 
     * @param isSlow whether the call was slow
     */
    public void recordSuccess(boolean isSlow) {
        totalCalls.incrementAndGet();
        if (isSlow) {
            slowCalls.incrementAndGet();
        }
        trimToWindowSize();
    }

    /**
     * Record a failed call.
     */
    public void recordFailure() {
        totalCalls.incrementAndGet();
        failedCalls.incrementAndGet();
        trimToWindowSize();
    }

    /**
     * Get failure rate as a percentage (0-100).
     */
    public float getFailureRate() {
        int total = totalCalls.get();
        if (total == 0) {
            return 0f;
        }
        return (failedCalls.get() * 100f) / total;
    }

    /**
     * Get slow call rate as a percentage (0-100).
     */
    public float getSlowCallRate() {
        int total = totalCalls.get();
        if (total == 0) {
            return 0f;
        }
        return (slowCalls.get() * 100f) / total;
    }

    /**
     * Get total number of recorded calls.
     */
    public int getTotalCalls() {
        return totalCalls.get();
    }

    /**
     * Get number of failed calls.
     */
    public int getFailedCalls() {
        return failedCalls.get();
    }

    /**
     * Get number of slow calls.
     */
    public int getSlowCalls() {
        return slowCalls.get();
    }

    /**
     * Reset all metrics.
     */
    public void reset() {
        totalCalls.set(0);
        failedCalls.set(0);
        slowCalls.set(0);
        totalDuration.set(0);
    }

    /**
     * Keep metrics within window size.
     * This is a simplified implementation that just caps the counts.
     * A more sophisticated implementation would use a ring buffer.
     */
    private void trimToWindowSize() {
        if (totalCalls.get() > windowSize) {
            // Scale down proportionally
            float ratio = (float) windowSize / totalCalls.get();
            failedCalls.set((int) (failedCalls.get() * ratio));
            slowCalls.set((int) (slowCalls.get() * ratio));
            totalCalls.set(windowSize);
        }
    }

    @Override
    public String toString() {
        return String.format("Metrics[total=%d, failed=%d (%.1f%%), slow=%d (%.1f%%)]",
                totalCalls.get(), failedCalls.get(), getFailureRate(),
                slowCalls.get(), getSlowCallRate());
    }
}
