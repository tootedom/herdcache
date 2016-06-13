package org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders;

import org.greencheek.caching.herdcache.memcached.metrics.MetricRecorder;
import org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.compression.Compression;

/**
 *
 */
public class FastSerializingTranscoderConfig {
    private final MetricRecorder metricRecorder;
    private final int maxContentLengthInBytes;
    private final int compressionThresholdInBytes;
    private final Compression compression;
    private final Class[] classesKnownToBeSerialized;
    private final boolean shareReferences;

    public FastSerializingTranscoderConfig(MetricRecorder metricRecorder,
                                           int maxContentLengthInBytes,
                                           int compressionThresholdInBytes,
                                           Compression compression,
                                           Class[] classesKnownToBeSerialized,
                                           boolean shareReferences) {
        this.metricRecorder = metricRecorder;
        this.maxContentLengthInBytes = maxContentLengthInBytes;
        this.compressionThresholdInBytes = compressionThresholdInBytes;
        this.compression = compression;
        this.classesKnownToBeSerialized = classesKnownToBeSerialized;
        this.shareReferences = shareReferences;
    }


    public MetricRecorder getMetricRecorder() {
        return metricRecorder;
    }

    public int getMaxContentLengthInBytes() {
        return maxContentLengthInBytes;
    }

    public int getCompressionThresholdInBytes() {
        return compressionThresholdInBytes;
    }

    public Compression getCompression() {
        return compression;
    }

    public Class[] getClassesKnownToBeSerialized() {
        return classesKnownToBeSerialized;
    }

    public boolean isShareReferences() {
        return shareReferences;
    }


}
