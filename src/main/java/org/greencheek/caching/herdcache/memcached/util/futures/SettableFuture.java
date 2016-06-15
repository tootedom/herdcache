package org.greencheek.caching.herdcache.memcached.util.futures;


import com.google.common.util.concurrent.ListenableFuture;

import javax.annotation.Nullable;

/**
 * Guava SettableFuture is final.  This interface is to allows us to have
 * a no operation version of SettableFuture
 */
public interface SettableFuture<V> extends ListenableFuture<V> {
    public boolean set(@Nullable V value);
    public boolean setException(Throwable throwable);
}
