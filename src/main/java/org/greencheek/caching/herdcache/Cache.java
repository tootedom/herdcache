package org.greencheek.caching.herdcache;


import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.greencheek.caching.herdcache.await.AwaitOnFuture;

import java.io.Serializable;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Created by dominictootell on 27/07/2014.
 */
public interface Cache<V> extends AwaitOnFuture<V> {
    static final Predicate CAN_ALWAYS_CACHE_VALUE = (X) -> true;


    default public ListenableFuture<V> apply(String key, Supplier<V> computation) {
        return apply(key, computation, MoreExecutors.sameThreadExecutor());
    }

    default public ListenableFuture<V> apply(String key, Supplier<V> computation, ListeningExecutorService executorService) {
        return apply(key, computation, executorService,CAN_ALWAYS_CACHE_VALUE);
    }

    default public ListenableFuture<V> get(String key) {
        return get(key, MoreExecutors.sameThreadExecutor());
    }

    public ListenableFuture<V> apply(String key, Supplier<V> computation, ListeningExecutorService executorService,
                                     Predicate<V> canCacheValueEvalutor);

    public ListenableFuture<V> get(String key,ListeningExecutorService executorService);
}
