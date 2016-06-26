package org.greencheek.caching.herdcache.await;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.greencheek.caching.herdcache.Cache;
import org.greencheek.caching.herdcache.lru.SimpleLastRecentlyUsedCache;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class AwaitOnFutureTest {

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
    public void testWaitForLimitedPeriod() {
        ListenableFuture<String> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "Key1";
        }, executorService);


        assertEquals("returned string should be the timeoutValue","timeoutValue",cache.awaitForFutureOrElse(val,"exceptionValue","timeoutValue",1000, TimeUnit.MILLISECONDS));
    }


    @Test
    public void testExceptionValue() {
        ListenableFuture<String> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
                throw new RuntimeException("error");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "Key1";
        }, executorService);


        assertEquals("returned string should be the exceptionValue","exceptionValue",cache.awaitForFutureOrElse(val,"exceptionValue","timeoutValue",2000, TimeUnit.MILLISECONDS));
    }


    @Test
    public void testValueReturned() {
        ListenableFuture<String> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "Key1";
        }, executorService);


        assertEquals("returned string should be the expected value","Key1",cache.awaitForFutureOrElse(val,"exceptionValue","timeoutValue",2000, TimeUnit.MILLISECONDS));

        val = cache.apply("Key2", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "Key2";
        }, executorService);

        assertEquals("returned string should be the expected value","Key2",cache.awaitForFutureOrElse(val,"exceptionValue"));


    }

}