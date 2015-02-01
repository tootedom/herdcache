package org.greencheek.caching.herdcache;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.time.Duration;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Created by dominictootell on 23/08/2014.
 */
public interface CacheWithExpiry<V> extends Cache<V> {
    public ListenableFuture<V> apply(String key, Supplier<V> computation, Duration timeToLive, ListeningExecutorService executorService);
    public ListenableFuture<V> apply(String key, Supplier<V> computation, Duration timeToLive,
                                     ListeningExecutorService executorService, Predicate<V> canCacheValueEvalutor);

}
