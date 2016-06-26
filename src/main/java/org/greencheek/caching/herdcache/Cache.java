package org.greencheek.caching.herdcache;


import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.greencheek.caching.herdcache.await.AwaitOnFuture;
import org.greencheek.caching.herdcache.memcached.metrics.MetricRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 *
 */
public interface Cache<V> extends AwaitOnFuture<V> {
    static final Predicate CAN_ALWAYS_CACHE_VALUE = (X) -> true;
    static final Predicate CACHED_VALUE_IS_ALWAYS_VALID = (X) -> true;
    static final Duration NO_TTL = Duration.ZERO;

    static final Logger cacheHitMissLogger   = LoggerFactory.getLogger("MemcachedCacheHitsLogger");

    public static void logCacheHit(MetricRecorder metricRecorder,String key, String cacheType) {
        metricRecorder.cacheHit(cacheType);
        cacheHitMissLogger.debug("{ \"cachehit\" : \"{}\", \"cachetype\" : \"{}\"}",key,cacheType);
    }

    public static void logCacheMiss(MetricRecorder metricRecorder,String key, String cacheType) {
        metricRecorder.cacheMiss(cacheType);
        cacheHitMissLogger.debug("{ \"cachemiss\" : \"{}\", \"cachetype\" : \"{}\"}",key,cacheType);
    }


    default public ListenableFuture<V> apply(String key, Supplier<V> computation) {
        return apply(key, computation, MoreExecutors.newDirectExecutorService());
    }

    default public ListenableFuture<V> apply(String key, Supplier<V> computation, ListeningExecutorService executorService) {
        return apply(key, computation, executorService,CAN_ALWAYS_CACHE_VALUE);
    }

    default public ListenableFuture<V> get(String key) {
        return get(key, MoreExecutors.newDirectExecutorService());
    }


    default public ListenableFuture<V> apply(String key, Supplier<V> computation, ListeningExecutorService executorService,
                                     Predicate<V> canCacheValueEvalutor) {
        return apply(key,computation,executorService,canCacheValueEvalutor,CACHED_VALUE_IS_ALWAYS_VALID);
    }

    public ListenableFuture<V> apply(String key, Supplier<V> computation, ListeningExecutorService executorService,
                                     Predicate<V> canCacheValueEvalutor,Predicate<V> isCachedValueUsable);


    public ListenableFuture<V> get(String key,ListeningExecutorService executorService);


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
     * Set the cache value under that given key with the specified value
     * @param keyString The key under which to cache the object
     * @param value Supplier that generates the value that might be stored in the cache
     * @return
     */
    default public ListenableFuture<V> set(String keyString, Supplier<V> value) {
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
    default public ListenableFuture<V> set(String keyString, V value, ListeningExecutorService executorService) {
        return set(keyString,() -> value,executorService);
    }


    /**
     * Set the cache value, with the given key (implementations will/may modify the key based on
     * hashing algorithms).  The value is set potentially in the background, using a thread from the
     * given executor.
     * @param keyString The key under which to cache the object
     * @param value Supplier that generates the value that might be stored in the cache
     * @param executorService The executor under which to execute the cache write operation
     * @return
     */
    default public ListenableFuture<V> set(String keyString, Supplier<V> value, ListeningExecutorService executorService) {
        return set(keyString,value,IsSupplierValueCachable.GENERATED_VALUE_IS_ALWAYS_CACHABLE,executorService);
    }


    /**
     * Set the cache value, with the given key (implementations will/may modify the key based on
     * hashing algorithms).  The value is set potentially in the background, using a thread from the
     * given executor.
     * @param keyString The key under which to cache the object
     * @param value Supplier that generates the value that might be stored in the cache
     * @param executorService The executor under which to execute the cache write operation
     * @return
     */
    public ListenableFuture<V> set(String keyString, Supplier<V> value, Predicate<V> canCacheValueEvalutor, ListeningExecutorService executorService);


}
