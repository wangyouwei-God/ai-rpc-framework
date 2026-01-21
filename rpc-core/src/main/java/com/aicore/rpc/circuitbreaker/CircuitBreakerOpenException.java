package com.aicore.rpc.circuitbreaker;

/**
 * Exception thrown when a circuit breaker is open and rejects calss.
 */
public class CircuitBreakerOpenException extends RuntimeException {

    private final String circuitBreakerName;
    private final CircuitBreakerState state;

    public CircuitBreakerOpenException(String circuitBreakerName, CircuitBreakerState state) {
        super("CircuitBreaker '" + circuitBreakerName + "' is " + state + " and does not permit calls");
        this.circuitBreakerName = circuitBreakerName;
        this.state = state;
    }

    public String getCircuitBreakerName() {
        return circuitBreakerName;
    }

    public CircuitBreakerState getState() {
        return state;
    }
}
