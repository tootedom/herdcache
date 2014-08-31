package org.greencheek.caching.herdcache.memcached;

import com.google.common.util.concurrent.*;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.MemcachedClientIF;
import net.spy.memcached.OperationTimeoutException;
import net.spy.memcached.internal.CheckedOperationTimeoutException;
import org.greencheek.caching.herdcache.CacheWithExpiry;
import org.greencheek.caching.herdcache.RequiresShutdown;
import org.greencheek.caching.herdcache.lru.CacheRequestFutureComputationCompleteNotifier;
import org.greencheek.caching.herdcache.lru.CacheValueComputationFailureHandler;
import org.greencheek.caching.herdcache.memcached.config.MemcachedCacheConfig;
import org.greencheek.caching.herdcache.memcached.factory.MemcachedClientFactory;
import org.greencheek.caching.herdcache.memcached.keyhashing.*;
import org.greencheek.caching.herdcache.memcached.spyconnectionfactory.SpyConnectionFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Created by dominictootell on 23/08/2014.
 */
 class BaseMemcachedCache<V> implements CacheWithExpiry<V>,RequiresShutdown,ClearableCache {

    public static ConnectionFactory createMemcachedConnectionFactory(MemcachedCacheConfig config) {
        return SpyConnectionFactoryBuilder.createConnectionFactory(
                config.getHashingType(), config.getFailureMode(),
                config.getHashAlgorithm(), config.getSerializingTranscoder(),
                config.getProtocol(),config.getReadBufferSize(),config.getKeyHashType());
    }


    public static final String CACHE_TYPE_VALUE_CALCULATION = "value_calculation_cache";
    public static final String CACHE_TYPE_CACHE_DISABLED = "disabled_cache";
    public static final String CACHE_TYPE_STALE_CACHE = "stale_distributed_cache";
    public static final String CACHE_TYPE_DISTRIBUTED_CACHE = "distributed_cache";

    private static final Logger logger  = LoggerFactory.getLogger(BaseMemcachedCache.class);
    private static final Logger cacheHitMissLogger   = LoggerFactory.getLogger("MemcachedCacheHitsLogger");


    private final MemcachedCacheConfig config;
    private final KeyHashing keyHashingFunction;
    private final String keyprefix;
    private final MemcachedClientFactory clientFactory;
    private final ConcurrentLinkedHashMap<String,ListenableFuture<V>> store;
    private final int staleMaxCapacityValue;
    private final Duration staleCacheAdditionalTimeToLiveValue;
    private final ConcurrentLinkedHashMap<String,ListenableFuture<V>> staleStore;

    private final long memcachedGetTimeoutInMillis;
    private final long staleCacheMemachedGetTimeoutInMillis;
    private final long waitForSetDurationInMillis;


    private final CacheValueComputationFailureHandler failureHandler;


    public BaseMemcachedCache(
            MemcachedClientFactory clientFactory,
            MemcachedCacheConfig config) {
        this.config = config;
        this.keyprefix = config.getKeyPrefix();
        keyHashingFunction = getKeyHashingFunction(config.getKeyHashType());
        this.clientFactory = clientFactory;

        int maxCapacity = config.getMaxCapacity();

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
        if(staleDuration==Duration.ZERO) {
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
        if(config.getStaleCacheMemachedGetTimeout() == Duration.ZERO) {
            staleCacheMemachedGetTimeoutInMillis = memcachedGetTimeoutInMillis;
        } else {
            staleCacheMemachedGetTimeoutInMillis = config.getStaleCacheMemachedGetTimeout().toMillis();
        }

        waitForSetDurationInMillis = config.getSetWaitDuration().toMillis();

        failureHandler = (String key, Throwable t) -> { store.remove(key); };

    }

    private MemcachedClientIF getMemcachedClient() {
        return clientFactory.getClient();
    }


    private boolean isEnabled() {
        return clientFactory.isEnabled();
    }

    private void logCacheHit(String key, String cacheType) {
        cacheHitMissLogger.debug("{ \"cachehit\" : \"{}\", \"cachetype\" : \"{}\"}",key,cacheType);
    }

    private void logCacheMiss(String key, String cacheType) {
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
           case JAVA_XXHASH:
               return new JavaXXHashKeyHashing();
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
            return keyHashingFunction.hash(keyprefix + key);
        } else {
            return keyHashingFunction.hash(key);
        }
    }

    private long getDuration(Duration timeToLive){
        if(timeToLive==null || timeToLive.toMillis() <= 0) {
            return 0;
        }
        else {
            long timeToLiveSec  = timeToLive.getSeconds();
            return (timeToLiveSec >= 1l) ? timeToLiveSec : 0;
        }


    }

    private void writeToDistributedCache(String key, V value,
                                         Duration timeToLive, boolean waitForMemcachedSet) {
        int entryTTLInSeconds = (int)getDuration(timeToLive);

        if( waitForMemcachedSet ) {
            Future<Boolean> futureSet = getMemcachedClient().set(key, entryTTLInSeconds, value);
            try {
                futureSet.get(waitForSetDurationInMillis, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                logger.warn("Exception waiting for memcached set to occur",e);
            }

        } else {
            try {
                getMemcachedClient().set(key, entryTTLInSeconds, value);
            } catch (Exception e) {
                logger.warn("Exception waiting for memcached set to occur");
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
                    new CacheRequestFutureComputationCompleteNotifier<V>(key, toBeComputedFuture, failureHandler));
            Futures.addCallback(computationFuture,
                    new FutureCallback<V>() {
                        @Override
                        public void onSuccess(V result) {
                            store.remove(key,toBeComputedFuture);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            store.remove(key,toBeComputedFuture);
                        }
                    });
            return toBeComputedFuture;
        } else {
            logCacheHit(key,CACHE_TYPE_VALUE_CALCULATION);
            return previousFuture;
        }
    }

    private ListenableFuture<V> getFromDistributedCache(String key,ListeningExecutorService ec) {
        return ec.submit(() -> getFromDistributedCache(key));
    }


    @Override
    public ListenableFuture<V> apply(String key, Supplier<V> computation, ListeningExecutorService executorService) {
        return apply(key,computation,config.getTimeToLive(),executorService);
    }

    @Override
    public ListenableFuture<V> get(String key, ListeningExecutorService executorService) {
        String keyString = getHashedKey(key);
        if(!isEnabled()) {
            warnCacheDisabled();
            ListenableFuture<V> previousFuture = store.get(key);
            if(previousFuture==null) {
                logCacheMiss(keyString, BaseMemcachedCache.CACHE_TYPE_CACHE_DISABLED);
                return Futures.immediateCheckedFuture(null);
            } else {
                logCacheHit(key, CACHE_TYPE_VALUE_CALCULATION);
                return previousFuture;
            }
        } else {
            ListenableFuture<V> future = store.get(keyString);
            if(future==null) {
                return getFromDistributedCache(keyString,executorService);
            }
            else {
                logCacheHit(keyString, BaseMemcachedCache.CACHE_TYPE_VALUE_CALCULATION);
                if(config.isUseStaleCache()) {
                    return getFutueForStaleDistributedCacheLookup(createStaleCacheKey(keyString), future, executorService);
                } else {
                    return future;
                }
            }
        }
    }

    @Override
    public ListenableFuture<V> apply(String key,
                                     Supplier<V> computation,
                                     Duration timeToLive,
                                     ListeningExecutorService executorService) {

        String keyString = getHashedKey(key);

        if(!isEnabled()) {
            warnCacheDisabled();
            return scheduleValueComputation(key,computation,executorService);
        }
        else {
            String staleCacheKey = null;
            Duration staleCacheExpiry = null;
            if(config.isUseStaleCache()) {
                staleCacheKey = createStaleCacheKey(keyString);
                staleCacheExpiry = timeToLive.plus(staleCacheAdditionalTimeToLiveValue);
            }

            SettableFuture<V> promise = SettableFuture.create();
            // create and store a new future for the to be generated value
            // first checking against local a cache to see if the computation is already
            // occurring

            ListenableFuture<V> existingFuture  = store.putIfAbsent(keyString, promise);
            //      val existingFuture : Future[Serializable] = store.get(keyString)
            if(existingFuture==null) {
                // check memcached.
                Object cachedObject = getFromDistributedCache(keyString);
                if(cachedObject == null)
                {
                    logger.debug("set requested for {}", keyString);
                    return cacheWriteFunction(computation, promise,
                            keyString, staleCacheKey,
                            timeToLive,staleCacheExpiry,executorService);
                }
                else {
                    if(config.isRemoveFutureFromInternalCacheBeforeSettingValue()) {
                        store.remove(keyString,promise);
                        promise.set((V)cachedObject);
                    } else {
                        promise.set((V)cachedObject);
                        store.remove(keyString, promise);
                    }
                    return promise;
                }
            }
            else  {
                System.out.println(existingFuture);
                System.out.flush();
                logCacheHit(keyString, BaseMemcachedCache.CACHE_TYPE_VALUE_CALCULATION);
                if(config.isUseStaleCache()) {
                    return getFutueForStaleDistributedCacheLookup(staleCacheKey,existingFuture,executorService);
                } else {
                    return existingFuture;
                }
            }
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
    private ListenableFuture<V> getFutueForStaleDistributedCacheLookup(String key,
                                                                       ListenableFuture<V> backendFuture,
                                                                       ListeningExecutorService ec) {

        // protection against thundering herd on stale memcached
        SettableFuture<V> promise = SettableFuture.create();

        ListenableFuture<V> existingFuture = staleStore.putIfAbsent(key, promise);

        if(existingFuture == null) {
            ec.submit(() -> getFromStaleDistributedCache(key, promise, backendFuture));
            return promise;
        }
        else {
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
    private void getFromStaleDistributedCache(String key,
                                              SettableFuture<V> promise,
                                              ListenableFuture<V> backendFuture) {

        Object item = getFromDistributedCache(key,this.staleCacheMemachedGetTimeoutInMillis, BaseMemcachedCache.CACHE_TYPE_STALE_CACHE);

        if(item==null) {
            Futures.addCallback(backendFuture, new FutureCallback<V>() {
                        @Override
                        public void onSuccess(V result) {
                            if(config.isRemoveFutureFromInternalCacheBeforeSettingValue()) {
                                staleStore.remove(key, promise);
                                promise.set(result);
                            } else {
                                promise.set(result);
                                staleStore.remove(key, promise);
                            }

                        }

                        @Override
                        public void onFailure(Throwable t) {
                            if(config.isRemoveFutureFromInternalCacheBeforeSettingValue()) {
                                staleStore.remove(key, promise);
                                promise.setException(t);
                            } else {
                                promise.setException(t);
                                staleStore.remove(key, promise);
                            }
                        }
                    });

        } else {
            promise.set((V) item);
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
    private ListenableFuture<V> cacheWriteFunction(Supplier<V> computation,
                                                   final SettableFuture<V> promise,
                                                   final String key, String staleCacheKey,
                                                   Duration itemExpiry,
                                                   Duration staleItemExpiry,
                                                   ListeningExecutorService executorService) {

        ListenableFuture<V> computationFuture = executorService.submit(() -> computation.get());
        Futures.addCallback(computationFuture,
                new FutureCallback<V>() {
                    @Override
                    public void onSuccess(V result) {
                        try {
                            if (config.isUseStaleCache()) {
                                // overwrite the stale cache entry
                                writeToDistributedCache(staleCacheKey, result, staleItemExpiry, false);
                            }
                            // write the cache entry
                            writeToDistributedCache(key, result, itemExpiry, config.isWaitForMemcachedSet());

                        } catch (Exception e) {
                            logger.error("problem setting key {} in memcached", key);
                        } finally {
                            if (config.isRemoveFutureFromInternalCacheBeforeSettingValue()) {
                                store.remove(key, promise);
                                promise.set(result);
                            } else {
                                promise.set(result);
                                store.remove(key, promise);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        if (config.isRemoveFutureFromInternalCacheBeforeSettingValue()) {
                            store.remove(key, promise);
                            promise.setException(t);
                        } else {
                            promise.setException(t);
                            store.remove(key, promise);
                        }
                    }
                });
        return promise;
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
    private V getFromDistributedCache(String key, long timeoutInMillis,
                                           String cacheType) {
        Object serialisedObj = null;
        try {
            Future<Object> future =  getMemcachedClient().asyncGet(key);
            Object cacheVal = future.get(timeoutInMillis,TimeUnit.MILLISECONDS);
            if(cacheVal==null){
                logCacheMiss(key,cacheType);
            } else {
                logCacheHit(key,cacheType);
                serialisedObj = cacheVal;
            }
        } catch ( OperationTimeoutException | CheckedOperationTimeoutException e) {
            logger.warn("timeout when retrieving key {} from memcached",key);
        } catch (TimeoutException e) {
            logger.warn("timeout when retrieving key {} from memcached", key);
        } catch(Exception e) {
            logger.warn("Unable to contact memcached for get({}): {}", key, e.getMessage());
        } catch(Throwable e) {
            logger.warn("Exception thrown when communicating with memcached for get({}): {}", key, e.getMessage());
        }


        return (V)serialisedObj;
    }

    /**
     * Obtains a item from the distributed cache.
     *
     * @param key The key under which to find a cached object.
     * @return The cached object
     */
    private V getFromDistributedCache(String key) {
        return getFromDistributedCache(key,memcachedGetTimeoutInMillis,CACHE_TYPE_DISTRIBUTED_CACHE);
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

    @Override
    public void clear(boolean waitForClear) {
        clearInternalCaches();
        Future<Boolean> future = getMemcachedClient().flush();
        if(waitForClear) {
            try {
                future.get();
            } catch (InterruptedException e) {
                logger.warn("Interrupted whilst waiting for cache clear to occur",e);
            } catch (ExecutionException e) {
                logger.warn("Exception whilst waiting for cache clear to occur",e);
            }
        }
    }
}
