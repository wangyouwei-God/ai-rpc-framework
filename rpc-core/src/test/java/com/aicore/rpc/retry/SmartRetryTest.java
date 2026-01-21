package com.aicore.rpc.retry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SmartRetry.
 */
class SmartRetryTest {

    private SmartRetry smartRetry;

    @BeforeEach
    void setUp() {
        RetryConfig config = RetryConfig.builder()
                .maxAttempts(3)
                .baseDelayMs(10) // Short delay for tests
                .maxDelayMs(100)
                .multiplier(2.0)
                .jitterFactor(0.1)
                .build();
        smartRetry = new SmartRetry(config);
    }

    @Test
    @DisplayName("Should succeed on first attempt")
    void testSuccessOnFirstAttempt() throws Exception {
        String result = smartRetry.execute(() -> "success");

        assertEquals("success", result);
        assertEquals(0, smartRetry.getTotalRetryCount());
    }

    @Test
    @DisplayName("Should retry on transient failure and eventually succeed")
    void testRetryOnTransientFailure() throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);

        String result = smartRetry.execute(() -> {
            int attempt = attempts.incrementAndGet();
            if (attempt < 3) {
                throw new IOException("Transient failure");
            }
            return "success";
        });

        assertEquals("success", result);
        assertEquals(3, attempts.get());
        assertTrue(smartRetry.getTotalRetryCount() > 0);
    }

    @Test
    @DisplayName("Should throw after exhausting all retries")
    void testExhaustRetries() {
        assertThrows(RetryExhaustedException.class, () -> {
            smartRetry.execute(() -> {
                throw new IOException("Always fails");
            });
        });
    }

    @Test
    @DisplayName("Should not retry on non-retryable exception")
    void testNoRetryOnNonRetryableException() {
        AtomicInteger attempts = new AtomicInteger(0);

        assertThrows(IllegalArgumentException.class, () -> {
            smartRetry.execute(() -> {
                attempts.incrementAndGet();
                throw new IllegalArgumentException("Business error");
            });
        });

        assertEquals(1, attempts.get()); // Should only try once
    }
}
