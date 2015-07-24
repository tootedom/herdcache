package org.greencheek.caching.herdcache.memcached;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.netflix.hystrix.HystrixCommand;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.HashAlgorithm;
import org.greencheek.caching.herdcache.Cache;
import org.greencheek.caching.herdcache.CacheWithExpiry;
import org.greencheek.caching.herdcache.RequiresShutdown;
import org.greencheek.caching.herdcache.RevalidateInBackgroundCapableCache;
import org.greencheek.caching.herdcache.domain.CacheableItemWithCreationDate;
import org.greencheek.caching.herdcache.memcached.config.builder.ElastiCacheCacheConfigBuilder;
import org.greencheek.caching.herdcache.memcached.keyhashing.KeyHashingType;
import org.greencheek.caching.herdcache.memcached.metrics.YammerMetricsRecorder;
import org.greencheek.caching.herdcache.memcached.spy.extensions.FastSerializingTranscoder;
import org.greencheek.caching.herdcache.memcached.spy.extensions.SerializingTranscoder;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.AsciiXXHashAlogrithm;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.JenkinsHash;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.XXHashAlogrithm;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonFactory;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonWrapper;
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
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by dominictootell on 25/08/2014.
 */
public class TestStaleWhileRevalidateMemcachedCaching {

    private MemcachedDaemonWrapper memcached;
    private ListeningExecutorService executorService;
    private RevalidateInBackgroundCapableCache<CacheableItemWithCreationDate<String>> cache;

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
    }




    @Test
    public void testBackgroundRevalidation() {

        String BACKGROUND_VALUE = "background";
        String STALE_VALUE = "value1";
        String STALE_VALUE2 = "value2";

        cache = new SpyMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(60))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        ListenableFuture<CacheableItemWithCreationDate<String>> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return new CacheableItemWithCreationDate(STALE_VALUE);
        }, Duration.ofMillis(0), executorService);

        ListenableFuture<CacheableItemWithCreationDate<String>> val2 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return new CacheableItemWithCreationDate(STALE_VALUE2);
        },Duration.ofMillis(0), executorService);


        assertEquals("Value for Key1 should be " + STALE_VALUE, STALE_VALUE, cache.awaitForFutureOrElse(val, null).getCachedItem());
        assertEquals("Value for Key1 should be " + STALE_VALUE, STALE_VALUE, cache.awaitForFutureOrElse(val2, null).getCachedItem());

        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());

        ListenableFuture<CacheableItemWithCreationDate<String>> val3 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {

            }
            return new CacheableItemWithCreationDate(BACKGROUND_VALUE);
        }, Duration.ofMillis(0), executorService, (CacheableItemWithCreationDate<String> v) -> true,
                (CacheableItemWithCreationDate<String> v) -> {
                    return !v.hasExpired(100);
                },false);

        assertEquals("Value for Key1 should be "+ STALE_VALUE, STALE_VALUE, cache.awaitForFutureOrElse(val3, null).getCachedItem());

        ListenableFuture<CacheableItemWithCreationDate<String>> val4 = cache.apply("Key1", () -> {
            return new CacheableItemWithCreationDate(STALE_VALUE2);
        },Duration.ofMillis(0), executorService);

        assertEquals("Value for Key1 should be " + STALE_VALUE, STALE_VALUE, cache.awaitForFutureOrElse(val4, null).getCachedItem());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ListenableFuture<CacheableItemWithCreationDate<String>> val5 = cache.apply("Key1", () -> {
            return new CacheableItemWithCreationDate(STALE_VALUE2);
        },Duration.ofMillis(0), executorService);

        assertEquals("Value for Key1 should be " + BACKGROUND_VALUE, BACKGROUND_VALUE, cache.awaitForFutureOrElse(val5, null).getCachedItem());


    }

}
