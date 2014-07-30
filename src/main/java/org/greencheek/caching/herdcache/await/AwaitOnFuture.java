package org.greencheek.caching.herdcache.await;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

/**
 * Created by dominictootell on 27/07/2014.
 */
public interface AwaitOnFuture<V> {

    default public V awaitForFutureOrElse(ListenableFuture<V> future, V onExceptionValue) {
        try {
            return future.get();
        } catch (Exception e) {
            return onExceptionValue;
        }
    }
}
