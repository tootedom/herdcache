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
}
