package org.greencheek.caching.herdcache;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.io.Serializable;
import java.time.Duration;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Extends {@link org.greencheek.caching.herdcache.CacheWithExpiry}
 * This extension allows the returning of a value that is retrieved from cache, that has been
 * deemed to not be a valid value (i.e. it is stale), whilst in the background the item is refreshed
 * from the supplied {@link java.util.function.Supplier} .
 */
public interface RevalidateInBackgroundCapableCache<V extends Serializable> extends CacheWithExpiry<V> {

    /**
     * obtain a value from the cache.  The cached value is only used if the @link #isCachedValueValid predict returns
     * return, or {@code #isCachedValueValid} is true.
     *
     * {@code #isCachedValueValid} Predicate evaluates the cached value, if it returns true, the cached value should be allowed,
     * otherwise the {@code #computation} Supplier is called to provide the value.
     *
     * The {@code #returnInvalidCachedItemWhileRevalidate} determines if the invalid cached object should be returned, whilst in
     * the background the @link #computation {@link java.util.function.Supplier} is executed to refresh the value
     *
     * The {@code #canCacheValueEvalutor} predicate is used to evaluate if the value returned by the
     * {@code #computation} Supplier should be cached or not.
     *
     * @param key The key to obtain/cache a value under
     * @param computation The function that would calculate the value to be cached
     * @param timeToLive How long the value should be cached for
     * @param executorService The executor service in which to run the futures.
     * @param canCacheValueEvalutor Should the value returned by the #computation Supplier be cached or not
     * @param isCachedValueValid Should the value returned by the cache be returned or not (and therefore the supplier called).
     * @param returnInvalidCachedItemWhileRevalidate
     * @return
     */
    public ListenableFuture<V> apply(String key,
                                     Supplier<V> computation,
                                     Duration timeToLive,
                                     ListeningExecutorService executorService,
                                     Predicate<V> canCacheValueEvalutor,Predicate<V> isCachedValueValid,
                                     boolean returnInvalidCachedItemWhileRevalidate);
}
