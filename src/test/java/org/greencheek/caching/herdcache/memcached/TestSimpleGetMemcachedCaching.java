package org.greencheek.caching.herdcache.memcached;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.FailureMode;
import net.spy.memcached.HashAlgorithm;
import org.greencheek.caching.herdcache.CacheWithExpiry;
import org.greencheek.caching.herdcache.RequiresShutdown;
import org.greencheek.caching.herdcache.memcached.config.builder.ElastiCacheCacheConfigBuilder;
import org.greencheek.caching.herdcache.memcached.keyhashing.KeyHashingType;
import org.greencheek.caching.herdcache.memcached.spy.extensions.FastSerializingTranscoder;
import org.greencheek.caching.herdcache.memcached.spy.extensions.SerializingTranscoder;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.AsciiXXHashAlogrithm;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.JenkinsHash;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.XXHashAlogrithm;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonFactory;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by dominictootell on 25/08/2014.
 */
public class TestSimpleGetMemcachedCaching {


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
    }

    @Test
    public void testGetReturnsFromMemcachedCache() {

        cache = new SpyMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(2))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        ListenableFuture<String> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "value1";
        }, executorService);

        ListenableFuture<String> val2 = cache.get("Key1", executorService);
        ListenableFuture<String> val3 = cache.get("Key1", executorService);
        ListenableFuture<String> val4 = cache.get("Key1", executorService);


        assertEquals("Value should be value1","value1",cache.awaitForFutureOrElse(val, null));
        assertEquals("Value should be value1","value1",cache.awaitForFutureOrElse(val2, null));
        assertEquals("Value should be value1","value1",cache.awaitForFutureOrElse(val3, null));
        assertEquals("Value should be value1","value1",cache.awaitForFutureOrElse(val4, null));


        try {
            Thread.sleep(5000);
            ListenableFuture<String> val5 = cache.get("Key1", executorService);

            assertNull("Value should be null",cache.awaitForFutureOrElse(val5, "one"));
            assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



    }

    @Test
    public void testApplyReturnsFromKilledMemcachedCache() {

        cache = new SpyMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(2))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setFailureMode(FailureMode.Cancel)
                        .buildMemcachedConfig()
        );

        ListenableFuture<String> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "value1";
        }, executorService);

        ListenableFuture<String> val2 = cache.get("Key1", executorService);
        ListenableFuture<String> val3 = cache.get("Key1", executorService);
        ListenableFuture<String> val4 = cache.get("Key1", executorService);


        assertEquals("Value should be value1","value1",cache.awaitForFutureOrElse(val, null));
        assertEquals("Value should be value1","value1",cache.awaitForFutureOrElse(val2, null));
        assertEquals("Value should be value1","value1",cache.awaitForFutureOrElse(val3, null));
        assertEquals("Value should be value1","value1",cache.awaitForFutureOrElse(val4, null));

        ListenableFuture<String> val5 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "value1";
        }, executorService);




        if(memcached!=null) {
            memcached.getDaemon().stop();
        }

        try {
            Thread.sleep(5000);

            ListenableFuture<String> val6 = cache.apply("Key1", () -> {
                return "memcacheddead";
            }, executorService);


            assertNotNull("Value should be null", cache.awaitForFutureOrElse(val6, "one"));
            assertEquals("Value should be memcacheddead", "memcacheddead",cache.awaitForFutureOrElse(val6, "one"));
            assertEquals(0, memcached.getDaemon().getCache().getCurrentItems());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



    }

    @Test
    public void testGetReturnsFromDisabledMemcachedCache() {

        cache = new SpyMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhosty:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(2))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        ListenableFuture<String> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "value1";
        }, executorService);

        ListenableFuture<String> val2 = cache.get("Key1", executorService);
        ListenableFuture<String> val3 = cache.get("Key1", executorService);
        ListenableFuture<String> val4 = cache.get("Key1", executorService);


        assertEquals("Value should be value1","value1",cache.awaitForFutureOrElse(val, null));
        assertEquals("Value should be value1","value1",cache.awaitForFutureOrElse(val2, null));
        assertEquals("Value should be value1","value1",cache.awaitForFutureOrElse(val3, null));
        assertEquals("Value should be value1","value1",cache.awaitForFutureOrElse(val4, null));


        try {
            Thread.sleep(5000);
            ListenableFuture<String> val5 = cache.get("Key1", executorService);

            assertNull("Value should be null",cache.awaitForFutureOrElse(val5, "one"));
            assertEquals(0, memcached.getDaemon().getCache().getCurrentItems());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



    }



}
