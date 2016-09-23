package org.greencheek.caching.herdcache.domain;

import java.util.Optional;

/**
 */
public class CacheItem<V> {
    private final Optional<V> value;
    private final boolean fromCache;
    private final String key;

    public CacheItem(String key, V value, boolean fromCache) {
        this.key = key;
        this.value = Optional.ofNullable(value);
        this.fromCache = fromCache;
    }

    public Optional<V> getValue() {
        return value;
    }

    public String getKey() { return key; }

    public boolean isFromCache() {
        return fromCache;
    }

    public String toString() {
        return value.toString();
    }
}