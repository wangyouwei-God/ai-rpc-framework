package com.aicore.rpc.retry;

/**
 * Configuration for retry behavior.
 */
public class RetryConfig {

    private final int maxAttempts;
    private final long baseDelayMs;
    private final long maxDelayMs;
    private final double multiplier;
    private final double jitterFactor;
    private final boolean retryOnTimeout;

    private RetryConfig(Builder builder) {
        this.maxAttempts = builder.maxAttempts;
        this.baseDelayMs = builder.baseDelayMs;
        this.maxDelayMs = builder.maxDelayMs;
        this.multiplier = builder.multiplier;
        this.jitterFactor = builder.jitterFactor;
        this.retryOnTimeout = builder.retryOnTimeout;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RetryConfig defaultConfig() {
        return builder().build();
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public long getBaseDelayMs() {
        return baseDelayMs;
    }

    public long getMaxDelayMs() {
        return maxDelayMs;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public double getJitterFactor() {
        return jitterFactor;
    }

    public boolean isRetryOnTimeout() {
        return retryOnTimeout;
    }

    public static class Builder {
        private int maxAttempts = 3;
        private long baseDelayMs = 100;
        private long maxDelayMs = 10000;
        private double multiplier = 2.0;
        private double jitterFactor = 0.5;
        private boolean retryOnTimeout = true;

        public Builder maxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder baseDelayMs(long baseDelayMs) {
            this.baseDelayMs = baseDelayMs;
            return this;
        }

        public Builder maxDelayMs(long maxDelayMs) {
            this.maxDelayMs = maxDelayMs;
            return this;
        }

        public Builder multiplier(double multiplier) {
            this.multiplier = multiplier;
            return this;
        }

        public Builder jitterFactor(double jitterFactor) {
            this.jitterFactor = jitterFactor;
            return this;
        }

        public Builder retryOnTimeout(boolean retryOnTimeout) {
            this.retryOnTimeout = retryOnTimeout;
            return this;
        }

        public RetryConfig build() {
            return new RetryConfig(this);
        }
    }
}
