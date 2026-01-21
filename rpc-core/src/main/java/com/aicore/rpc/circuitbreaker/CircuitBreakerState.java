package com.aicore.rpc.circuitbreaker;

/**
 * Circuit Breaker states.
 */
public enum CircuitBreakerState {
    /**
     * Normal state. Requests are allowed through.
     * Failure rate is monitored.
     */
    CLOSED,

    /**
     * Failure threshold exceeded. All requests are rejected.
     * Waits for recovery duration before transitioning to HALF_OPEN.
     */
    OPEN,

    /**
     * Testing state. A limited number of requests are allowed through
     * to determine if the service has recovered.
     */
    HALF_OPEN
}
