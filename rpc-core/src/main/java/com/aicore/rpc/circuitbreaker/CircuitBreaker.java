package com.aicore.rpc.circuitbreaker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Circuit Breaker implementation with sliding window metrics.
 * Thread-safe implementation using atomic operations.
 */
public class CircuitBreaker {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreaker.class);

    private final String name;
    private final CircuitBreakerConfig config;
    private final AtomicReference<CircuitBreakerState> state;
    private final AtomicLong lastStateTransitionTime;
    private final SlidingWindowMetrics metrics;
    private final AtomicInteger halfOpenCallCount;

    public CircuitBreaker(String name, CircuitBreakerConfig config) {
        this.name = name;
        this.config = config;
        this.state = new AtomicReference<>(CircuitBreakerState.CLOSED);
        this.lastStateTransitionTime = new AtomicLong(System.currentTimeMillis());
        this.metrics = new SlidingWindowMetrics(config.getSlidingWindowSize());
        this.halfOpenCallCount = new AtomicInteger(0);
    }

    public CircuitBreaker(String name) {
        this(name, CircuitBreakerConfig.defaultConfig());
    }

    /**
     * Check if the circuit breaker allows the call to proceed.
     * 
     * @return true if the call is allowed, false if rejected
     */
    public boolean allowRequest() {
        CircuitBreakerState currentState = state.get();

        switch (currentState) {
            case CLOSED:
                return true;

            case OPEN:
                if (shouldTransitionToHalfOpen()) {
                    transitionTo(CircuitBreakerState.HALF_OPEN);
                    halfOpenCallCount.set(0);
                    return true;
                }
                return false;

            case HALF_OPEN:
                int currentCount = halfOpenCallCount.incrementAndGet();
                return currentCount <= config.getPermittedNumberOfCallsInHalfOpenState();

            default:
                return false;
        }
    }

    /**
     * Record a successful call.
     * 
     * @param durationMs call duration in milliseconds
     */
    public void recordSuccess(long durationMs) {
        boolean isSlow = durationMs >= config.getSlowCallDurationThresholdMs();
        metrics.recordSuccess(isSlow);

        CircuitBreakerState currentState = state.get();
        if (currentState == CircuitBreakerState.HALF_OPEN) {
            if (shouldCloseCircuit()) {
                transitionTo(CircuitBreakerState.CLOSED);
                metrics.reset();
            }
        }
    }

    /**
     * Record a failed call.
     */
    public void recordFailure() {
        metrics.recordFailure();

        CircuitBreakerState currentState = state.get();
        if (currentState == CircuitBreakerState.CLOSED) {
            if (shouldOpenCircuit()) {
                transitionTo(CircuitBreakerState.OPEN);
            }
        } else if (currentState == CircuitBreakerState.HALF_OPEN) {
            transitionTo(CircuitBreakerState.OPEN);
        }
    }

    /**
     * Get current state of the circuit breaker.
     */
    public CircuitBreakerState getState() {
        return state.get();
    }

    /**
     * Get the name of this circuit breaker.
     */
    public String getName() {
        return name;
    }

    /**
     * Get current metrics.
     */
    public SlidingWindowMetrics getMetrics() {
        return metrics;
    }

    private boolean shouldOpenCircuit() {
        if (metrics.getTotalCalls() < config.getMinimumNumberOfCalls()) {
            return false;
        }

        float failureRate = metrics.getFailureRate();
        float slowCallRate = metrics.getSlowCallRate();

        return failureRate >= config.getFailureRateThreshold()
                || slowCallRate >= config.getSlowCallRateThreshold();
    }

    private boolean shouldCloseCircuit() {
        int halfOpenCalls = halfOpenCallCount.get();
        if (halfOpenCalls < config.getPermittedNumberOfCallsInHalfOpenState()) {
            return false;
        }

        float failureRate = metrics.getFailureRate();
        return failureRate < config.getFailureRateThreshold();
    }

    private boolean shouldTransitionToHalfOpen() {
        long elapsed = System.currentTimeMillis() - lastStateTransitionTime.get();
        return elapsed >= config.getWaitDurationInOpenStateMs();
    }

    private void transitionTo(CircuitBreakerState newState) {
        CircuitBreakerState oldState = state.getAndSet(newState);
        if (oldState != newState) {
            lastStateTransitionTime.set(System.currentTimeMillis());
            logger.info("[CircuitBreaker:{}] State transition: {} -> {}", name, oldState, newState);
        }
    }

    /**
     * Force the circuit breaker to a specific state. For testing purposes.
     */
    public void forceState(CircuitBreakerState newState) {
        transitionTo(newState);
        if (newState == CircuitBreakerState.CLOSED) {
            metrics.reset();
        }
    }
}
