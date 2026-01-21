package com.aicore.rpc.timeout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AdaptiveTimeout.
 */
class AdaptiveTimeoutTest {

    private AdaptiveTimeout adaptiveTimeout;

    @BeforeEach
    void setUp() {
        AdaptiveTimeoutConfig config = AdaptiveTimeoutConfig.builder()
                .minTimeoutMs(100)
                .maxTimeoutMs(30000)
                .defaultTimeoutMs(10000)
                .safetyFactor(1.5)
                .percentile(99.0)
                .minimumSamples(5)
                .build();
        adaptiveTimeout = new AdaptiveTimeout("test-timeout", config);
    }

    @Test
    @DisplayName("Initial timeout should be default value")
    void testInitialTimeout() {
        assertEquals(10000, adaptiveTimeout.getTimeoutMs());
        assertEquals(10, adaptiveTimeout.getTimeoutSeconds());
    }

    @Test
    @DisplayName("Should use default timeout when samples are insufficient")
    void testDefaultTimeoutWithInsufficientSamples() {
        // Record only 3 samples (less than minimumSamples = 5)
        adaptiveTimeout.recordLatency(100);
        adaptiveTimeout.recordLatency(200);
        adaptiveTimeout.recordLatency(300);

        // Should still use default timeout
        assertEquals(10000, adaptiveTimeout.getTimeoutMs());
    }

    @Test
    @DisplayName("Should calculate adaptive timeout based on P99 latency")
    void testAdaptiveTimeoutCalculation() {
        // Record enough samples
        for (int i = 0; i < 10; i++) {
            adaptiveTimeout.recordLatency(100); // 100ms latency
        }

        // Timeout should be around P99 * safetyFactor = 100 * 1.5 = 150ms
        // But capped at minTimeout = 100ms
        long timeout = adaptiveTimeout.getTimeoutMs();
        assertTrue(timeout >= 100 && timeout <= 200, "Timeout should be around 150ms, got: " + timeout);
    }

    @Test
    @DisplayName("Should respect maximum timeout limit")
    void testMaxTimeoutLimit() {
        // Record very high latency values
        for (int i = 0; i < 10; i++) {
            adaptiveTimeout.recordLatency(50000); // 50 seconds
        }

        // Should be capped at maxTimeout = 30000ms
        assertEquals(30000, adaptiveTimeout.getTimeoutMs());
    }

    @Test
    @DisplayName("Should reset correctly")
    void testReset() {
        for (int i = 0; i < 10; i++) {
            adaptiveTimeout.recordLatency(100);
        }

        adaptiveTimeout.reset();

        assertEquals(10000, adaptiveTimeout.getTimeoutMs());
        assertEquals(0, adaptiveTimeout.getStatistics().getSampleCount());
    }
}
