package org.greencheek.caching.herdcache.lru.expiry;

import com.google.common.util.concurrent.*;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import org.greencheek.caching.herdcache.Cache;
import org.greencheek.caching.herdcache.IsSupplierValueCachable;

import java.sql.Time;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;


/**
 * Do not use.  Not fully supported.  Will be removed in future version
 * @param <V>
 */
@Deprecated
public class ExpiringLastRecentlyUsedCache<V> implements Cache<V> {

    private enum TimedEntryType { TTL_ONLY, TTL_WITH_IDLE}

    private final ConcurrentMap<String,TimedEntry<V>> store;
    private final ExpiryTimes expiryTimes;
    private final TimedEntryType timedEntryType;

    private final CacheValueAndEntryComputationFailureHandler failureHandler;


    public ExpiringLastRecentlyUsedCache(int maxCapacity,
                                         long timeToLive, long timeToIdle, TimeUnit timeUnit) {
        this(maxCapacity,maxCapacity,timeToLive,timeToIdle,timeUnit);
    }

    public ExpiringLastRecentlyUsedCache(int maxCapacity,int initialCapacity,
                                         long timeToLive, long timeToIdle, TimeUnit timeUnit) {
        expiryTimes = new ExpiryTimes(timeToIdle,timeToLive,timeUnit);

        if(timeToLive<1) {
            throw new InstantiationError("Time To Live must be greater than 0");
        }

        if(timeToIdle < 1) {
            timedEntryType = TimedEntryType.TTL_ONLY;
        } else {
            timedEntryType = TimedEntryType.TTL_WITH_IDLE;
        }

        store =  new ConcurrentLinkedHashMap.Builder<String, TimedEntry<V>>()
                .initialCapacity(initialCapacity)
                .maximumWeightedCapacity(maxCapacity)
                .build();

        failureHandler = (String key,TimedEntry entry, Throwable t) -> { store.remove(key,entry); };
    }

    public int size() {
        return store.size();
    }

    public ListenableFuture<V> apply(String key, Supplier<V> computation, ListeningExecutorService executorService,
                                     Predicate<V> canCacheValueEvalutor,Predicate<V> isCachedValueUsable) {
        TimedEntry<V> value = store.get(key);
        if(value==null) {
            return insertTimedEntry(key,computation,executorService,canCacheValueEvalutor);
        } else {
            if(value.hasNotExpired(expiryTimes)) {
                value.touch();
                return value.getFuture();
            } else {
                return insertTimedEntry(key,computation,executorService,canCacheValueEvalutor);
            }
        }
    }

    @Override
    public ListenableFuture<V> get(String key, ListeningExecutorService executorService) {
        TimedEntry<V> value = store.get(key);
        if(value==null) {
            return Futures.immediateCheckedFuture(null);
        } else {
            if(value.hasNotExpired(expiryTimes)) {
                value.touch();
                return value.getFuture();
            } else {
                return Futures.immediateCheckedFuture(null);
            }
        }
    }

    @Override
    public ListenableFuture<V> set(String keyString, Supplier<V> value, Predicate<V> canCacheValueEvalutor, ListeningExecutorService executorService) {
        return insertTimedEntry(keyString,value,executorService,canCacheValueEvalutor);
    }


    private TimedEntry<V> createTimedEntry(SettableFuture<V> future) {
        TimedEntry<V> entry;
        switch (timedEntryType) {
            case TTL_WITH_IDLE:
                entry = new IdleTimedEntryWithExpiry<>(future);
                break;
            default:
                entry = new TimedEntryWithExpiry<>(future);
                break;
        }
        return entry;
    }

    private ListenableFuture<V>  insertTimedEntry(String key, Supplier<V> computation,
                                                  ListeningExecutorService executorService,
                                                  Predicate<V> canCacheValueEvalutor) {
        SettableFuture<V> toBeComputedFuture =  SettableFuture.create();
        TimedEntry<V> newEntry = createTimedEntry(toBeComputedFuture);

        FutureCallback<V> callback = new CacheEntryRequestFutureComputationCompleteNotifier<V>(key,newEntry, toBeComputedFuture, failureHandler,
                (result) -> {
                    if(!canCacheValueEvalutor.test(result)) {
                        store.remove(key,newEntry);
                    }
                });



        TimedEntry<V> previousTimedEntry = store.put(key, newEntry);
        if(previousTimedEntry==null) {
            ListenableFuture<V> computationFuture = executorService.submit(() -> computation.get());
            Futures.addCallback(computationFuture, callback);
            return newEntry.getFuture();
        }
        else {
            if(previousTimedEntry.hasNotExpired(expiryTimes)) {
                newEntry.setCreatedAt(previousTimedEntry.getCreatedAt());
                Futures.addCallback(previousTimedEntry.getFuture(),callback);

            } else {
                ListenableFuture<V> computationFuture = executorService.submit(() -> computation.get());
                Futures.addCallback(computationFuture,callback);
            }
            return newEntry.getFuture();
        }


    }


}
