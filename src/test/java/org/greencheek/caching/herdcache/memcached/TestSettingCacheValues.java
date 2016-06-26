package org.greencheek.caching.herdcache.memcached;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import net.spy.memcached.ConnectionFactoryBuilder;
import org.greencheek.caching.herdcache.CacheWithExpiry;
import org.greencheek.caching.herdcache.IsCachedValueUsable;
import org.greencheek.caching.herdcache.IsSupplierValueCachable;
import org.greencheek.caching.herdcache.RequiresShutdown;
import org.greencheek.caching.herdcache.memcached.config.builder.ElastiCacheCacheConfigBuilder;
import org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.SerializingTranscoder;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonFactory;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonWrapper;
import org.greencheek.caching.herdcache.memcached.util.TestCacheValues;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

/**
 *
 */
public class TestSettingCacheValues {


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

        executorService.shutdownNow();
    }

    @Test
    public void testSetKeyAndValue() {

        cache = new SpyMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        ListenableFuture<String> val = cache.set("Key1", "value1");
        assertEquals("Value should be 'value1'","value1",cache.awaitForFutureOrElse(val, null));

        try {
            ListenableFuture<String> val5 = cache.get("Key1", executorService);

            assertEquals("Value should be 'value1'", "value1", cache.awaitForFutureOrElse(val5, null));
            assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSetKeyAndValueAsSupplier() {

        cache = new SpyMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        ListenableFuture<String> val = cache.set("Key1", () -> "value1");
        assertEquals("Value should be 'value1'","value1",cache.awaitForFutureOrElse(val, null));

        try {
            ListenableFuture<String> val5 = cache.get("Key1", executorService);

            assertEquals("Value should be 'value1'", "value1", cache.awaitForFutureOrElse(val5, null));
            assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSetValueWithExpiry() {

        cache = new SpyMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        ListenableFuture<String> val = cache.set("Key1", "value1",Duration.ofSeconds(1));
        assertEquals("Value should be 'value1'","value1",cache.awaitForFutureOrElse(val, null));

        try {
            ListenableFuture<String> val2 = cache.get("Key1", executorService);
            assertEquals("Value should be 'value1'", "value1", cache.awaitForFutureOrElse(val2, null));

            Thread.sleep(2500);
            ListenableFuture<String> val3 = cache.get("Key1", executorService);
            assertNull("Value should be null", cache.awaitForFutureOrElse(val3, null));

            assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSetValueWithExpiryAndCanCacheValuePredicate() {

        cache = new SpyMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        ListenableFuture<String> val = cache.set("Key1", () -> "value1",Duration.ofSeconds(1));
        assertEquals("Value should be 'value1'","value1",cache.awaitForFutureOrElse(val, null));

        try {
            ListenableFuture<String> val2 = cache.get("Key1", executorService);
            assertEquals("Value should be 'value1'", "value1", cache.awaitForFutureOrElse(val2, null));

            Thread.sleep(2500);
            ListenableFuture<String> val3 = cache.get("Key1", executorService);
            assertNull("Value should be null", cache.awaitForFutureOrElse(val3, null));

            assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
        } catch (Exception e) {
            e.printStackTrace();
        }


        ListenableFuture<String> val4 = cache.set("Key1", () -> "value1",Duration.ofSeconds(1), (v) -> !v.equals("value1"), executorService);
        assertEquals("Value should be 'value1'","value1",cache.awaitForFutureOrElse(val4, null));

        try {
            ListenableFuture<String> val5 = cache.get("Key1", executorService);
            assertNull("Value should be null", cache.awaitForFutureOrElse(val5, null));

            Thread.sleep(2500);
            ListenableFuture<String> val6 = cache.get("Key1", executorService);
            assertNull("Value should be null", cache.awaitForFutureOrElse(val6, null));

            assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSetKeyAndValueWithExecutorService() {

        cache = new SpyMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        ListenableFuture<String> val = cache.set("Key1WithExecutor", "value1",executorService);
        assertEquals("Value should be 'value1'","value1",cache.awaitForFutureOrElse(val, null));

        try {
            ListenableFuture<String> val5 = cache.get("Key1WithExecutor", executorService);

            assertEquals("Value should be 'value1'","value1",cache.awaitForFutureOrElse(val5, null));
            assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Test
    public void testSetNullValue() {

        cache = new SpyMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        ListenableFuture<String> val = cache.set("Key1WithExecutor", () -> null,executorService);
        assertNull("Value should be null", cache.awaitForFutureOrElse(val, "bob1"));

        try {
            ListenableFuture<String> val5 = cache.get("Key1WithExecutor", executorService);

            assertNull("Value should be null", cache.awaitForFutureOrElse(val5, "bob1"));
            assertEquals(0, memcached.getDaemon().getCache().getCurrentItems());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSetNullValueAndNoCaching() {

        cache = new SpyMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        ListenableFuture<String> val = cache.set("Key1WithExecutor", () -> null,(x) -> false,executorService);
        assertNull("Value should be null", cache.awaitForFutureOrElse(val, "bob1"));

        try {
            ListenableFuture<String> val5 = cache.get("Key1WithExecutor", executorService);

            assertNull("Value should be null", cache.awaitForFutureOrElse(val5, "bob1"));
            assertEquals(0, memcached.getDaemon().getCache().getCurrentItems());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSetNoCachingValue() {

        cache = new SpyMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        ListenableFuture<String> val = cache.set("Key1WithExecutor", () -> "value1",(cacheValue) ->  cacheValue.equals("valueToCache"),executorService);
        assertEquals("Value should be 'value1", "value1", cache.awaitForFutureOrElse(val, null));

        try {
            ListenableFuture<String> val5 = cache.get("Key1WithExecutor", executorService);

            assertNull("Value should be null",cache.awaitForFutureOrElse(val5, null));
            assertEquals(0, memcached.getDaemon().getCache().getCurrentItems());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSetKeyAndValueWithExecutorServiceWithTTL() {

        cache = new SpyMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        ListenableFuture<String> val = cache.set("Key1WithTTL", () -> "value1",Duration.ofSeconds(2),
                IsSupplierValueCachable.GENERATED_VALUE_IS_ALWAYS_CACHABLE,executorService);

        assertEquals("Value should be 'value1'","value1",cache.awaitForFutureOrElse(val, null));

        try {
            ListenableFuture<String> val5 = cache.get("Key1WithTTL", executorService);

            assertEquals("Value should be 'value1'","value1",cache.awaitForFutureOrElse(val5, null));
            assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(3000);
            ListenableFuture<String> val5 = cache.get("Key1WithTTL", executorService);

            assertNull("Value should be 'value1'", cache.awaitForFutureOrElse(val5, null));

        } catch (Exception e) {
            fail("Unable to wait for expiry");
        }
    }

    @Test
    public void testSetKeyAndValueWithExceptionL() {

        cache = new SpyMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        ListenableFuture<String> val = cache.set("Key1WithTTL", () -> "value1",Duration.ofSeconds(10),
                IsSupplierValueCachable.GENERATED_VALUE_IS_ALWAYS_CACHABLE,executorService);

        assertEquals("Value should be 'value1'","value1",cache.awaitForFutureOrElse(val, null));

        try {
            ListenableFuture<String> val5 = cache.get("Key1WithTTL", executorService);

            assertEquals("Value should be 'value1'","value1",cache.awaitForFutureOrElse(val5, null));
            assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());
        } catch (Exception e) {
            e.printStackTrace();
        }

        ListenableFuture<String> exceptionVal = cache.set("Key1WithTTL", () -> {throw new RuntimeException("Exception Thrown");},Duration.ofSeconds(10),
                IsSupplierValueCachable.GENERATED_VALUE_IS_ALWAYS_CACHABLE,executorService);


        try {
            exceptionVal.get();
            fail("");
        } catch (InterruptedException e) {
            fail("InterruptedException not expected");
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            assertTrue(t instanceof RuntimeException);
            assertEquals("Exception Thrown", t.getMessage());
        }

        try {
            ListenableFuture<String> val5 = cache.get("Key1WithTTL", executorService);
            assertEquals("Value should be 'value1'", "value1", cache.awaitForFutureOrElse(val5, null));
        } catch (Exception e) {
            fail("Unable to wait for expiry");
        }
    }

}
