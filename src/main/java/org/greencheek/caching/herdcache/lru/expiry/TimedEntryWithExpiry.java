package org.greencheek.caching.herdcache.lru.expiry;

import com.google.common.util.concurrent.SettableFuture;

/**
 * Created by dominictootell on 28/07/2014.
 */
public class TimedEntryWithExpiry<V> implements TimedEntry<V> {

    private final SettableFuture<V> future;
    private volatile long createdAt;

    public TimedEntryWithExpiry(SettableFuture<V> promise) {
        future = promise;
        createdAt = System.nanoTime();
    }

    @Override
    public SettableFuture<V> getFuture() {
        return future;
    }

    @Override
    public boolean hasNotExpired(ExpiryTimes expiryTimes) {
        long now = System.nanoTime();
        return (
                (createdAt + expiryTimes.getTimeToLiveInNanos() ) > now
        );
    }

    @Override
    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

}
