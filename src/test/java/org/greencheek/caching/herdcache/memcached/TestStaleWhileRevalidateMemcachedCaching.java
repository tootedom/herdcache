package org.greencheek.caching.herdcache.memcached;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import net.spy.memcached.ConnectionFactoryBuilder;
import org.greencheek.caching.herdcache.RequiresShutdown;
import org.greencheek.caching.herdcache.RevalidateInBackgroundCapableCache;
import org.greencheek.caching.herdcache.domain.CacheableItemWithCreationDate;
import org.greencheek.caching.herdcache.memcached.config.builder.ElastiCacheCacheConfigBuilder;
import org.greencheek.caching.herdcache.memcached.keyhashing.KeyHashingType;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonFactory;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

        executorService.shutdownNow();

    }

    @Test
    public void testBackgroundRevalidation() {

        String BACKGROUND_VALUE = "background";
        String STALE_VALUE = "value1";
        String STALE_VALUE2 = "value2";
        String STALE_VALUE3 = "value3";

        ListeningExecutorService executorService = MoreExecutors.listeningDecorator(new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1)));

        try {
            cache = new SpyMemcachedCache<>(
                    new ElastiCacheCacheConfigBuilder()
                            .setMemcachedHosts("localhost:" + memcached.getPort())
                            .setTimeToLive(Duration.ofSeconds(60))
                            .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                            .setWaitForMemcachedSet(true)
                            .setRemoveFutureFromInternalCacheBeforeSettingValue(true)
                            .setKeyHashType(KeyHashingType.NONE)
                            .buildMemcachedConfig()

            );

            // Set Key1 to STALE_VALUE
            ListenableFuture<CacheableItemWithCreationDate<String>> val = cache.apply("Key1",
                    () -> {
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                        }
                        return new CacheableItemWithCreationDate(STALE_VALUE);

                    }
                    , Duration.ofMillis(0), executorService);

            // This will get the future from above.
            ListenableFuture<CacheableItemWithCreationDate<String>> val2 = cache.apply("Key1",
                    () -> new CacheableItemWithCreationDate(STALE_VALUE2), Duration.ofMillis(0), executorService);


            assertEquals("Value for Key1 should be " + STALE_VALUE, STALE_VALUE, cache.awaitForFutureOrElse(val, null).getCachedItem());
            assertEquals("Value for Key1 should be " + STALE_VALUE, STALE_VALUE, cache.awaitForFutureOrElse(val2, null).getCachedItem());

            // Check there is 1 item in the cache
            assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());

            // let the object rot in the cache for a bit
            // So that it becomes stale.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Execute a apply for Key1, which we say cannot be older than 100ms. (We waited 1s, so it will be considered
            // not valid (stale).
            //
            // However, we allow stale items so the STALE item will be returned
            System.out.println("doing");
            ListenableFuture<CacheableItemWithCreationDate<String>> val3 = executeStaleWhileRevalidate(BACKGROUND_VALUE,executorService);

            // Check we got the stale value
            assertEquals("Value for Key1 should be " + STALE_VALUE, STALE_VALUE, cache.awaitForFutureOrElse(val3, null).getCachedItem());


            // Execute apply with no stale checking (everything is valid), so the STALE_VALUE2 is returned
            ListenableFuture<CacheableItemWithCreationDate<String>> val4 = cache.apply("Key1",
                    () ->  new CacheableItemWithCreationDate(STALE_VALUE2),Duration.ofMillis(0), executorService);

            assertEquals("Value for Key1 should be " + STALE_VALUE, STALE_VALUE, cache.awaitForFutureOrElse(val4, null).getCachedItem());

            // Execute apply with stale checking (item is not valid), so the we wait on supplier to complete (the future
            // from the previous "stale while revalidate" isn't captured anywhere currently (TODO).
            // The val5 will be "queue" on the executed.  Therefore it is waiting until the previous write occurs
            // The get looks up on the caller, not the background so Key1 will override the background revalidate threads
            // write.
            ListenableFuture<CacheableItemWithCreationDate<String>> val5 = cache.apply("Key1",
                    () ->  new CacheableItemWithCreationDate(STALE_VALUE3),Duration.ofMillis(0),executorService,
                    (CacheableItemWithCreationDate<String> v) -> true,
                    (CacheableItemWithCreationDate<String> v) -> {
                        return !v.hasExpired(100);
                    });

            assertEquals("Value for Key1 should be " + STALE_VALUE3, STALE_VALUE3, cache.awaitForFutureOrElse(val5, null).getCachedItem());


            ListenableFuture<CacheableItemWithCreationDate<String>> val6 = cache.apply("Key1", () -> {
                return new CacheableItemWithCreationDate(STALE_VALUE2);
            }, Duration.ofMillis(0), executorService);

            assertEquals("Value for Key1 should be " + STALE_VALUE3, STALE_VALUE3, cache.awaitForFutureOrElse(val6, null).getCachedItem());


            // Put some values in the cache
            for(int i = 1;i<4;i++ ) {
                ListenableFuture<CacheableItemWithCreationDate<String>> item = cache.apply("revalidate" +i,
                        () -> new CacheableItemWithCreationDate(STALE_VALUE), Duration.ofMillis(0), executorService);

                cache.awaitForFutureOrElse(item,null);

                // Don't want to hit the executor limit here.
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


            // Make the item in the cache expired
            try {
                Thread.sleep(200);
            } catch (Exception e) {

            }

            ListenableFuture<CacheableItemWithCreationDate<String>> revalidate1 = executeStaleWhileRevalidate("revalidate1",BACKGROUND_VALUE + "1",executorService);
            ListenableFuture<CacheableItemWithCreationDate<String>> revalidate1a = executeStaleWhileRevalidate("revalidate1",BACKGROUND_VALUE  + "1a",executorService);
            ListenableFuture<CacheableItemWithCreationDate<String>> revalidate1b = executeStaleWhileRevalidate("revalidate1",BACKGROUND_VALUE + "1b",executorService);
            ListenableFuture<CacheableItemWithCreationDate<String>> revalidate2 = executeStaleWhileRevalidate("revalidate2",BACKGROUND_VALUE + "2",executorService);

            // This wouldn't have been possible
            ListenableFuture<CacheableItemWithCreationDate<String>> revalidate3 = executeStaleWhileRevalidate("revalidate3", BACKGROUND_VALUE + "3", executorService);

            assertEquals("Value for revalidate1 should be " + STALE_VALUE, STALE_VALUE, cache.awaitForFutureOrElse(revalidate1, null).getCachedItem());
            assertEquals("Value for revalidate1a should be " + STALE_VALUE, STALE_VALUE, cache.awaitForFutureOrElse(revalidate1a, null).getCachedItem());
            assertEquals("Value for revalidate1b should be " + STALE_VALUE, STALE_VALUE, cache.awaitForFutureOrElse(revalidate1b, null).getCachedItem());
            assertEquals("Value for revalidate2 should be " + STALE_VALUE, STALE_VALUE, cache.awaitForFutureOrElse(revalidate2, null).getCachedItem());


            try {
                Thread.sleep(1500);
            } catch (Exception e) {

            }

            ListenableFuture<CacheableItemWithCreationDate<String>> revalidate1c = executeStaleWhileRevalidate("revalidate1",BACKGROUND_VALUE + "1c",executorService);
            assertEquals("Value for revalidate1c should be " + BACKGROUND_VALUE + "1", BACKGROUND_VALUE + "1", cache.awaitForFutureOrElse(revalidate1c, null).getCachedItem());


            assertEquals("Value for revalidate3 should be " + STALE_VALUE, STALE_VALUE, cache.awaitForFutureOrElse(revalidate3, null).getCachedItem());

            // The previous background thread would have failed with an exception, therefore the value is still stale not background3
            revalidate3 = executeStaleWhileRevalidate("revalidate3", BACKGROUND_VALUE + "3", executorService);
            assertEquals("Value for revalidate3 should be " + STALE_VALUE, STALE_VALUE, cache.awaitForFutureOrElse(revalidate3, null).getCachedItem());

            revalidate1b = executeStaleWhileRevalidate("revalidate1",BACKGROUND_VALUE + "1b",executorService);
            assertEquals("Value for revalidate1 should be " + BACKGROUND_VALUE + "1", BACKGROUND_VALUE + "1", cache.awaitForFutureOrElse(revalidate1b, null).getCachedItem());


        }  finally {
            executorService.shutdownNow();
        }

    }


    private ListenableFuture<CacheableItemWithCreationDate<String>> executeStaleWhileRevalidate(String key,String value, ListeningExecutorService executorService) {
        return cache.apply(key, () -> {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {

                    }
                    return new CacheableItemWithCreationDate(value);
                },
                Duration.ofMillis(0), executorService, (CacheableItemWithCreationDate<String> v) -> true,
                (CacheableItemWithCreationDate<String> v) -> {
                    return !v.hasExpired(100);
                }, true);
    }


    private ListenableFuture<CacheableItemWithCreationDate<String>> executeStaleWhileRevalidate(String value, ListeningExecutorService executorService) {
        return executeStaleWhileRevalidate("Key1",value,executorService);
    }
}
