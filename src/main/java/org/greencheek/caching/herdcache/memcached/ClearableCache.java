package org.greencheek.caching.herdcache.memcached;

/**
 * Created by dominictootell on 31/08/2014.
 */
public interface ClearableCache {
    public void clear(boolean waitForClear);
}
