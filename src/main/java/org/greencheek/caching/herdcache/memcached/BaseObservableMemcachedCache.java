package org.greencheek.caching.herdcache.memcached;


import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import net.spy.memcached.ConnectionFactory;
import org.greencheek.caching.herdcache.*;
import org.greencheek.caching.herdcache.domain.CacheItem;
import org.greencheek.caching.herdcache.memcached.config.ElastiCacheCacheConfig;
import org.greencheek.caching.herdcache.memcached.config.MemcachedCacheConfig;
import org.greencheek.caching.herdcache.memcached.factory.*;
import org.greencheek.caching.herdcache.memcached.metrics.MetricRecorder;
import org.greencheek.caching.herdcache.memcached.operations.*;
import org.greencheek.caching.herdcache.memcached.spyconnectionfactory.SpyConnectionFactoryBuilder;
import org.greencheek.caching.herdcache.memcached.util.CacheMetricStrings;
import org.greencheek.caching.herdcache.util.CacheKeyCreatorFactory;
import org.greencheek.caching.herdcache.util.DurationToSeconds;
import org.greencheek.caching.herdcache.util.SubscriptionCompleter;
import org.greencheek.caching.herdcache.util.futures.GuavaSettableFuture;
import org.greencheek.caching.herdcache.util.futures.SettableFuture;
import org.greencheek.caching.herdcache.util.keycreators.CacheKeyCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Single;
import rx.SingleSubscriber;
import rx.functions.Actions;
import rx.schedulers.Schedulers;

import java.io.Serializable;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 *
 */
abstract class BaseObservableMemcachedCache<V extends Serializable> implements ObservableCache<V>
{


    public static ConnectionFactory createMemcachedConnectionFactory(MemcachedCacheConfig config) {
        return SpyConnectionFactoryBuilder.createConnectionFactory(
                config.getFailureMode(),
                config.getHashAlgorithm(), config.getSerializingTranscoder(),
                config.getProtocol(),config.getReadBufferSize(),config.getKeyHashType(),
                config.getLocatorFactory(), config.getKeyValidationType(), config.getListenerCallbackExecutor());
    }

    public static ReferencedClientFactory createReferenceClientFactory(ElastiCacheCacheConfig config) {
        return new SpyMemcachedReferencedClientFactory<>(createMemcachedConnectionFactory(config.getMemcachedCacheConfig()));
    }




    private final CacheWrite cacheWriter;
    private final CacheRead<V> cacheReader;
    private static final Logger logger  = LoggerFactory.getLogger(BaseObservableMemcachedCache.class);

    private final MemcachedCacheConfig config;
    private final MemcachedClientFactory clientFactory;
    private final ConcurrentMap<String,Single<CacheItem<V>>> store;



    private final long memcachedGetTimeoutInMillis;


    private final MetricRecorder metricRecorder;
    private final CacheKeyCreator cacheKeyCreator;
    private final long millisToWaitForDelete;
    private final boolean waitForMemcachedSet;

    public BaseObservableMemcachedCache(MemcachedCacheConfig config) {
        this(null,config);
    }

    public BaseObservableMemcachedCache(
            MemcachedClientFactory clientFactory,
            MemcachedCacheConfig config) {
        this.config = config;
        cacheKeyCreator = CacheKeyCreatorFactory.DEFAULT_INSTANCE.create(config);
        if(clientFactory == null) {
            this.clientFactory = buildClientFactory(config);
        } else {
            this.clientFactory = clientFactory;
        }

        int maxCapacity = config.getMaxCapacity();

        this.store = createInternalCache(config.isHerdProtectionEnabled(),maxCapacity,maxCapacity);

        memcachedGetTimeoutInMillis = config.getMemcachedGetTimeout().toMillis();

        metricRecorder = config.getMetricsRecorder();

        cacheReader = new BasicCacheRead<>();

        cacheWriter = new WaitForCacheWrite(metricRecorder,config.getSetWaitDuration().toMillis());

        millisToWaitForDelete = config.getWaitForRemove().toMillis();

        waitForMemcachedSet = config.isWaitForMemcachedSet();
    }

    public abstract MemcachedClientFactory buildClientFactory(Object cfg);

    private ConcurrentMap createInternalCache(boolean createCache,
                                            int initialCapacity,
                                            int maxCapacity) {
        if(createCache) {
            return new ConcurrentLinkedHashMap.Builder()
                    .initialCapacity(initialCapacity)
                    .maximumWeightedCapacity(maxCapacity)
                    .build();
        } else {
            return new NoOpConcurrentMap();
        }

    }


    public static final void logMemcachedWriteError(Throwable t) {
        if(t instanceof RejectedExecutionException) {
            logger.warn("Scheduler rejected execution.",t);
        }
        else {
            logger.warn("Unexpected Exception occurred during memcached write on Scheduler.",t);
        }
    }

    private void warnCacheDisabled() {
        logger.warn("Cache is disabled");
    }


    private String getHashedKey(String key) {
        return cacheKeyCreator.createKey(key);
    }

    /**
     * Used when the cache is disabled (all hosts are down for maintenance, etc).
     * The {@link java.util.function.Supplier} is submitted to the executor, and
     * a Future returned
     *
     * @param key The key for the item generated by the supplier
     * @param computation The supplier that generates the value
     * @return
     */
    private Single<CacheItem<V>> scheduleValueComputation(final String key,
                                                         final Supplier<V> computation) {

        Single<CacheItem<V>> single = Single.create(new Single.OnSubscribe<CacheItem<V>>() {
            @Override
            public void call(SingleSubscriber<? super CacheItem<V>> singleSubscriber) {
                try {
                    singleSubscriber.onSuccess(new CacheItem<V>(key, computation.get(),false));
                } catch (Throwable e) {
                    singleSubscriber.onError(e);
                }
            }
        }).toObservable().cacheWithInitialCapacity(1).toSingle();

        Cache.logCacheMiss(metricRecorder, key, CacheMetricStrings.CACHE_TYPE_CACHE_DISABLED);
        return single;
    }

    private V getFromDistributedCache(final ReferencedClient client,
                                                        final String keyString) {
            boolean ok;
            V result = null;
            try {
                result = cacheReader.getFromDistributedCache(client, keyString,
                                                             memcachedGetTimeoutInMillis,
                                                             CacheMetricStrings.CACHE_TYPE_DISTRIBUTED_CACHE,
                                                             metricRecorder);
                ok = true;
            } catch (Throwable throwable) {
                ok = false;
            }

            if(ok && result!=null) {
                Cache.logCacheHit(metricRecorder,keyString, CacheMetricStrings.CACHE_TYPE_ALL);
            } else {
                Cache.logCacheMiss(metricRecorder, keyString, CacheMetricStrings.CACHE_TYPE_ALL);
            }

            return result;

    }




    @Override
    public Single<CacheItem<V>> get(String key) {
        final String keyString = getHashedKey(key);
        final ReferencedClient client = clientFactory.getClient();
        if(!client.isAvailable()) {
            warnCacheDisabled();
            Cache.logCacheMiss(metricRecorder, keyString, CacheMetricStrings.CACHE_TYPE_CACHE_DISABLED);
            return Single.just(new CacheItem<V>(keyString,null,false));
        } else {
            Single<CacheItem<V>> future = store.get(keyString);
            if(future==null) {

                Cache.logCacheMiss(metricRecorder, keyString, CacheMetricStrings.CACHE_TYPE_VALUE_CALCULATION);

                Single<CacheItem<V>> item = Single.create(new Single.OnSubscribe<CacheItem<V>>() {
                    @Override
                    public void call(SingleSubscriber<? super CacheItem<V>> singleSubscriber) {
                        V result = getFromDistributedCache(client,keyString);
                        singleSubscriber.onSuccess(new CacheItem<V>(keyString,result,result != null));
                    }
                }).toObservable().cacheWithInitialCapacity(1).toSingle();

                return item;
            }
            else {
                Cache.logCacheHit(metricRecorder, keyString, CacheMetricStrings.CACHE_TYPE_VALUE_CALCULATION);
                return future;
            }
        }
    }

    private Single<CacheItem<V>> writeToCache(final ReferencedClient client,
                                   final CacheItem<V> item,
                                   final String key,
                                   final int entryTTLInSeconds,
                                   final Predicate<V> isSupplierValueCachable) {
        return Single.create( sub -> {
            boolean isNotNullResults = false;
            boolean isCacheable = false;
            Optional<V> valueToWriteToCache = item.getValue();
            if (valueToWriteToCache.isPresent()) {
                isNotNullResults = true;
                V value = valueToWriteToCache.get();
                if(isSupplierValueCachable.test(value)) {
                    isCacheable = true;
                    cacheWriter.writeToDistributedCache(client, key, value, entryTTLInSeconds);
                }
            }

            if(!isNotNullResults || !isCacheable) {
                logger.debug("Cache Value cannot be cached.  It has to be either not null:({}), or cachable as determine by predicate:({}). " +
                        "Therefore, not storing in memcached", isNotNullResults, isCacheable);
            }
            sub.onSuccess(item);
        });
    }

    @Override
    public Single<CacheItem<V>> apply(final String key, final Supplier<V> computation, final Duration timeToLive,
                                      final Predicate<V> isSupplierValueCachable,final Predicate<V> isCachedValueValid) {
        {

            final String keyString = getHashedKey(key);

            ReferencedClient client = clientFactory.getClient();
            if (!client.isAvailable()) {
                warnCacheDisabled();
                return scheduleValueComputation(keyString, computation);
            } else {

                Single<CacheItem<V>> item = Single.create(new Single.OnSubscribe<CacheItem<V>>() {
                    @Override
                    public void call(SingleSubscriber<? super CacheItem<V>> singleSubscriber) {
                        Cache.logCacheMiss(metricRecorder, keyString, CacheMetricStrings.CACHE_TYPE_VALUE_CALCULATION);
                        // check memcached.

                        V cachedObject = cacheReader.getFromDistributedCache(client,
                                keyString,
                                memcachedGetTimeoutInMillis,
                                CacheMetricStrings.CACHE_TYPE_DISTRIBUTED_CACHE,
                                metricRecorder);

                        boolean cachedObjectFoundInCache = cachedObject != null;
                        boolean validCachedObject = (cachedObjectFoundInCache && isCachedValueValid.test(cachedObject));

                        if (validCachedObject) {
                            Cache.logCacheHit(metricRecorder, keyString, CacheMetricStrings.CACHE_TYPE_ALL);

                            SubscriptionCompleter.completeWithValue(singleSubscriber, keyString, cachedObject, store, true,
                                    config.isRemoveFutureFromInternalCacheBeforeSettingValue());

                        } else {
                            // write with normal semantics
                            logger.debug("set requested for {}", keyString);
                            Cache.logCacheMiss(metricRecorder, keyString, CacheMetricStrings.CACHE_TYPE_ALL);
                            SupplierStatus<V> value = callSupplier(computation);

                            notifySubscriberAndWriteToCache(client,keyString,value,singleSubscriber,isSupplierValueCachable,timeToLive);
                        }
                    }
                }).toObservable().cacheWithInitialCapacity(1).toSingle();

                // create and store a new future for the to be generated value
                // first checking against local a cache to see if the computation is already
                // occurring
                Single<CacheItem<V>> existingFuture = store.putIfAbsent(keyString, item);
                if (existingFuture == null) {
                    Cache.logCacheMiss(metricRecorder, keyString, CacheMetricStrings.CACHE_TYPE_VALUE_CALCULATION);
                    return item;
                } else {
                    Cache.logCacheHit(metricRecorder, keyString, CacheMetricStrings.CACHE_TYPE_VALUE_CALCULATION);
                    return existingFuture;
                }
            }
        }
    }

    private void notifySubscriberAndWriteToCache(ReferencedClient client,
                                                 String keyString,
                                                 SupplierStatus<V> value,
                                                 SingleSubscriber<? super CacheItem<V>> singleSubscriber,
                                                 Predicate<V> canCacheValueEvalutor,
                                                 Duration timeToLive)
    {
        if(value.isError()) {
            notifySubscriberOnError(keyString,value.getThrowable(),singleSubscriber,store);
        } else {
            // Set the item in the cache
            V results = value.getValue();
            CacheItem<V> cacheItem = new CacheItem<>(keyString,results,false);

            if(waitForMemcachedSet) {
                    writeToCache(client, cacheItem, keyString,
                            DurationToSeconds.getSeconds(timeToLive),
                            canCacheValueEvalutor)
                            .subscribeOn(Schedulers.immediate())
                            .subscribe(Actions.empty(),BaseObservableMemcachedCache::logMemcachedWriteError);

                notifySubscriberOnSuccess(keyString,cacheItem,singleSubscriber,store);
            } else {
                notifySubscriberOnSuccess(keyString,cacheItem,singleSubscriber,store);

                writeToCache(client, cacheItem, keyString,
                        DurationToSeconds.getSeconds(timeToLive),
                        canCacheValueEvalutor)
                        .subscribeOn(config.getWaitForMemcachedSetRxScheduler())
                        .subscribe(Actions.empty(),BaseObservableMemcachedCache::logMemcachedWriteError);

            }
        }
    }

    @Override
    public Single<CacheItem<V>> set(String key, Supplier<V> computation,Duration timeToLive,
                                   Predicate<V> canCacheValueEvalutor) {
        final String keyString = getHashedKey(key);


        ReferencedClient client = clientFactory.getClient();
        if(!client.isAvailable()) {
            warnCacheDisabled();
            return scheduleValueComputation(keyString,computation);
        }
        else {
            Single<CacheItem<V>> item = Single.create(new Single.OnSubscribe<CacheItem<V>>() {
                @Override
                public void call(SingleSubscriber<? super CacheItem<V>> singleSubscriber) {

                        // write with normal semantics
                    final SettableFuture<V> promise = new GuavaSettableFuture<>();
                    logger.debug("set requested for {}", keyString);
                    Cache.logCacheMiss(metricRecorder, keyString, CacheMetricStrings.CACHE_TYPE_ALL);

                    SupplierStatus<V> value = callSupplier(computation);

                    notifySubscriberAndWriteToCache(client,keyString,value,singleSubscriber,canCacheValueEvalutor,timeToLive);
                }

            }).toObservable().cacheWithInitialCapacity(1).toSingle();

           return item;
        }

    }


    private void setCacheWriteMetrics(String timerMetricName, long duration, String counterMetricName) {
        metricRecorder.setDuration(CacheMetricStrings.CACHE_TYPE_VALUE_CALCULATION_ALL_TIMER,duration);
        metricRecorder.setDuration(timerMetricName,duration);
        metricRecorder.incrementCounter(counterMetricName);
    }

    /**
     * write to memcached when the future completes, the generated value,
     * against the given key, with the specified expiry
     * @param computation  The future that will generate the value
     * @return SupplierStatus This will contain the throwable or the suppliers computed value.
     */
    private SupplierStatus<V> callSupplier(final Supplier<V> computation)
    {
        final long startNanos =  System.nanoTime();
        V results = null;
        boolean ok = false;
        long time;
        Throwable error = null;
        try {
            results = computation.get();
            ok = true;
        } catch(Throwable err) {
            ok = false;
            error = err;
        } finally {
            time = System.nanoTime()-startNanos;
        }

        if (ok) {
            setCacheWriteMetrics(CacheMetricStrings.CACHE_TYPE_VALUE_CALCULATION_SUCCESS_TIMER,
                    time,
                    CacheMetricStrings.CACHE_TYPE_VALUE_CALCULATION_SUCCESS_COUNTER);
            return new SupplierStatus<V>(results);
        } else {
            setCacheWriteMetrics(CacheMetricStrings.CACHE_TYPE_VALUE_CALCULATION_FAILURE_TIMER,
                    time,
                    CacheMetricStrings.CACHE_TYPE_VALUE_CALCULATION_FAILURE_COUNTER);
            return new SupplierStatus<V>(error);
        }
    }

    private void notifySubscriberOnSuccess(final String key, final CacheItem<V> value,final SingleSubscriber<? super CacheItem<V>> subscriber,
                                           final ConcurrentMap<String, Single<CacheItem<V>>> cachedFutureStore) {
        SubscriptionCompleter.completeWithValue(subscriber, key, value, cachedFutureStore, config.isRemoveFutureFromInternalCacheBeforeSettingValue());
    }

    private void notifySubscriberOnError(final String key, final Throwable error,final SingleSubscriber<? super CacheItem<V>> subscriber,
                                           final ConcurrentMap<String, Single<CacheItem<V>>> cachedFutureStore) {
        SubscriptionCompleter.completeWithException(subscriber, key, error, cachedFutureStore,
                                                    config.isRemoveFutureFromInternalCacheBeforeSettingValue());

    }

    @Override
    public void shutdown() {
        clearInternalCaches();
        clientFactory.shutdown();
    }


    private void clearInternalCaches() {
        store.clear();
    }


    private static Boolean waitForDelete(Future<Boolean> future,long millisToWait,
                                      String key,String cacheBeingCleared) {
        Boolean removed = Boolean.FALSE;
        try {
            if (millisToWait > 0) {
                removed =  future.get(millisToWait, TimeUnit.MICROSECONDS);
            } else {
                removed = future.get();
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted whilst waiting for {} clear({}) to occur",cacheBeingCleared, key, e);
        } catch (ExecutionException e) {
            logger.warn("Exception whilst waiting for {} clear({}) to occur",cacheBeingCleared, key, e);
        } catch (TimeoutException e) {
            logger.warn("Timeout whilst waiting for {} clear({}) to occur",cacheBeingCleared, key, e);
        }
        return removed;
    }
    /**
     * removes/deletes a given key from memcached.  Waiting for the remove if
     * config.getWaitForRemove is greater than zero
     * @param key
     */
    @Override
    public Single<Boolean> clear(String key) {
        final ReferencedClient client = clientFactory.getClient();
        if (client.isAvailable()) {
            final String keyString = getHashedKey(key);

            Single<Boolean> single = Single.create(new Single.OnSubscribe<Boolean>() {
                @Override
                public void call(SingleSubscriber<? super Boolean> singleSubscriber) {
                    Future<Boolean> future = client.delete(keyString);
                    singleSubscriber.onSuccess(waitForDelete(future,millisToWaitForDelete,keyString,"cache"));
                }
            }).toObservable().cacheWithInitialCapacity(1).toSingle();

            return single;
        }
        else {
            return Single.create(sub-> sub.onSuccess(Boolean.FALSE));
        }
    }
}
