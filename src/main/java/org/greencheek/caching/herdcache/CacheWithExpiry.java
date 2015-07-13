package org.greencheek.caching.herdcache;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.time.Duration;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 *
 */
public interface CacheWithExpiry<V> extends Cache<V> {
    public ListenableFuture<V> apply(String key, Supplier<V> computation, Duration timeToLive, ListeningExecutorService executorService);

    /**
     * @param key The key to obtain/cache a value under
     * @param computation The function that would calculate the value to be cached
     * @param timeToLive How long the value should be cached for
     * @param executorService The executor service in which to run the futures.
     * @param isSupplierValueCachable Should the value returned by the #computation Supplier be cached or not
     * @return
     */
    public ListenableFuture<V> apply(String key, Supplier<V> computation, Duration timeToLive,
                                     ListeningExecutorService executorService, Predicate<V> isSupplierValueCachable);


    /**
     * obtain a value from the cache.  The cached value is only used if the @link #isCachedValueValid predict returns
     * return.  The Predicate evaluates the cached value, if it returns true, the cached value should be allowed,
     * otherwise the @link #computation Supplier is called to provide the value.  The @link #canCacheValueEvalutor
     * predicate is used to evaluate if the value returned by the @link #computation Supplier should be cached or not.
     * The item is stored in the cache with an infinite TTL
     *
     * @param key The key to obtain/cache a value under
     * @param computation The function that would calculate the value to be cached
     * @param executorService The executor service in which to run the futures.
     * @param isSupplierValueCachable Should the value returned by the #computation Supplier be cached or not
     * @param isCachedValueValid Should the value returned by the cache be returned or not (and therefore the supplier called).
     * @return
     */
    default ListenableFuture<V> apply(String key, Supplier<V> computation,
                                      ListeningExecutorService executorService, Predicate<V> isSupplierValueCachable,
                                      Predicate<V> isCachedValueValid) {
        return apply(key,computation,NO_TTL,executorService,isSupplierValueCachable,isCachedValueValid);
    }

    /**
     * obtain a value from the cache.  The cached value is only used if the @link #isCachedValueValid predict returns
     * return.  The Predicate evaluates the cached value, if it returns true, the cached value should be allowed,
     * otherwise the @link #computation Supplier is called to provide the value.  The @link #canCacheValueEvalutor
     * predicate is used to evaluate if the value returned by the @link #computation Supplier should be cached or not.
     *
     * @param key The key to obtain/cache a value under
     * @param computation The function that would calculate the value to be cached
     * @param timeToLive How long the value should be cached for
     * @param executorService The executor service in which to run the futures.
     * @param isSupplierValueCachable Should the value returned by the #computation Supplier be cached or not
     * @param isCachedValueValid Should the value returned by the cache be returned or not (and therefore the supplier called).
     * @return
     */
    public ListenableFuture<V> apply(String key, Supplier<V> computation, Duration timeToLive,
                                     ListeningExecutorService executorService, Predicate<V> isSupplierValueCachable,
                                     Predicate<V> isCachedValueValid);

}
