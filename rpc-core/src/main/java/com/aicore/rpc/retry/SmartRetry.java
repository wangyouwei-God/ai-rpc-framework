package com.aicore.rpc.retry;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Smart retry executor with exponential backoff and jitter.
 * Integrates with circuit breaker to avoid retrying on open circuits.
 */
public class SmartRetry {

    private static final Logger logger = LoggerFactory.getLogger(SmartRetry.class);

    private final RetryConfig config;
    private final BackoffStrategy backoffStrategy;
    private final AtomicInteger totalRetryCount;
    private final AtomicInteger successfulRetryCount;

    public SmartRetry(RetryConfig config) {
        this.config = config;
        this.backoffStrategy = new BackoffStrategy(config);
        this.totalRetryCount = new AtomicInteger(0);
        this.successfulRetryCount = new AtomicInteger(0);
    }

    public SmartRetry() {
        this(RetryConfig.defaultConfig());
    }

    /**
     * Execute an operation with retry logic.
     * 
     * @param operation the operation to execute
     * @param <T>       return type
     * @return the result of the operation
     * @throws Exception if all retry attempts fail
     */
    public <T> T execute(Callable<T> operation) throws Exception {
        Exception lastException = null;
        int maxAttempts = config.getMaxAttempts();

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                T result = operation.call();
                if (attempt > 0) {
                    successfulRetryCount.incrementAndGet();
                }
                return result;
            } catch (Exception e) {
                lastException = e;

                // Check if we should retry
                if (!RetryPredicate.isRetryable(e, config)) {
                    throw e;
                }

                // Check if we have more attempts
                if (attempt >= maxAttempts - 1) {
                    break;
                }

                // Calculate and apply backoff delay
                long delay = backoffStrategy.calculateDelay(attempt);
                totalRetryCount.incrementAndGet();

                logger.warn("Attempt {} failed: {}. Retrying in {}ms...",
                        attempt + 1, e.getClass().getSimpleName(), delay);

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }

        // All attempts failed
        throw new RetryExhaustedException(
                "All " + maxAttempts + " retry attempts failed", lastException);
    }

    /**
     * Execute an operation with retry, returning a result or throwing.
     * Convenience method for lambda expressions.
     * 
     * @param operation the operation
     * @param <T>       return type
     * @return the result
     */
    public <T> T executeWithRetry(RetryableOperation<T> operation) throws Exception {
        return execute(() -> operation.execute());
    }

    /**
     * Get total number of retries performed.
     */
    public int getTotalRetryCount() {
        return totalRetryCount.get();
    }

    /**
     * Get number of retries that eventually succeeded.
     */
    public int getSuccessfulRetryCount() {
        return successfulRetryCount.get();
    }

    /**
     * Get retry success rate.
     */
    public double getRetrySuccessRate() {
        int total = totalRetryCount.get();
        if (total == 0) {
            return 1.0;
        }
        return (double) successfulRetryCount.get() / total;
    }

    /**
     * Functional interface for retryable operations.
     */
    @FunctionalInterface
    public interface RetryableOperation<T> {
        T execute() throws Exception;
    }
}
