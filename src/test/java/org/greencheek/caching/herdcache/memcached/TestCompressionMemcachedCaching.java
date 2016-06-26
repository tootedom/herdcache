package org.greencheek.caching.herdcache.memcached;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.netflix.hystrix.HystrixCommand;
import com.thimbleware.jmemcached.Key;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.HashAlgorithm;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.greencheek.caching.herdcache.Cache;
import org.greencheek.caching.herdcache.CacheWithExpiry;
import org.greencheek.caching.herdcache.RequiresShutdown;
import org.greencheek.caching.herdcache.exceptions.UnableToScheduleCacheGetExecutionException;
import org.greencheek.caching.herdcache.exceptions.UnableToSubmitSupplierForExecutionException;
import org.greencheek.caching.herdcache.memcached.config.builder.ElastiCacheCacheConfigBuilder;
import org.greencheek.caching.herdcache.memcached.keyhashing.KeyHashingType;
import org.greencheek.caching.herdcache.memcached.metrics.YammerMetricsRecorder;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.AsciiXXHashAlogrithm;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.JenkinsHash;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.XXHashAlogrithm;
import org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.FastSerializingTranscoder;
import org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.SerializingTranscoder;
import org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.compression.CompressionAlgorithm;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonFactory;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonWrapper;
import org.greencheek.caching.herdcache.memcached.util.TestCacheValues;
import org.greencheek.caching.herdcache.util.BackEndRequest;
import org.greencheek.caching.herdcache.util.Content;
import org.greencheek.caching.herdcache.util.RestClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by dominictootell on 25/08/2014.
 */
public class TestCompressionMemcachedCaching {

    private MemcachedDaemonWrapper memcached;
    private ListeningExecutorService executorService;
    private CacheWithExpiry cache;

    @Before
    public void setUp() {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

        memcached = MemcachedDaemonFactory.createMemcachedDaemon(false);

        if (memcached.getDaemon() == null) {
            throw new RuntimeException("Unable to start local memcached");
        }


    }

    @After
    public void tearDown() {
        if (memcached != null) {
            memcached.getDaemon().stop();
        }

        if (cache != null && cache instanceof RequiresShutdown) {
            ((RequiresShutdown) cache).shutdown();
        }

        executorService.shutdownNow();

    }

    @Test
    public void testNoCompressionHashingMemcachedCache() throws ExecutionException, InterruptedException {
        String key = "Key1";

        cache = new SpyMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(60))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setKeyHashType(KeyHashingType.NONE)
                        .setCompressionAlgorithm(CompressionAlgorithm.NONE)
                        .buildMemcachedConfig()
        );

        ListenableFuture<String> val = cache.apply(key, () -> TestCacheValues.LARGE_CACHE_VALUE, executorService);

        assertEquals("Value should be key1", TestCacheValues.LARGE_CACHE_VALUE, cache.awaitForFutureOrElse(val, null));
        assertEquals("Value should be key1", TestCacheValues.LARGE_CACHE_VALUE, cache.get(key).get());

        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
        assertEquals("Should have same byte length",TestCacheValues.LARGE_CACHE_VALUE_BYTES.length, memcached.getDaemon().getCache().getCurrentBytes());
    }

    @Test
    public void testLZ4CompressionHashingMemcachedCache() throws ExecutionException, InterruptedException {
        String key = "Key1";

        cache = new SpyMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(60))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setKeyHashType(KeyHashingType.NONE)
                        .setCompressionAlgorithm(CompressionAlgorithm.LZ4_NATIVE)
                        .buildMemcachedConfig()
        );

        ListenableFuture<String> val = cache.apply(key, () -> TestCacheValues.LARGE_CACHE_VALUE, executorService);

        assertEquals("Value should be key1", TestCacheValues.LARGE_CACHE_VALUE, cache.awaitForFutureOrElse(val, null));
        assertEquals("Value should be key1", TestCacheValues.LARGE_CACHE_VALUE, cache.get(key).get());

        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
        assertNotEquals("Should have same byte length", TestCacheValues.LARGE_CACHE_VALUE_BYTES.length, memcached.getDaemon().getCache().getCurrentBytes());
    }


    @Test
    public void testSnappyCompressionHashingMemcachedCache() throws ExecutionException, InterruptedException {
        long snappyLength = 0;

        String key = "Key1";
        try {
            cache = new SpyMemcachedCache<>(
                    new ElastiCacheCacheConfigBuilder()
                            .setMemcachedHosts("localhost:" + memcached.getPort())
                            .setTimeToLive(Duration.ofSeconds(60))
                            .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                            .setWaitForMemcachedSet(true)
                            .setKeyHashType(KeyHashingType.NONE)
                            .setCompressionAlgorithm(CompressionAlgorithm.SNAPPY)
                            .buildMemcachedConfig()
            );

            ListenableFuture<String> val = cache.apply(key, () -> TestCacheValues.LARGE_CACHE_VALUE, executorService);

            assertEquals("Value should be key1", TestCacheValues.LARGE_CACHE_VALUE, cache.awaitForFutureOrElse(val, null));
            assertEquals("Value should be key1", TestCacheValues.LARGE_CACHE_VALUE, cache.get(key).get());

            assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
            assertNotEquals("Should have same byte length", TestCacheValues.LARGE_CACHE_VALUE_BYTES.length, memcached.getDaemon().getCache().getCurrentBytes());
        } finally {
            if (cache != null && cache instanceof RequiresShutdown) {
                ((RequiresShutdown) cache).shutdown();
            }
        }

        memcached.getDaemon().getCache().flush_all();

        cache = new SpyMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(60))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setKeyHashType(KeyHashingType.NONE)
                        .setCompressionAlgorithm(CompressionAlgorithm.LZ4_NATIVE)
                        .buildMemcachedConfig()
        );


        ListenableFuture<String> val = cache.apply(key, () -> TestCacheValues.LARGE_CACHE_VALUE, executorService);

        assertEquals("Value should be key1", TestCacheValues.LARGE_CACHE_VALUE, cache.awaitForFutureOrElse(val, null));

        assertNotEquals(snappyLength, memcached.getDaemon().getCache().getCurrentBytes());
    }


    @Test
    public void testSnappyCompressionToLZ4MemcachedCache() throws ExecutionException, InterruptedException {
        long snappyLength = 0;

        String key = "Key1";
        try {
            cache = new SpyMemcachedCache<>(
                    new ElastiCacheCacheConfigBuilder()
                            .setMemcachedHosts("localhost:" + memcached.getPort())
                            .setTimeToLive(Duration.ofSeconds(60))
                            .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                            .setWaitForMemcachedSet(true)
                            .setKeyHashType(KeyHashingType.NONE)
                            .setCompressionAlgorithm(CompressionAlgorithm.SNAPPY)
                            .buildMemcachedConfig()
            );

            ListenableFuture<String> val = cache.apply(key, () -> TestCacheValues.LARGE_CACHE_VALUE, executorService);

            assertEquals("Value should be key1", TestCacheValues.LARGE_CACHE_VALUE, cache.awaitForFutureOrElse(val, null));
            assertEquals("Value should be key1", TestCacheValues.LARGE_CACHE_VALUE, cache.get(key).get());

            assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
            assertNotEquals("Should have same byte length", TestCacheValues.LARGE_CACHE_VALUE_BYTES.length, memcached.getDaemon().getCache().getCurrentBytes());
        } finally {
            if (cache != null && cache instanceof RequiresShutdown) {
                ((RequiresShutdown) cache).shutdown();
            }
        }

        try {
            cache = new SpyMemcachedCache<>(
                    new ElastiCacheCacheConfigBuilder()
                            .setMemcachedHosts("localhost:" + memcached.getPort())
                            .setTimeToLive(Duration.ofSeconds(60))
                            .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                            .setWaitForMemcachedSet(true)
                            .setKeyHashType(KeyHashingType.NONE)
                            .setCompressionAlgorithm(CompressionAlgorithm.LZ4_NATIVE)
                            .buildMemcachedConfig()
            );


            ListenableFuture<String> val = cache.apply(key, () -> TestCacheValues.LARGE_CACHE_VALUE2, executorService);

            assertEquals("Value should 'Override' for key1", TestCacheValues.LARGE_CACHE_VALUE2, cache.awaitForFutureOrElse(val, null));

            assertEquals("Value should 'Override' for key1", TestCacheValues.LARGE_CACHE_VALUE2, cache.get(key).get());
        } finally {
            if (cache != null && cache instanceof RequiresShutdown) {
                ((RequiresShutdown) cache).shutdown();
            }
        }

        try {
            cache = new SpyMemcachedCache<>(
                    new ElastiCacheCacheConfigBuilder()
                            .setMemcachedHosts("localhost:" + memcached.getPort())
                            .setTimeToLive(Duration.ofSeconds(60))
                            .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                            .setWaitForMemcachedSet(true)
                            .setKeyHashType(KeyHashingType.NONE)
                            .setCompressionAlgorithm(CompressionAlgorithm.SNAPPY)
                            .buildMemcachedConfig()
            );


            ListenableFuture<String> val = cache.apply(key, () -> TestCacheValues.LARGE_CACHE_VALUE, executorService);

            assertEquals("Value should 'Override' for key1", TestCacheValues.LARGE_CACHE_VALUE, cache.awaitForFutureOrElse(val, null));

            assertEquals("Value should 'Override' for key1", TestCacheValues.LARGE_CACHE_VALUE, cache.get(key).get());
        } finally {
            if (cache != null && cache instanceof RequiresShutdown) {
                ((RequiresShutdown) cache).shutdown();
            }
        }

    }
}
