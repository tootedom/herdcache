package org.greencheek.caching.herdcache.memcached.config;

import java.time.Duration;

/**
 * Created by dominictootell on 24/08/2014.
 */
public class ElastiCacheCacheConfig {

    private final MemcachedCacheConfig memcachedCacheConfig;
    private final Duration configPollingTime;
    private final Duration initialConfigPollingDelay;
    private final Duration connectionTimeoutInMillis;
    private final Duration idleReadTimeout;
    private final Duration reconnectDelay;
    private final Duration delayBeforeClientClose;
    private final int numberOfConsecutiveInvalidConfigurationsBeforeReconnect;
    private final boolean updateConfigVersionOnDnsTimeout;

    public ElastiCacheCacheConfig(MemcachedCacheConfig memcachedConf,
                                  Duration configPollingTime,
            Duration initialConfigPollingDelay,
            Duration connectionTimeoutInMillis,
            Duration idleReadTimeout,
            Duration reconnectDelay,
            Duration delayBeforeClientClose,
            int numberOfConsecutiveInvalidConfigurationsBeforeReconnect,
            boolean updateConfigVersionOnDnsTimeout) {
        this.memcachedCacheConfig = memcachedConf;
        this.configPollingTime = configPollingTime;
        this.initialConfigPollingDelay = initialConfigPollingDelay;
        this.connectionTimeoutInMillis = connectionTimeoutInMillis;
        this.idleReadTimeout = idleReadTimeout;
        this.reconnectDelay = reconnectDelay;
        this.delayBeforeClientClose = delayBeforeClientClose;
        this.numberOfConsecutiveInvalidConfigurationsBeforeReconnect = numberOfConsecutiveInvalidConfigurationsBeforeReconnect;
        this.updateConfigVersionOnDnsTimeout = updateConfigVersionOnDnsTimeout;
    }

    public MemcachedCacheConfig getMemcachedCacheConfig() {
        return memcachedCacheConfig;
    }

    public Duration getConfigPollingTime() {
        return configPollingTime;
    }

    public Duration getInitialConfigPollingDelay() {
        return initialConfigPollingDelay;
    }

    public Duration getConnectionTimeoutInMillis() {
        return connectionTimeoutInMillis;
    }

    public Duration getIdleReadTimeout() {
        return idleReadTimeout;
    }

    public Duration getReconnectDelay() {
        return reconnectDelay;
    }

    public Duration getDelayBeforeClientClose() {
        return delayBeforeClientClose;
    }

    public int getNumberOfConsecutiveInvalidConfigurationsBeforeReconnect() {
        return numberOfConsecutiveInvalidConfigurationsBeforeReconnect;
    }

    public boolean isUpdateConfigVersionOnDnsTimeout() {
        return updateConfigVersionOnDnsTimeout;
    }
}
