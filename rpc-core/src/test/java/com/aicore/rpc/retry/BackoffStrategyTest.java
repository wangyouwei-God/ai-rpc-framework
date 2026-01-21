package com.aicore.rpc.retry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BackoffStrategy.
 */
class BackoffStrategyTest {

    @Test
    @DisplayName("Should calculate exponential backoff correctly")
    void testExponentialBackoff() {
        RetryConfig config = RetryConfig.builder()
                .baseDelayMs(100)
                .maxDelayMs(10000)
                .multiplier(2.0)
                .jitterFactor(0) // No jitter for predictable testing
                .build();
        BackoffStrategy strategy = new BackoffStrategy(config);

        assertEquals(100, strategy.calculateDelay(0)); // 100 * 2^0 = 100
        assertEquals(200, strategy.calculateDelay(1)); // 100 * 2^1 = 200
        assertEquals(400, strategy.calculateDelay(2)); // 100 * 2^2 = 400
        assertEquals(800, strategy.calculateDelay(3)); // 100 * 2^3 = 800
    }

    @Test
    @DisplayName("Should cap at max delay")
    void testMaxDelayCap() {
        RetryConfig config = RetryConfig.builder()
                .baseDelayMs(100)
                .maxDelayMs(500)
                .multiplier(2.0)
                .jitterFactor(0)
                .build();
        BackoffStrategy strategy = new BackoffStrategy(config);

        // 100 * 2^3 = 800, but should be capped at 500
        assertEquals(500, strategy.calculateDelay(3));
        assertEquals(500, strategy.calculateDelay(10));
    }

    @Test
    @DisplayName("Should apply jitter within range")
    void testJitter() {
        RetryConfig config = RetryConfig.builder()
                .baseDelayMs(100)
                .maxDelayMs(10000)
                .multiplier(2.0)
                .jitterFactor(0.5) // 50% jitter
                .build();
        BackoffStrategy strategy = new BackoffStrategy(config);

        // For attempt 0, base delay is 100. With 50% jitter, range is 50-150
        long delay = strategy.calculateDelay(0);
        assertTrue(delay >= 50 && delay <= 150, "Delay with jitter should be in range: " + delay);
    }

    @Test
    @DisplayName("Should calculate full jitter correctly")
    void testFullJitter() {
        RetryConfig config = RetryConfig.builder()
                .baseDelayMs(100)
                .maxDelayMs(10000)
                .multiplier(2.0)
                .build();
        BackoffStrategy strategy = new BackoffStrategy(config);

        // Full jitter should be between 0 and max
        long delay = strategy.calculateDelayFullJitter(0);
        assertTrue(delay >= 0 && delay <= 100, "Full jitter delay should be in range: " + delay);
    }
}
