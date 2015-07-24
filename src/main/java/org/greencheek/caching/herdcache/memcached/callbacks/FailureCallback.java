package org.greencheek.caching.herdcache.memcached.callbacks;

/**
 * Functional interface that is executed when a cache failure occurs
 * (A timeout, or exception thrown, for example)
 */
@FunctionalInterface
public interface FailureCallback {
    public void onFailure(Throwable t);
}
