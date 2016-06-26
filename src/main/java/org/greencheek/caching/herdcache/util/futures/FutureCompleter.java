package org.greencheek.caching.herdcache.util.futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ConcurrentMap;

/**
 */
public class FutureCompleter {
    public static <V> void completeWithValue(SettableFuture<V> promise, String keyString, V cachedObject,
                                             ConcurrentMap<String, ListenableFuture<V>> internalCache,
                                             boolean removeFromInternalCacheBeforeCompletion) {
        if (removeFromInternalCacheBeforeCompletion) {
            internalCache.remove(keyString);
            promise.set(cachedObject);
        } else {
            promise.set(cachedObject);
            internalCache.remove(keyString);
        }
    }

    public static <V> void completeWithException(SettableFuture<V> promise, String keyString, Throwable exception,
                                                 ConcurrentMap<String, ListenableFuture<V>> internalCache,
                                                 boolean removeFromInternalCacheBeforeCompletion) {
        if (removeFromInternalCacheBeforeCompletion) {
            internalCache.remove(keyString);
            promise.setException(exception);
        } else {
            promise.setException(exception);
            internalCache.remove(keyString);
        }
    }
}
