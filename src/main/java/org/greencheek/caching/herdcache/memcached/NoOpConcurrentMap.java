package org.greencheek.caching.herdcache.memcached;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 *
 */
public class NoOpConcurrentMap<V> implements ConcurrentMap<String,ListenableFuture<V>> {
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
    public ListenableFuture<V> get(Object key) {
        return null;
    }

    @Override
    public ListenableFuture<V> put(String key, ListenableFuture<V> value) {
        return null;
    }

    @Override
    public ListenableFuture<V> remove(Object key) {
        return null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends ListenableFuture<V>> m) {

    }

    @Override
    public void clear() {

    }

    @Override
    public Set<String> keySet() {
        return null;
    }

    @Override
    public Collection<ListenableFuture<V>> values() {
        return null;
    }

    @Override
    public Set<Entry<String, ListenableFuture<V>>> entrySet() {
        return null;
    }

    @Override
    public ListenableFuture<V> putIfAbsent(String key, ListenableFuture<V> value) {
        return null;
    }

    @Override
    public boolean remove(Object key, Object value) {
        return false;
    }

    @Override
    public boolean replace(String key, ListenableFuture<V> oldValue, ListenableFuture<V> newValue) {
        return false;
    }

    @Override
    public ListenableFuture<V> replace(String key, ListenableFuture<V> value) {
        return null;
    }
}
