package org.greencheek.caching.herdcache.lru.expiry;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.greencheek.caching.herdcache.Cache;
import org.greencheek.caching.herdcache.await.AwaitOnFuture;
import org.greencheek.caching.herdcache.lru.SimpleLastRecentlyUsedCache;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ExpiringLastRecentlyUsedCacheTest implements AwaitOnFuture {

    private ListeningExecutorService executorService;
    private Cache<String> cache;

    @Before
    public void setUp() {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
        cache = new ExpiringLastRecentlyUsedCache<>(100,100,1000,1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testItemExpires() throws Exception {
        ListenableFuture<String> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "key1";
        }, executorService);


        ListenableFuture<String> val2 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "key2";
        }, executorService);




        assertEquals("Value should be key1","key1",this.awaitForFutureOrElse(val, null));
        assertEquals("Value should be key1","key1",this.awaitForFutureOrElse(val2, null));

        Thread.sleep(2000);

        ListenableFuture<String> val3 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "key3";
        }, executorService);


        assertEquals("Value should be key1", "key3", this.awaitForFutureOrElse(val3, null));

    }

    @Test
    public void testIdleItemExpires() {

        cache = new ExpiringLastRecentlyUsedCache<>(100,100,1000,200, TimeUnit.MILLISECONDS);


        ListenableFuture<String> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "key1";
        }, executorService);



        assertEquals("Value should be key1", "key1", this.awaitForFutureOrElse(val, null));

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ListenableFuture<String> val2 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "key2";
        }, executorService);

        assertEquals("Value should be key2","key2",this.awaitForFutureOrElse(val2, null));


    }

    @Test
    public void doNotCacheException() {
        ListenableFuture<String> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(100);
                throw new RuntimeException();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "key1";
        }, executorService);


        assertNotEquals("Value should be key1", "key1", this.awaitForFutureOrElse(val, null));

        ListenableFuture<String> val2 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "key2";
        }, executorService);

        assertEquals("Value should be key2","key2",this.awaitForFutureOrElse(val2, null));



    }
}