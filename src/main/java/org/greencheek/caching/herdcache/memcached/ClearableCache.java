package org.greencheek.caching.herdcache.memcached;

/**
 *
 */
public interface ClearableCache<V> {
    public void clear(boolean waitForClear);
    public void clear();

    /**
     * Removes a specify key from the cache
     * @param key
     */
    public void clear(String key);
}
