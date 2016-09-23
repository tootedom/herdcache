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

    public Optional<V> optional() {
        return value;
    }

    /**
     * Returns null if the optional is empty
     * @return
     */
    public V value() {
        if(value.isPresent()) {
            return value.get();
        } else {
            return null;
        }
    }

    public V value(V defaultVal) {
        return value.isPresent() ? value.get() : defaultVal;
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

    public boolean isEmpty() {
        return !value.isPresent();
    }

    public boolean hasValue() {
        return value.isPresent();
    }
}