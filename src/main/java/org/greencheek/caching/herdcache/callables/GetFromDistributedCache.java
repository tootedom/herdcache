package org.greencheek.caching.herdcache.callables;

import java.util.concurrent.Callable;

import org.greencheek.caching.herdcache.Cache;
import org.greencheek.caching.herdcache.memcached.factory.ReferencedClient;
import org.greencheek.caching.herdcache.memcached.metrics.MetricRecorder;
import org.greencheek.caching.herdcache.memcached.operations.CacheRead;
import org.greencheek.caching.herdcache.memcached.util.CacheMetricStrings;

/**
 *
 */
public class GetFromDistributedCache<V> implements Callable<V> {

    private final String keyString;
    private final MetricRecorder metricRecorder;
    private final String metricToIncrement;
    private final ReferencedClient client;
    private final long getRequestTimeoutInMillis;
    private final CacheRead<V> cacheReaderImpl;

    public GetFromDistributedCache(String key,
                                   MetricRecorder metricRecorder,
                                   long getTimeoutInMillis,
                                   ReferencedClient client,
                                   String metricToIncrement,
                                   CacheRead<V> cacheReader) {
        this.keyString = key;
        this.metricRecorder = metricRecorder;
        this.getRequestTimeoutInMillis = getTimeoutInMillis;
        this.client = client;
        this.metricToIncrement = metricToIncrement;
        this.cacheReaderImpl = cacheReader;
    }

    @Override
    public V call() throws Exception {
        boolean ok;
        V result = null;
        try {
            result = cacheReaderImpl.getFromDistributedCache(client,
                    keyString,
                    getRequestTimeoutInMillis
                    ,metricToIncrement,
                    metricRecorder);
            ok = true;
        } catch (Throwable throwable) {
            ok = false;
        }

        if(ok && result!=null) {
            Cache.logCacheHit(metricRecorder,keyString, CacheMetricStrings.CACHE_TYPE_ALL);
        } else {
            Cache.logCacheMiss(metricRecorder, keyString, CacheMetricStrings.CACHE_TYPE_ALL);
        }
        return result;
    }
}
