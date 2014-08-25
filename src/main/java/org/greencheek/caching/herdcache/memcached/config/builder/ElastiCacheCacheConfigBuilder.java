package org.greencheek.caching.herdcache.memcached.config.builder;

import org.greencheek.caching.herdcache.memcached.config.ElastiCacheCacheConfig;
import org.greencheek.caching.herdcache.memcached.config.MemcachedCacheConfig;

import java.time.Duration;

/**
 * Created by dominictootell on 24/08/2014.
 */
public class ElastiCacheCacheConfigBuilder extends MemcachedCacheConfigBuilder {

    private Duration configPollingTime  = Duration.ofSeconds(60);
    private Duration initialConfigPollingDelay = Duration.ZERO;
    private Duration connectionTimeoutInMillis = Duration.ofMillis(3000);
    private Duration idleReadTimeout = Duration.ofSeconds(125);
    private Duration reconnectDelay = Duration.ofSeconds(5);
    private Duration delayBeforeClientClose = Duration.ofSeconds(10);
    private int numberOfConsecutiveInvalidConfigurationsBeforeReconnect = 3;
    private boolean updateConfigVersionOnDnsTimeout = true;

    public ElastiCacheCacheConfigBuilder setConfigPollingTime(Duration configPollingTime) {
        this.configPollingTime = configPollingTime;
        return this;
    }

    public ElastiCacheCacheConfigBuilder setInitialConfigPollingDelay(Duration initialConfigPollingDelay) {
        this.initialConfigPollingDelay = initialConfigPollingDelay;
        return this;
    }

    public ElastiCacheCacheConfigBuilder setConnectionTimeoutInMillis(Duration connectionTimeoutInMillis) {
        this.connectionTimeoutInMillis = connectionTimeoutInMillis;
        return this;
    }

    public ElastiCacheCacheConfigBuilder setIdleReadTimeout(Duration idleReadTimeout) {
        this.idleReadTimeout = idleReadTimeout;
        return this;
    }

    public ElastiCacheCacheConfigBuilder setReconnectDelay(Duration reconnectDelay) {
        this.reconnectDelay = reconnectDelay;
        return this;
    }

    public ElastiCacheCacheConfigBuilder setDelayBeforeClientClose(Duration delayBeforeClientClose) {
        this.delayBeforeClientClose = delayBeforeClientClose;
        return this;
    }

    public ElastiCacheCacheConfigBuilder setNumberOfConsecutiveInvalidConfigurationsBeforeReconnect(int numberOfConsecutiveInvalidConfigurationsBeforeReconnect) {
        this.numberOfConsecutiveInvalidConfigurationsBeforeReconnect = numberOfConsecutiveInvalidConfigurationsBeforeReconnect;
        return this;
    }

    public ElastiCacheCacheConfigBuilder setUpdateConfigVersionOnDnsTimeout(boolean updateConfigVersionOnDnsTimeout) {
        this.updateConfigVersionOnDnsTimeout = updateConfigVersionOnDnsTimeout;
        return this;
    }


    @Override
    public ElastiCacheCacheConfig buildElastiCacheMemcachedConfig() {
//        return new ElastiCacheCacheConfig(buildMemcachedConfig());
        return null;
    }

    @Override
    public MemcachedCacheConfig buildMemcachedConfig() {
        return super.buildMemcachedConfig();
    }
}
