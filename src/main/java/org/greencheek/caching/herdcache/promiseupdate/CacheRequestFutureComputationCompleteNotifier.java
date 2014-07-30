package org.greencheek.caching.herdcache.promiseupdate;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.SettableFuture;

/**
 * Created by dominictootell on 28/07/2014.
 */
public class CacheRequestFutureComputationCompleteNotifier<V> implements FutureCallback<V> {

    private final String cacheKey;
    private final SettableFuture<V> cacheRequestFuture;
    private final CacheValueComputationFailureHandler failureHandler;

    public CacheRequestFutureComputationCompleteNotifier(String key, SettableFuture<V> settableFuture,
                                                         CacheValueComputationFailureHandler failureHandler) {
        this.cacheKey = key;
        this.cacheRequestFuture = settableFuture;
        this.failureHandler = failureHandler;
    }

    @Override
    public void onSuccess(V result) {
        cacheRequestFuture.set(result);
    }

    @Override
    public void onFailure(Throwable t) {
        failureHandler.onFailure(cacheKey,t);
        cacheRequestFuture.setException(t);
    }
}
