package org.greencheek.caching.herdcache.memcached.operations;

import org.greencheek.caching.herdcache.memcached.factory.ReferencedClient;
import org.greencheek.caching.herdcache.memcached.metrics.MetricRecorder;

/**
 *
 */
public interface CacheWrite {
    public void writeToDistributedCache(ReferencedClient client,
                                         String key,
                                         Object valueToCache,
                                         int entryTTLInSeconds,
                                         boolean waitForMemcachedSet,
                                         long waitForSetDurationInMillis,
                                         MetricRecorder metricsRecorder);

}
