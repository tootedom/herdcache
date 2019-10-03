package org.greencheek.caching.herdcache.memcached;

//import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.*;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import net.spy.memcached.ConnectionFactory;
import org.greencheek.caching.herdcache.*;
import org.greencheek.caching.herdcache.callables.GetFromDistributedCache;
import org.greencheek.caching.herdcache.exceptions.UnableToScheduleCacheGetExecutionException;
import org.greencheek.caching.herdcache.exceptions.UnableToSubmitSupplierForExecutionException;
import org.greencheek.caching.herdcache.lru.CacheRequestFutureComputationCompleteNotifier;
import org.greencheek.caching.herdcache.lru.CacheValueComputationFailureHandler;
import org.greencheek.caching.herdcache.memcached.config.ElastiCacheCacheConfig;
import org.greencheek.caching.herdcache.memcached.config.MemcachedCacheConfig;
import org.greencheek.caching.herdcache.memcached.factory.*;
import org.greencheek.caching.herdcache.memcached.metrics.MetricRecorder;
import org.greencheek.caching.herdcache.memcached.operations.*;
import org.greencheek.caching.herdcache.memcached.spy.extensions.connection.NoValidationConnectionFactory;
import org.greencheek.caching.herdcache.memcached.spyconnectionfactory.SpyConnectionFactoryBuilder;
import org.greencheek.caching.herdcache.memcached.util.CacheMetricStrings;
import org.greencheek.caching.herdcache.util.CacheKeyCreatorFactory;
import org.greencheek.caching.herdcache.util.DurationToSeconds;
import org.greencheek.caching.herdcache.util.StaleCacheKeyCreator;
import org.greencheek.caching.herdcache.util.futures.FutureCompleter;
import org.greencheek.caching.herdcache.util.futures.SettableFuture;
import org.greencheek.caching.herdcache.util.futures.DoNothingSettableFuture;
import org.greencheek.caching.herdcache.util.futures.GuavaSettableFuture;
import org.greencheek.caching.herdcache.util.keycreators.CacheKeyCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 *
 */
 abstract class BaseMemcachedCache<V extends Serializable> implements RequiresShutdown,ClearableCache,
        SerializableOnlyCacheWithExpiry<V>, RevalidateInBackgroundCapableCache<V>
{


    public static NoValidationConnectionFactory createMemcachedConnectionFactory(MemcachedCacheConfig config) {
        return SpyConnectionFactoryBuilder.createConnectionFactory(
                config.getFailureMode(),
                config.getHashAlgorithm(), config.getSerializingTranscoder(),
                config.getProtocol(),config.getReadBufferSize(),config.getKeyHashType(),
                config.getLocatorFactory(),config.getKeyValidationType(), config.getListenerCallbackExecutor());
    }

    public static ReferencedClientFactory createReferenceClientFactory(ElastiCacheCacheConfig config) {
        switch(config.getClientType()) {
            case SPY:
                return new SpyMemcachedReferencedClientFactory<>(createMemcachedConnectionFactory(config.getMemcachedCacheConfig()));
            case FOLSOM:
                return new FolsomReferencedClientFactory<>(config);
            default:
                return new SpyMemcachedReferencedClientFactory<>(createMemcachedConnectionFactory(config.getMemcachedCacheConfig()));
        }
    }



    private final CacheWrite cacheWriter;
    private final CacheWrite staleCacheWriter;
    private final CacheRead<V> cacheReader;
    private static final Logger logger  = LoggerFactory.getLogger(BaseMemcachedCache.class);

    private final SettableFuture<V> DUMMY_FUTURE_NOT_TO_RETURN = new DoNothingSettableFuture<>();

    private final ConcurrentMap<String,ListenableFuture<V>> DO_NOTHING_MAP = new NoOpConcurrentMap<>();

    private final MemcachedCacheConfig config;
    private final MemcachedClientFactory clientFactory;
    private final ConcurrentMap<String,ListenableFuture<V>> store;
    private final int staleMaxCapacityValue;
    private final Duration staleCacheAdditionalTimeToLiveValue;
    private final ConcurrentMap<String,ListenableFuture<V>> staleStore;
    private final ConcurrentMap<String,ListenableFuture<V>> backgroundRevalidationStore;



    private final long memcachedGetTimeoutInMillis;
    private final long staleCacheMemachedGetTimeoutInMillis;

    private final CacheValueComputationFailureHandler failureHandler;

    private final MetricRecorder metricRecorder;
    private final CacheKeyCreator cacheKeyCreator;


    public BaseMemcachedCache(MemcachedCacheConfig config) {
        this(null,config);
    }

    public BaseMemcachedCache(
            MemcachedClientFactory clientFactory,
            MemcachedCacheConfig config) {
        this.config = config;
        if(clientFactory == null) {
            this.clientFactory = buildClientFactory(config);
        } else {
            this.clientFactory = clientFactory;
        }


        cacheKeyCreator = CacheKeyCreatorFactory.DEFAULT_INSTANCE.create(config);

        int maxCapacity = config.getMaxCapacity();

        this.store = createInternalCache(config.isHerdProtectionEnabled(),maxCapacity,maxCapacity);

        this.backgroundRevalidationStore = createInternalCache(true,maxCapacity,maxCapacity);

        int staleCapacity = config.getStaleMaxCapacity();
        if(staleCapacity<=0) {
            staleMaxCapacityValue = maxCapacity;
        } else {
            staleMaxCapacityValue = staleCapacity;
        }

        Duration staleDuration = config.getStaleCacheAdditionalTimeToLive();
        if(staleDuration.compareTo(Duration.ZERO)<=0) {
            staleCacheAdditionalTimeToLiveValue = config.getTimeToLive();
        } else {
            staleCacheAdditionalTimeToLiveValue = staleDuration;
        }

        staleStore = config.isUseStaleCache() ? createInternalCache(config.isUseStaleCache(),staleMaxCapacityValue,staleMaxCapacityValue) : null;


        memcachedGetTimeoutInMillis = config.getMemcachedGetTimeout().toMillis();

        if(config.getStaleCacheMemachedGetTimeout().compareTo(Duration.ZERO) <=0) {
            staleCacheMemachedGetTimeoutInMillis = memcachedGetTimeoutInMillis;
        } else {
            staleCacheMemachedGetTimeoutInMillis = config.getStaleCacheMemachedGetTimeout().toMillis();
        }

        failureHandler = (String key, Throwable t) -> { store.remove(key); };

        metricRecorder = config.getMetricsRecorder();

        cacheReader = new BasicCacheRead<>();

        cacheWriter = config.isWaitForMemcachedSet() ?
                new WaitForCacheWrite(metricRecorder,config.getSetWaitDuration().toMillis()) :
                new NoWaitForCacheWrite(metricRecorder);

        staleCacheWriter = new NoWaitForCacheWrite(metricRecorder);
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
//                (ConcurrentMap)Caffeine.newBuilder()
//                .maximumSize(maxCapacity)
//                .initialCapacity(maxCapacity)
//                .build()
//                .asMap();
        } else {
            return new NoOpConcurrentMap();
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
     * @param executorService The executor service that runs the supplier and produces a value.
     * @return
     */
    private ListenableFuture<V> scheduleValueComputation(final String key,
                                                         final Supplier<V> computation,
                                                         final ListeningExecutorService executorService) {
        com.google.common.util.concurrent.SettableFuture<V> toBeComputedFuture =  com.google.common.util.concurrent.SettableFuture.create();
        ListenableFuture<V> previousFuture = store.putIfAbsent(key, toBeComputedFuture);
        if(previousFuture==null) {
            Cache.logCacheMiss(metricRecorder, key, CacheMetricStrings.CACHE_TYPE_CACHE_DISABLED);
            try {
                executorService.submit(
                        () -> {
                            CacheRequestFutureComputationCompleteNotifier notifier = new CacheRequestFutureComputationCompleteNotifier<V>(key,
                                    toBeComputedFuture, failureHandler,
                                    (V result) -> store.remove(key));

                            V results = null;
                            try {
                                results = computation.get();
                                notifier.onSuccess(results);
                            } catch(Throwable e) {
                                notifier.onFailure(e);
                            }
                            return results;
                        });
            } catch(Throwable failedToSubmit) {
                metricRecorder.incrementCounter(CacheMetricStrings.CACHE_TYPE_CACHE_DISABLED_REJECTION);
                logger.warn("Unable able to submit computation (Supplier) to executor in order to obtain the value for key {}", key, failedToSubmit);
                toBeComputedFuture.setException(failedToSubmit);
            }


            return toBeComputedFuture;
        } else {
            Cache.logCacheHit(metricRecorder, key, CacheMetricStrings.CACHE_TYPE_VALUE_CALCULATION);
            return previousFuture;
        }
    }

    private ListenableFuture<V> getFromDistributedCache(final ReferencedClient client,
                                                        final String key,
                                                        final ListeningExecutorService ec) {
        try {
            return ec.submit(
                            new GetFromDistributedCache<V>(key,
                                    metricRecorder,
                                    memcachedGetTimeoutInMillis,
                                    client,
                                    CacheMetricStrings.CACHE_TYPE_DISTRIBUTED_CACHE,
                                    cacheReader)
                    );
        } catch(Throwable failedToSubmit) {
            final SettableFuture<V> promise = new GuavaSettableFuture<>();
            metricRecorder.incrementCounter(CacheMetricStrings.CACHE_TYPE_DISTRIBUTED_CACHE_REJECTION);
            String message = "Unable able to submit computation (Supplier) to executor in order to obtain the value for key: " + key;
            logger.warn(message, failedToSubmit);
            promise.setException(new UnableToScheduleCacheGetExecutionException(message,failedToSubmit));
            return promise;
        }
    }

    @Override
    public ListenableFuture<V> get(String key) {
        return get(key, MoreExecutors.newDirectExecutorService());
    }


    @Override
    public ListenableFuture<V> get(String key, ListeningExecutorService executorService) {
        final String keyString = getHashedKey(key);
        ReferencedClient client = clientFactory.getClient();
        if(!client.isAvailable()) {
            warnCacheDisabled();
            ListenableFuture<V> previousFuture = store.get(keyString);
            if(previousFuture==null) {
                Cache.logCacheMiss(metricRecorder, keyString, CacheMetricStrings.CACHE_TYPE_CACHE_DISABLED);
                return Futures.immediateCheckedFuture(null);
            } else {
                Cache.logCacheHit(metricRecorder, keyString, CacheMetricStrings.CACHE_TYPE_VALUE_CALCULATION);
                return previousFuture;
            }
        } else {
            ListenableFuture<V> future = store.get(keyString);
            if(future==null) {
                Cache.logCacheMiss(metricRecorder, keyString, CacheMetricStrings.CACHE_TYPE_VALUE_CALCULATION);
                ListenableFuture<V> futureForCacheLookup = getFromDistributedCache(client,keyString,executorService);
                return futureForCacheLookup;
            }
            else {
                Cache.logCacheHit(metricRecorder, keyString, CacheMetricStrings.CACHE_TYPE_VALUE_CALCULATION);
                if(config.isUseStaleCache()) {
                    return getFutureForStaleDistributedCacheLookup(client,
                            StaleCacheKeyCreator.createKey(config, keyString), future);
                } else {
                    return future;
                }
            }
        }
    }

    @Override
    public ListenableFuture<V> apply(String key, Supplier<V> computation, ListeningExecutorService executorService) {
        return apply(key,computation,config.getTimeToLive(),executorService);
    }

    @Override
    public ListenableFuture<V> apply(String key, Supplier<V> computation, ListeningExecutorService executorService,
                                     Predicate<V> canCacheValueEvalutor) {
        return apply(key,computation,config.getTimeToLive(),executorService,canCacheValueEvalutor);
    }

    @Override
    public ListenableFuture<V> apply(String key, Supplier<V> computation, ListeningExecutorService executorService,
                                     Predicate<V> canCacheValueEvalutor,Predicate<V> isCachedValueValid) {
        return apply(key,computation,config.getTimeToLive(),executorService,canCacheValueEvalutor,isCachedValueValid);
    }


    @Override
    public ListenableFuture<V> apply(String key,
                                     Supplier<V> computation,
                                     Duration timeToLive,
                                     ListeningExecutorService executorService) {
          return apply(key,computation,timeToLive,executorService,CAN_ALWAYS_CACHE_VALUE);
    }

    @Override
    public ListenableFuture<V> apply(String key,
                                     Supplier<V> computation,
                                     Duration timeToLive,
                                     ListeningExecutorService executorService,
                                     Predicate<V> canCacheValueEvalutor) {
        return apply(key,computation,timeToLive,executorService,canCacheValueEvalutor,CACHED_VALUE_IS_ALWAYS_VALID);
    }


    @Override
    public ListenableFuture<V> apply(String key,
                                     Supplier<V> computation,
                                     Duration timeToLive,
                                     ListeningExecutorService executorService,
                                     IsSupplierValueCachable<V> canCacheValueEvalutor,IsCachedValueUsable<V> isCachedValueValid) {
        return apply(key,computation,timeToLive,executorService,(Predicate<V>)canCacheValueEvalutor,(Predicate<V>)isCachedValueValid);
    }

    @Override
    public ListenableFuture<V> apply(String key,
                                     Supplier<V> computation,
                                     Duration timeToLive,
                                     ListeningExecutorService executorService,
                                     Predicate<V> canCacheValueEvalutor,Predicate<V> isCachedValueValid) {
        return this.apply(key, computation, timeToLive, executorService, canCacheValueEvalutor, isCachedValueValid, false);
    }


    @Override
    public ListenableFuture<V> apply(String key,
                                     Supplier<V> computation,
                                     ListeningExecutorService executorService,
                                     Predicate<V> canCacheValueEvalutor,Predicate<V> isCachedValueValid,
                                     boolean returnInvalidCachedItemWhileRevalidate) {
        return apply(key,computation,config.getTimeToLive(),executorService,canCacheValueEvalutor,isCachedValueValid,returnInvalidCachedItemWhileRevalidate);
    }

    @Override
    public ListenableFuture<V> apply(String key,
                                     Supplier<V> computation,
                                     Duration timeToLive,
                                     ListeningExecutorService executorService,
                                     Predicate<V> canCacheValueEvalutor,Predicate<V> isCachedValueValid,
                                     boolean returnInvalidCachedItemWhileRevalidate)
    {

        String keyString = getHashedKey(key);

        ReferencedClient client = clientFactory.getClient();
        if(!client.isAvailable()) {
            warnCacheDisabled();
            return scheduleValueComputation(keyString,computation,executorService);
        }
        else {
            final SettableFuture<V> promise = new GuavaSettableFuture<>();
            // create and store a new future for the to be generated value
            // first checking against local a cache to see if the computation is already
            // occurring
            ListenableFuture<V> existingFuture  = store.putIfAbsent(keyString, promise);
            if(existingFuture==null) {
                Cache.logCacheMiss(metricRecorder, keyString, CacheMetricStrings.CACHE_TYPE_VALUE_CALCULATION);
                // check memcached.

                V cachedObject = cacheReader.getFromDistributedCache(client,
                        keyString,
                        memcachedGetTimeoutInMillis,
                        CacheMetricStrings.CACHE_TYPE_DISTRIBUTED_CACHE,
                        metricRecorder);

                boolean cachedObjectFoundInCache = cachedObject!=null;
                boolean validCachedObject = (cachedObjectFoundInCache && isCachedValueValid.test(cachedObject));
                boolean doRevalidationInBackground = returnInvalidCachedItemWhileRevalidate && cachedObjectFoundInCache && !validCachedObject;

                if(validCachedObject || doRevalidationInBackground) {
                    Cache.logCacheHit(metricRecorder, keyString, CacheMetricStrings.CACHE_TYPE_ALL);

                    FutureCompleter.completeWithValue(promise, keyString, cachedObject, store,
                            config.isRemoveFutureFromInternalCacheBeforeSettingValue());

                    if(doRevalidationInBackground) {
                        // return the future, but schedule update in background
                        // without tying to current future to the background update
                        //
                        performBackgroundRevalidationIfNeeded(keyString, client, computation, timeToLive, executorService, canCacheValueEvalutor);
                    }
                }
                else {
                    // write with normal semantics
                    logger.debug("set requested for {}", keyString);
                    Cache.logCacheMiss(metricRecorder, keyString, CacheMetricStrings.CACHE_TYPE_ALL);
                    Throwable exceptionDuringWrite = cacheWriteFunction(client, computation,
                            keyString, timeToLive, executorService,
                            canCacheValueEvalutor, promise,store);

                    if(exceptionDuringWrite!=null) {
                        FutureCompleter.completeWithException(promise, keyString, exceptionDuringWrite, store,
                                config.isRemoveFutureFromInternalCacheBeforeSettingValue());
                    }
                }

                return promise;

            } else {
                return returnStaleOrCachedItem(client,keyString,existingFuture,executorService);
            }
        }
    }

    @Override
    public ListenableFuture<V> set(String keyString, Supplier<V> value, Predicate<V> canCacheValueEvalutor, ListeningExecutorService executorService) {
        return set(keyString, value, config.getTimeToLive(), canCacheValueEvalutor, executorService);
    }

    @Override
    public ListenableFuture<V> set(String key, Supplier<V> computation,Duration timeToLive,
                                   Predicate<V> canCacheValueEvalutor,
                                   ListeningExecutorService executorService) {
        String keyString = getHashedKey(key);


        ReferencedClient client = clientFactory.getClient();
        if(!client.isAvailable()) {
            warnCacheDisabled();
            return scheduleValueComputation(keyString,computation,executorService);
        }
        else {
            // write with normal semantics
            final SettableFuture<V> promise = new GuavaSettableFuture<>();

            logger.debug("set requested for {}", keyString);
            Cache.logCacheMiss(metricRecorder, keyString, CacheMetricStrings.CACHE_TYPE_ALL);
            Throwable exceptionDuringWrite = cacheWriteFunction(client, computation,
                    keyString, timeToLive, executorService,
                    canCacheValueEvalutor, promise, DO_NOTHING_MAP);


            if (exceptionDuringWrite != null) {
                promise.setException(exceptionDuringWrite);
            }
            return promise;
        }

    }




    private void performBackgroundRevalidationIfNeeded(final String keyString,
                                                       final ReferencedClient client,
                                                       final Supplier<V> computation,
                                                       final Duration timeToLive,
                                                       final ListeningExecutorService executorService,
                                                       final Predicate<V> canCacheValueEvalutor) {

        ListenableFuture<V> previousEntry = backgroundRevalidationStore.putIfAbsent(keyString,DUMMY_FUTURE_NOT_TO_RETURN);

        if(previousEntry!=null) {
            return;
        } else {
            Throwable ableSubmitForExecution = cacheWriteFunction(client, computation,
                                                                keyString, timeToLive, executorService,
                                                                canCacheValueEvalutor,
                    DUMMY_FUTURE_NOT_TO_RETURN,backgroundRevalidationStore);

            if(ableSubmitForExecution!=null) {
                backgroundRevalidationStore.remove(keyString);
            }
        }
    }


    /**
     * Checks if we should: return the future that has been found in the herd cache map, which is a another call that is
     * either obtaining the item from the cache or calculating the value by calling the {@link java.util.function.Supplier}
     * or if we should perform a lookup for a stale cached value.
     *
     * @param client The client (spy)
     * @param keyRequested The key that has been request
     * @param cachedFuture the existing apply(..) lookup
     * @param executor The executor service to run any futures on.
     * @return
     */
    private  ListenableFuture<V> returnStaleOrCachedItem(ReferencedClient client, String keyRequested,ListenableFuture<V> cachedFuture,
                                                         ListeningExecutorService executor) {
        Cache.logCacheHit(metricRecorder, keyRequested, CacheMetricStrings.CACHE_TYPE_VALUE_CALCULATION);
        if(config.isUseStaleCache()) {
            String staleCacheKey = StaleCacheKeyCreator.createKey(config, keyRequested);
            return getFutureForStaleDistributedCacheLookup(client, staleCacheKey, cachedFuture);
        } else {
            return cachedFuture;
        }
    }

    /**
     * Returns a future that either:
     * <ul>
     *     <li>contains the stale value from memcached</li>
     *     <li>the future that is calculating the value from the given supplier (this future is already executing)</li>
     * </ul>
     *
     *
     * @param key The stale cache key
     * @param backendFuture The future that is actually calculating the fresh cache entry
     * @return  A future that will contain the result
     */
    private ListenableFuture<V> getFutureForStaleDistributedCacheLookup(ReferencedClient client,
                                                                        String key,
                                                                        ListenableFuture<V> backendFuture) {

        // protection against thundering herd on stale memcached
        SettableFuture<V> promise = new GuavaSettableFuture<>();

        ListenableFuture<V> existingFuture = staleStore.putIfAbsent(key, promise);

        if (existingFuture == null) {
            Cache.logCacheMiss(metricRecorder, key, CacheMetricStrings.CACHE_TYPE_STALE_VALUE_CALCULATION);

            V item = cacheReader.getFromDistributedCache(client,
                    key,
                    this.staleCacheMemachedGetTimeoutInMillis,
                    CacheMetricStrings.CACHE_TYPE_STALE_CACHE,
                    metricRecorder);

            if(item==null) {
                FutureCompleter.completeWithValue(promise, key, null, staleStore,
                        config.isRemoveFutureFromInternalCacheBeforeSettingValue());
                return backendFuture;
            } else {
                FutureCompleter.completeWithValue(promise, key, item, staleStore,
                        config.isRemoveFutureFromInternalCacheBeforeSettingValue());
                return promise;
            }

        } else {
            Cache.logCacheHit(metricRecorder, key, CacheMetricStrings.CACHE_TYPE_STALE_VALUE_CALCULATION);
            try {
                V item = promise.get();
                if(item==null) {
                    return backendFuture;
                } else {
                    return promise;
                }
            } catch (Throwable e) {
                return backendFuture;
            }
        }
    }


    private Runnable createCacheWriteRunnable(final ReferencedClient client,
                                              final Supplier<V> computation,
                                              final String key,
                                              final Duration itemExpiry,
                                              final Predicate<V> canCacheValue,
                                              final SettableFuture<V> future,
                                              final ConcurrentMap<String, ListenableFuture<V>> cachedFutures)
    {
        return () -> {
            final long startNanos =  System.nanoTime();
            Throwable throwable = null;
            try {
                V results = computation.get();
                long time = System.nanoTime()-startNanos;
                boolean isNotNullResults = (results != null);
                boolean isCacheable = canCacheValue.test(results);
                if (isNotNullResults & isCacheable) {
                    writeToDistributedStaleCache(client, key, itemExpiry, results);
                    // write the cache entry
                    cacheWriter.writeToDistributedCache(client,
                            key,
                            results,
                            DurationToSeconds.getSeconds(itemExpiry));
                } else {
                    logger.debug("Cache Value cannot be cached.  It has to be either not null:({}), or cachable as determine by predicate:({}). " +
                            "Therefore, not storing in memcached",isNotNullResults,isCacheable);
                }

                setCacheWriteMetrics(CacheMetricStrings.CACHE_TYPE_VALUE_CALCULATION_SUCCESS_TIMER,
                        time,
                        CacheMetricStrings.CACHE_TYPE_VALUE_CALCULATION_SUCCESS_COUNTER);

                FutureCompleter.completeWithValue(future, key, results, cachedFutures,
                        config.isRemoveFutureFromInternalCacheBeforeSettingValue());

            } catch(Throwable err){
                throwable = err;
                long time = System.nanoTime()-startNanos;

                setCacheWriteMetrics(CacheMetricStrings.CACHE_TYPE_VALUE_CALCULATION_FAILURE_TIMER,
                        time,
                        CacheMetricStrings.CACHE_TYPE_VALUE_CALCULATION_FAILURE_COUNTER);

                FutureCompleter.completeWithException(future, key, throwable, cachedFutures,
                        config.isRemoveFutureFromInternalCacheBeforeSettingValue());
            }
        };
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
     * @param key The key against which to store an item
     * @param itemExpiry the expiry for the item
     * @return true if the computation has been successfully submitted to the executorService for
     *              obtaining of the value from the {@code computation}.  false if the computation could not be
     *              scheduled for computation
     */
    private Throwable cacheWriteFunction(final ReferencedClient client,
                                                   final Supplier<V> computation,
                                                   final String key,
                                                   final Duration itemExpiry,
                                                   final ListeningExecutorService executorService,
                                                   final Predicate<V> canCacheValue,
                                                   final SettableFuture<V> future,
                                                   final ConcurrentMap<String,ListenableFuture<V>> cachedFutureStore

    ) {
        try {
            executorService.submit(
                    createCacheWriteRunnable(client, computation, key, itemExpiry, canCacheValue, future, cachedFutureStore));

        } catch(Throwable failedToSubmit) {
            metricRecorder.incrementCounter(CacheMetricStrings.CACHE_TYPE_VALUE_CALCULATION_REJECTION_COUNTER);
            String message = "Unable able to submit computation (Supplier) to executor in order to obtain the value for key: " + key;
            logger.warn(message,failedToSubmit);
            return new UnableToSubmitSupplierForExecutionException(message,failedToSubmit);
        }

        return null;
    }

    private void writeToDistributedStaleCache(ReferencedClient client,String key,Duration ttl,
                                   V valueToWriteToCache) {
        if (config.isUseStaleCache()) {
            String staleCacheKey = StaleCacheKeyCreator.createKey(config,key);
            Duration staleCacheExpiry = ttl.plus(staleCacheAdditionalTimeToLiveValue);
            // overwrite the stale cache entry
            staleCacheWriter.writeToDistributedCache(client,
                    staleCacheKey,
                    valueToWriteToCache,
                    DurationToSeconds.getSeconds(staleCacheExpiry));

        }
    }


    @Override
    public void shutdown() {
        clearInternalCaches();
        clientFactory.shutdown();
    }


    private void clearInternalCaches() {
        store.clear();
        if(config.isUseStaleCache()) {
            staleStore.clear();
        }
        backgroundRevalidationStore.clear();
    }

    public void clear() {
        clear(false);
    }

    @Override
    public void clear(boolean waitForClear) {
        clearInternalCaches();
        ReferencedClient client = clientFactory.getClient();
        if (client.isAvailable()) {
            Future<Boolean> future = client.flush();
            if(future!=null) {
                long millisToWait = config.getWaitForRemove().toMillis();
                if (waitForClear || millisToWait > 0) {
                    try {
                        if (millisToWait > 0) {
                            future.get(millisToWait, TimeUnit.MILLISECONDS);
                        } else {
                            future.get();
                        }
                    } catch (InterruptedException e) {
                        logger.warn("Interrupted whilst waiting for cache clear to occur", e);
                    } catch (ExecutionException e) {
                        logger.warn("Exception whilst waiting for cache clear to occur", e);
                    } catch (TimeoutException e) {
                        logger.warn("Timeout whilst waiting for cache clear to occur", e);
                    }
                }
            }
        }
    }


    private void waitForDelete(Future<Boolean> future,long millisToWait,
                               String key,String cacheBeingCleared
                ) {
        try {
            if (millisToWait > 0) {
                future.get(millisToWait, TimeUnit.MICROSECONDS);
            } else {
                future.get();
            }
        } catch (InterruptedException e) {
            logger.warn("Interrupted whilst waiting for {} clear({}) to occur",cacheBeingCleared, key, e);
        } catch (ExecutionException e) {
            logger.warn("Exception whilst waiting for {} clear({}) to occur",cacheBeingCleared, key, e);
        } catch (TimeoutException e) {
            logger.warn("Timeout whilst waiting for {} clear({}) to occur",cacheBeingCleared, key, e);
        }
    }
    /**
     * removes/deletes a given key from memcached.  Waiting for the remove if
     * config.getWaitForRemove is greater than zero
     * @param key
     */
    @Override
    public void clear(String key) {
        ReferencedClient client = clientFactory.getClient();
        if (client.isAvailable()) {
            key = getHashedKey(key);
            long millisToWait = config.getWaitForRemove().toMillis();
            if (config.isUseStaleCache()) {
                Future<Boolean> staleCacheFuture = client.delete(StaleCacheKeyCreator.createKey(config, key));
                if (staleCacheFuture != null) {
                    waitForDelete(staleCacheFuture, millisToWait, key, "stale cache");
                }
            }
            Future<Boolean> future = client.delete(key);
            if (future != null) {
                waitForDelete(future, millisToWait, key, "cache");
            }
        }
    }
}
