package org.greencheek.caching.herdcache;


import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.greencheek.caching.herdcache.await.AwaitOnFuture;

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Created by dominictootell on 27/07/2014.
 */
public interface Cache<V> extends AwaitOnFuture<V> {
    public ListenableFuture<V> apply(String key, Supplier<V> computation, ListeningExecutorService executorService);
    default public ListenableFuture<V> get(String key) {
        return get(key, MoreExecutors.sameThreadExecutor());
    }
    public ListenableFuture<V> get(String key,ListeningExecutorService executorService);
}
