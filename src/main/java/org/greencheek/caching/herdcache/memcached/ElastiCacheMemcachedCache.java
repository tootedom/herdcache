package org.greencheek.caching.herdcache.memcached;

import org.greencheek.caching.herdcache.memcached.config.ElastiCacheCacheConfig;
import org.greencheek.caching.herdcache.memcached.config.Host;
import org.greencheek.caching.herdcache.memcached.config.MemcachedCacheConfig;
import org.greencheek.caching.herdcache.memcached.config.hostparsing.CommaSeparatedHostAndPortStringParser;
import org.greencheek.caching.herdcache.memcached.config.hostparsing.HostStringParser;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.client.ElastiCacheConfigHostsParser;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.client.ElastiCacheServerConnectionDetails;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.client.LocalhostElastiCacheServerConnectionDetails;
import org.greencheek.caching.herdcache.memcached.factory.ElastiCacheClientFactory;
import org.greencheek.caching.herdcache.memcached.factory.MemcachedClientFactory;
import org.greencheek.caching.herdcache.memcached.factory.ReferencedClientFactory;

import java.io.Serializable;
import java.util.List;

/**
 *
 */
public class ElastiCacheMemcachedCache<V extends Serializable> extends BaseMemcachedCache<V> {


    public ElastiCacheMemcachedCache(ElastiCacheCacheConfig config) {
       this(config.getMemcachedCacheConfig(),config);
    }

    private ElastiCacheMemcachedCache(MemcachedCacheConfig mConfig,ElastiCacheCacheConfig config) {
        super(new ElastiCacheClientFactory(
                createReferenceClientFactory(config),
                ElastiCacheConfigHostsParser.parseElastiCacheConfigHosts(config.getElastiCacheConfigHosts()),
                config.getConfigPollingTime(),
                config.getInitialConfigPollingDelay(),
                config.getIdleReadTimeout(),
                config.getReconnectDelay(),
                config.getDelayBeforeClientClose(),
                mConfig.getHostResolver(),
                mConfig.getDnsConnectionTimeout(),
                config.isUpdateConfigVersionOnDnsTimeout(),
                config.getNumberOfConsecutiveInvalidConfigurationsBeforeReconnect(),
                config.getConnectionTimeoutInMillis(),
                config.getClusterUpdatedObservers(),
                config.getConfigUrlUpdater(),
                config.isUpdateConfigOnlyOnVersionChange()
        ),mConfig);
    }


    @Override
    public MemcachedClientFactory buildClientFactory(Object cfg) {
        ElastiCacheCacheConfig config = (ElastiCacheCacheConfig) cfg;
        MemcachedCacheConfig mConfig = config.getMemcachedCacheConfig();
        return new ElastiCacheClientFactory(
                createReferenceClientFactory(config),
                ElastiCacheConfigHostsParser.parseElastiCacheConfigHosts(config.getElastiCacheConfigHosts()),
                config.getConfigPollingTime(),
                config.getInitialConfigPollingDelay(),
                config.getIdleReadTimeout(),
                config.getReconnectDelay(),
                config.getDelayBeforeClientClose(),
                mConfig.getHostResolver(),
                mConfig.getDnsConnectionTimeout(),
                config.isUpdateConfigVersionOnDnsTimeout(),
                config.getNumberOfConsecutiveInvalidConfigurationsBeforeReconnect(),
                config.getConnectionTimeoutInMillis(),
                config.getClusterUpdatedObservers(),
                config.getConfigUrlUpdater(),
                config.isUpdateConfigOnlyOnVersionChange());

    }



}
