package org.greencheek.caching.herdcache.memcached;

//import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.*;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
//import io.netty.util.concurrent.Promise;
import net.spy.memcached.ConnectionFactory;
import org.greencheek.caching.herdcache.*;
import org.greencheek.caching.herdcache.exceptions.UnableToScheduleCacheGetExecutionException;
import org.greencheek.caching.herdcache.exceptions.UnableToSubmitSupplierForExecutionException;
import org.greencheek.caching.herdcache.lru.CacheRequestFutureComputationCompleteNotifier;
import org.greencheek.caching.herdcache.lru.CacheValueComputationFailureHandler;
import org.greencheek.caching.herdcache.memcached.config.ElastiCacheCacheConfig;
import org.greencheek.caching.herdcache.memcached.config.MemcachedCacheConfig;
import org.greencheek.caching.herdcache.memcached.factory.*;
import org.greencheek.caching.herdcache.memcached.keyhashing.*;
import org.greencheek.caching.herdcache.memcached.metrics.MetricRecorder;
import org.greencheek.caching.herdcache.memcached.spyconnectionfactory.SpyConnectionFactoryBuilder;
import org.greencheek.caching.herdcache.memcached.callbacks.FailureCallback;
import org.greencheek.caching.herdcache.memcached.callbacks.SuccessCallback;
import org.greencheek.caching.herdcache.memcached.util.futures.SettableFuture;
import org.greencheek.caching.herdcache.memcached.util.futures.DoNothingSettableFuture;
import org.greencheek.caching.herdcache.memcached.util.futures.GuavaSettableFuture;
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
 class BaseMemcachedCache<V extends Serializable> implements RequiresShutdown,ClearableCache,
        SerializableOnlyCacheWithExpiry<V>, RevalidateInBackgroundCapableCache<V>
         {

    public static ConnectionFactory createMemcachedConnectionFactory(MemcachedCacheConfig config) {
        return SpyConnectionFactoryBuilder.createConnectionFactory(
                config.getFailureMode(),
                config.getHashAlgorithm(), config.getSerializingTranscoder(),
                config.getProtocol(),config.getReadBufferSize(),config.getKeyHashType(),
                config.getLocatorFactory());
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

    public static final String CACHE_TYPE_VALUE_CALCULATION = "value_calculation_cache";
    public static final String CACHE_TYPE_VALUE_CALCULATION_ALL_TIMER = "value_calculation_time";
    public static final String CACHE_TYPE_VALUE_CALCULATION_SUCCESS_TIMER = "value_calculation_success_latency";
    public static final String CACHE_TYPE_VALUE_CALCULATION_FAILURE_TIMER = "value_calculation_failure_latency";
    public static final String CACHE_TYPE_VALUE_CALCULATION_SUCCESS_COUNTER = "value_calculation_success";
    public static final String CACHE_TYPE_VALUE_CALCULATION_FAILURE_COUNTER = "value_calculation_failure";
    public static final String CACHE_TYPE_VALUE_CALCULATION_REJECTION_COUNTER= "value_calculation_rejected_execution";
    public static final String CACHE_TYPE_STALE_VALUE_CALCULATION = "stale_value_calculation_cache";
    public static final String CACHE_TYPE_CACHE_DISABLED = "disabled_cache";
    public static final String CACHE_TYPE_CACHE_DISABLED_REJECTION = "disabled_cache";

    public static final String CACHE_TYPE_STALE_CACHE = "stale_distributed_cache";
    public static final String CACHE_TYPE_DISTRIBUTED_CACHE = "distributed_cache";
    public static final String CACHE_TYPE_DISTRIBUTED_CACHE_WRITES_COUNTER="distributed_cache_writes";
    public static final String CACHE_TYPE_DISTRIBUTED_CACHE_REJECTION = "distributed_cache_rejection";
    public static final String CACHE_TYPE_ALL = "cache";


    private static final Logger logger  = LoggerFactory.getLogger(BaseMemcachedCache.class);
    private static final Logger cacheHitMissLogger   = LoggerFactory.getLogger("MemcachedCacheHitsLogger");

    private final SettableFuture<V> DUMMY_FUTURE_NOT_TO_RETURN = new DoNothingSettableFuture<>();

    private final ConcurrentMap<String,ListenableFuture<V>> DO_NOTHING_MAP = new NoOpConcurrentMap<>();

    private final MemcachedCacheConfig config;
    private final KeyHashing keyHashingFunction;
    private final String keyprefix;
    private final MemcachedClientFactory clientFactory;
    private final ConcurrentMap<String,ListenableFuture<V>> store;
    private final int staleMaxCapacityValue;
    private final Duration staleCacheAdditionalTimeToLiveValue;
    private final ConcurrentMap<String,ListenableFuture<V>> staleStore;
    private final ConcurrentMap<String,ListenableFuture<V>> backgroundRevalidationStore;



    private final long memcachedGetTimeoutInMillis;
    private final long staleCacheMemachedGetTimeoutInMillis;
    private final long waitForSetDurationInMillis;

    private final CacheValueComputationFailureHandler failureHandler;

    private final MetricRecorder metricRecorder;


    public BaseMemcachedCache(
            MemcachedClientFactory clientFactory,
            MemcachedCacheConfig config) {
        this.config = config;
        this.keyprefix = config.getKeyPrefix();
        keyHashingFunction = getKeyHashingFunction(config.getKeyHashType());
        this.clientFactory = clientFactory;

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

        waitForSetDurationInMillis = config.getSetWaitDuration().toMillis();

        failureHandler = (String key, Throwable t) -> { store.remove(key); };

        metricRecorder = config.getMetricsRecorder();
    }

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

    private boolean isEnabled() {
        return clientFactory.isEnabled();
    }

    private void logCacheHit(String key, String cacheType) {
        metricRecorder.cacheHit(cacheType);
        cacheHitMissLogger.debug("{ \"cachehit\" : \"{}\", \"cachetype\" : \"{}\"}",key,cacheType);
    }

    private void logCacheMiss(String key, String cacheType) {
        metricRecorder.cacheMiss(cacheType);
        cacheHitMissLogger.debug("{ \"cachemiss\" : \"{}\", \"cachetype\" : \"{}\"}",key,cacheType);
    }

    private void warnCacheDisabled() {
        logger.warn("Cache is disabled");
    }



    private KeyHashing getKeyHashingFunction(KeyHashingType type) {
       switch (type) {
           case NONE:
               return new NoKeyHashing();
           case NATIVE_XXHASH:
               return new FastestXXHashKeyHashing();
           case NATIVE_XXHASH_64:
               return new XXHashKeyHashing(true,true);
           case JAVA_XXHASH:
               return new JavaXXHashKeyHashing();
           case JAVA_XXHASH_64:
               return new XXHashKeyHashing(false,true);
           case MD5_UPPER:
               return new MessageDigestHashing(KeyHashing.MD5,Runtime.getRuntime().availableProcessors()*2,true);
           case SHA256_UPPER:
               return new MessageDigestHashing(KeyHashing.SHA256,Runtime.getRuntime().availableProcessors()*2,true);
           case MD5_LOWER:
               return new MessageDigestHashing(KeyHashing.MD5,Runtime.getRuntime().availableProcessors()*2,false);
           case SHA256_LOWER:
               return new MessageDigestHashing(KeyHashing.SHA256,Runtime.getRuntime().availableProcessors()*2,false);
           default:
               return new FastestXXHashKeyHashing();
       }
    }

    private String getHashedKey(String key) {
        if(config.hasKeyPrefix()) {
            if(config.isHashKeyPrefix()) {
                return keyHashingFunction.hash(keyprefix + key);
            } else {
                return keyprefix + keyHashingFunction.hash(key);
            }
        } else {
            return keyHashingFunction.hash(key);
        }
    }

    private long getDuration(Duration timeToLive){
        if(timeToLive==null || timeToLive == Duration.ZERO) {
            return 0;
        }
        else {
            long timeToLiveSec  = timeToLive.getSeconds();
            return (timeToLiveSec >= 1l) ? timeToLiveSec : 0;
        }


    }

    private void writeToDistributedCache(ReferencedClient client,
                                         String key, Object valueToCache,
                                         int entryTTLInSeconds, boolean waitForMemcachedSet) {
        try {
            Future futureSet = client.set(key, entryTTLInSeconds, valueToCache);
            if(waitForMemcachedSet) {
                try {
                    futureSet.get(waitForSetDurationInMillis, TimeUnit.MILLISECONDS);
                } catch (Throwable e) {
                    logger.warn("Exception waiting for memcached set to occur for key {}",key, e);
                }
            }
        } catch (Throwable e) {
             logger.warn("Exception performing memcached set for key {}",key, e);
        }
    }

    private void writeToDistributedCache(ReferencedClient client,
                                         String key, V value,
                                         Duration timeToLive, boolean waitForMemcachedSet) {
        metricRecorder.incrementCounter(CACHE_TYPE_DISTRIBUTED_CACHE_WRITES_COUNTER);
        writeToDistributedCache(client, key, value, (int)getDuration(timeToLive),waitForMemcachedSet);

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
            logCacheMiss(key,CACHE_TYPE_CACHE_DISABLED);
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
                metricRecorder.incrementCounter(CACHE_TYPE_CACHE_DISABLED_REJECTION);
                logger.warn("Unable able to submit computation (Supplier) to executor in order to obtain the value for key {}", key, failedToSubmit);
                toBeComputedFuture.setException(failedToSubmit);
            }


            return toBeComputedFuture;
        } else {
            logCacheHit(key,CACHE_TYPE_VALUE_CALCULATION);
            return previousFuture;
        }
    }

    private ListenableFuture<V> getFromDistributedCache(final ReferencedClient client,
                                                        final String key,
                                                        final ListeningExecutorService ec) {
        try {
            return ec.submit(
                            createGetFromDistributedCacheCallable(() -> getFromDistributedCache(client, key, memcachedGetTimeoutInMillis, CACHE_TYPE_DISTRIBUTED_CACHE),
                                    key)
                    );
        } catch(Throwable failedToSubmit) {
            final SettableFuture<V> promise = new GuavaSettableFuture<>();
            metricRecorder.incrementCounter(CACHE_TYPE_DISTRIBUTED_CACHE_REJECTION);
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
                logCacheMiss(keyString, CACHE_TYPE_CACHE_DISABLED);
                return Futures.immediateCheckedFuture(null);
            } else {
                logCacheHit(keyString, CACHE_TYPE_VALUE_CALCULATION);
                return previousFuture;
            }
        } else {
            ListenableFuture<V> future = store.get(keyString);
            if(future==null) {
                logCacheMiss(keyString, CACHE_TYPE_VALUE_CALCULATION);
                ListenableFuture<V> futureForCacheLookup = getFromDistributedCache(client,keyString,executorService);
                return futureForCacheLookup;
            }
            else {
                logCacheHit(keyString, CACHE_TYPE_VALUE_CALCULATION);
                if(config.isUseStaleCache()) {
                    return getFutureForStaleDistributedCacheLookup(client, createStaleCacheKey(keyString), future);
                } else {
                    return future;
                }
            }
        }
    }

    private Callable<V> createGetFromDistributedCacheCallable(final Supplier<V> supplier, final String keyString) {
        return () -> {
            boolean ok = false;
            V result = null;
            try {
                result = supplier.get();
                ok = true;
            } catch (Throwable throwable) {
                ok = false;
            }

            if(ok && result!=null) {
                logCacheHit(keyString, CACHE_TYPE_ALL);
            } else {
                logCacheMiss(keyString,CACHE_TYPE_ALL);
            }
            return result;
        };
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
                logCacheMiss(keyString, CACHE_TYPE_VALUE_CALCULATION);
                // check memcached.

                V cachedObject = getFromDistributedCache(client,keyString,memcachedGetTimeoutInMillis,CACHE_TYPE_DISTRIBUTED_CACHE);

                boolean cachedObjectFoundInCache = cachedObject!=null;
                boolean validCachedObject = (cachedObjectFoundInCache && isCachedValueValid.test(cachedObject));
                boolean doRevalidationInBackground = returnInvalidCachedItemWhileRevalidate && cachedObjectFoundInCache && !validCachedObject;

                if(validCachedObject || doRevalidationInBackground) {
                    logCacheHit(keyString,CACHE_TYPE_ALL);
                    removeFutureFromInternalCache(promise, keyString, cachedObject, store);
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
                    logCacheMiss(keyString, CACHE_TYPE_ALL);
                    Throwable exceptionDuringWrite = cacheWriteFunction(client, computation,
                            keyString, timeToLive, executorService,
                            canCacheValueEvalutor, promise,store);
//                            (V result) -> removeFutureFromInternalCache(promise, keyString, result, store),
//                            (Throwable t) -> removeFutureFromInternalCacheWithException(promise, keyString, t, store));

                    if(exceptionDuringWrite!=null) {
                        removeFutureFromInternalCacheWithException(promise, keyString, exceptionDuringWrite, store);
                    }
                }

                return promise;

            } else {
                return returnStaleOrCachedItem(client,keyString,existingFuture,executorService);
            }
        }
    }

    @Override
    public ListenableFuture<V> set(String keyString, V value) {
        return set(keyString,value,MoreExecutors.newDirectExecutorService());
    }

    @Override
    public ListenableFuture<V> set(String keyString, V value, ListeningExecutorService executorService) {
        return set(keyString, () -> value,config.getTimeToLive(),Cache.CAN_ALWAYS_CACHE_VALUE, executorService);
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
            logCacheMiss(keyString, CACHE_TYPE_ALL);
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
//
//                                                                (V result) -> backgroundRevalidationStore.remove(keyString),
//                                                                (Throwable error) -> backgroundRevalidationStore.remove(keyString));

            if(ableSubmitForExecution!=null) {
                backgroundRevalidationStore.remove(keyString);
            }
        }
    }

    private void removeFutureFromInternalCache(SettableFuture<V> promise,String keyString, V cachedObject,
                                               ConcurrentMap<String,ListenableFuture<V>> internalCache) {
        if(config.isRemoveFutureFromInternalCacheBeforeSettingValue()) {
            internalCache.remove(keyString);
            promise.set(cachedObject);
        } else {
            promise.set(cachedObject);
            internalCache.remove(keyString);
        }
    }

    private void removeFutureFromInternalCacheWithException(SettableFuture<V> promise,String keyString, Throwable exception,
                                               ConcurrentMap<String,ListenableFuture<V>> internalCache) {
        if(config.isRemoveFutureFromInternalCacheBeforeSettingValue()) {
            internalCache.remove(keyString);
            promise.setException(exception);
        } else {
            promise.setException(exception);
            internalCache.remove(keyString);
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
        logCacheHit(keyRequested, CACHE_TYPE_VALUE_CALCULATION);
        if(config.isUseStaleCache()) {
            String staleCacheKey = createStaleCacheKey(keyRequested);
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
            logCacheMiss(key, BaseMemcachedCache.CACHE_TYPE_STALE_VALUE_CALCULATION);

            V item = getFromDistributedCache(client, key, this.staleCacheMemachedGetTimeoutInMillis, CACHE_TYPE_STALE_CACHE);

            if(item==null) {
                removeFutureFromInternalCache(promise, key, null, staleStore);
                return backendFuture;
            } else {
                removeFutureFromInternalCache(promise, key, item, staleStore);
                return promise;
            }

        } else {
            logCacheHit(key, BaseMemcachedCache.CACHE_TYPE_STALE_VALUE_CALCULATION);
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


    private Callable<V> createCacheWriteCallable(final ReferencedClient client,
                                                 final Supplier<V> computation,
                                                 final String key,
                                                 final Duration itemExpiry,
                                                 final Predicate<V> canCacheValue,
                                                 final SettableFuture<V> future,
                                                 final ConcurrentMap<String,ListenableFuture<V>> cachedFutures) {
//                                                 final SuccessCallback<V> successFutureCallBack,
//                                                 final FailureCallback failureFutureCallBack) {
        return () -> {
            final long startNanos =  System.nanoTime();
            boolean ok = false;
            V results = null;
            Throwable throwable = null;
            try {
                results = computation.get();
                ok = true;
            } catch(Throwable err){
                throwable = err;
            } finally {
                long time = System.nanoTime()-startNanos;
                metricRecorder.setDuration(CACHE_TYPE_VALUE_CALCULATION_ALL_TIMER,time);

                if(ok) {
                    metricRecorder.setDuration(CACHE_TYPE_VALUE_CALCULATION_SUCCESS_TIMER,time);

                    if (results != null) {
                        if (canCacheValue.test(results)) {
                            writeToDistributedStaleCache(client, key, itemExpiry, results);
                            // write the cache entry
                            writeToDistributedCache(client, key, results, itemExpiry, config.isWaitForMemcachedSet());
                        } else {
                            logger.debug("Cache Value cannot be cached, as determine by predicate. Therefore, not storing in memcached");
                        }
                    } else {
                        logger.debug("Cache Value computation was null, not storing in memcached");
                    }

                    metricRecorder.incrementCounter(CACHE_TYPE_VALUE_CALCULATION_SUCCESS_COUNTER);
                    removeFutureFromInternalCache(future,key,results,cachedFutures);
//                    successFutureCallBack.onSuccess(results);

                } else {
                    metricRecorder.incrementCounter(CACHE_TYPE_VALUE_CALCULATION_FAILURE_COUNTER);
                    metricRecorder.setDuration(CACHE_TYPE_VALUE_CALCULATION_FAILURE_TIMER,time);
                    removeFutureFromInternalCacheWithException(future,key,throwable,cachedFutures);
//                    failureFutureCallBack.onFailure(throwable);
                }
                return results;
            }
        };
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
//                                                   final SuccessCallback<V> successFutureCallBack,
//                                                   final FailureCallback failureFutureCallBack

    ) {
        try {
            executorService.submit(
//                    createCacheWriteCallable(client,computation,key,itemExpiry,canCacheValue,successFutureCallBack,failureFutureCallBack));
                    createCacheWriteCallable(client,computation,key,itemExpiry,canCacheValue,future,cachedFutureStore));

        } catch(Throwable failedToSubmit) {
            metricRecorder.incrementCounter(CACHE_TYPE_VALUE_CALCULATION_REJECTION_COUNTER);
            String message = "Unable able to submit computation (Supplier) to executor in order to obtain the value for key: " + key;
            logger.warn(message,failedToSubmit);
            return new UnableToSubmitSupplierForExecutionException(message,failedToSubmit);
        }

        return null;
    }

    private void writeToDistributedStaleCache(ReferencedClient client,String key,Duration ttl,
                                   V valueToWriteToCache) {
        if (config.isUseStaleCache()) {
            String staleCacheKey =  createStaleCacheKey(key);
            Duration staleCacheExpiry = ttl.plus(staleCacheAdditionalTimeToLiveValue);
            // overwrite the stale cache entry
            writeToDistributedCache(client, staleCacheKey, valueToWriteToCache, staleCacheExpiry, false);
        }
    }

    /**
     * Returns an Object from the distributed cache.  The object will be
     * an instance of Serializable.  If no item existed in the cached
     * null WILL be returned
     *
     * @param key The key to find in the distributed cache
     * @param timeoutInMillis The amount of time to wait for the get on the distributed cache
     * @param cacheType The cache type.  This is output to the log when a hit or miss is logged
     * @return
     */
    private V getFromDistributedCache(ReferencedClient client,String key, long timeoutInMillis,
                                      String cacheType) {
        V serialisedObj = null;
        long nanos = System.nanoTime();
        try {
            serialisedObj = (V) client.get(key,timeoutInMillis, TimeUnit.MILLISECONDS);
            if(serialisedObj==null){
                logCacheMiss(key,cacheType);
            } else {
                logCacheHit(key,cacheType);
            }
        } catch(Throwable e) {
            logger.warn("Exception thrown when communicating with memcached for get({}): {}", key, e.getMessage());
        } finally {
            metricRecorder.incrementCounter(cacheType);
            metricRecorder.setDuration(cacheType,System.nanoTime()-nanos);
        }

        return serialisedObj;
    }

    private String createStaleCacheKey(String key) {
        return config.getStaleCachePrefix() + key;
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
                Future<Boolean> staleCacheFuture = client.delete(createStaleCacheKey(key));
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
