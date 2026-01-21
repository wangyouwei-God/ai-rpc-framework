package com.aicore.rpc.retry;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Calculates retry delay using exponential backoff with jitter.
 * 
 * Algorithm:
 * 1. Base delay: baseDelay * multiplier^attempt
 * 2. Apply jitter: delay += random(-jitter, +jitter)
 * 3. Cap at maxDelay
 */
public class BackoffStrategy {

    private final RetryConfig config;

    public BackoffStrategy(RetryConfig config) {
        this.config = config;
    }

    /**
     * Calculate the delay before the next retry attempt.
     * 
     * @param attempt current attempt number (0-based)
     * @return delay in milliseconds
     */
    public long calculateDelay(int attempt) {
        // Exponential backoff: baseDelay * multiplier^attempt
        double delay = config.getBaseDelayMs() * Math.pow(config.getMultiplier(), attempt);

        // Cap at max delay
        delay = Math.min(delay, config.getMaxDelayMs());

        // Add jitter to prevent thundering herd
        if (config.getJitterFactor() > 0) {
            double jitter = delay * config.getJitterFactor();
            double randomJitter = ThreadLocalRandom.current().nextDouble(-jitter, jitter);
            delay += randomJitter;
        }

        // Ensure delay is not negative
        return Math.max(0, (long) delay);
    }

    /**
     * Calculate delay with full jitter (AWS style).
     * delay = random(0, min(cap, baseDelay * 2^attempt))
     * 
     * @param attempt current attempt number
     * @return delay in milliseconds
     */
    public long calculateDelayFullJitter(int attempt) {
        double maxDelay = config.getBaseDelayMs() * Math.pow(config.getMultiplier(), attempt);
        maxDelay = Math.min(maxDelay, config.getMaxDelayMs());
        return ThreadLocalRandom.current().nextLong(0, Math.max(1, (long) maxDelay));
    }

    /**
     * Calculate delay with decorrelated jitter.
     * delay = min(cap, random(baseDelay, previousDelay * 3))
     * 
     * @param previousDelay the delay from the previous attempt
     * @return delay in milliseconds
     */
    public long calculateDelayDecorrelated(long previousDelay) {
        long minDelay = config.getBaseDelayMs();
        long maxDelay = Math.min(config.getMaxDelayMs(), previousDelay * 3);
        if (maxDelay <= minDelay) {
            return minDelay;
        }
        return ThreadLocalRandom.current().nextLong(minDelay, maxDelay);
    }
}
