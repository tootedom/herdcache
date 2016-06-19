package org.greencheek.caching.herdcache.memcached.operations;

import org.greencheek.caching.herdcache.memcached.factory.ReferencedClient;
import org.greencheek.caching.herdcache.memcached.metrics.MetricRecorder;


/**
 * Performs the read from the distributed
 */
public interface CacheRead<V> {

    public V getFromDistributedCache(ReferencedClient client,
                                     String key,
                                     long timeoutInMillis,
                                     String cacheType,
                                     MetricRecorder metricRecorder);
}
