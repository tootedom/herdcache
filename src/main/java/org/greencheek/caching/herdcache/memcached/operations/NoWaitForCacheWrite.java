package org.greencheek.caching.herdcache.memcached.operations;

import org.greencheek.caching.herdcache.memcached.factory.ReferencedClient;
import org.greencheek.caching.herdcache.memcached.metrics.MetricRecorder;
import org.greencheek.caching.herdcache.memcached.util.CacheMetricStrings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 */
public class NoWaitForCacheWrite implements CacheWrite {
    private static Logger LOGGER = LoggerFactory.getLogger(CacheWrite.class);

    private final MetricRecorder metricRecorder;

    public NoWaitForCacheWrite(MetricRecorder metricRecorder) {
        this.metricRecorder = metricRecorder;
    }

    @Override
    public void writeToDistributedCache(ReferencedClient client,
                                        String key,
                                        Object valueToCache,
                                        int entryTTLInSeconds
    ) {
        try {
            metricRecorder.incrementCounter(CacheMetricStrings.CACHE_TYPE_DISTRIBUTED_CACHE_WRITES_COUNTER);
            client.set(key, entryTTLInSeconds, valueToCache);
        } catch (Throwable e) {
            LOGGER.warn("Exception performing memcached set for key {}.  Error {}",key, e.getMessage(), e);
        }
    }
}
