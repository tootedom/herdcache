package org.greencheek.caching.herdcache.memcached.predicates;

import org.greencheek.caching.herdcache.IsCachedValueUsable;
import org.greencheek.caching.herdcache.memcached.CachedItem;

import java.io.Serializable;


/**
 * A Predicate that is supplied with a {@link org.greencheek.caching.herdcache.memcached.CachedItem}
 * to evaluate if the cached item is valid or not.
 *
 */
public interface CachedItemPredicate<V extends Serializable> extends IsCachedValueUsable<CachedItem<V>> {
    boolean test(CachedItem<V> t);
}
