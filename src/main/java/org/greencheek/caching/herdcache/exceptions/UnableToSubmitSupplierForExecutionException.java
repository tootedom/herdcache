package org.greencheek.caching.herdcache.exceptions;

/**
 * Represents an exception occurred attempting to submit a task to
 * the executor service that obtains a value from a user provided {@link java.util.function.Supplier}
 */
public class UnableToSubmitSupplierForExecutionException extends ScheduledExecutionException {
    public UnableToSubmitSupplierForExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public Throwable fillInStackTrace() {
        return null;
    }
}
