package org.greencheek.caching.herdcache.lru.expiry;

/**
 * Created by dominictootell on 28/07/2014.
 */
public interface CacheValueAndEntryComputationFailureHandler {
    public void onFailure(String key,TimedEntry entry, Throwable e);
}
