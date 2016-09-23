package org.greencheek.caching.herdcache.memcached;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 *
 */
public class NoOpConcurrentMap<V> implements ConcurrentMap<String,V> {
    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean containsKey(Object key) {
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Override
    public V get(Object key) {
        return null;
    }

    @Override
    public V put(String key, V value) {
        return null;
    }

    @Override
    public V remove(Object key) {
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends V> m) {

    }

    @Override
    public void clear() {

    }

    @Override
    public Set<String> keySet() {
        return null;
    }

    @Override
    public Collection<V> values() {
        return null;
    }

    @Override
    public Set<Entry<String, V>> entrySet() {
        return null;
    }

    @Override
    public V putIfAbsent(String key, V value) {
        return null;
    }

    @Override
    public boolean remove(Object key, Object value) {
        return false;
    }

    @Override
    public boolean replace(String key, V oldValue, V newValue) {
        return false;
    }

    @Override
    public V replace(String key, V value) {
        return null;
    }
}
