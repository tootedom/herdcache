package org.greencheek.caching.herdcache;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.greencheek.caching.herdcache.domain.CacheItem;
import rx.Scheduler;
import rx.Single;

import java.io.Serializable;
import java.time.Duration;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.Optional;

/**
 *
 */
public interface ObservableCache<V extends Serializable>  {
    static final Predicate CAN_ALWAYS_CACHE_VALUE = (X) -> true;
    static final Predicate CACHED_VALUE_IS_ALWAYS_VALID = (X) -> true;
    static final Duration NO_TTL = Duration.ZERO;


    /**
     * Reads from the cache only.
     * @param key The key under which the item might be stored in the cache.
     * @return {@link java.util.Optional#EMPTY} if no such item, or what was stored in the cache.
     */
    public Single<CacheItem<V>> get(String key);


    default public Single<CacheItem<V>> apply(String key, Supplier<V> computation, Duration timeToLive) {
        return apply(key,computation,timeToLive,CAN_ALWAYS_CACHE_VALUE,CACHED_VALUE_IS_ALWAYS_VALID);
    }

    /**
     * @param key The key to obtain/cache a value under
     * @param computation The function that would calculate the value to be cached
     * @param timeToLive How long the value should be cached for
     * @param isSupplierValueCachable Should the value returned by the #computation Supplier be cached or not
     * @return
     */
    default public Single<CacheItem<V>> apply(String key, Supplier<V> computation, Duration timeToLive,
                                     Predicate<V> isSupplierValueCachable) {
        return apply(key,computation,timeToLive,isSupplierValueCachable,CACHED_VALUE_IS_ALWAYS_VALID);
    }

    /**
     * obtain a value from the cache.  The cached value is only used if the @link #isCachedValueValid predict returns
     * return.  The Predicate evaluates the cached value, if it returns true, the cached value should be allowed,
     * otherwise the @link #computation Supplier is called to provide the value.  The @link #canCacheValueEvalutor
     * predicate is used to evaluate if the value returned by the @link #computation Supplier should be cached or not.
     * The item is stored in the cache with a TTL (time to live) that is determined by the implementation.  The default
     * is for an infinite TTL (ZERO seconds)
     *
     * @param key The key to obtain/cache a value under
     * @param computation The function that would calculate the value to be cached
     * @param isSupplierValueCachable Should the value returned by the #computation Supplier be cached or not
     * @param isCachedValueValid Should the value returned by the cache be returned or not (and therefore the supplier called).
     * @return
     */
    default Single<CacheItem<V>> apply(String key, Supplier<V> computation,
                                      Predicate<V> isSupplierValueCachable,
                                      Predicate<V> isCachedValueValid) {
        return apply(key,computation,NO_TTL,isSupplierValueCachable,isCachedValueValid);
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
     * @param isSupplierValueCachable Should the value returned by the #computation Supplier be cached or not
     * @param isCachedValueValid Should the value returned by the cache be returned or not (and therefore the supplier called).
     * @return
     */
    public Single<CacheItem<V>> apply(String key, Supplier<V> computation, Duration timeToLive,
                                     Predicate<V> isSupplierValueCachable,
                                     Predicate<V> isCachedValueValid);



    /**
     * Set the cache value under that given key with the specified value
     * @param keyString The key under which to cache the object
     * @param value Supplier that generates the value that might be stored in the cache
     * @param timeToLive How long the value should be cached for
     * @return
     */
    default public Single<CacheItem<V>> set(String keyString, Supplier<V> value, Duration timeToLive) {
        return set(keyString,value,timeToLive,IsSupplierValueCachable.GENERATED_VALUE_IS_ALWAYS_CACHABLE);
    }




    /**
     * Set the cache value, with the given key (implementations will/may modify the key based on
     * hashing algorithms).  The value is set potentially in the background, using a thread from the
     * given executor.
     * @param keyString The key under which to cache the object
     * @param value The value to store in the cache
     * @return
     */
    default public Single<CacheItem<V>> set(String keyString, V value, Duration timeToLive) {
        return set(keyString,() -> value ,timeToLive);
    }



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
     * @return
     */
    public Single<CacheItem<V>> set(String keyString, Supplier<V> computation, Duration timeToLive,
                                    Predicate<V> canCacheValueEvaluator);


    /**
     * Removes an item from the cache
     * @param key The item to remove from the cache
     */
    public Single<Boolean> clear(String key);

    /**
     * shutdown the cache
     */
    public void shutdown();

}