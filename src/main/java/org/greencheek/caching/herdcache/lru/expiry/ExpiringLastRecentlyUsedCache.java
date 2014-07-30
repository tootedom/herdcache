package org.greencheek.caching.herdcache.lru.expiry;

import com.google.common.util.concurrent.*;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import org.greencheek.caching.herdcache.Cache;

import java.io.Serializable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Created by dominictootell on 28/07/2014.
 */
public class ExpiringLastRecentlyUsedCache<V extends Serializable> implements Cache<V> {

    private final ConcurrentMap<String,TimedEntry<V>> store;
    private final long timeToLiveInNanos;
    private final long timeToIdleInNanos;
    private final CacheValueAndEntryComputationFailureHandler failureHandler;


    public ExpiringLastRecentlyUsedCache(long maxCapacity,int initialCapacity,
                                         long timeToLive, long timeToIdle, TimeUnit timeUnit) {
        timeToLiveInNanos = TimeUnit.NANOSECONDS.convert(timeToLive,timeUnit);
        timeToIdleInNanos = TimeUnit.NANOSECONDS.convert(timeToIdle,timeUnit);

        store =  new ConcurrentLinkedHashMap.Builder<String, TimedEntry<V>>()
                .initialCapacity(initialCapacity)
                .maximumWeightedCapacity(maxCapacity)
                .build();

        failureHandler = (String key,TimedEntry entry, Throwable t) -> { store.remove(key,entry); };
    }


    @Override
    public ListenableFuture<V> apply(String key, Supplier<V> computation, ListeningExecutorService executorService) {
        SettableFuture<V> toBeComputedFuture =  SettableFuture.create();
        TimedEntry<V> newEntry = new TimedEntry(toBeComputedFuture);

        TimedEntry<V> previousTimedEntry = store.putIfAbsent(key, newEntry);
        if(previousTimedEntry==null) {
            ListenableFuture<V> computationFuture = executorService.submit(() -> computation.get());
            Futures.addCallback(computationFuture,
                    new CacheEntryRequestFutureComputationCompleteNotifier<V>(key,newEntry, toBeComputedFuture, failureHandler));
            return toBeComputedFuture;
        } else {
            if(hasNotExpired(previousTimedEntry)) {
                previousTimedEntry.refresh();
                return previousTimedEntry.getFuture();
            } else {
                store.remove(key,previousTimedEntry);
                return insertTimedEntry(key,computation,executorService);
            }

        }

    }

    private ListenableFuture<V>  insertTimedEntry(String key, Supplier<V> computation, ListeningExecutorService executorService) {
        SettableFuture<V> toBeComputedFuture =  SettableFuture.create();
        TimedEntry<V> newEntry = new TimedEntry(toBeComputedFuture);

        FutureCallback<V> callback = new CacheEntryRequestFutureComputationCompleteNotifier<V>(key,newEntry, toBeComputedFuture, failureHandler);

        TimedEntry<V> previousTimedEntry = store.put(key, newEntry);
        if(previousTimedEntry==null) {
            ListenableFuture<V> computationFuture = executorService.submit(() -> computation.get());
            Futures.addCallback(computationFuture, callback);
            return newEntry.getFuture();
        }
        else {
            if(hasNotExpired(previousTimedEntry)) {
                newEntry.setCreatedAt(previousTimedEntry.getCreatedAt());
                Futures.addCallback(previousTimedEntry.getFuture(),callback);

            } else {
                ListenableFuture<V> computationFuture = executorService.submit(() -> computation.get());
                Futures.addCallback(computationFuture,callback);
            }
            return newEntry.getFuture();
        }
    }

    private boolean hasNotExpired(TimedEntry<V> entry) {
        long now = System.nanoTime();
        return (
                (entry.getCreatedAt() + timeToLiveInNanos ) > now &&
                (entry.getLastAccessed() + timeToIdleInNanos) > now
        );
    }
}
