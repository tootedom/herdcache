package org.greencheek.caching.herdcache.lru;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import org.greencheek.caching.herdcache.Cache;
import org.greencheek.caching.herdcache.IsCachedValueUsable;
import org.greencheek.caching.herdcache.IsSupplierValueCachable;

import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.function.Supplier;


/**
 * Not fully supported.  Do not use.  Will be removed in future version
 * @param <V>
 */
@Deprecated
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
                    new CacheRequestFutureComputationCompleteNotifier<V>(key, toBeComputedFuture, failureHandler, (result) -> {
                        if (!canCacheValueEvalutor.test(result)) {
                            store.remove(key, toBeComputedFuture);
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

    @Override
    public ListenableFuture<V> set(String keyString, Supplier<V> value, Predicate<V> canCacheValueEvalutor, ListeningExecutorService executorService) {
        return apply(keyString,value,executorService,canCacheValueEvalutor,IsCachedValueUsable.CACHED_VALUE_IS_ALWAYS_USABLE);
    }

    /**
     * returns the size of the cache
     * @return
     */
    public int size() {
        return store.size();
    }
}
