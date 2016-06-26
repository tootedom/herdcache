package org.greencheek.caching.herdcache.util.keycreators;

import org.greencheek.caching.herdcache.memcached.keyhashing.KeyHashing;

/**
 */
public class NoPrefixCacheKeyCreator implements CacheKeyCreator {

    private final KeyHashing hasher;

    public NoPrefixCacheKeyCreator(KeyHashing hasher) {
        this.hasher = hasher;
    }

    @Override
    public String createKey(String key) {
        return hasher.hash(key);
    }
}
