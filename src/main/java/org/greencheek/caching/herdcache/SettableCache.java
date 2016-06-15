package org.greencheek.caching.herdcache;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.io.Serializable;
import java.time.Duration;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Allows the setting of a cache value from a Supplier, without first checking the cache.
 * Simple set(...) methods, with no cache lookup first.  For use when you know the cache is empty
 */
public interface SettableCache<V extends Serializable> extends CacheWithExpiry<V> {


}
