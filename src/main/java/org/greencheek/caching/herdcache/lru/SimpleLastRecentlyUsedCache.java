package org.greencheek.caching.herdcache.lru;

import com.google.common.util.concurrent.*;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import org.greencheek.caching.herdcache.Cache;

import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;


public class SimpleLastRecentlyUsedCache<V> implements Cache<V> {

    private final ConcurrentMap<String,ListenableFuture<V>> store;
    private final CacheValueComputationFailureHandler failureHandler;

    public SimpleLastRecentlyUsedCache(int maxCapacity ) {
        this(maxCapacity,maxCapacity);
    }

    public SimpleLastRecentlyUsedCache() {
        this(100,100);
    }

    public SimpleLastRecentlyUsedCache(int initialCapacity,int maxCapacity) {
        store =  new ConcurrentLinkedHashMap.Builder<String, ListenableFuture<V>>()
                .initialCapacity(initialCapacity)
                .maximumWeightedCapacity(maxCapacity)
                .build();

        failureHandler = (String key, Throwable t) -> { store.remove(key); };
    }

    @Override
    public ListenableFuture<V> apply(final String key, Supplier<V> computation, ListeningExecutorService executorService) {
        SettableFuture<V> toBeComputedFuture =  SettableFuture.create();
        ListenableFuture<V> previousFuture = store.putIfAbsent(key, toBeComputedFuture);
        if(previousFuture==null) {
            ListenableFuture<V> computationFuture = executorService.submit(() -> computation.get());
            Futures.addCallback(computationFuture,
                    new CacheRequestFutureComputationCompleteNotifier<V>(key,toBeComputedFuture,failureHandler));
            return toBeComputedFuture;
        } else {
            return previousFuture;
        }

    }

    @Override
    public ListenableFuture<V> get(String key, ListeningExecutorService executorService) {
        return store.get(key);
    }
}
