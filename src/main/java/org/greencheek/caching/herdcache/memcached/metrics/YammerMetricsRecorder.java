package org.greencheek.caching.herdcache.memcached.metrics;

import com.codahale.metrics.MetricRegistry;

import java.util.concurrent.TimeUnit;

/**
 * uses https://dropwizard.github.io/metrics to record metrics
 */
public class YammerMetricsRecorder implements MetricRecorder {
    private final MetricRegistry registry;
    private final String prefix;
    private final boolean hasPrefix;

    public YammerMetricsRecorder(MetricRegistry registry) {
        this(registry,null);
    }

    public YammerMetricsRecorder(MetricRegistry registry, String prefix) {
        this.registry = registry;
        this.prefix = prefix;
        if(prefix==null || prefix.trim().length()==0) {
            hasPrefix = false;
        } else {
            hasPrefix = true;
        }
    }

    private String getMetricName(String metricName) {
        return hasPrefix ? prefix + metricName : metricName;
    }


    @Override
    public void cacheHit(String metricName) {
        metricName = getMetricName(metricName);
        registry.counter(metricName+"_hitcount").inc();
        registry.meter(metricName+"_hitrate").mark();
    }

    @Override
    public void cacheMiss(String metricName) {
        metricName = getMetricName(metricName);
        registry.counter(metricName+"_misscount").inc();
        registry.meter(metricName+"_missrate").mark();
    }

    @Override
    public void incrementCounter(String metricName) {
        metricName = getMetricName(metricName);
        registry.counter(metricName+"_count").inc();
    }

    @Override
    public void setDuration(String metricName, long nanos) {
        metricName = getMetricName(metricName);
        registry.timer(metricName+"_timer").update(nanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void updateHistogram(String metricName, long update) {
        metricName = getMetricName(metricName);
        registry.histogram(metricName).update(update);
    }
}
