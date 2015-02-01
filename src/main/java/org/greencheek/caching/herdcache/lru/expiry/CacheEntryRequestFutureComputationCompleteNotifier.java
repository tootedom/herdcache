package org.greencheek.caching.herdcache.lru.expiry;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.SettableFuture;

import java.util.function.Consumer;

/**
 * Created by dominictootell on 28/07/2014.
 */
public class CacheEntryRequestFutureComputationCompleteNotifier<V> implements FutureCallback<V> {

    private final String cacheKey;
    private final TimedEntry<V> entry;
    private final SettableFuture<V> cacheRequestFuture;
    private final CacheValueAndEntryComputationFailureHandler failureHandler;
    private final Consumer<V> canCacheResultEvaluator;

    public CacheEntryRequestFutureComputationCompleteNotifier(String key, TimedEntry<V> entry, SettableFuture<V> settableFuture,
                                                              CacheValueAndEntryComputationFailureHandler failureHandler,
                                                              Consumer<V> canCacheEvaluator) {
        this.cacheKey = key;
        this.entry = entry;
        this.cacheRequestFuture = settableFuture;
        this.failureHandler = failureHandler;
        this.canCacheResultEvaluator = canCacheEvaluator;
    }

    @Override
    public void onSuccess(V result) {
        cacheRequestFuture.set(result);
        canCacheResultEvaluator.accept(result);
    }

    @Override
    public void onFailure(Throwable t) {
        failureHandler.onFailure(cacheKey,entry,t);
        cacheRequestFuture.setException(t);
    }
}
