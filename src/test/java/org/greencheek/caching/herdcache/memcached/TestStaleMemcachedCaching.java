package org.greencheek.caching.herdcache.memcached;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import net.spy.memcached.ConnectionFactoryBuilder;
import org.greencheek.caching.herdcache.CacheWithExpiry;
import org.greencheek.caching.herdcache.RequiresShutdown;
import org.greencheek.caching.herdcache.memcached.config.builder.ElastiCacheCacheConfigBuilder;
import org.greencheek.caching.herdcache.memcached.keyhashing.KeyHashingType;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonFactory;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

/**
 * Created by dominictootell on 25/08/2014.
 */
public class TestStaleMemcachedCaching {
    private MemcachedDaemonWrapper memcached;
    private ListeningExecutorService executorService;
    private CacheWithExpiry<String> cache;

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
    public void testMemcachedCache() throws InterruptedException {

        cache = new MemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(1))
                        .setUseStaleCache(true)
                        .setStaleCacheAdditionalTimeToLive(Duration.ofSeconds(4))
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
            return "will be stale";
        }, executorService);

        assertEquals("Value should be key1","will be stale",cache.awaitForFutureOrElse(val, null));

        Thread.sleep(2500);


        ListenableFuture<String> val2 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "will not generate";
        }, executorService);


        ListenableFuture<String> val3 = cache.apply("Key1", () -> {
            return "kjlkjlkj";
        }, executorService);


        assertEquals("Value should be key1","will be stale",cache.awaitForFutureOrElse(val3, null));

        Thread.sleep(2500);

        assertEquals("Value should be key1","will not generate",cache.awaitForFutureOrElse(val2, null));



    }

    @Test
    public void testMemcachedCacheFunctionsWhenHostsNotAvailable() throws InterruptedException {

        cache = new MemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:11111")
                        .setTimeToLive(Duration.ofSeconds(1))
                        .setUseStaleCache(true)
                        .setStaleCacheAdditionalTimeToLive(Duration.ofSeconds(4))
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
            return "will be stale";
        }, executorService);

        assertEquals("Value should be key1","will be stale",cache.awaitForFutureOrElse(val, null));

        Thread.sleep(2500);


        ListenableFuture<String> val2 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "will not generate";
        }, executorService);


        ListenableFuture<String> val3 = cache.apply("Key1", () -> {
            return "kjlkjlkj";
        }, executorService);


        assertEquals("Value should be key1","will not generate",cache.awaitForFutureOrElse(val3, null));

        Thread.sleep(2500);

        assertEquals("Value should be key1","will not generate",cache.awaitForFutureOrElse(val2, null));



    }

    @Test
    public void testStaleMemcachedCacheWithNoSuchItems() throws InterruptedException {

        cache = new MemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(1))
                        .setUseStaleCache(true)
                        .setStaleCacheAdditionalTimeToLive(Duration.ofSeconds(4))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setKeyHashType(KeyHashingType.NONE)
                        .buildMemcachedConfig()
        );

        ListenableFuture<String> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "will be stale";
        }, executorService);

        assertEquals("Value should be key1","will be stale",cache.awaitForFutureOrElse(val, null));

        ListenableFuture<String> val_again = cache.apply("Key1", () -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "will not generate";
        }, executorService);

        assertEquals("Value should be key1","will be stale",cache.awaitForFutureOrElse(val_again, null));

        Thread.sleep(1500);

        memcached.getDaemon().getCache().flush_all();

        ListenableFuture<String> val2 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "will not generate";
        }, executorService);


        ListenableFuture<String> val3 = cache.apply("Key1", () -> {
            return "kjlkjlkj";
        }, executorService);


        assertEquals("Value should be key1","will not generate",cache.awaitForFutureOrElse(val3, null));


        Thread.sleep(2500);

        assertEquals("Value should be key1","will not generate",cache.awaitForFutureOrElse(val2, null));

        ListenableFuture<String> valStaleEntry = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "will be stale entry";
        }, executorService);


        assertEquals("Value should be key1","will be stale entry",cache.awaitForFutureOrElse(valStaleEntry, null));

        Thread.sleep(2000);

        ListenableFuture<String> valStaleEntry2 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "will not generate";
        }, executorService);

        assertEquals("Value should be key1","will be stale entry",cache.awaitForFutureOrElse(cache.get("Key1"), null));

    }
}
