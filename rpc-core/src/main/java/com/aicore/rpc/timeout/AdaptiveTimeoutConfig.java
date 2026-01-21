package com.aicore.rpc.timeout;

/**
 * Configuration for adaptive timeout calculation.
 */
public class AdaptiveTimeoutConfig {

    private final long minTimeoutMs;
    private final long maxTimeoutMs;
    private final long defaultTimeoutMs;
    private final double safetyFactor;
    private final double percentile;
    private final int minimumSamples;
    private final int sampleWindowSize;

    private AdaptiveTimeoutConfig(Builder builder) {
        this.minTimeoutMs = builder.minTimeoutMs;
        this.maxTimeoutMs = builder.maxTimeoutMs;
        this.defaultTimeoutMs = builder.defaultTimeoutMs;
        this.safetyFactor = builder.safetyFactor;
        this.percentile = builder.percentile;
        this.minimumSamples = builder.minimumSamples;
        this.sampleWindowSize = builder.sampleWindowSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static AdaptiveTimeoutConfig defaultConfig() {
        return builder().build();
    }

    public long getMinTimeoutMs() {
        return minTimeoutMs;
    }

    public long getMaxTimeoutMs() {
        return maxTimeoutMs;
    }

    public long getDefaultTimeoutMs() {
        return defaultTimeoutMs;
    }

    public double getSafetyFactor() {
        return safetyFactor;
    }

    public double getPercentile() {
        return percentile;
    }

    public int getMinimumSamples() {
        return minimumSamples;
    }

    public int getSampleWindowSize() {
        return sampleWindowSize;
    }

    public static class Builder {
        private long minTimeoutMs = 100;
        private long maxTimeoutMs = 30000;
        private long defaultTimeoutMs = 10000;
        private double safetyFactor = 1.5;
        private double percentile = 99.0;
        private int minimumSamples = 10;
        private int sampleWindowSize = 1000;

        public Builder minTimeoutMs(long minTimeoutMs) {
            this.minTimeoutMs = minTimeoutMs;
            return this;
        }

        public Builder maxTimeoutMs(long maxTimeoutMs) {
            this.maxTimeoutMs = maxTimeoutMs;
            return this;
        }

        public Builder defaultTimeoutMs(long defaultTimeoutMs) {
            this.defaultTimeoutMs = defaultTimeoutMs;
            return this;
        }

        public Builder safetyFactor(double safetyFactor) {
            this.safetyFactor = safetyFactor;
            return this;
        }

        public Builder percentile(double percentile) {
            this.percentile = percentile;
            return this;
        }

        public Builder minimumSamples(int minimumSamples) {
            this.minimumSamples = minimumSamples;
            return this;
        }

        public Builder sampleWindowSize(int sampleWindowSize) {
            this.sampleWindowSize = sampleWindowSize;
            return this;
        }

        public AdaptiveTimeoutConfig build() {
            return new AdaptiveTimeoutConfig(this);
        }
    }
}
