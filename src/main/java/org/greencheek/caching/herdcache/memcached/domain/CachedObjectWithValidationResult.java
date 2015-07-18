package org.greencheek.caching.herdcache.memcached.domain;

/**
 * Represents an object that has been retrieve from the cache
 * and along with it a result determining if that cached value has been
 * determined worthy enought to by used according to a {@link org.greencheek.caching.herdcache.IsCachedValueUsable}
 * or a {@link java.util.function.Predicate}
 */
public class CachedObjectWithValidationResult<V> {

    public final V cachedObject;
    public final boolean isCachedObjectValid;

    public CachedObjectWithValidationResult(V cachedObject, boolean isValid) {
        this.cachedObject = cachedObject;
        this.isCachedObjectValid = isValid;
    }

}
