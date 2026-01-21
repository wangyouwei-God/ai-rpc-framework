package com.aicore.rpc.integration;

import com.aicore.rpc.circuitbreaker.*;
import com.aicore.rpc.retry.*;
import com.aicore.rpc.timeout.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for resilience components working together.
 * Tests CircuitBreaker + SmartRetry + AdaptiveTimeout integration.
 */
class ResilienceIntegrationTest {

    private CircuitBreaker circuitBreaker;
    private SmartRetry smartRetry;
    private AdaptiveTimeout adaptiveTimeout;

    @BeforeEach
    void setUp() {
        // Setup Circuit Breaker
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(5)
                .slidingWindowSize(10)
                .waitDurationInOpenStateMs(100)
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
        circuitBreaker = new CircuitBreaker("integration-test-" + System.nanoTime(), cbConfig);

        // Setup Smart Retry
        RetryConfig retryConfig = RetryConfig.builder()
                .maxAttempts(3)
                .baseDelayMs(10)
                .maxDelayMs(50)
                .multiplier(2.0)
                .jitterFactor(0.1)
                .build();
        smartRetry = new SmartRetry(retryConfig);

        // Setup Adaptive Timeout with correct API
        AdaptiveTimeoutConfig atConfig = AdaptiveTimeoutConfig.builder()
                .minTimeoutMs(100)
                .maxTimeoutMs(5000)
                .safetyFactor(1.5)
                .build();
        adaptiveTimeout = new AdaptiveTimeout("integration-test-" + System.nanoTime(), atConfig);
    }

    @Test
    @DisplayName("Retry should work with circuit breaker allowing requests")
    void testRetryWithCircuitBreakerClosed() throws Exception {
        AtomicInteger attempts = new AtomicInteger(0);

        String result = smartRetry.execute(() -> {
            if (!circuitBreaker.allowRequest()) {
                throw new CircuitBreakerOpenException("cb", CircuitBreakerState.OPEN);
            }
            int attempt = attempts.incrementAndGet();
            if (attempt < 2) {
                circuitBreaker.recordFailure();
                throw new IOException("Transient failure");
            }
            circuitBreaker.recordSuccess(10);
            return "success";
        });

        assertEquals("success", result);
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
    }

    @Test
    @DisplayName("Circuit breaker should open after retries exhaust")
    void testCircuitBreakerOpensAfterRepeatedFailures() {
        // Generate enough failures to open the circuit breaker
        for (int i = 0; i < 5; i++) {
            try {
                smartRetry.execute(() -> {
                    if (circuitBreaker.allowRequest()) {
                        circuitBreaker.recordFailure();
                    }
                    throw new IOException("Persistent failure");
                });
            } catch (Exception e) {
                // Expected
            }
        }

        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
    }

    @Test
    @DisplayName("Adaptive timeout should adjust based on recorded latencies")
    void testAdaptiveTimeoutAdjustment() {
        // Record some latencies
        for (int i = 0; i < 100; i++) {
            adaptiveTimeout.recordLatency(50 + (i % 50)); // 50-99ms range
        }

        long timeout = adaptiveTimeout.getTimeoutMs();

        // Timeout should be based on P99 * safetyFactor, within bounds
        assertTrue(timeout >= 100, "Timeout should be at least minTimeout");
        assertTrue(timeout <= 5000, "Timeout should be at most maxTimeout");
    }

    @Test
    @DisplayName("All components should work together in a success scenario")
    void testFullIntegration() throws Exception {
        AtomicInteger callCount = new AtomicInteger(0);

        String result = smartRetry.execute(() -> {
            // Check circuit breaker
            if (!circuitBreaker.allowRequest()) {
                throw new CircuitBreakerOpenException("cb", CircuitBreakerState.OPEN);
            }

            // Get adaptive timeout
            long timeout = adaptiveTimeout.getTimeoutMs();
            assertTrue(timeout > 0);

            int count = callCount.incrementAndGet();
            long startTime = System.currentTimeMillis();

            // Simulate some work
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long latency = System.currentTimeMillis() - startTime;
            adaptiveTimeout.recordLatency(latency);
            circuitBreaker.recordSuccess(latency);

            return "result-" + count;
        });

        assertEquals("result-1", result);
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
    }

    @Test
    @DisplayName("Should handle failure and recovery cycle")
    void testFailureAndRecoveryCycle() throws Exception {
        // Phase 1: Generate failures to open circuit
        for (int i = 0; i < 5; i++) {
            if (circuitBreaker.allowRequest()) {
                circuitBreaker.recordFailure();
            }
        }
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());

        // Phase 2: Wait for circuit to transition to half-open
        Thread.sleep(150); // Wait for open duration

        // Phase 3: Make a request to trigger transition to half-open
        assertTrue(circuitBreaker.allowRequest());
        assertEquals(CircuitBreakerState.HALF_OPEN, circuitBreaker.getState());

        // Phase 4: A failure in half-open should open the circuit again
        circuitBreaker.recordFailure();
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
    }
}
