package org.greencheek.caching.herdcache.perf.benchmarks.cache;

import net.spy.memcached.ConnectionFactoryBuilder;
import org.greencheek.caching.herdcache.CacheWithExpiry;
import org.greencheek.caching.herdcache.RequiresShutdown;
import org.greencheek.caching.herdcache.memcached.config.builder.ElastiCacheCacheConfigBuilder;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.JenkinsHash;
import org.greencheek.caching.herdcache.memcached.spy.extensions.locator.LocatorFactory;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.time.Duration;

/**
 * Created by dominictootell on 03/04/2015.
 */

@State(Scope.Benchmark)
public class CacheObject {
    public CacheWithExpiry<String> cache;

    @Setup
    public void setUp() {
        cache = new org.greencheek.caching.herdcache.memcached.SpyMemcachedCache<String>(new ElastiCacheCacheConfigBuilder()
                .setMemcachedHosts("localhost:11211")
                .setTimeToLive(Duration.ofSeconds(1))
                .setProtocol(ConnectionFactoryBuilder.Protocol.BINARY)
                .setLocatorFactory(LocatorFactory.KETAMA_CEILING_ARRAY)
                .setHashAlgorithm(new JenkinsHash())
                .buildMemcachedConfig());
    }

    @TearDown
    public void tearDown() {
        ((RequiresShutdown)cache).shutdown();
    }


}
