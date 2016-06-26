package org.greencheek.caching.herdcache.util.keycreators;

/**
 * Given a string that represents the key under which the item
 * the caller wishes to store the item in the cache, a new key will be
 * return depending upon if the caller has requested keys to be hashed, or
 * prefixes to be appended.
 */
public interface CacheKeyCreator {
    String createKey(String key);
}
