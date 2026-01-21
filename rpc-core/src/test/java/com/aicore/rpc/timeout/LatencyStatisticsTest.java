package com.aicore.rpc.timeout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LatencyStatistics.
 */
class LatencyStatisticsTest {

    private LatencyStatistics stats;

    @BeforeEach
    void setUp() {
        stats = new LatencyStatistics(100);
    }

    @Test
    @DisplayName("Initial state should have zero samples")
    void testInitialState() {
        assertEquals(0, stats.getSampleCount());
        assertEquals(-1, stats.getAverage()); // Returns -1 when no samples
    }

    @Test
    @DisplayName("Should record single sample correctly")
    void testRecordSingleSample() {
        stats.record(100);

        assertEquals(1, stats.getSampleCount());
        assertEquals(100, stats.getAverage());
        assertEquals(100, stats.getMin());
        assertEquals(100, stats.getMax());
    }

    @Test
    @DisplayName("Should calculate average correctly")
    void testCalculateAverage() {
        stats.record(100);
        stats.record(200);
        stats.record(300);

        assertEquals(3, stats.getSampleCount());
        assertEquals(200, stats.getAverage());
    }

    @Test
    @DisplayName("Should calculate min and max correctly")
    void testMinMax() {
        stats.record(50);
        stats.record(100);
        stats.record(150);

        assertEquals(50, stats.getMin());
        assertEquals(150, stats.getMax());
    }

    @Test
    @DisplayName("Should calculate P50 percentile correctly")
    void testP50Percentile() {
        // Add values 1-100
        for (int i = 1; i <= 100; i++) {
            stats.record(i);
        }

        // P50 should be around 50
        long p50 = stats.getP50();
        assertTrue(p50 >= 45 && p50 <= 55, "P50 should be around 50, got: " + p50);
    }

    @Test
    @DisplayName("Should calculate P99 percentile correctly")
    void testP99Percentile() {
        // Add values 1-100
        for (int i = 1; i <= 100; i++) {
            stats.record(i);
        }

        // P99 should be around 99
        long p99 = stats.getP99();
        assertTrue(p99 >= 95 && p99 <= 100, "P99 should be around 99, got: " + p99);
    }

    @Test
    @DisplayName("Should reset correctly")
    void testReset() {
        stats.record(100);
        stats.record(200);
        stats.reset();

        assertEquals(0, stats.getSampleCount());
    }
}
