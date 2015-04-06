package org.greencheek.caching.herdcache.perf.benchmarks.cachetests.cacheobjects;

import net.spy.memcached.ConnectionFactoryBuilder;
import org.greencheek.caching.herdcache.CacheWithExpiry;
import org.greencheek.caching.herdcache.RequiresShutdown;
import org.greencheek.caching.herdcache.memcached.config.builder.ElastiCacheCacheConfigBuilder;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.time.Duration;

/**
 * Created by dominictootell on 03/04/2015.
 */

@State(Scope.Benchmark)
public class BinaryAsciiOnlyKeysSpyMemcachedCache {
    public CacheWithExpiry<String> cache;

    @Setup
    public void setUp() {
        cache = new org.greencheek.caching.herdcache.memcached.SpyMemcachedCache<String>(new ElastiCacheCacheConfigBuilder()
                .setMemcachedHosts("localhost:11211")
                .setTimeToLive(Duration.ofSeconds(60))
                .setProtocol(ConnectionFactoryBuilder.Protocol.BINARY)
                .buildMemcachedConfig());
    }

    @TearDown
    public void tearDown() {
        ((RequiresShutdown)cache).shutdown();
    }


}
