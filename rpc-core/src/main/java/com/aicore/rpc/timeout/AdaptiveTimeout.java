package com.aicore.rpc.timeout;

/**
 * Adaptive timeout calculator based on historical latency statistics.
 * 
 * Algorithm:
 * 1. Collect latency samples in a sliding window
 * 2. Calculate P99 (or configured percentile) latency
 * 3. Apply safety factor: timeout = P99 * safetyFactor
 * 4. Clamp to [minTimeout, maxTimeout] range
 * 5. Use default timeout if insufficient samples
 */
public class AdaptiveTimeout {

    private final String name;
    private final AdaptiveTimeoutConfig config;
    private final LatencyStatistics statistics;
    private volatile long currentTimeoutMs;

    public AdaptiveTimeout(String name, AdaptiveTimeoutConfig config) {
        this.name = name;
        this.config = config;
        this.statistics = new LatencyStatistics(config.getSampleWindowSize());
        this.currentTimeoutMs = config.getDefaultTimeoutMs();
    }

    public AdaptiveTimeout(String name) {
        this(name, AdaptiveTimeoutConfig.defaultConfig());
    }

    /**
     * Record a successful call latency and update timeout.
     * 
     * @param latencyMs the latency in milliseconds
     */
    public void recordLatency(long latencyMs) {
        statistics.record(latencyMs);
        updateTimeout();
    }

    /**
     * Get the current calculated timeout.
     * 
     * @return timeout in milliseconds
     */
    public long getTimeoutMs() {
        return currentTimeoutMs;
    }

    /**
     * Get the current timeout in seconds.
     * 
     * @return timeout in seconds
     */
    public int getTimeoutSeconds() {
        return (int) Math.ceil(currentTimeoutMs / 1000.0);
    }

    /**
     * Get the name of this adaptive timeout instance.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the underlying latency statistics.
     */
    public LatencyStatistics getStatistics() {
        return statistics;
    }

    /**
     * Update the timeout based on current statistics.
     */
    private void updateTimeout() {
        if (statistics.getSampleCount() < config.getMinimumSamples()) {
            // Not enough samples, use default
            currentTimeoutMs = config.getDefaultTimeoutMs();
            return;
        }

        // Calculate percentile latency
        long percentileLatency = statistics.getPercentile(config.getPercentile());
        if (percentileLatency <= 0) {
            currentTimeoutMs = config.getDefaultTimeoutMs();
            return;
        }

        // Apply safety factor
        long calculatedTimeout = (long) (percentileLatency * config.getSafetyFactor());

        // Clamp to configured range
        currentTimeoutMs = Math.max(config.getMinTimeoutMs(),
                Math.min(config.getMaxTimeoutMs(), calculatedTimeout));
    }

    /**
     * Reset statistics and timeout to default.
     */
    public void reset() {
        statistics.reset();
        currentTimeoutMs = config.getDefaultTimeoutMs();
    }

    @Override
    public String toString() {
        return String.format("AdaptiveTimeout[name=%s, timeout=%dms, samples=%d, p99=%dms]",
                name, currentTimeoutMs, statistics.getSampleCount(), statistics.getP99());
    }
}
