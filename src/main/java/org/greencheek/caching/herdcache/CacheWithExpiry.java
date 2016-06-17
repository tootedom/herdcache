package org.greencheek.caching.herdcache;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

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


    /**
     * Set the cache value under that given key with the specified value
     * @param keyString The key under which to cache the object
     * @param value The value to store in the cache
     * @return
     */
    default public ListenableFuture<V> set(String keyString, V value) {
        return set(keyString,value, MoreExecutors.newDirectExecutorService());
    }

    /**
     * Set the cache value, with the given key (implementations will/may modify the key based on
     * hashing algorithms).  The value is set potentially in the background, using a thread from the
     * given executor.
     * @param keyString The key under which to cache the object
     * @param value The value to store in the cache
     * @param executorService The executor under which to execute the cache write operation
     * @return
     */
    public ListenableFuture<V> set(String keyString, V value, ListeningExecutorService executorService);


    /**
     * Set the cache value, with the given key (implementations will/may modify the key based on
     * hashing algorithms).  The value is set potentially in the background, using a thread from the
     * given executor.
     *
     * The value may or may not be written to the cache based on the outcome of the {@code canCacheValueEvaluator}
     *
     * @param keyString The key under which to cache the object
     * @param computation Supplier that generates the value that might be stored in the cache
     * @param timeToLive The amount of time that the value should be stored in the cache
     * @param canCacheValueEvaluator Predicate the says if the cache value generated by the supplier should be cached or not
     * @param executorService The executor under which to execute the cache write operation
     * @return
     */
    public ListenableFuture<V> set(String keyString, Supplier<V> computation,Duration timeToLive,
                                   Predicate<V> canCacheValueEvaluator,
                                   ListeningExecutorService executorService);

}
