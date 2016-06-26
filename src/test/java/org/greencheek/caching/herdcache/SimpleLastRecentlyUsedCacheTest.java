package org.greencheek.caching.herdcache;

import com.google.common.util.concurrent.*;
import org.greencheek.caching.herdcache.await.AwaitOnFuture;
import org.greencheek.caching.herdcache.lru.SimpleLastRecentlyUsedCache;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class SimpleLastRecentlyUsedCacheTest implements AwaitOnFuture {

    private ListeningExecutorService executorService;
    private Cache<String> cache;

    @Before
    public void setUp() {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
        cache = new SimpleLastRecentlyUsedCache<>();
    }

    @After
    public void tearDown() {
        executorService.shutdownNow();
    }


    @Test
    public void testAddCallbackOnExecutedFuture() {
        ListenableFuture<String> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "key1";
        }, executorService);


        assertEquals("Value should be key1","key1",this.awaitForFutureOrElse(val, null));

        final CountDownLatch l = new CountDownLatch(10);

        Futures.addCallback(val,createCallback(l));
        Futures.addCallback(val,createCallback(l));
        Futures.addCallback(val,createCallback(l));
        Futures.addCallback(val,createCallback(l));
        Futures.addCallback(val,createCallback(l));
        Futures.addCallback(val,createCallback(l));
        Futures.addCallback(val,createCallback(l));
        Futures.addCallback(val,createCallback(l));
        Futures.addCallback(val,createCallback(l));
        Futures.addCallback(val,createCallback(l));

        boolean done = false;
        try {
            done = l.await(100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Futures.addCallback(val,new FutureCallback<String>() {
            @Override
            public void onSuccess(String result) {

            }

            @Override
            public void onFailure(Throwable t) {

            }
        },executorService);

        assertTrue(done);


    }

    private FutureCallback<String> createCallback(final CountDownLatch globalLatch) {
        return new FutureCallback<String>() {

            final CountDownLatch latch = globalLatch;

            @Override
            public void onSuccess(String result) {
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable t) {

            }
        };
    }

    @Test
    public void testCanObtainSameValue() throws Exception {

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


        ListenableFuture<String> val3 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "key2";
        }, executorService);


        assertEquals("Value should be key1","key1",this.awaitForFutureOrElse(val, null));
        assertEquals("Value should be key1","key1",this.awaitForFutureOrElse(val2, null));
        assertEquals("Value should be key1","key1",this.awaitForFutureOrElse(val3, null));

        assertEquals("Value should be key1","key1",this.awaitForFutureOrElse(cache.get("Key1"),null));
        assertEquals("Value should not exist",null,this.awaitForFutureOrElse(cache.get(UUID.randomUUID().toString()),null));

    }

    @Test
    public void testCanObtainValueAfterFutureIsComplete() throws Exception {

        ListenableFuture<String> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "key1";
        }, executorService);

        assertEquals("Value should be key1","key1",this.awaitForFutureOrElse(val, null));


        ListenableFuture<String> val2 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "key2";
        }, executorService);

        ListenableFuture<String> val3 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "key2";
        }, executorService);

        assertEquals("Value should be key1","key1",this.awaitForFutureOrElse(val2, null));
        assertEquals("Value should be key1","key1",this.awaitForFutureOrElse(val3, null));

    }

    @Test
    public void testRuntimeExceptionIsNotCached() {
        ListenableFuture<String> val = cache.apply("Key1", () -> {
            throw new RuntimeException("exception thrown");
        }, executorService);

        assertNull("Value should be key1", this.awaitForFutureOrElse(val, null));

        ListenableFuture<String> val2 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                return "key1";
            }

        }, executorService);

        assertEquals("Value should be key1","key1",this.awaitForFutureOrElse(val2, null));

    }

    @Test
    public void testPredicateStopCaching() {
        AtomicInteger count = new AtomicInteger(0);
        SimpleLastRecentlyUsedCache cache = new SimpleLastRecentlyUsedCache<>();

        ListenableFuture<String> val = cache.apply("Key1", () -> {
            count.incrementAndGet();
            return "value1";
        }, executorService, (cacheValue) -> !cacheValue.equals("value1"));

        assertEquals("Value should be value1 but not cached", "value1", this.awaitForFutureOrElse(val, null));

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals("Nothing should be in the cache", 0, cache.size());

        ListenableFuture<String> val2 = cache.apply("Key1", () -> {
            try {
                count.incrementAndGet();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                return "key1";
            }

        }, executorService);

        ListenableFuture<String> val3 = cache.apply("Key1", () -> {
            try {
                count.incrementAndGet();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                return "key1";
            }

        }, executorService);


        assertEquals("One item should be in the cache",1,cache.size());

        assertEquals("Value should be key1","key1",this.awaitForFutureOrElse(val2, null));
        assertEquals("Value should be key1","key1",this.awaitForFutureOrElse(val3, null));

        ListenableFuture<String> val4 = cache.apply("Key1", () -> {
            count.incrementAndGet();
            return "key1fsdfsdf";
        }, executorService);

        assertEquals("Value should be key1","key1",this.awaitForFutureOrElse(val4, null));

        assertEquals("Value should only have been calculated twice",2,count.get());
    }

    @Test
    public void testCanObtainDifferentValuesAfterFutureIsComplete() throws Exception {

        ListenableFuture<String> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                return "key1";
            }

        }, executorService);

        assertEquals("Value should be key1","key1",this.awaitForFutureOrElse(val, null));


        ListenableFuture<String> val2 = cache.apply("Key2", () -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                return "key2";
            }
        }, executorService);



        ListenableFuture<String> val3 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "key2";
        }, executorService);

        ListenableFuture<String> val4 = cache.apply("Key1", () -> {
            throw new RuntimeException("exception thrown");
        }, executorService);

        assertEquals("Value should be key2","key2",this.awaitForFutureOrElse(val2, null));
        assertEquals("Value should be key1","key1",this.awaitForFutureOrElse(val3, null));
        assertEquals("Value should be key1","key1",this.awaitForFutureOrElse(val4, null));




    }

}