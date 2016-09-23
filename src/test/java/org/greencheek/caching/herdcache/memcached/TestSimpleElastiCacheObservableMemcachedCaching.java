package org.greencheek.caching.herdcache.memcached;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.HashAlgorithm;
import org.greencheek.caching.herdcache.Cache;
import org.greencheek.caching.herdcache.CacheWithExpiry;
import org.greencheek.caching.herdcache.ObservableCache;
import org.greencheek.caching.herdcache.RequiresShutdown;
import org.greencheek.caching.herdcache.domain.CacheItem;
import org.greencheek.caching.herdcache.memcached.config.builder.ElastiCacheCacheConfigBuilder;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.server.StringServer;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.AsciiXXHashAlogrithm;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.JenkinsHash;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.XXHashAlogrithm;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonFactory;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rx.Single;
import rx.schedulers.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * Created by dominictootell on 25/08/2014.
 */
public class TestSimpleElastiCacheObservableMemcachedCaching {


    private MemcachedDaemonWrapper memcached;
    private ListeningExecutorService executorService;
    private ObservableCache cache;

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

    private void testHashAlgorithm(HashAlgorithm algo) {

        String[] configurationsMessage = new String[]{
                "CONFIG cluster 0 147\r\n" + "1\r\n" + "localhost|127.0.0.1|" + memcached.getPort() + "\r\n" + "\nEND\r\n"
        };

        StringServer server = new StringServer(configurationsMessage, 0, TimeUnit.SECONDS);
        server.before(configurationsMessage, TimeUnit.SECONDS, -1, false);


        try {
            cache = new ElastiCacheObservableMemcachedCache<String>(
                    new ElastiCacheCacheConfigBuilder()
                            .setElastiCacheConfigHosts("localhost:" + server.getPort())
                            .setConfigPollingTime(Duration.ofSeconds(10))
                            .setInitialConfigPollingDelay(Duration.ofSeconds(0))
                            .setReconnectDelay(Duration.ofSeconds(10))
                            .setNumberOfConsecutiveInvalidConfigurationsBeforeReconnect(3)
                            .setTimeToLive(Duration.ofSeconds(2))
                            .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                            .setWaitForMemcachedSet(true)
                            .setHashAlgorithm(algo)
                            .setConnectionTimeoutInMillis(Duration.ofSeconds(2))
                            .setDelayBeforeClientClose(Duration.ofSeconds(1))
                            .setUpdateConfigVersionOnDnsTimeout(true)
                            .setDnsConnectionTimeout(Duration.ofSeconds(2))
                            .setRemoveFutureFromInternalCacheBeforeSettingValue(true)
                            .buildElastiCacheMemcachedConfig()
            );

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            Single<CacheItem<String>> val = cache.apply("Key1", () -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return "TO BE STALE CONTENT";
            }, Duration.ofSeconds(2));


            Single<CacheItem<String>> val2 = cache.apply("Key1", () -> {
                return "B";
            },  Duration.ofSeconds(2));


            Single<CacheItem<String>> val3 = cache.apply("Key1", () -> {
                return "C";
            }, Duration.ofSeconds(2));

            assertEquals("Value should be key1", "TO BE STALE CONTENT", val.toBlocking().value().value());
            assertEquals("Value should be key1", "TO BE STALE CONTENT", val2.toBlocking().value().value());
            assertEquals("Value should be key1", "TO BE STALE CONTENT", val3.toBlocking().value().value());


            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            Single<CacheItem<String>> passThrough = cache.apply("Key1", () -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return "New Value";
            },  Duration.ofSeconds(2));


            Single<CacheItem<String>> val4 = cache.apply("Key1", () -> {
                return "E";
            },  Duration.ofSeconds(2));


            Single<CacheItem<String>> val5 = cache.apply("Key1", () -> {
                return "F";
            }, Duration.ofSeconds(2));


            assertEquals("Value should be key1", "New Value", val4.toBlocking().value().value());
            assertEquals("Value should be key1", "New Value", val5.toBlocking().value().value());

            assertEquals("Value should be key1", "New Value", passThrough.toBlocking().value().value());


            Single<CacheItem<String>> val6 = cache.apply("Key1", () -> {
                return "G";
            },  Duration.ofSeconds(2));


            assertEquals("Value should be key1", "New Value", val6.toBlocking().value().value());


            assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
        }
        finally {
            server.after();
            if(cache instanceof RequiresShutdown) {
                ((RequiresShutdown)cache).shutdown();
            }
        }

    }

    @Test
    public void testJenkinsHashAlgorithm() {
        testHashAlgorithm(new JenkinsHash());
    }


    @Test
    public void testXXHashAlgorithm() {
        testHashAlgorithm(new XXHashAlogrithm());
    }

    @Test
    public void testAsciiXXHashAlgorithm() {
        testHashAlgorithm(new AsciiXXHashAlogrithm());
    }


    @Test
    public void testMultiplyApplies() {
        String[] configurationsMessage = new String[]{
                "CONFIG cluster 0 147\r\n" + "1\r\n" + "localhost|127.0.0.1|" + memcached.getPort() + "\r\n" + "\nEND\r\n"
        };

        StringServer server = new StringServer(configurationsMessage, 0, TimeUnit.SECONDS);
        server.before(configurationsMessage, TimeUnit.SECONDS, -1, false);

        cache = new ElastiCacheObservableMemcachedCache<String>(
                new ElastiCacheCacheConfigBuilder()
                        .setElastiCacheConfigHosts("localhost:" + server.getPort())
                        .setConfigPollingTime(Duration.ofSeconds(10))
                        .setInitialConfigPollingDelay(Duration.ofSeconds(0))
                        .setReconnectDelay(Duration.ofSeconds(10))
                        .setNumberOfConsecutiveInvalidConfigurationsBeforeReconnect(3)
                        .setTimeToLive(Duration.ofSeconds(2))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setConnectionTimeoutInMillis(Duration.ofSeconds(2))
                        .setDelayBeforeClientClose(Duration.ofSeconds(1))
                        .setUpdateConfigVersionOnDnsTimeout(true)
                        .setDnsConnectionTimeout(Duration.ofSeconds(2))
                        .setRemoveFutureFromInternalCacheBeforeSettingValue(true)
                        .buildElastiCacheMemcachedConfig()
        );

        testThreadedAccess(cache);
    }

    private void testThreadedAccess(ObservableCache<String> cache) {
        AtomicInteger matches = new AtomicInteger(0);

        ConcurrentHashMap<String, Single<CacheItem<String>>> futures = new ConcurrentHashMap<>();
        for (int i = 0; i < 50000; i++) {
            final String val = randInt(0, 1000);
            Single<CacheItem<String>> future = cache.apply(val, () -> {
                try {
                    Thread.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return val;
            }, Duration.ofSeconds(60));

            futures.putIfAbsent(val, future);
        }


        final CountDownLatch latch = new CountDownLatch(futures.size());
        for (Map.Entry<String, Single<CacheItem<String>>> entry : futures.entrySet()) {
            final String value = entry.getKey();
            Single<CacheItem<String>> item = entry.getValue();
            item.subscribeOn(Schedulers.computation())
            .observeOn(Schedulers.io())
            .subscribe(stringValue -> {
                if(value.equals(stringValue.value())) {
                    matches.incrementAndGet();
                }
                latch.countDown();
            });

        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(futures.size(),matches.get());

    }

    public String randInt(int min,int max) {

        // NOTE: Usually this should be a field rather than a method
        // variable so that it is not re-seeded every call.
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        return ""+ rand.nextInt((max - min) + 1) + min;
    }
}
