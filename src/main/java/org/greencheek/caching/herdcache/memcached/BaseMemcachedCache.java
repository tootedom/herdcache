package org.greencheek.caching.herdcache.memcached;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.*;
import net.spy.memcached.ConnectionFactory;
import org.greencheek.caching.herdcache.*;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Created by dominictootell on 23/08/2014.
 */
 class BaseMemcachedCache<V extends Serializable> implements RequiresShutdown,ClearableCache,
        SerializableOnlyCacheWithExpiry<V>, RevalidateInBackgroundCapableCache<V>
         {

    public static ConnectionFactory createMemcachedConnectionFactory(MemcachedCacheConfig config) {
        return SpyConnectionFactoryBuilder.createConnectionFactory(
                config.getHashingType(), config.getFailureMode(),
                config.getHashAlgorithm(), config.getSerializingTranscoder(),
                config.getProtocol(),config.getReadBufferSize(),config.getKeyHashType());
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
    public static final String CACHE_TYPE_STALE_VALUE_CALCULATION = "stale_value_calculation_cache";
    public static final String CACHE_TYPE_CACHE_DISABLED = "disabled_cache";
    public static final String CACHE_TYPE_STALE_CACHE = "stale_distributed_cache";
    public static final String CACHE_TYPE_DISTRIBUTED_CACHE = "distributed_cache";

    private static final Logger logger  = LoggerFactory.getLogger(BaseMemcachedCache.class);
    private static final Logger cacheHitMissLogger   = LoggerFactory.getLogger("MemcachedCacheHitsLogger");

    private static final Consumer DO_NOTHING_CONSUMER = (result) -> {};

    private final MemcachedCacheConfig config;
    private final KeyHashing keyHashingFunction;
    private final String keyprefix;
    private final MemcachedClientFactory clientFactory;
    private final ConcurrentMap<String,ListenableFuture<V>> store;
    private final int staleMaxCapacityValue;
    private final Duration staleCacheAdditionalTimeToLiveValue;
    private final ConcurrentMap<String,ListenableFuture<V>> staleStore;
    private final ConcurrentMap<String,String> backgroundRevalidationStore;



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

        this.store = (ConcurrentMap)Caffeine.newBuilder()
                .maximumSize(maxCapacity)
                .initialCapacity(maxCapacity)
                .build()
                .asMap();

        this.backgroundRevalidationStore = (ConcurrentMap)Caffeine.newBuilder()
                .maximumSize(maxCapacity)
                .initialCapacity(maxCapacity)
                .build()
                .asMap();

//        this.store = new ConcurrentLinkedHashMap.Builder<String, ListenableFuture<V>>()
//                .initialCapacity(maxCapacity)
//                .maximumWeightedCapacity(maxCapacity)
//                .build();

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

//        staleStore = config.isUseStaleCache() ?
//                new ConcurrentLinkedHashMap.Builder<String, ListenableFuture<V>>()
//                        .initialCapacity(staleMaxCapacityValue)
//                        .maximumWeightedCapacity(staleMaxCapacityValue)
//                        .build() : null;

        staleStore = config.isUseStaleCache() ?
                (ConcurrentMap)Caffeine.newBuilder()
                        .maximumSize(staleMaxCapacityValue)
                        .initialCapacity(staleMaxCapacityValue)
                        .build()
                        .asMap() :
                null;

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
                } catch (Exception e) {
                    logger.warn("Exception waiting for memcached set to occur", e);
                }
            }
        } catch (Exception e) {
             logger.warn("Exception performing memcached set", e);
        }
    }

    private void writeToDistributedCache(ReferencedClient client,
                                         String key, V value,
                                         Duration timeToLive, boolean waitForMemcachedSet) {
        metricRecorder.incrementCounter("distributed_cache_writes");
        writeToDistributedCache(client, key, value, (int)getDuration(timeToLive),waitForMemcachedSet);

    }

    private ListenableFuture<V> scheduleValueComputation(String key,Supplier<V> computation, ListeningExecutorService executorService) {
        SettableFuture<V> toBeComputedFuture =  SettableFuture.create();
        ListenableFuture<V> previousFuture = store.putIfAbsent(key, toBeComputedFuture);
        if(previousFuture==null) {
            logCacheMiss(key,CACHE_TYPE_CACHE_DISABLED);
            ListenableFuture<V> computationFuture = executorService.submit(() -> computation.get());
            Futures.addCallback(computationFuture,
                    new CacheRequestFutureComputationCompleteNotifier<V>(key, toBeComputedFuture, failureHandler,DO_NOTHING_CONSUMER));

            Futures.addCallback(computationFuture,
                    new FutureCallback<V>() {
                        @Override
                        public void onSuccess(V result) {
                            store.remove(key);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            store.remove(key);
                        }
                    });
            return toBeComputedFuture;
        } else {
            logCacheHit(key,CACHE_TYPE_VALUE_CALCULATION);
            return previousFuture;
        }
    }

    private ListenableFuture<V> getFromDistributedCache(ReferencedClient client,String key,ListeningExecutorService ec) {
        return ec.submit(() -> getFromDistributedCache(client, key, memcachedGetTimeoutInMillis, CACHE_TYPE_DISTRIBUTED_CACHE));
    }

    @Override
    public ListenableFuture<V> get(String key) {
        return get(key, MoreExecutors.newDirectExecutorService());
    }


    @Override
    public ListenableFuture<V> get(String key, ListeningExecutorService executorService) {
        String keyString = getHashedKey(key);
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
                return getFromDistributedCache(client,keyString,executorService);
            }
            else {
                logCacheHit(keyString, CACHE_TYPE_VALUE_CALCULATION);
                if(config.isUseStaleCache()) {
                    return getFutueForStaleDistributedCacheLookup(client,createStaleCacheKey(keyString), future, executorService);
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
            final SettableFuture<V> promise = SettableFuture.create();
            // create and store a new future for the to be generated value
            // first checking against local a cache to see if the computation is already
            // occurring
            ListenableFuture<V> existingFuture  = store.putIfAbsent(keyString, promise);

            if(existingFuture==null) {
                logCacheMiss(keyString, CACHE_TYPE_VALUE_CALCULATION);
                // check memcached.

                V cachedObject = getFromDistributedCache(client,keyString,memcachedGetTimeoutInMillis,CACHE_TYPE_DISTRIBUTED_CACHE);

                boolean cachedObjectNotFoundInCache = cachedObject==null;
                boolean validCachedObject = (!cachedObjectNotFoundInCache && isCachedValueValid.test(cachedObject));

                boolean cacheWriteOccurred = false;

                if(cachedObjectNotFoundInCache) {
                    // write with normal semantics
                    logger.debug("set requested for {}", keyString);
                    cacheWriteOccurred = cacheWriteFunction(client, computation, promise,
                                                           keyString, timeToLive, executorService,
                                                           canCacheValueEvalutor,
                            (V result) -> removeFutureFromInternalCache(promise, keyString, result, store),
                            (Throwable t) -> removeFutureFromInternalCacheWithException(promise, keyString, t, store));
                }
                else if(!validCachedObject) {
                    if (returnInvalidCachedItemWhileRevalidate) {
                        // return the future, but schedule update in background
                        // without tying to current future to the background update
                        //
                        performBackgroundRevalidationIfNeeded(keyString,client,computation,timeToLive,executorService,canCacheValueEvalutor);

                    } else {
                        logger.debug("set requested for {}", keyString);
                        cacheWriteOccurred = cacheWriteFunction(client, computation, promise,keyString,
                                                               timeToLive, executorService,canCacheValueEvalutor,
                                (V result) -> removeFutureFromInternalCache(promise, keyString, result, store),
                                (Throwable t) -> removeFutureFromInternalCacheWithException(promise, keyString, t, store));
                    }
                }

                if(cacheWriteOccurred==false) {
                    removeFutureFromInternalCache(promise, keyString, cachedObject, store);
                }
                return promise;
            } else {
                return returnStaleOrCachedItem(client,keyString,existingFuture,executorService);
            }
        }
    }

    private void performBackgroundRevalidationIfNeeded(final String keyString,
                                                       final ReferencedClient client,
                                                       final Supplier<V> computation,
                                                       final Duration timeToLive,
                                                       final ListeningExecutorService executorService,
                                                       final Predicate<V> canCacheValueEvalutor) {

        String previousEntry = backgroundRevalidationStore.putIfAbsent(keyString,keyString);

        if(previousEntry!=null) {
            return;
        } else {
            cacheWriteFunction(client, computation, null,
                    keyString,
                    timeToLive, executorService,
                    canCacheValueEvalutor,
                    (V result) -> backgroundRevalidationStore.remove(keyString),
                    (Throwable error) -> backgroundRevalidationStore.remove(keyString));
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
            return getFutueForStaleDistributedCacheLookup(client,staleCacheKey,cachedFuture,executor);
        } else {
            return cachedFuture;
        }
    }

    /**
     * returns a future that is consulting the stale memcached cache.  If the item is not in the
     * cache, the backendFuture will be invoked (complete the operation).
     *
     * @param key The stale cache key
     * @param backendFuture The future that is actually calculating the fresh cache entry
     * @param ec  The require execution context to run the stale cache key.
     * @return  A future that will result in the stored Serializable object
     */
    private ListenableFuture<V> getFutueForStaleDistributedCacheLookup(ReferencedClient client,
                                                                       String key,
                                                                       ListenableFuture<V> backendFuture,
                                                                       ListeningExecutorService ec) {

        // protection against thundering herd on stale memcached
        SettableFuture<V> promise = SettableFuture.create();

        ListenableFuture<V> existingFuture = staleStore.putIfAbsent(key, promise);

        if (existingFuture == null) {
            logCacheMiss(key, BaseMemcachedCache.CACHE_TYPE_STALE_VALUE_CALCULATION);
            ec.submit(() -> getFromStaleDistributedCache(client,key, promise, backendFuture));
            return promise;
        }
        else {
            logCacheHit(key, BaseMemcachedCache.CACHE_TYPE_STALE_VALUE_CALCULATION);
            return existingFuture;
        }
    }

    /**
     * Talks to memcached to find a cached entry. If the entry does not exist, the backend Future will
     * be 'consulted' and it's value with be returned.
     *
     * @param key The cache key to lookup
     * @param promise the promise on which requests are waiting.
     * @param backendFuture the future that is running the long returning calculation that creates a fresh entry.
     */
    private void getFromStaleDistributedCache(final ReferencedClient client,
                                              final String key,
                                              final SettableFuture<V> promise,
                                              final ListenableFuture<V> backendFuture) {

        V item = getFromDistributedCache(client,key,this.staleCacheMemachedGetTimeoutInMillis, CACHE_TYPE_STALE_CACHE);

        if(item==null) {
            Futures.addCallback(backendFuture, new FutureCallback<V>() {
                        @Override
                        public void onSuccess(V result) {
                            removeFutureFromInternalCache(promise,key,result,staleStore);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            removeFutureFromInternalCacheWithException(promise, key, t, staleStore);
                        }
                    });

        } else {
            removeFutureFromInternalCache(promise,key,(V)item,staleStore);
        }

    }



    /**
     * write to memcached when the future completes, the generated value,
     * against the given key, with the specified expiry
     * @param computation  The future that will generate the value
     * @param promise The promise that is stored in the thurdering herd local cache
     * @param key The key against which to store an item
     * @param itemExpiry the expiry for the item
     * @return true if the computation has been successfully submitted to the executorService for
     *              obtaining of the value from the {@code computation}.  false if the computation could not be
     *              scheduled for computation
     */
    private boolean cacheWriteFunction(final ReferencedClient client,
                                                   final Supplier<V> computation,
                                                   final SettableFuture<V> promise,
                                                   final String key,
                                                   final Duration itemExpiry,
                                                   final ListeningExecutorService executorService,
                                                   final Predicate<V> canCacheValue,
                                                   final SuccessCallback<V> successFutureCallBack,
                                                   final FailureCallback failureFutureCallBack

    ) {
        final long startNanos = System.nanoTime();
        ListenableFuture<V> computationFuture = null;
        try {
            computationFuture = executorService.submit(() -> computation.get());
        } catch(Throwable failedToSubmit) {
            logger.warn("Unable able to submit computation (Supplier) to executor in order to obtain the value for key {}", key,failedToSubmit);
            return false;
        }
        Futures.addCallback(computationFuture,
                new FutureCallback<V>() {
                    @Override
                    public void onSuccess(V result) {
                        try {
                            metricRecorder.setDuration("value_calculation_time",System.nanoTime()-startNanos);
                            if(result!=null) {
                                if(canCacheValue.test(result)) {
                                    writeToDistributedStaleCache(client, key, itemExpiry, result);
                                    // write the cache entry
                                    writeToDistributedCache(client, key, result, itemExpiry, config.isWaitForMemcachedSet());
                                } else {
                                    logger.debug("Cache Value cannot be cached, as determine by predicate. Therefore, not storing in memcached");
                                }
                            } else {
                                logger.debug("Cache Value computation was null, not storing in memcached");
                            }

                        } catch (Exception e) {
                            logger.error("problem setting key {} in memcached", key,e);
                        } finally {
                            metricRecorder.incrementCounter("value_calculation_success");
                            successFutureCallBack.onSuccess(result);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        metricRecorder.incrementCounter("value_calculation_failure");
                        metricRecorder.setDuration("value_calculation",System.nanoTime()-startNanos);
                        failureFutureCallBack.onFailure(t);
                    }
                });
        return true;
    }

    private void writeToDistributedStaleCache(ReferencedClient client,String key,Duration ttl,
                                   V valueToWriteToCache) {
        if (config.isUseStaleCache()) {
            String staleCacheKey =  createStaleCacheKey(key);;
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
