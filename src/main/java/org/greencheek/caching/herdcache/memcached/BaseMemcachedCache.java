package org.greencheek.caching.herdcache.memcached;

//import com.github.benmanes.caffeine.cache.Caffeine;
//import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.util.concurrent.*;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import net.spy.memcached.ConnectionFactory;
import org.greencheek.caching.herdcache.*;
import org.greencheek.caching.herdcache.lru.CacheRequestFutureComputationCompleteNotifier;
import org.greencheek.caching.herdcache.lru.CacheValueComputationFailureHandler;
import org.greencheek.caching.herdcache.memcached.config.ElastiCacheCacheConfig;
import org.greencheek.caching.herdcache.memcached.config.MemcachedCacheConfig;
import org.greencheek.caching.herdcache.memcached.domain.CachedObjectWithValidationResult;
import org.greencheek.caching.herdcache.memcached.predicates.CachedItemPredicate;
import org.greencheek.caching.herdcache.memcached.factory.*;
import org.greencheek.caching.herdcache.memcached.keyhashing.*;
import org.greencheek.caching.herdcache.memcached.metrics.MetricRecorder;
import org.greencheek.caching.herdcache.memcached.spyconnectionfactory.SpyConnectionFactoryBuilder;
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
        SerializableOnlyCacheWithExpiry<V>,
        SerializableOnlyCacheWithUserSuppliedExpiry<V> {

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

//        this.store = (ConcurrentMap)Caffeine.newBuilder().maximumSize(maxCapacity).initialCapacity(maxCapacity).build().asMap();

        this.store = new ConcurrentLinkedHashMap.Builder<String, ListenableFuture<V>>()
                .initialCapacity(maxCapacity)
                .maximumWeightedCapacity(maxCapacity)
                .build();

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

        staleStore = config.isUseStaleCache() ?
                new ConcurrentLinkedHashMap.Builder<String, ListenableFuture<V>>()
                        .initialCapacity(staleMaxCapacityValue)
                        .maximumWeightedCapacity(staleMaxCapacityValue)
                        .build() : null;


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
        if(timeToLive==null) {
            return 0;
        }
        else {
            long timeToLiveSec  = timeToLive.getSeconds();
            return (timeToLiveSec >= 1l) ? timeToLiveSec : 0;
        }


    }

    private void writeToDistributedCache(boolean isUsingCachedItemWrapper,ReferencedClient client,
                                         String key, V value,
                                         Duration timeToLive, boolean waitForMemcachedSet) {
        metricRecorder.incrementCounter("distributed_cache_writes");
        int entryTTLInSeconds;
        Object valueToCache = value;
        if (isUsingCachedItemWrapper) {
            valueToCache = new CachedItem<>(value);
            entryTTLInSeconds = 0;
        } else {
            entryTTLInSeconds = (int) getDuration(timeToLive);
        }

        if (waitForMemcachedSet) {
            Future<Boolean> futureSet = client.set(key, entryTTLInSeconds, valueToCache);
            try {
                futureSet.get(waitForSetDurationInMillis, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                logger.warn("Exception waiting for memcached set to occur", e);
            }
        } else {
            try {
                client.set(key, entryTTLInSeconds, valueToCache);
            } catch (Exception e) {
                logger.warn("Exception waiting for memcached set to occur", e);
            }
        }
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
        return ec.submit(() -> {
            Object item = getFromDistributedCache(client,key);
            if(item !=null && item instanceof CachedItem) {
                item = ((CachedItem)item).getCachedItem();
            }
            return (V)item;
        });
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
    public ListenableFuture<V> getOrSet(String key, Supplier<V> thebackendcall, long getInspectionTTLInMillis, Predicate<V> canUseCachedValue,
                      Predicate<V> shouldCacheBackendCall, ListeningExecutorService executor) {
        return apply(true,key, thebackendcall, Duration.ofMillis(getInspectionTTLInMillis),executor,shouldCacheBackendCall,canUseCachedValue);
    }

    private boolean evaluateIfCachedValueIsValid(CachedItem cachedItem, Duration timeToLive,Predicate<V> isCachedItemValid) {
        if(isCachedItemValid instanceof CachedItemPredicate) {
            CachedItemPredicate tester = (CachedItemPredicate)isCachedItemValid;
            if (timeToLive == Duration.ZERO) {
                return tester.test(cachedItem);
            } else {
                return (cachedItem.isLive(timeToLive) && tester.test(cachedItem));
            }
        } else {
            if (timeToLive == Duration.ZERO) {
                return isCachedItemValid.test((V) cachedItem.getCachedItem());
            } else {
                return (cachedItem.isLive(timeToLive) && isCachedItemValid.test((V) cachedItem.getCachedItem()));
            }
        }
    }

    @Override
    public ListenableFuture<V> apply(String key,
                                     Supplier<V> computation,
                                     Duration timeToLive,
                                     ListeningExecutorService executorService,
                                     Predicate<V> canCacheValueEvalutor,Predicate<V> isCachedValueValid) {
        return apply(false,key,computation,timeToLive,executorService,canCacheValueEvalutor,isCachedValueValid);

    }


    @Override
    public ListenableFuture<V> apply(String key,
                                     Supplier<V> computation,
                                     Duration timeToLive,
                                     ListeningExecutorService executorService,
                                     IsSupplierValueCachable<V> canCacheValueEvalutor,IsCachedValueUsable<V> isCachedValueValid) {
        return apply(false,key,computation,timeToLive,executorService,canCacheValueEvalutor,isCachedValueValid);

    }


    private ListenableFuture<V> apply(boolean isUsingCachedItemWrapper,String key,
                                     Supplier<V> computation,
                                     Duration timeToLive,
                                     ListeningExecutorService executorService,
                                     Predicate<V> canCacheValueEvalutor,Predicate<V> isCachedValueValid)
    {

        String keyString = getHashedKey(key);

        ReferencedClient client = clientFactory.getClient();
        if(!client.isAvailable()) {
            warnCacheDisabled();
            return scheduleValueComputation(keyString,computation,executorService);
        }
        else {
            SettableFuture<V> promise = SettableFuture.create();
            // create and store a new future for the to be generated value
            // first checking against local a cache to see if the computation is already
            // occurring
            ListenableFuture<V> existingFuture  = store.putIfAbsent(keyString, promise);
            //      val existingFuture : Future[Serializable] = store.get(keyString)
            if(existingFuture==null) {
                logCacheMiss(keyString, CACHE_TYPE_VALUE_CALCULATION);
                // check memcached.

                CachedObjectWithValidationResult<V> validatedCachedObject = validateCachedObject(getFromDistributedCache(client,keyString),
                        timeToLive,isCachedValueValid);

                if(!validatedCachedObject.isCachedObjectValid)
                {
                    logger.debug("set requested for {}", keyString);
                    cacheWriteFunction(isUsingCachedItemWrapper,client, computation, promise,
                            keyString,
                            timeToLive, executorService,
                            canCacheValueEvalutor);
                }
                else {
                    removeFutureFromInternalCache(promise,keyString,validatedCachedObject.cachedObject,store);
                }
                return promise;
            } else {
                return returnStaleOrCachedItem(client,keyString,existingFuture,executorService);
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


    private CachedObjectWithValidationResult<V> validateCachedObject(Object cachedObject, Duration timeToLive,
                                                                     Predicate<V> isCachedValueValid) {

        boolean validCachedObject;
        if(cachedObject instanceof CachedItem) {
            CachedItem<V> item = (CachedItem)cachedObject;
            validCachedObject = (item != null && evaluateIfCachedValueIsValid(item,timeToLive,isCachedValueValid));
            if(validCachedObject == true) {
                cachedObject = item.getCachedItem();
            }
        }
        else {
            validCachedObject = (cachedObject != null && isCachedValueValid.test((V)cachedObject));
        }

        return new CachedObjectWithValidationResult<>((V)cachedObject,validCachedObject);
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

        Object item = getFromDistributedCache(client,key,this.staleCacheMemachedGetTimeoutInMillis, CACHE_TYPE_STALE_CACHE);

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
            if(item instanceof CachedItem) {
                item = ((CachedItem)item).getCachedItem();
            }
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
     * @return
     */
    private void cacheWriteFunction(final boolean isUsingCachedItemWrapper,final ReferencedClient client,
                                                   final Supplier<V> computation,
                                                   final SettableFuture<V> promise,
                                                   final String key,
                                                   final Duration itemExpiry,
                                                   final ListeningExecutorService executorService,
                                                   final Predicate<V> canCacheValue) {
        final long startNanos = System.nanoTime();
        ListenableFuture<V> computationFuture = executorService.submit(() -> computation.get());
        Futures.addCallback(computationFuture,
                new FutureCallback<V>() {
                    @Override
                    public void onSuccess(V result) {
                        try {
                            metricRecorder.setDuration("value_calculation_time",System.nanoTime()-startNanos);
                            if(result!=null) {
                                if(canCacheValue.test(result)) {
                                    if (config.isUseStaleCache()) {
                                        String staleCacheKey =  createStaleCacheKey(key);;
                                        Duration staleCacheExpiry = itemExpiry.plus(staleCacheAdditionalTimeToLiveValue);;
                                        // overwrite the stale cache entry
                                        writeToDistributedCache(isUsingCachedItemWrapper, client, staleCacheKey, result, staleCacheExpiry, false);
                                    }
                                    // write the cache entry
                                    writeToDistributedCache(isUsingCachedItemWrapper, client, key, result, itemExpiry, config.isWaitForMemcachedSet());
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
                            removeFutureFromInternalCache(promise,key,result,store);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        metricRecorder.incrementCounter("value_calculation_failure");
                        metricRecorder.setDuration("value_calculation",System.nanoTime()-startNanos);
                        removeFutureFromInternalCacheWithException(promise, key, t, store);
                    }
                });
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
    private Object getFromDistributedCache(ReferencedClient client,String key, long timeoutInMillis,
                                      String cacheType) {
        Object serialisedObj = null;
        long nanos = System.nanoTime();
        try {
            Object cacheVal = client.get(key,timeoutInMillis, TimeUnit.MILLISECONDS);
            if(cacheVal==null){
                logCacheMiss(key,cacheType);
            } else {
                logCacheHit(key,cacheType);
                serialisedObj = cacheVal;
            }
        } catch(Throwable e) {
            logger.warn("Exception thrown when communicating with memcached for get({}): {}", key, e.getMessage());
        } finally {
            metricRecorder.incrementCounter(cacheType);
            metricRecorder.setDuration(cacheType,System.nanoTime()-nanos);
        }

        return serialisedObj;
    }

    /**
     * Obtains a item from the distributed cache.
     *
     * @param key The key under which to find a cached object.
     * @return The cached object
     */
    private Object getFromDistributedCache(ReferencedClient client,String key) {
        return getFromDistributedCache(client,key,memcachedGetTimeoutInMillis,CACHE_TYPE_DISTRIBUTED_CACHE);
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
