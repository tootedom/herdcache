package org.greencheek.caching.herdcache.exceptions;

/**
 * Represents an exception occurred attempting to submit a task to
 * the executor service
 */
public class ScheduledExecutionException extends RuntimeException {
    public ScheduledExecutionException(String message,Throwable cause) {
        super(message,cause);
    }

    @Override
    public Throwable fillInStackTrace() {
        return null;
    }
}
