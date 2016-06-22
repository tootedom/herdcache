package org.greencheek.caching.herdcache.util.futures;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * DO NOT USE outside of hercache
 */
public class DoNothingSettableFuture<V> implements SettableFuture<V> {

    @Override
    public boolean set(@Nullable V value) {
        return true;
    }

    @Override
    public boolean setException(Throwable throwable) {
        return true;
    }

    @Override
    public void addListener(Runnable listener, Executor executor) {

    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return true;
    }

    @Override
    public boolean isCancelled() {
        return true;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }
}
