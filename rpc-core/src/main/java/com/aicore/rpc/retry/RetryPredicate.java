package com.aicore.rpc.retry;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import com.aicore.rpc.circuitbreaker.CircuitBreakerOpenException;

/**
 * Determines if an exception is retryable.
 * Transient failures (network issues, timeouts) are retryable.
 * Permanent failures (business errors) are not retryable.
 */
public class RetryPredicate {

    /**
     * Check if the given exception should trigger a retry.
     * 
     * @param throwable the exception to check
     * @param config    retry configuration
     * @return true if the exception is retryable
     */
    public static boolean isRetryable(Throwable throwable, RetryConfig config) {
        if (throwable == null) {
            return false;
        }

        // Never retry circuit breaker open exceptions
        if (throwable instanceof CircuitBreakerOpenException) {
            return false;
        }

        // Timeout exceptions
        if (throwable instanceof TimeoutException ||
                throwable instanceof SocketTimeoutException) {
            return config.isRetryOnTimeout();
        }

        // Connection failures - always retryable
        if (throwable instanceof ConnectException) {
            return true;
        }

        // IO exceptions - usually retryable
        if (throwable instanceof java.io.IOException) {
            return true;
        }

        // Check the cause recursively
        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            return isRetryable(cause, config);
        }

        // Default: not retryable
        return false;
    }

    /**
     * Check if the given exception is retryable using default config.
     */
    public static boolean isRetryable(Throwable throwable) {
        return isRetryable(throwable, RetryConfig.defaultConfig());
    }
}
