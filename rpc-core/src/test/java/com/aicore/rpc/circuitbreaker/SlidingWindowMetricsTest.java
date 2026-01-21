package com.aicore.rpc.circuitbreaker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SlidingWindowMetrics.
 */
class SlidingWindowMetricsTest {

    private SlidingWindowMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new SlidingWindowMetrics(10);
    }

    @Test
    @DisplayName("Initial state should have zero counts")
    void testInitialState() {
        assertEquals(0, metrics.getTotalCalls());
        assertEquals(0, metrics.getFailedCalls());
        assertEquals(0, metrics.getSlowCalls());
        assertEquals(0f, metrics.getFailureRate());
        assertEquals(0f, metrics.getSlowCallRate());
    }

    @Test
    @DisplayName("Should record success correctly")
    void testRecordSuccess() {
        metrics.recordSuccess(false);

        assertEquals(1, metrics.getTotalCalls());
        assertEquals(0, metrics.getFailedCalls());
        assertEquals(0, metrics.getSlowCalls());
    }

    @Test
    @DisplayName("Should record slow success correctly")
    void testRecordSlowSuccess() {
        metrics.recordSuccess(true);

        assertEquals(1, metrics.getTotalCalls());
        assertEquals(0, metrics.getFailedCalls());
        assertEquals(1, metrics.getSlowCalls());
    }

    @Test
    @DisplayName("Should record failure correctly")
    void testRecordFailure() {
        metrics.recordFailure();

        assertEquals(1, metrics.getTotalCalls());
        assertEquals(1, metrics.getFailedCalls());
    }

    @Test
    @DisplayName("Should calculate failure rate correctly")
    void testFailureRateCalculation() {
        // 3 successes, 2 failures = 40% failure rate
        metrics.recordSuccess(false);
        metrics.recordSuccess(false);
        metrics.recordSuccess(false);
        metrics.recordFailure();
        metrics.recordFailure();

        assertEquals(5, metrics.getTotalCalls());
        assertEquals(40f, metrics.getFailureRate(), 0.01f);
    }

    @Test
    @DisplayName("Should calculate slow call rate correctly")
    void testSlowCallRateCalculation() {
        // 3 normal, 2 slow = 40% slow rate
        metrics.recordSuccess(false);
        metrics.recordSuccess(false);
        metrics.recordSuccess(false);
        metrics.recordSuccess(true);
        metrics.recordSuccess(true);

        assertEquals(5, metrics.getTotalCalls());
        assertEquals(40f, metrics.getSlowCallRate(), 0.01f);
    }

    @Test
    @DisplayName("Should reset all metrics")
    void testReset() {
        metrics.recordSuccess(true);
        metrics.recordFailure();
        metrics.recordSuccess(false);

        metrics.reset();

        assertEquals(0, metrics.getTotalCalls());
        assertEquals(0, metrics.getFailedCalls());
        assertEquals(0, metrics.getSlowCalls());
    }

    @Test
    @DisplayName("Should trim to window size when exceeding limit")
    void testTrimToWindowSize() {
        // Record 15 calls, window size is 10
        for (int i = 0; i < 15; i++) {
            if (i < 6) {
                metrics.recordFailure(); // 6 failures
            } else {
                metrics.recordSuccess(false); // 9 successes
            }
        }

        // Should be trimmed to window size
        assertTrue(metrics.getTotalCalls() <= 10);
        // Failure rate should be preserved proportionally (6/15 = 40%)
        assertTrue(metrics.getFailureRate() > 0);
    }

    @Test
    @DisplayName("toString should include all metrics")
    void testToString() {
        metrics.recordSuccess(false);
        metrics.recordFailure();

        String str = metrics.toString();

        assertTrue(str.contains("total=2"));
        assertTrue(str.contains("failed=1"));
    }
}
