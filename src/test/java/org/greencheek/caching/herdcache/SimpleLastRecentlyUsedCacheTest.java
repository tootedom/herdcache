package org.greencheek.caching.herdcache;

import com.google.common.util.concurrent.*;
import org.greencheek.caching.herdcache.await.AwaitOnFuture;
import org.greencheek.caching.herdcache.lru.SimpleLastRecentlyUsedCache;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Executors;

import static org.junit.Assert.*;

public class SimpleLastRecentlyUsedCacheTest implements AwaitOnFuture {

    private ListeningExecutorService executorService;
    private Cache<String> cache;

    @Before
    public void setUp() {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
        cache = new SimpleLastRecentlyUsedCache<>();
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