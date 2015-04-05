package org.greencheek.caching.herdcache.memcached;

import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.HashAlgorithm;
import org.greencheek.caching.herdcache.memcached.config.MemcachedClientType;
import org.greencheek.caching.herdcache.memcached.config.builder.ElastiCacheCacheConfigBuilder;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.client.ClientClusterUpdateObserver;

import java.time.Duration;

/**
 * Created by dominictootell on 05/04/2015.
 */
public class TestFolsomMultipleHostsWithNonCachingConfigElastiCacheMemcachedCaching extends TestMultipleHostsWithNonCachingConfigElastiCacheMemcachedCaching {
    ElastiCacheMemcachedCache<String> createCache(int configServerPort,HashAlgorithm algo,ClientClusterUpdateObserver observer) {
        return new ElastiCacheMemcachedCache<String>(
                new ElastiCacheCacheConfigBuilder()
                        .setElastiCacheConfigHosts("localhost:" + configServerPort)
                        .setConfigPollingTime(Duration.ofSeconds(9))
                        .setInitialConfigPollingDelay(Duration.ofSeconds(0))
                        .setTimeToLive(Duration.ofSeconds(2))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setSetWaitDuration(Duration.ofSeconds(10))
                        .setHashAlgorithm(algo)
                        .setDelayBeforeClientClose(Duration.ofSeconds(1))
                        .setDnsConnectionTimeout(Duration.ofSeconds(2))
                        .setUseStaleCache(true)
                        .setStaleCacheAdditionalTimeToLive(Duration.ofSeconds(4))
                        .setRemoveFutureFromInternalCacheBeforeSettingValue(true)
                        .addElastiCacheClientClusterUpdateObserver(observer)
                        .setMemcachedClientType(MemcachedClientType.FOLSOM)
                        .buildElastiCacheMemcachedConfig()
        );
    }
}
