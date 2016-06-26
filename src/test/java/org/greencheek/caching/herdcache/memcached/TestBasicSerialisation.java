package org.greencheek.caching.herdcache.memcached;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.FailureMode;
import org.greencheek.caching.herdcache.CacheWithExpiry;
import org.greencheek.caching.herdcache.IsCachedValueUsable;
import org.greencheek.caching.herdcache.IsSupplierValueCachable;
import org.greencheek.caching.herdcache.RequiresShutdown;
import org.greencheek.caching.herdcache.memcached.config.builder.ElastiCacheCacheConfigBuilder;
import org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.BaseSerializingTranscoder;
import org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.SerializingTranscoder;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonFactory;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonWrapper;
import org.greencheek.caching.herdcache.memcached.util.TestCacheValues;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

/**
 * Created by dominictootell on 25/08/2014.
 */
public class TestBasicSerialisation {


    private MemcachedDaemonWrapper memcached;
    private ListeningExecutorService executorService;
    private CacheWithExpiry cache;

    @Before
    public void setUp() {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

        memcached = MemcachedDaemonFactory.createMemcachedDaemon(false);

        if(memcached.getDaemon()==null) {
            throw new RuntimeException("Unable to start local memcached");
        }


    }

    @After
    public void tearDown() {
        if(memcached!=null) {
            memcached.getDaemon().stop();
        }

        if(cache!=null && cache instanceof RequiresShutdown) {
            ((RequiresShutdown) cache).shutdown();
        }

        executorService.shutdownNow();
    }

    @Test
    public void testBasicSerializingTranscoder() {

        cache = new SpyMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(2))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setSerializingTranscoder(new SerializingTranscoder())
                        .buildMemcachedConfig()
        );

        ListenableFuture<String> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return TestCacheValues.LARGE_CACHE_VALUE;
        }, executorService, IsSupplierValueCachable.GENERATED_VALUE_IS_ALWAYS_CACHABLE, IsCachedValueUsable.CACHED_VALUE_IS_ALWAYS_USABLE);

        ListenableFuture<String> val2 = cache.get("Key1", executorService);
        ListenableFuture<String> val3 = cache.get("Key1", executorService);
        ListenableFuture<String> val4 = cache.get("Key1", executorService);


        assertEquals("Value should be TestCacheValues.LARGE_CACHE_VALUE",TestCacheValues.LARGE_CACHE_VALUE,cache.awaitForFutureOrElse(val, null));
        assertEquals("Value should be TestCacheValues.LARGE_CACHE_VALUE",TestCacheValues.LARGE_CACHE_VALUE,cache.awaitForFutureOrElse(val2, null));
        assertEquals("Value should be TestCacheValues.LARGE_CACHE_VALUE",TestCacheValues.LARGE_CACHE_VALUE,cache.awaitForFutureOrElse(val3, null));
        assertEquals("Value should be TestCacheValues.LARGE_CACHE_VALUE",TestCacheValues.LARGE_CACHE_VALUE,cache.awaitForFutureOrElse(val4, null));


        try {
            Thread.sleep(5000);
            ListenableFuture<String> val5 = cache.get("Key1", executorService);

            assertNull("Value should be null",cache.awaitForFutureOrElse(val5, "one"));
            assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



    }



}
