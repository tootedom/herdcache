package org.greencheek.caching.herdcache.memcached;

import net.spy.memcached.ConnectionFactoryBuilder;
import org.greencheek.caching.herdcache.CacheWithExpiry;
import org.greencheek.caching.herdcache.RequiresShutdown;
import org.greencheek.caching.herdcache.memcached.config.MemcachedClientType;
import org.greencheek.caching.herdcache.memcached.config.builder.ElastiCacheCacheConfigBuilder;
import org.greencheek.caching.herdcache.memcached.metrics.YammerMetricsRecorder;

import java.time.Duration;

/**
 * Created by dominictootell on 05/04/2015.
 */
public class TestFolsomStaleMemcachedCaching extends TestStaleMemcachedCaching {
    CacheWithExpiry<String> createCache(int port) {
        if(cache!=null) {
            ((RequiresShutdown)cache).shutdown();
        }

        cache = new FolsomMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + port)
                        .setTimeToLive(Duration.ofSeconds(1))
                        .setUseStaleCache(true)
                        .setStaleCacheAdditionalTimeToLive(Duration.ofSeconds(4))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setStaleCachePrefix("staleprefix")
                        .setWaitForMemcachedSet(true)
                        .setMetricsRecorder(new YammerMetricsRecorder(registry))
                        .setMemcachedClientType(MemcachedClientType.FOLSOM)
                        .buildElastiCacheMemcachedConfig()
        );

        return cache;
    }

}
