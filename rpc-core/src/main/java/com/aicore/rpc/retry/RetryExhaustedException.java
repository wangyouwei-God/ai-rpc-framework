package com.aicore.rpc.retry;

/**
 * Exception thrown when all retry attempts have been exhausted.
 */
public class RetryExhaustedException extends RuntimeException {

    private final int attemptCount;

    public RetryExhaustedException(String message, Throwable cause) {
        super(message, cause);
        this.attemptCount = -1;
    }

    public RetryExhaustedException(String message, Throwable cause, int attemptCount) {
        super(message, cause);
        this.attemptCount = attemptCount;
    }

    public int getAttemptCount() {
        return attemptCount;
    }
}
