package org.greencheek.caching.herdcache.memcached.metrics;

/**
 * Does nothing.  Does not record any metrics
 */
public class NoOpMetricRecorder implements MetricRecorder {
    @Override
    public void cacheHit(String metricName) {

    }

    @Override
    public void cacheMiss(String metricName) {

    }

    @Override
    public void incrementCounter(String metricName) {

    }

    @Override
    public void setDuration(String metricName, long nanos) {

    }

    @Override
    public void updateHistogram(String metricName,long update) {

    }
}
