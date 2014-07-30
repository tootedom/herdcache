package org.greencheek.caching.herdcache.lru.expiry;

import com.google.common.util.concurrent.SettableFuture;

/**
 * Created by dominictootell on 28/07/2014.
 */
public class TimedEntry<V> {

    private final SettableFuture<V> future;
    private volatile long createdAt;
    private volatile long lastAccessed;

    public TimedEntry(SettableFuture<V> promise) {
        future = promise;
        init();
    }

    public void refresh() {
        // we dont care whether we overwrite a potentially newer value
        lastAccessed = System.nanoTime();
    }

    void init() {
        createdAt = System.nanoTime();
        lastAccessed = System.nanoTime();
    }


    public SettableFuture<V> getFuture() {
        return future;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastAccessed() {
        return lastAccessed;
    }
}
