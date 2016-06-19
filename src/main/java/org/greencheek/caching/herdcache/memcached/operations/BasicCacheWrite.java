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
public class BasicCacheWrite implements CacheWrite {
    private static Logger LOGGER = LoggerFactory.getLogger(CacheWrite.class);

    @Override
    public void writeToDistributedCache(ReferencedClient client,
                                        String key,
                                        Object valueToCache,
                                        int entryTTLInSeconds,
                                        boolean waitForMemcachedSet,
                                        long waitForSetDurationInMillis,
                                        MetricRecorder metricRecorder
    ) {
        try {
            metricRecorder.incrementCounter(CacheMetricStrings.CACHE_TYPE_DISTRIBUTED_CACHE_WRITES_COUNTER);

            Future futureSet = client.set(key, entryTTLInSeconds, valueToCache);
            if(waitForMemcachedSet) {
                try {
                    futureSet.get(waitForSetDurationInMillis, TimeUnit.MILLISECONDS);
                } catch (Throwable e) {
                    LOGGER.warn("Exception waiting for memcached set to occur for key {}",key, e);
                }
            }
        } catch (Throwable e) {
            LOGGER.warn("Exception performing memcached set for key {}",key, e);
        }
    }
}
