package org.greencheek.caching.herdcache.lru.expiry;

import java.util.concurrent.TimeUnit;

/**
 * Created by dominictootell on 31/07/2014.
 */
public class ExpiryTimes {
    private final long timeToIdleInNanos;
    private final long timeToLiveInNanos;


    public ExpiryTimes(long timeToIdleInNanos, long timeToLiveInNanos, TimeUnit unit) {
        this.timeToIdleInNanos = TimeUnit.NANOSECONDS.convert(timeToIdleInNanos,unit);
        this.timeToLiveInNanos = TimeUnit.NANOSECONDS.convert(timeToLiveInNanos,unit);
    }

    public long getTimeToIdleInNanos() {
        return timeToIdleInNanos;
    }

    public long getTimeToLiveInNanos() {
        return timeToLiveInNanos;
    }
}
