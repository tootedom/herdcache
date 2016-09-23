package org.greencheek.caching.herdcache.memcached;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import net.spy.memcached.ConnectionFactoryBuilder;
import org.greencheek.caching.herdcache.CacheWithExpiry;
import org.greencheek.caching.herdcache.IsSupplierValueCachable;
import org.greencheek.caching.herdcache.ObservableCache;
import org.greencheek.caching.herdcache.RequiresShutdown;
import org.greencheek.caching.herdcache.domain.CacheItem;
import org.greencheek.caching.herdcache.memcached.config.builder.ElastiCacheCacheConfigBuilder;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonFactory;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rx.Single;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

/**
 *
 */
public class TestObservableSettingCacheValues {


    private MemcachedDaemonWrapper memcached;
    private ListeningExecutorService executorService;
    private ObservableCache<String> cache;

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

        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        Single<CacheItem<String>> val = cache.set("Key1", "value1",Duration.ZERO);

        val.subscribe(item ->
                assertEquals("Value should be 'value1'", "value1", item.getValue().get()));

        val = cache.get("Key1");

        val.subscribe(item ->
                assertEquals("Value should be 'value1'", "value1", item.getValue().get()));

        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());

    }

    @Test
    public void testSetKeyAndValueAsSupplier() {

        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        Single<CacheItem<String>> val = cache.set("Key1", () -> "value1",Duration.ZERO);

        val.subscribe(item ->
                assertEquals("Value should be 'value1'", "value1", item.getValue().get()));

        val = cache.get("Key1");

        val.subscribe(item ->
                assertEquals("Value should be 'value1'", "value1", item.getValue().get()));

        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());


    }

    @Test
    public void testSetValueWithExpiry() {

        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        Single<CacheItem<String>> val = cache.set("Key1", () -> "value1",Duration.ofSeconds(1));

        val.subscribe(item ->
                assertEquals("Value should be 'value1'", "value1", item.getValue().get()));

        val = cache.get("Key1");

        val.subscribe(item ->
                assertEquals("Value should be 'value1'", "value1", item.getValue().get()));

        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());


        try {
            Thread.sleep(2500);
            val = cache.get("Key1");

            val.subscribe(item ->
                    assertFalse("Value should be 'null'",item.getValue().isPresent()));

        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }


}
