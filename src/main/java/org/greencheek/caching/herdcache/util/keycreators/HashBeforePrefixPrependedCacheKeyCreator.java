package org.greencheek.caching.herdcache.util.keycreators;

import org.greencheek.caching.herdcache.memcached.keyhashing.KeyHashing;

/**
 *
 */
public class HashBeforePrefixPrependedCacheKeyCreator implements CacheKeyCreator {

    private final KeyHashing hasher;
    private final String prefix;

    public HashBeforePrefixPrependedCacheKeyCreator(KeyHashing hasher, String prefix) {
        this.hasher = hasher;
        this.prefix = prefix;
    }

    @Override
    public String createKey(String key) {
        return prefix + hasher.hash(key);
    }
}
