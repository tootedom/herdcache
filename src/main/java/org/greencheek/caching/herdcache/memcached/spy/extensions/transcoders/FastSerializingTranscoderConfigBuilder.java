package org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders;

import net.spy.memcached.CachedData;
import org.greencheek.caching.herdcache.memcached.metrics.MetricRecorder;
import org.greencheek.caching.herdcache.memcached.metrics.NoOpMetricRecorder;
import org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.compression.Compression;
import org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.compression.SnappyCompression;

/**
 *
 */
public class FastSerializingTranscoderConfigBuilder {
    private MetricRecorder metricRecorder = new NoOpMetricRecorder();
    private int maxContentLengthInBytes = CachedData.MAX_SIZE;
    private int compressionThresholdInBytes = BaseSerializingTranscoder.DEFAULT_COMPRESSION_THRESHOLD;
    private Compression compression = new SnappyCompression();
    private Class[] classesKnownToBeSerialized = null;
    private boolean shareReferences = true;


    public MetricRecorder getMetricRecorder() {
        return metricRecorder;
    }

    public FastSerializingTranscoderConfigBuilder setMetricRecorder(MetricRecorder metricRecorder) {
        this.metricRecorder = metricRecorder;
        return this;
    }


    public int getMaxContentLengthInBytes() {
        return maxContentLengthInBytes;
    }

    public FastSerializingTranscoderConfigBuilder setMaxContentLengthInBytes(int maxContentLengthInBytes) {
        this.maxContentLengthInBytes = maxContentLengthInBytes;
        return this;
    }

    public int getCompressionThresholdInBytes() {
        return compressionThresholdInBytes;
    }

    public FastSerializingTranscoderConfigBuilder setCompressionThresholdInBytes(int compressionThresholdInBytes) {
        this.compressionThresholdInBytes = compressionThresholdInBytes;
        return this;
    }

    public Compression getCompression() {
        return compression;
    }

    public FastSerializingTranscoderConfigBuilder setCompression(Compression compression) {
        this.compression = compression;
        return this;
    }

    public Class[] getClassesKnownToBeSerialized() {
        return classesKnownToBeSerialized;
    }

    public FastSerializingTranscoderConfigBuilder setClassesKnownToBeSerialized(Class[] classesKnownToBeSerialized) {
        this.classesKnownToBeSerialized = classesKnownToBeSerialized;
        return this;
    }

    public boolean isShareReferences() {
        return shareReferences;
    }

    public FastSerializingTranscoderConfigBuilder setShareReferences(boolean shareReferences) {
        this.shareReferences = shareReferences;
        return this;
    }

    public FastSerializingTranscoderConfig build() {
        return new FastSerializingTranscoderConfig(
                getMetricRecorder(),
                getMaxContentLengthInBytes(),
                getCompressionThresholdInBytes(),
                getCompression(),
                getClassesKnownToBeSerialized(),
                isShareReferences());
    }
}
