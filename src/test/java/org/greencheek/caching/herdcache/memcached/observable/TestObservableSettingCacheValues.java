package org.greencheek.caching.herdcache.memcached.observable;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import net.spy.memcached.ConnectionFactoryBuilder;
import org.greencheek.caching.herdcache.ObservableCache;
import org.greencheek.caching.herdcache.RequiresShutdown;
import org.greencheek.caching.herdcache.domain.CacheItem;
import org.greencheek.caching.herdcache.memcached.SpyObservableMemcachedCache;
import org.greencheek.caching.herdcache.memcached.config.KeyValidationType;
import org.greencheek.caching.herdcache.memcached.config.builder.ElastiCacheCacheConfigBuilder;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonFactory;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonWrapper;
import org.greencheek.caching.herdcache.util.EmptyBlockingQueue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rx.Scheduler;
import rx.Single;
import rx.functions.Action1;
import rx.plugins.RxJavaHooks;
import rx.schedulers.Schedulers;

import java.time.Duration;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 *
 */
public class TestObservableSettingCacheValues {


    private MemcachedDaemonWrapper memcached;
    private ObservableCache<String> cache;

    @Before
    public void setUp() {
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

       cache.shutdown();

    }

    @Test
    public void testSetKeyAndValue() {

        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setKeyValidationType(KeyValidationType.ALWAYS)
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
        String item = val.toBlocking().value().value();

        assertEquals("Value should be 'value1'", "value1", item);

        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());


        try {
            Thread.sleep(2500);
            val = cache.get("Key1");

            CacheItem<String> nullItem = val.toBlocking().value();
            assertFalse("Value should be 'null'",nullItem.hasValue());

        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }



    @Test
    public void testNoCacheAvailable() {

        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("")
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        Single<CacheItem<String>> val = cache.set("Key1", () -> "value1",Duration.ofSeconds(1));

        val.subscribe(item ->
                assertEquals("Value should be 'value1'", "value1", item.getValue().get()));

        val = cache.get("Key1");
        String item = val.toBlocking().value().value();

        assertNull("Value should be 'value1'", item);

        assertEquals(0, memcached.getDaemon().getCache().getCurrentItems());


        final String value = "from supplier";
        Single<CacheItem<String>> applyCache = cache.apply("Key1", () -> value,Duration.ofSeconds(1));

        assertEquals("Value should be '"+ value +"'", value, applyCache.toBlocking().value().value());




        applyCache = cache.apply("Key1", () -> {
            throw new RuntimeException("blah blah blah");
        },Duration.ofSeconds(1));

        try {
            assertEquals("Value should be '" + value + "'", value, applyCache.toBlocking().value());
            fail("exception expected");
        } catch (RuntimeException e) {

        }

    }



    @Test
    public void testDeleteCacheValues() {

        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        Single<CacheItem<String>> val = cache.set("Key1", () -> "value1",Duration.ofSeconds(60));

        val.subscribe(item ->
                assertEquals("Value should be 'value1'", "value1", item.getValue().get()));

        val = cache.get("Key1");
        String item = val.toBlocking().value().value();

        assertNotNull("Value should be 'value1'", item);

        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());


        final String value = "from supplier";
        Single<CacheItem<String>> applyCache = cache.apply("Key2", () -> value,Duration.ofSeconds(60));

        assertEquals("Value should be '"+ value +"'", value, applyCache.toBlocking().value().value());




        applyCache = cache.apply("Key2", () -> {
            throw new RuntimeException("blah blah blah");
        },Duration.ofSeconds(60));


        assertEquals("Value should be '" + value + "'", value, applyCache.toBlocking().value().value());

        assertEquals(2, memcached.getDaemon().getCache().getCurrentItems());
        assertEquals(3, memcached.getDaemon().getCache().getGetCmds());
        assertEquals(2, memcached.getDaemon().getCache().getSetCmds());

        Single<Boolean> delete = cache.clear("Key1");
        Single<Boolean> delete2 = cache.clear("Key2");

        assertTrue(delete.toBlocking().value().booleanValue());
        assertTrue(delete2.toBlocking().value().booleanValue());


        final String newValue = "from supplier new value";
        applyCache = cache.apply("Key2", () -> newValue,Duration.ofSeconds(60));

        assertEquals("Value should be '"+ newValue +"'", newValue, applyCache.toBlocking().value().value());

        memcached.getDaemon().stop();

        delete2 = cache.clear("Key2");
        assertFalse(delete2.toBlocking().value().booleanValue());

    }

    @Test
    public void testDeleteCacheValuesSmallWait() {

        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setWaitForRemove(Duration.ofMillis(0))
                        .setKeyPrefix(Optional.of("elastic"))
                        .buildMemcachedConfig()

        );

        String value = "value1";
        Single<CacheItem<String>> val = cache.set("Key1", () -> value,Duration.ofSeconds(60));

        CacheItem<String> item1 = val.toBlocking().value();
        assertEquals("elasticKey1", item1.getKey());

        Single<CacheItem<String>> val2 = cache.set("Key2", () -> value,Duration.ofSeconds(60));
        assertEquals("elasticKey2", val2.toBlocking().value().getKey());

        val.subscribe(item ->
                assertEquals("Value should be '" + value + "'", value, item.getValue().get()));

        val = cache.get("Key1");
        String item = val.toBlocking().value().value();

        assertNotNull("Value should be 'value1'", item);

        assertEquals(2, memcached.getDaemon().getCache().getCurrentItems());

        Single<Boolean> delete = cache.clear("Key1");
        assertNotNull(delete.toBlocking().value().booleanValue());


    }


    @Test
    public void testLazyCacheSet() {

        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(false)
                        .setWaitForRemove(Duration.ofMillis(0))
                        .setKeyPrefix(Optional.of("elastic"))
                        .buildMemcachedConfig()

        );

        String value = "value1";
        Single<CacheItem<String>> val = cache.set("Key1", () -> value,Duration.ofSeconds(60));
        Single<CacheItem<String>> val2 = cache.apply("Key2", () -> value, Duration.ofSeconds(60));

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(0, memcached.getDaemon().getCache().getCurrentItems());
    }



    @Test
    public void testCacheSetSubscribeOn() {

        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setWaitForRemove(Duration.ofMillis(0))
                        .setKeyPrefix(Optional.of("elastic"))
                        .buildMemcachedConfig()

        );

        CountDownLatch latch = new CountDownLatch(1);
        String value = "value1";
        Single<CacheItem<String>> val = cache.apply("Key1", () -> {
            System.out.println("Supplier Value: " + Thread.currentThread().getName());
            return value;
        }, Duration.ofSeconds(60));
        val = val.subscribeOn(Schedulers.io());
        val = val.observeOn(Schedulers.computation());

        val.subscribe(calculatedValue -> {
            System.out.println("subscription: " + Thread.currentThread().getName());
            latch.countDown();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());


    }


    @Test
    public void testLazyCacheGet() {

        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setWaitForRemove(Duration.ofMillis(0))
                        .setKeyPrefix(Optional.of("elastic"))
                        .buildMemcachedConfig()

        );

        String value = "value1";
        Single<CacheItem<String>> val = cache.set("Key1", () -> value, Duration.ofSeconds(60));

        assertEquals(value, val.toBlocking().value().value());
        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());

        Single<CacheItem<String>> item = cache.get("Key1");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(0, memcached.getDaemon().getCache().getGetCmds());

    }


    class MyFunkyException extends RuntimeException {

    }

    @Test
    public void testRuntimeException() {

        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setWaitForRemove(Duration.ofMillis(0))
                        .setKeyPrefix(Optional.of("elastic"))
                        .buildMemcachedConfig()

        );

        Single<CacheItem<String>> val = cache.set("Key1", () -> {throw new MyFunkyException();} , Duration.ofSeconds(60));

        boolean errorThrown = false;
        try {
            String item =  val.toBlocking().value().value();
        } catch (MyFunkyException e) {
            errorThrown = true;
        }

        assertTrue("should have thrown custom exception",errorThrown);

        final AtomicBoolean success = new AtomicBoolean(false);
        final AtomicBoolean failure = new AtomicBoolean(false);

        assertEquals(0, memcached.getDaemon().getCache().getCurrentItems());

        val.subscribe(
                new Action1<CacheItem<String>>() {
                    @Override
                    public void call(CacheItem<String> stringCacheItem) {
                        success.set(true);
                    }
                },
                new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        if(throwable instanceof MyFunkyException) {
                            failure.set(true);
                        }
                    }
                });

        assertFalse("should have thrown an exception",success.get());
        assertTrue("should have thrown custom exception", failure.get());
    }

    @Test
    public void testLazyCacheDelete() {

        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setWaitForRemove(Duration.ofSeconds(5))
                        .setWaitForRemove(Duration.ofMillis(0))
                        .setKeyPrefix(Optional.of("elastic"))
                        .buildMemcachedConfig()

        );

        String value = "value1";
        Single<CacheItem<String>> val = cache.set("Key1", () -> value, Duration.ofSeconds(60));

        assertEquals(value, val.toBlocking().value().value());
        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());

        Single<Boolean> item = cache.clear("Key1");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());

    }


    @Test
    public void testErrorSubscriber() {

        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setWaitForRemove(Duration.ofMillis(1))
                        .buildMemcachedConfig()
        );

        Single<CacheItem<String>> val = cache.set("Key1", () -> {throw new RuntimeException("lkkjlkj");},Duration.ofSeconds(60));

        final AtomicBoolean hasErrored = new AtomicBoolean(false);

        val.subscribe(item -> {
            hasErrored.set(false);
        }, throwa -> {
            hasErrored.set(true);
        });


        assertTrue("should have errored", hasErrored.get());


        val = cache.apply("Key1", () -> {throw new RuntimeException("lkkjlkj");},Duration.ofSeconds(60));

        hasErrored.set(false);

        val.subscribe(item -> {
            hasErrored.set(false);
        }, throwa -> {
            hasErrored.set(true);
        });


        assertTrue("should have errored",hasErrored.get());
    }

    @Test
    public void testErrorOnMemcachedWriteProvidedScheduler() {


        ThreadPoolExecutor p = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new EmptyBlockingQueue(),
                new ThreadPoolExecutor.AbortPolicy());

        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setWaitForMemcachedSetRxScheduler(Schedulers.from(p))
                        .setWaitForMemcachedSet(false)
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setKeyValidationType(KeyValidationType.ALWAYS)
                        .buildMemcachedConfig()
        );

        AtomicBoolean keepRunning = new AtomicBoolean(true);

        p.execute(new Runnable() {
            @Override
            public void run() {
                while(keepRunning.get()){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        CacheItem<String> val = cache.set("Key1", "value1", Duration.ZERO).toBlocking().value();
        keepRunning.set(false);

        // No need to assert anything, an unhandled scheduling exception will fail this test for us :)
    }

    @Test
    public void testErrorOnMemcachedWriteScheduler() {

        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setWaitForMemcachedSet(false)
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setKeyValidationType(KeyValidationType.ALWAYS)
                        .buildMemcachedConfig()
        );

        ThreadPoolExecutor ex = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new EmptyBlockingQueue(),
                new ThreadPoolExecutor.AbortPolicy());

        Scheduler sh = Schedulers.from(ex);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger error = new AtomicInteger(0);

        RxJavaHooks.setOnError((t) -> {
            latch.countDown();
            error.incrementAndGet();
        });

        cache.set("Key1", "value1",Duration.ZERO).subscribeOn(sh).observeOn(sh).subscribe((val) -> {
            System.out.println(val);
            latch.countDown();
        },(t) -> {
            latch.countDown();
            error.incrementAndGet();
        });


        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals(1,error.get());

    }



    @Test
    public void testFromCacheSetting() {

        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setWaitForRemove(Duration.ofMillis(1))
                        .buildMemcachedConfig()
        );

        Single<CacheItem<String>> get = cache.get("Key1");
        assertFalse("value should not be from cache",get.toBlocking().value().isFromCache());


        String value = "value";
        Single<CacheItem<String>> val = cache.apply("Key1", () -> value, Duration.ofSeconds(60));



        assertFalse("value should not be from cache",val.toBlocking().value().isFromCache());
        assertEquals("value should not be from cache", value,val.toBlocking().value().value());


        val = cache.apply("Key1", () -> value, Duration.ofSeconds(60));

        assertTrue("value should not be from cache", val.toBlocking().value().isFromCache());
        assertEquals("value should not be from cache", value,val.toBlocking().value().value());


    }



    @Test
    public void testLargeKeyValue() {

        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:11211")// + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setWaitForRemove(Duration.ofMillis(1))
                        .setKeyValidationType(KeyValidationType.NONE)
                        .buildMemcachedConfig()
        );

        String key = "blahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblah"+
                "blahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblah"+
                "blahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblah"+
                "blahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblah"+
                "blahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblah"+
                "blahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblah"+
                "blahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblah"+
                "blahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblah"+
                "blahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblah"+
                "blahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblah"+
                "blahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblah"+
                "blahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblah"+
                "blahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblah"+
                "blahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblah"+
                "blahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblahblah";

        String value = "chickenshop";
        Single<CacheItem<String>> val = cache.set(key, () -> value,Duration.ofSeconds(60));
        assertEquals("Value should be '"+value+"'", value, val.toBlocking().value().value());
//

    }
}
