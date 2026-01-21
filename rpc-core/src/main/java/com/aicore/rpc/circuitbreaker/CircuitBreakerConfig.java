package com.aicore.rpc.circuitbreaker;

/**
 * Configuration for Circuit Breaker.
 */
public class CircuitBreakerConfig {

    private final int failureRateThreshold;
    private final int slowCallRateThreshold;
    private final long slowCallDurationThresholdMs;
    private final long waitDurationInOpenStateMs;
    private final int slidingWindowSize;
    private final int minimumNumberOfCalls;
    private final int permittedNumberOfCallsInHalfOpenState;

    private CircuitBreakerConfig(Builder builder) {
        this.failureRateThreshold = builder.failureRateThreshold;
        this.slowCallRateThreshold = builder.slowCallRateThreshold;
        this.slowCallDurationThresholdMs = builder.slowCallDurationThresholdMs;
        this.waitDurationInOpenStateMs = builder.waitDurationInOpenStateMs;
        this.slidingWindowSize = builder.slidingWindowSize;
        this.minimumNumberOfCalls = builder.minimumNumberOfCalls;
        this.permittedNumberOfCallsInHalfOpenState = builder.permittedNumberOfCallsInHalfOpenState;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CircuitBreakerConfig defaultConfig() {
        return builder().build();
    }

    public int getFailureRateThreshold() {
        return failureRateThreshold;
    }

    public int getSlowCallRateThreshold() {
        return slowCallRateThreshold;
    }

    public long getSlowCallDurationThresholdMs() {
        return slowCallDurationThresholdMs;
    }

    public long getWaitDurationInOpenStateMs() {
        return waitDurationInOpenStateMs;
    }

    public int getSlidingWindowSize() {
        return slidingWindowSize;
    }

    public int getMinimumNumberOfCalls() {
        return minimumNumberOfCalls;
    }

    public int getPermittedNumberOfCallsInHalfOpenState() {
        return permittedNumberOfCallsInHalfOpenState;
    }

    public static class Builder {
        private int failureRateThreshold = 50;
        private int slowCallRateThreshold = 100;
        private long slowCallDurationThresholdMs = 3000;
        private long waitDurationInOpenStateMs = 30000;
        private int slidingWindowSize = 100;
        private int minimumNumberOfCalls = 10;
        private int permittedNumberOfCallsInHalfOpenState = 5;

        public Builder failureRateThreshold(int failureRateThreshold) {
            this.failureRateThreshold = failureRateThreshold;
            return this;
        }

        public Builder slowCallRateThreshold(int slowCallRateThreshold) {
            this.slowCallRateThreshold = slowCallRateThreshold;
            return this;
        }

        public Builder slowCallDurationThresholdMs(long slowCallDurationThresholdMs) {
            this.slowCallDurationThresholdMs = slowCallDurationThresholdMs;
            return this;
        }

        public Builder waitDurationInOpenStateMs(long waitDurationInOpenStateMs) {
            this.waitDurationInOpenStateMs = waitDurationInOpenStateMs;
            return this;
        }

        public Builder slidingWindowSize(int slidingWindowSize) {
            this.slidingWindowSize = slidingWindowSize;
            return this;
        }

        public Builder minimumNumberOfCalls(int minimumNumberOfCalls) {
            this.minimumNumberOfCalls = minimumNumberOfCalls;
            return this;
        }

        public Builder permittedNumberOfCallsInHalfOpenState(int permittedNumberOfCallsInHalfOpenState) {
            this.permittedNumberOfCallsInHalfOpenState = permittedNumberOfCallsInHalfOpenState;
            return this;
        }

        public CircuitBreakerConfig build() {
            return new CircuitBreakerConfig(this);
        }
    }
}
