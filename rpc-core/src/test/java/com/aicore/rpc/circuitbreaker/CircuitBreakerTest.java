package com.aicore.rpc.circuitbreaker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CircuitBreaker.
 */
class CircuitBreakerTest {

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        CircuitBreakerConfig config = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(5)
                .slidingWindowSize(10)
                .waitDurationInOpenStateMs(1000)
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();
        circuitBreaker = new CircuitBreaker("test-cb", config);
    }

    @Test
    @DisplayName("Initial state should be CLOSED")
    void testInitialState() {
        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
    }

    @Test
    @DisplayName("Should allow requests in CLOSED state")
    void testAllowRequestInClosedState() {
        assertTrue(circuitBreaker.allowRequest());
    }

    @Test
    @DisplayName("Should transition to OPEN when failure rate exceeds threshold")
    void testTransitionToOpenOnHighFailureRate() {
        // Record some calls to meet minimum
        for (int i = 0; i < 5; i++) {
            circuitBreaker.recordFailure();
        }

        // Should transition to OPEN (100% failure rate > 50% threshold)
        assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
    }

    @Test
    @DisplayName("Should reject requests in OPEN state")
    void testRejectRequestInOpenState() {
        // Force to OPEN state
        circuitBreaker.forceState(CircuitBreakerState.OPEN);

        // Should not allow immediately
        assertFalse(circuitBreaker.allowRequest());
    }

    @Test
    @DisplayName("Should stay CLOSED when failure rate is below threshold")
    void testStayClosedWhenFailureRateBelowThreshold() {
        // Record 4 successes and 1 failure (20% failure rate < 50% threshold)
        for (int i = 0; i < 4; i++) {
            circuitBreaker.recordSuccess(10);
        }
        circuitBreaker.recordFailure();

        assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
    }

    @Test
    @DisplayName("Should record success correctly")
    void testRecordSuccess() {
        circuitBreaker.recordSuccess(100);

        SlidingWindowMetrics metrics = circuitBreaker.getMetrics();
        assertEquals(1, metrics.getTotalCalls());
        assertEquals(0, metrics.getFailureRate());
    }

    @Test
    @DisplayName("Should record failure correctly")
    void testRecordFailure() {
        circuitBreaker.recordFailure();

        SlidingWindowMetrics metrics = circuitBreaker.getMetrics();
        assertEquals(1, metrics.getTotalCalls());
    }
}
