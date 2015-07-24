package org.greencheek.caching.herdcache.memcached.callbacks;

/**
 * Functional interface that is executed when a cache failure occurs
 * (A timeout, or exception thrown, for example)
 */
@FunctionalInterface
public interface SuccessCallback<V> {
    public void onSuccess(V result);
}
