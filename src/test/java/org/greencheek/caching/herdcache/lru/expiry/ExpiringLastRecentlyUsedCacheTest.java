package org.greencheek.caching.herdcache.lru.expiry;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.greencheek.caching.herdcache.Cache;
import org.greencheek.caching.herdcache.await.AwaitOnFuture;
import org.greencheek.caching.herdcache.lru.SimpleLastRecentlyUsedCache;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class ExpiringLastRecentlyUsedCacheTest implements AwaitOnFuture {

    private ListeningExecutorService executorService;
    private Cache<String> cache;

    @Before
    public void setUp() {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
        cache = new ExpiringLastRecentlyUsedCache<>(1000,1000,10000,1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testExpiryOnlyItemsExpiresFromTimeToLive() throws Exception {
        testItemExpires(new ExpiringLastRecentlyUsedCache<>(1000,1000,1000,0, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testExpiryAndItemItemsExpiresFromTimeToLive() throws Exception {
        testItemExpires(new ExpiringLastRecentlyUsedCache<>(1000,1000,1000,2000, TimeUnit.MILLISECONDS));
    }

    private void testItemExpires(Cache<String> cache) throws Exception {
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

        assertEquals("Value should be key1","key3",this.awaitForFutureOrElse(cache.get("Key1"),null));

        assertEquals("Value should not exist",null,this.awaitForFutureOrElse(cache.get(UUID.randomUUID().toString()),null));



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

        assertEquals("Value should be null", null,this.awaitForFutureOrElse(cache.get("Key1"), null));
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

    @Test
    public void testNoIdle() {
        cache = new ExpiringLastRecentlyUsedCache<>(100,100,5000,-1, TimeUnit.MILLISECONDS);

        ListenableFuture<String> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(2000);
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

        assertEquals("Value should be key1","key1",this.awaitForFutureOrElse(val2, null));
    }

    @Test(expected = InstantiationError.class)
    public void testMustHaveAPositiveExpiry() {
        cache = new ExpiringLastRecentlyUsedCache<>(100,100,-1,-1, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testThreadedAccessForExpiryOnlyEntry() {
        cache = new ExpiringLastRecentlyUsedCache<>(10000,10000,5000,0, TimeUnit.MILLISECONDS);
        testThreadedAccess(cache);
    }

    @Test
    public void testThreadedAccessForExpiryAndIdleEntry() {
        cache = new ExpiringLastRecentlyUsedCache<>(10000,10000,5000,1000, TimeUnit.MILLISECONDS);
        testThreadedAccess(cache);
    }

    private void testThreadedAccess(Cache<String> cache) {
        ExecutorService threadPool = Executors.newFixedThreadPool(10);
        ConcurrentHashMap<String,ConcurrentLinkedQueue<ListenableFuture<String>>> futures = new ConcurrentHashMap<>();
        for(int i = 0;i<50000;i++) {
            final String val = randInt(0,1000);

             threadPool.submit(() -> {
                 ConcurrentLinkedQueue<ListenableFuture<String>> queuedFutures = new ConcurrentLinkedQueue<ListenableFuture<String>>();
                 ListenableFuture<String> future = cache.apply(val, () -> {
                     try {
                         Thread.sleep(2);
                     } catch (InterruptedException e) {
                         e.printStackTrace();
                     }
                     return val;
                 }, executorService);

                 queuedFutures.add(future);
                 ConcurrentLinkedQueue<ListenableFuture<String>> prev = futures.putIfAbsent(val, queuedFutures);
                 if(prev!=null) {
                     prev.add(future);
                 }
             });
        }

        for(Map.Entry<String,ConcurrentLinkedQueue<ListenableFuture<String>>> entry : futures.entrySet()) {
            String value = entry.getKey();
            ConcurrentLinkedQueue<ListenableFuture<String>> queue = entry.getValue();
            while(!queue.isEmpty()) {
                assertEquals("Value should be " + value, value, this.awaitForFutureOrElse(queue.poll(), null));
            }
        }


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