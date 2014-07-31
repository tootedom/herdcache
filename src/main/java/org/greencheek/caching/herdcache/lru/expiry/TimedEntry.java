package org.greencheek.caching.herdcache.lru.expiry;

import com.google.common.util.concurrent.SettableFuture;

/**
 * Created by dominictootell on 31/07/2014.
 */
public interface TimedEntry<V> {
    public SettableFuture<V> getFuture();
    public long getCreatedAt();
    public void setCreatedAt(long expiry);
    public boolean hasNotExpired(ExpiryTimes expiries);

    default public void touch() {

    }


}
