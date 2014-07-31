package org.greencheek.caching.herdcache.lru.expiry;

import com.google.common.util.concurrent.SettableFuture;

/**
 * Created by dominictootell on 28/07/2014.
 */
public class IdleTimedEntryWithExpiry<V> implements TimedEntry<V> {

    private final SettableFuture<V> future;
    private volatile long createdAt;
    private volatile long lastAccessed;

    public IdleTimedEntryWithExpiry(SettableFuture<V> promise) {
        future = promise;
        createdAt = System.nanoTime();
        lastAccessed = System.nanoTime();
    }

    @Override
    public SettableFuture<V> getFuture() {
        return future;
    }

    @Override
    public boolean hasNotExpired(ExpiryTimes expiryTimes) {
        long now = System.nanoTime();
        return (
                (createdAt + expiryTimes.getTimeToLiveInNanos() ) > now &&
                (lastAccessed + expiryTimes.getTimeToIdleInNanos()) > now
        );
    }

    @Override
    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
        lastAccessed = System.nanoTime();
    }

    @Override
    public void touch() {
        lastAccessed = System.nanoTime();
    }

}
