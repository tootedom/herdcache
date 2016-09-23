package org.greencheek.caching.herdcache.util;

import org.greencheek.caching.herdcache.domain.CacheItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Single;
import rx.SingleSubscriber;

import java.util.concurrent.ConcurrentMap;

/**
 * Created by dominictootell on 22/09/2016.
 */
public class SubscriptionCompleter {
    private static Logger LOGGER = LoggerFactory.getLogger(SubscriptionCompleter.class);

    public static <V> CacheItem<V> completeWithValue(SingleSubscriber<? super CacheItem<V>>  promise, String keyString, V cachedObject,
                                             ConcurrentMap<String, Single<CacheItem<V>>> internalCache,boolean fromCache,
                                             boolean removeFromInternalCacheBeforeCompletion) {
        CacheItem<V> value = new CacheItem<V>(keyString,cachedObject,fromCache);
        try {
            if (removeFromInternalCacheBeforeCompletion) {
                internalCache.remove(keyString);
                promise.onSuccess(value);
            } else {
                promise.onSuccess(value);
                internalCache.remove(keyString);
            }
        } catch (Throwable e) {
            LOGGER.error("Unable to call subscriber for key:({}) and value:({})",keyString,cachedObject,e);
        }
        return value;
    }

    public static <V> CacheItem<V> completeWithValue(SingleSubscriber<? super CacheItem<V>>  promise, String keyString, CacheItem<V> value,
                                                     ConcurrentMap<String, Single<CacheItem<V>>> internalCache,
                                                     boolean removeFromInternalCacheBeforeCompletion) {
        try {
            if (removeFromInternalCacheBeforeCompletion) {
                internalCache.remove(keyString);
                promise.onSuccess(value);
            } else {
                promise.onSuccess(value);
                internalCache.remove(keyString);
            }
        } catch (Throwable e) {
            LOGGER.error("Unable to call subscriber for key:({}) and value:({})",keyString,value,e);
        }
        return value;
    }

    public static <V> CacheItem<V> completeWithException(SingleSubscriber<? super CacheItem<V>> promise, String keyString, Throwable exception,
                                                 ConcurrentMap<String, Single<CacheItem<V>>> internalCache,
                                                 boolean removeFromInternalCacheBeforeCompletion) {
        try {
            if (removeFromInternalCacheBeforeCompletion) {
                internalCache.remove(keyString);
                promise.onError(exception);
            } else {
                promise.onError(exception);
                internalCache.remove(keyString);
            }
        } catch (Throwable e) {
            LOGGER.error("Unable to call subscriber with exception for key:({}) and value:({})",keyString,exception.getMessage(),e);
        }

        return new CacheItem<V>(keyString,null,false);
    }
}
