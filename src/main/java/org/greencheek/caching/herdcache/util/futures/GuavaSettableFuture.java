package org.greencheek.caching.herdcache.util.futures;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 */
public class GuavaSettableFuture<V> implements SettableFuture<V> {

    private final com.google.common.util.concurrent.SettableFuture<V> wrappedFuture = com.google.common.util.concurrent.SettableFuture.create();

    @Override
    public boolean set(@Nullable V value) {
        return wrappedFuture.set(value);
    }

    @Override
    public boolean setException(Throwable throwable) {
        return wrappedFuture.setException(throwable);
    }

    @Override
    public void addListener(Runnable listener, Executor executor) {
        wrappedFuture.addListener(listener,executor);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return wrappedFuture.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return wrappedFuture.isCancelled();
    }

    @Override
    public boolean isDone() {
        return wrappedFuture.isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return wrappedFuture.get();
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return wrappedFuture.get(timeout,unit);
    }
}
