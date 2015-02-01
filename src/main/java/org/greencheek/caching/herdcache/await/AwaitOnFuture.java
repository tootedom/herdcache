package org.greencheek.caching.herdcache.await;

import com.google.common.util.concurrent.ListenableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface AwaitOnFuture<V> {

    static final Logger LOG = LoggerFactory.getLogger(AwaitOnFuture.class);

    default public V awaitForFutureOrElse(ListenableFuture<V> future, V onExceptionValue) {
        try {
            return future.get();
        } catch (Exception e) {
            LOG.error("exception waiting for future",e);
            return onExceptionValue;
        }
    }

    default public V awaitForFutureOrElse(ListenableFuture<V> future, V onExceptionValue, V onTimeoutValue,
                                          long duration, TimeUnit timeUnit) {
        try {
            return future.get(duration,timeUnit);
        } catch (TimeoutException e) {
            LOG.warn("timeout waiting for future",e);
            return onTimeoutValue;
        } catch(Exception e ) {
            LOG.error("exception waiting for future",e);
            return onExceptionValue;
        }
    }
}
