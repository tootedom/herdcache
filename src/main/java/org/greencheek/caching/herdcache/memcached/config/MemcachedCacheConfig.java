package org.greencheek.caching.herdcache.memcached.config;

import net.spy.memcached.*;
import net.spy.memcached.transcoders.Transcoder;
import org.greencheek.caching.herdcache.memcached.config.hostparsing.HostStringParser;
import org.greencheek.caching.herdcache.memcached.dns.lookup.HostResolver;
import org.greencheek.caching.herdcache.memcached.keyhashing.KeyHashingType;
import org.greencheek.caching.herdcache.memcached.metrics.MetricRecorder;
import org.greencheek.caching.herdcache.memcached.spy.extensions.locator.LocatorFactory;

import java.time.Duration;
import java.util.Optional;

/**
 *
 */
public class MemcachedCacheConfig {

    private final Duration timeToLive;
    private final int maxCapacity;
    private final String memcachedHosts;
    private final FailureMode failureMode;
    private final HashAlgorithm hashAlgorithm;
    private final Transcoder<Object> serializingTranscoder;
    private final ConnectionFactoryBuilder.Protocol protocol;
    private final int readBufferSize;
    private final Duration memcachedGetTimeout;
    private final Duration dnsConnectionTimeout;
    private final boolean waitForMemcachedSet;
    private final Duration setWaitDuration;
    private final KeyHashingType keyHashType;
    private final String keyPrefix;
    private final boolean hasKeyPrefix;
    private final boolean asciiOnlyKeys;
    private final HostStringParser hostStringParser;
    private final HostResolver hostResolver;
    private final boolean useStaleCache;
    private final Duration staleCacheAdditionalTimeToLive;
    private final String staleCachePrefix;
    private final int staleMaxCapacity;
    private final Duration staleCacheMemachedGetTimeout;
    private final boolean removeFutureFromInternalCacheBeforeSettingValue;
    private final boolean hashKeyPrefix;
    private final Duration waitForRemove;
    private final MetricRecorder metricsRecorder;
    private final LocatorFactory locatorFactory;
    private final boolean herdProtectionEnabled;


    public MemcachedCacheConfig(Duration timeToLive,
                                int maxCapacity,
                                String hosts,
                                FailureMode failureMode,
                                HashAlgorithm hashAlgorithm,
                                Transcoder<Object> serializingTranscoder,
                                ConnectionFactoryBuilder.Protocol protocol,
                                int readBufferSize,
                                Duration memcachedGetTimeout,
                                Duration dnsConnectionTimeout,
                                boolean waitForMemcachedSet,
                                Duration setWaitDuration,
                                KeyHashingType keyHashType,
                                Optional<String> keyPrefix,
                                boolean asciiOnlyKeys,
                                HostStringParser hostStringParser,
                                HostResolver hostResolver,
                                boolean useStaleCache,
                                Duration staleCacheAdditionalTimeToLive,
                                String staleCachePrefix,
                                int staleMaxCapacity,
                                Duration staleCacheMemachedGetTimeout,
                                boolean removeFutureFromInternalCacheBeforeSettingValue,
                                boolean hashKeyPrefix,
                                Duration waitForRemove,
                                MetricRecorder metricsRecorder,
                                LocatorFactory locatorFactory,
                                boolean herdProtectionEnabled) {
        this.timeToLive =  timeToLive;
        this.maxCapacity = maxCapacity;
        this.memcachedHosts = hosts;
        this.failureMode = failureMode;
        this.hashAlgorithm = hashAlgorithm;
        this.serializingTranscoder = serializingTranscoder;
        this.protocol = protocol;
        this.readBufferSize = readBufferSize;
        this.memcachedGetTimeout  = memcachedGetTimeout;
        this.dnsConnectionTimeout = dnsConnectionTimeout;
        this.waitForMemcachedSet  = waitForMemcachedSet;
        this.setWaitDuration = setWaitDuration;
        this.keyHashType = keyHashType;
        this.hasKeyPrefix = keyPrefix.isPresent();
        this.keyPrefix = keyPrefix.orElse("");
        this.asciiOnlyKeys = asciiOnlyKeys;
        this.hostStringParser = hostStringParser;
        this.hostResolver = hostResolver;
        this.useStaleCache = useStaleCache;
        this.staleCacheAdditionalTimeToLive = staleCacheAdditionalTimeToLive;
        this.staleCachePrefix = staleCachePrefix;
        this.staleMaxCapacity = staleMaxCapacity;
        this.staleCacheMemachedGetTimeout = staleCacheMemachedGetTimeout;
        this.removeFutureFromInternalCacheBeforeSettingValue = removeFutureFromInternalCacheBeforeSettingValue;
        this.hashKeyPrefix = hashKeyPrefix;
        this.waitForRemove = waitForRemove;
        this.metricsRecorder = metricsRecorder;
        this.locatorFactory = locatorFactory;
        this.herdProtectionEnabled = herdProtectionEnabled;
    }

    public Duration getTimeToLive() {
        return timeToLive;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public String getMemcachedHosts() {
        return memcachedHosts;
    }

    public FailureMode getFailureMode() {
        return failureMode;
    }

    public HashAlgorithm getHashAlgorithm() {
        return hashAlgorithm;
    }

    public Transcoder<Object> getSerializingTranscoder() {
        return serializingTranscoder;
    }

    public ConnectionFactoryBuilder.Protocol getProtocol() {
        return protocol;
    }

    public int getReadBufferSize() {
        return readBufferSize;
    }

    public Duration getMemcachedGetTimeout() {
        return memcachedGetTimeout;
    }

    public Duration getDnsConnectionTimeout() {
        return dnsConnectionTimeout;
    }

    public boolean isWaitForMemcachedSet() {
        return waitForMemcachedSet;
    }

    public Duration getSetWaitDuration() {
        return setWaitDuration;
    }

    public KeyHashingType getKeyHashType() {
        return keyHashType;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public boolean isAsciiOnlyKeys() {
        return asciiOnlyKeys;
    }

    public HostStringParser getHostStringParser() {
        return hostStringParser;
    }

    public HostResolver getHostResolver() {
        return hostResolver;
    }

    public boolean isUseStaleCache() {
        return useStaleCache;
    }

    public Duration getStaleCacheAdditionalTimeToLive() {
        return staleCacheAdditionalTimeToLive;
    }

    public String getStaleCachePrefix() {
        return staleCachePrefix;
    }

    public int getStaleMaxCapacity() {
        return staleMaxCapacity;
    }

    public Duration getStaleCacheMemachedGetTimeout() {
        return staleCacheMemachedGetTimeout;
    }

    public boolean hasKeyPrefix() {
        return hasKeyPrefix;
    }

    public boolean isRemoveFutureFromInternalCacheBeforeSettingValue() {
        return removeFutureFromInternalCacheBeforeSettingValue;
    }

    public boolean isHashKeyPrefix() {
        return hashKeyPrefix;
    }

    public String toString() {
        StringBuilder b = new StringBuilder(256);
        b.append("maxCapacity=").append(getMaxCapacity());
        b.append("memcachedHosts=").append(getMemcachedHosts());
        b.append("isWaitForMemcachedSet=").append(isWaitForMemcachedSet());
        b.append("waitForSetDuration=").append(getSetWaitDuration());
        b.append("timeToLive=").append(getTimeToLive());
        return b.toString();

    }


    public Duration getWaitForRemove() {
        return waitForRemove;
    }

    public MetricRecorder getMetricsRecorder() {
        return metricsRecorder;
    }

    public LocatorFactory getLocatorFactory() {
        return locatorFactory;
    }

    /**
     * Should the internal future caches be enabled (herd protection for backends)
     * @return
     */
    public boolean isHerdProtectionEnabled() {
        return herdProtectionEnabled;
    }
}
