package org.greencheek.caching.herdcache.lru;

import com.google.common.util.concurrent.*;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import org.greencheek.caching.herdcache.Cache;

import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
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

    public ListenableFuture<V> apply(String key, Supplier<V> computation, ListeningExecutorService executorService,
                                     Predicate<V> canCacheValueEvalutor, Predicate<V> cachedValueIsAlwaysValid) {
        SettableFuture<V> toBeComputedFuture =  SettableFuture.create();
        ListenableFuture<V> previousFuture = store.putIfAbsent(key, toBeComputedFuture);
        if(previousFuture==null) {
            ListenableFuture<V> computationFuture = executorService.submit(() -> computation.get());
            Futures.addCallback(computationFuture,
                    new CacheRequestFutureComputationCompleteNotifier<V>(key,toBeComputedFuture,failureHandler,(result) -> {
                        if(!canCacheValueEvalutor.test(result)) {
                            store.remove(key,toBeComputedFuture);
                        }
                    }));
            return toBeComputedFuture;
        } else {
            return previousFuture;
        }
    }

    @Override
    public ListenableFuture<V> get(String key, ListeningExecutorService executorService) {
        ListenableFuture future = store.get(key);
        if(future==null) {
            return Futures.immediateCheckedFuture(null);
        } else {
            return future;
        }
    }

    /**
     * returns the size of the cache
     * @return
     */
    public int size() {
        return store.size();
    }
}
