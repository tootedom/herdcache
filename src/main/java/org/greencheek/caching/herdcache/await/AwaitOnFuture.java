package org.greencheek.caching.herdcache.await;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface AwaitOnFuture<V> {

    default public V awaitForFutureOrElse(ListenableFuture<V> future, V onExceptionValue) {
        try {
            return future.get();
        } catch (Exception e) {
            return onExceptionValue;
        }
    }

    default public V awaitForFutureOrElse(ListenableFuture<V> future, V onExceptionValue, V onTimeoutValue,
                                          long duration, TimeUnit timeUnit) {
        try {
            return future.get(duration,timeUnit);
        } catch (TimeoutException e) {
            return onTimeoutValue;
        } catch(Exception e ) {
            return onExceptionValue;
        }
    }
}
