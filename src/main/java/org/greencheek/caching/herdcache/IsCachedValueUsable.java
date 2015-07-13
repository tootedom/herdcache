package org.greencheek.caching.herdcache;

import java.io.Serializable;
import java.util.function.Predicate;

/**
 * When a value is retrieved from the cache, this function can be called to determine
 * if that cached value is valid to use or not.  If not, the backend (the supplier) is called
 * to generate a value
 */
public interface IsCachedValueUsable<V extends Serializable> extends Predicate<V> {
    static final IsCachedValueUsable CACHED_VALUE_IS_ALWAYS_USABLE = (V) -> true;

}
