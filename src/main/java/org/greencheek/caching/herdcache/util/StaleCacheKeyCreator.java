package org.greencheek.caching.herdcache.util;

import org.greencheek.caching.herdcache.memcached.config.MemcachedCacheConfig;

/**
 *
 */
public class StaleCacheKeyCreator {
    public static String createKey(MemcachedCacheConfig config, String key) {
        return config.getStaleCachePrefix() + key;
    }
}
