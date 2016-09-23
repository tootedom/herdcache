package org.greencheek.caching.herdcache.memcached;

/**
 * Created by dominictootell on 23/09/2016.
 */
class SupplierStatus<V> {
    private final V value;
    private final Throwable throwable;

    public SupplierStatus(V value) {
        this.value = value;
        this.throwable=null;
    }

    public SupplierStatus(Throwable t) {
        this.throwable = t;
        this.value = null;
    }

    public boolean isError() {
        return this.throwable != null;
    }

    public V getValue() {
        return value;
    }

    public Throwable getThrowable() {
        return throwable;
    }
}
