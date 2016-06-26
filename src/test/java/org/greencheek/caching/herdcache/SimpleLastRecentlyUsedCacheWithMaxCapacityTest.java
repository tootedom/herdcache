package org.greencheek.caching.herdcache;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.greencheek.caching.herdcache.await.AwaitOnFuture;
import org.greencheek.caching.herdcache.lru.SimpleLastRecentlyUsedCache;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SimpleLastRecentlyUsedCacheWithMaxCapacityTest implements AwaitOnFuture {

    private ListeningExecutorService executorService;
    private Cache<String> cache;

    @Before
    public void setUp() {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
        cache = new SimpleLastRecentlyUsedCache<>(2);
    }

    @After
    public void tearDown() {
        executorService.shutdownNow();
    }


    @Test
    public void testMaxCapacity() throws Exception {

        ListenableFuture<String> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "key1";
        }, executorService);


        ListenableFuture<String> val2 = cache.apply("key2", () -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "key2";
        }, executorService);


        ListenableFuture<String> val3 = cache.apply("key3", () -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "key3";
        }, executorService);

        ListenableFuture<String> val4 = cache.apply("key1", () -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "key_new";
        }, executorService);


        assertEquals("Value should be key1","key1",this.awaitForFutureOrElse(val, null));
        assertEquals("Value should be key2","key2",this.awaitForFutureOrElse(val2, null));
        assertEquals("Value should be key3","key3",this.awaitForFutureOrElse(val3, null));
        assertEquals("Value should be key1","key_new",this.awaitForFutureOrElse(val4, null));

    }
}