package org.greencheek.caching.herdcache.lru;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.SettableFuture;

import java.util.function.Consumer;

/**
 * Created by dominictootell on 28/07/2014.
 */
public class CacheRequestFutureComputationCompleteNotifier<V> implements FutureCallback<V> {

    private final String cacheKey;
    private final SettableFuture<V> cacheRequestFuture;
    private final CacheValueComputationFailureHandler failureHandler;
    private final Consumer<V> cacheResultConsumer;


    public CacheRequestFutureComputationCompleteNotifier(String key, SettableFuture<V> settableFuture,
                                                         CacheValueComputationFailureHandler failureHandler,
                                                         Consumer<V> cacheResultConsumer) {
        this.cacheKey = key;
        this.cacheRequestFuture = settableFuture;
        this.failureHandler = failureHandler;
        this.cacheResultConsumer = cacheResultConsumer;
    }

    @Override
    public void onSuccess(V result) {
        cacheRequestFuture.set(result);
        cacheResultConsumer.accept(result);

    }

    @Override
    public void onFailure(Throwable t) {
        failureHandler.onFailure(cacheKey,t);
        cacheRequestFuture.setException(t);
    }
}
