package org.greencheek.caching.herdcache.memcached.metrics;

/**
 *
 */
public interface MetricRecorder {
    public void cacheHit(String metricName);
    public void cacheMiss(String metricName);
    public void incrementCounter(String metricName);
    public void setDuration(String metricName,long nanos);
}
