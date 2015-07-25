package org.greencheek.caching.herdcache.exceptions;

/**
 * Represents an exception occurred attempting to submit a task to
 * the executor service.  The task being submitted is that of a GET from
 * the cache.
 */
public class UnableToScheduleCacheGetExecutionException extends ScheduledExecutionException {
    public UnableToScheduleCacheGetExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public Throwable fillInStackTrace() {
        return null;
    }
}
