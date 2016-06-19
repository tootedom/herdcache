package org.greencheek.caching.herdcache.memcached.operations;

import org.greencheek.caching.herdcache.Cache;
import org.greencheek.caching.herdcache.memcached.factory.ReferencedClient;
import org.greencheek.caching.herdcache.memcached.metrics.MetricRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 */
public class BasicCacheRead<V> implements CacheRead<V> {

    private static Logger LOGGER = LoggerFactory.getLogger(CacheRead.class);

    @Override
    public V getFromDistributedCache(ReferencedClient client,
                                     String key,
                                     long timeoutInMillis,
                                     String cacheType,
                                     MetricRecorder metricRecorder) {
        V serialisedObj = null;
        long nanos = System.nanoTime();
        try {
            serialisedObj = (V) client.get(key,timeoutInMillis, TimeUnit.MILLISECONDS);
            if(serialisedObj==null){
                Cache.logCacheMiss(metricRecorder, key, cacheType);
            } else {
                Cache.logCacheHit(metricRecorder, key, cacheType);
            }
        } catch(Throwable e) {
            LOGGER.warn("Exception thrown when communicating with memcached for get({}): {}", key, e.getMessage());
        } finally {
            metricRecorder.incrementCounter(cacheType);
            metricRecorder.setDuration(cacheType,System.nanoTime()-nanos);
        }

        return serialisedObj;
    }
}
