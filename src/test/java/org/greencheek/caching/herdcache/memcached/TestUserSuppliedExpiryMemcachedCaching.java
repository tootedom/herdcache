package org.greencheek.caching.herdcache.memcached;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.HashAlgorithm;
import org.greencheek.caching.herdcache.*;
import org.greencheek.caching.herdcache.memcached.config.builder.ElastiCacheCacheConfigBuilder;
import org.greencheek.caching.herdcache.memcached.keyhashing.KeyHashingType;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.AsciiXXHashAlogrithm;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.JenkinsHash;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.XXHashAlogrithm;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonFactory;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonWrapper;
import org.greencheek.caching.herdcache.memcached.util.TestCacheValues;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 *
 */
public class TestUserSuppliedExpiryMemcachedCaching {

    private MemcachedDaemonWrapper memcached;
    private ListeningExecutorService executorService;
    private SerializableOnlyCacheWithUserSuppliedExpiry<String> cache;
    private String cacheKey ="key1";

    @Before
    public void setUp() {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

        memcached = MemcachedDaemonFactory.createMemcachedDaemon(false);

        if (memcached.getDaemon() == null) {
            throw new RuntimeException("Unable to start local memcached");
        }


        cache = new SpyMemcachedCache<String>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ZERO)
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setKeyHashType(KeyHashingType.NONE)
                        .setKeyPrefix(Optional.of("elastic"))
                        .buildMemcachedConfig()
        );


    }

    @After
    public void tearDown() {
        if (memcached != null) {
            memcached.getDaemon().stop();
        }

        if (cache != null && cache instanceof RequiresShutdown) {
            ((RequiresShutdown) cache).shutdown();
        }
    }

    private <V extends Serializable> ListenableFuture<V> getValueNoWrite(SerializableOnlyCacheWithUserSuppliedExpiry<V> cache,
                               long ttl,
                               Supplier<V> function) {

        return cache.getOrSet(cacheKey,function,ttl,IsCachedValueUsable.CACHED_VALUE_IS_ALWAYS_USABLE,(X) -> false, executorService);
    }

    private <V extends Serializable> ListenableFuture<V> getValueNoWrite(SerializableOnlyCacheWithUserSuppliedExpiry<V> cache,
                                                                         Duration ttl,
                                                                         Supplier<V> function) {

        return cache.getOrSet(cacheKey,function,ttl,CacheWithExpiry.CACHED_VALUE_IS_ALWAYS_VALID,(X) -> false, executorService);
    }

    private <V extends Serializable> ListenableFuture<V> getValue(SerializableOnlyCacheWithUserSuppliedExpiry<V> cache,
                                                                  Duration ttl,
                                                                  Supplier<V> function) {

        return cache.getOrSet(cacheKey,function,ttl,CacheWithExpiry.CACHED_VALUE_IS_ALWAYS_VALID,CacheWithExpiry.CAN_ALWAYS_CACHE_VALUE, executorService);
    }

    private <V extends Serializable> ListenableFuture<V> getValue(SerializableOnlyCacheWithUserSuppliedExpiry<V> cache,
                                                                  long ttl,
                                                                  Supplier<V> function) {

        return cache.getOrSet(cacheKey,function,ttl,CacheWithExpiry.CACHED_VALUE_IS_ALWAYS_VALID,CacheWithExpiry.CAN_ALWAYS_CACHE_VALUE, executorService);
    }

    private void testItemOnlyExpiresForUserSuppliedExpiryTTL(HashAlgorithm algo) {
        cache = new SpyMemcachedCache<String>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(1))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setHashAlgorithm(algo)
                        .setKeyPrefix(Optional.of("elastic"))
                        .buildMemcachedConfig()
        );


        try {
            ListenableFuture<String> value = cache.getOrSet(cacheKey, () -> {
                return "cached version";
            }, executorService);
            assertEquals("value should be generated", "cached version",value.get());
        } catch (Exception e) {
            fail("unable to generate tests value");
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            fail("unable to sleep, in order to test cache expiry");
        }

        try {
            String value = cache.get(cacheKey).get();
            assertEquals("value should be from the cache", "cached version",value);
        } catch (Exception e) {
            fail("unable to obtain cached value");
        }

        Supplier<String> supplier = () ->{ return "supplier value";};

        assertEquals("Value should be from the supplier", "supplier value",
                cache.awaitForFutureOrElse(getValueNoWrite(cache, 1000, supplier), null));

        assertEquals("Value should not be from the supplier", "cached version",
                cache.awaitForFutureOrElse(getValue(cache,10000,supplier), null));

        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());

    }

    @Test
    public void testJenkinsHashAlgorithm() {
        testItemOnlyExpiresForUserSuppliedExpiryTTL(new JenkinsHash());
    }


    @Test
    public void testXXHashAlgorithm() {
        testItemOnlyExpiresForUserSuppliedExpiryTTL(new XXHashAlogrithm());
    }

    @Test
    public void testAsciiXXHashAlgorithm() {
        testItemOnlyExpiresForUserSuppliedExpiryTTL(new AsciiXXHashAlogrithm());
    }

    @Test
    public void testItemOnlyExpiresForUserSuppliedExpiryTTLAsADuration() {
        cache = new SpyMemcachedCache<String>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(1))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setKeyPrefix(Optional.of("elastic"))
                        .buildMemcachedConfig()
        );


        try {
            ListenableFuture<String> value = cache.getOrSet(cacheKey, () -> {
                return "cached version";
            }, executorService);
            assertEquals("value should be generated", "cached version",value.get());
        } catch (Exception e) {
            fail("unable to generate tests value");
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            fail("unable to sleep, in order to test cache expiry");
        }

        try {
            String value = cache.get(cacheKey).get();
            assertEquals("value should be from the cache", "cached version",value);
        } catch (Exception e) {
            fail("unable to obtain cached value");
        }

        Supplier<String> supplier = () ->{ return "supplier value";};

        assertEquals("Value should be from the supplier", "supplier value",
                cache.awaitForFutureOrElse(getValueNoWrite(cache, Duration.ofMillis(1000), supplier), null));

        assertEquals("Value should not be from the supplier", "cached version",
                cache.awaitForFutureOrElse(getValue(cache,Duration.ofMillis(10000),supplier), null));

        assertEquals(1, memcached.getDaemon().getCache().getCurrentItems());

    }

    @Test
    public void testUserSuppliedCachedItemPredicate() {
        cache = new SpyMemcachedCache<String>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(1))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setKeyHashType(KeyHashingType.NONE)
                        .setKeyPrefix(Optional.of("elastic"))
                        .buildMemcachedConfig()
        );

        final String supplierValueString = "supplier_value";
        final String helloWorld = "hello world";
        Supplier<String> random = () -> UUID.randomUUID().toString();
        Supplier<String> supplierValue = () -> supplierValueString;
        Supplier<String> helloSupplier = () -> helloWorld;

        try {
            ListenableFuture<String> value = cache.getOrSet(cacheKey, () -> {
                return "cached version";
            }, IsCachedValueUsable.CACHED_VALUE_IS_ALWAYS_USABLE, IsSupplierValueCachable.GENERATED_VALUE_IS_ALWAYS_CACHABLE, executorService);
            assertEquals("value should be generated", "cached version",value.get());
        } catch (Exception e) {
            fail("unable to generate tests value");
        }


        assertEquals("Value should not be from the supplier", "cached version",
                cache.awaitForFutureOrElse(cache.getOrSet(cacheKey, random,
                         (CachedItem<String> item) -> {
                             return "cached version".equals(item.getCachedItem());
                         },
                         executorService),null));

        assertNotEquals("Value should be from the supplier", "cached version",
                cache.awaitForFutureOrElse(cache.getOrSet(cacheKey, supplierValue,
                        (CachedItem<String> item) -> {
                            return !"cached version".equals(item.getCachedItem());
                        },
                        executorService), null));

        assertEquals("Value should be from the cache", supplierValueString,
                cache.awaitForFutureOrElse(cache.getOrSet(cacheKey, random,
                        (CachedItem<String> item) -> {
                            return item.getCreationDate().isBefore(Instant.now());
                        },
                        executorService),null));

        assertEquals("Value should be from the cache", helloWorld,
                cache.awaitForFutureOrElse(cache.getOrSet(cacheKey, helloSupplier,
                        (CachedItem<String> item) -> {
                            return item.getCreationDate().isAfter(Instant.now());
                        },
                        (x) -> false,
                        executorService),null));

        assertEquals("Value should be from the cache", supplierValueString,
                cache.awaitForFutureOrElse(cache.getOrSet(cacheKey, helloSupplier,
                        (CachedItem<String> item) -> {
                            return item.getCreationDate().isBefore(Instant.now());
                        },
                        (x) -> false,
                        executorService),null));

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            fail("Unable to wait for 200 millis");
        }

        assertNotEquals("Value should not be from the cache", supplierValueString,
                cache.awaitForFutureOrElse(cache.getOrSet(cacheKey, random,
                        (CachedItem<String> item) -> {
                            return !item.hasExpired(Duration.ofMillis(100));
                        },
                        (x) -> false,
                        executorService), null));


    }


    @Test
    public void testBasicUserSuppliedTTLIsHonouredGetAndSet() throws Exception {
        Duration twoHundredMillis = Duration.ofMillis(200);

        AtomicInteger timesCalled = new AtomicInteger(0);

        String s = cache.getOrSet("item",() -> { timesCalled.incrementAndGet(); return "bob"; } , twoHundredMillis ).get();

        assertEquals("bob",s);

        assertEquals("bob", cache.getOrSet("item", () -> {
            timesCalled.incrementAndGet();
            return "bob";
        }, 200).get());


        assertEquals("bob",cache.getOrSet("item",() -> { timesCalled.incrementAndGet(); return "bob"; } , twoHundredMillis ).get());


        Thread.sleep(500);

        assertEquals("bob",cache.getOrSet("item",() -> { timesCalled.incrementAndGet(); return "bob"; } , twoHundredMillis ).get());

        assertEquals(2, timesCalled.get());

    }

    @Test
    public void testCanCacheValueIsCalled() throws Exception {

        //     default public ListenableFuture<V> getOrSet(String key, Supplier<V> theBackEndCall,
        // Predicate<V> canUseCachedValue,Predicate<V> shouldCacheBackendCall,ListeningExecutorService executor ) {

        Duration twoHundredMillis = Duration.ofMillis(200);
        AtomicInteger canUseCachedValueCalled = new AtomicInteger(0);

        AtomicInteger timesBackendCalled = new AtomicInteger(0);

        Supplier<String> backendCall = () -> { int val = timesBackendCalled.incrementAndGet(); return "bob" + val;};

        IsCachedValueUsable<String> canCacheValue = (String v) -> { canUseCachedValueCalled.incrementAndGet();return !v.equals("bob2");};

        assertEquals("bob1", cache.getOrSet("item",backendCall , twoHundredMillis,canCacheValue ).get());

        assertEquals("bob1", cache.getOrSet("item", backendCall, twoHundredMillis,canCacheValue).get());


        assertEquals("bob1",cache.getOrSet("item",backendCall, twoHundredMillis ,canCacheValue).get());

        Thread.sleep(500);

        assertEquals("bob2",cache.getOrSet("item",backendCall , twoHundredMillis ,canCacheValue).get());

        assertEquals(2, timesBackendCalled.get());

        assertEquals("bob3",cache.getOrSet("item",backendCall , twoHundredMillis ,canCacheValue).get());

        assertEquals(3, timesBackendCalled.get());

        assertEquals(3,canUseCachedValueCalled.get());
    }

    @Test
    public void testCanCacheValueIsCalledAndCacheValueShouldBeUsed() throws Exception {

        //     default public ListenableFuture<V> getOrSet(String key, Supplier<V> theBackEndCall,
        // Predicate<V> canUseCachedValue,Predicate<V> shouldCacheBackendCall,ListeningExecutorService executor ) {

        Duration twoHundredMillis = Duration.ofMillis(300);


        AtomicInteger valueStoredInCachedIsToBeUsedCounter = new AtomicInteger(0);

        AtomicInteger canCacheBackendValueCounter = new AtomicInteger(0);

        AtomicInteger timesBackendCalled = new AtomicInteger(0);


        Supplier<String> backendCall = () -> {
            int val = timesBackendCalled.incrementAndGet();
            return "bob" + val;
        };

        IsSupplierValueCachable<String> canCacheValue = (String v) -> {
            canCacheBackendValueCounter.incrementAndGet();
            return !v.equals("bob2");
        };

        IsCachedValueUsable<String> shouldUseCachedValue = (String v) -> {
            valueStoredInCachedIsToBeUsedCounter.incrementAndGet();
            return !v.equals("bob1");
        };


        // Nothing is cached yet, return the value from backend, this can be cached as it is not bob2
        assertEquals("bob1", cache.getOrSet("item", backendCall, twoHundredMillis.toMillis(), shouldUseCachedValue, canCacheValue).get());

        // bob1 is cached, but we shouldn't use this value, therefore we need to call the backend.  However, bob2 should not be cached
        assertEquals("bob2", cache.getOrSet("item", backendCall, twoHundredMillis, shouldUseCachedValue, canCacheValue).get());

        // bob1 is cached, but we shouldn't use this value.  Therefore call the backend.  This returns bob3 which is cacheable
        assertEquals("bob3", cache.getOrSet("item", backendCall, twoHundredMillis, shouldUseCachedValue, canCacheValue).get());

        Thread.sleep(500);

        // nothing is in the cache.  Call the backend.  This returns bob4 which is cachable
        assertEquals("bob4", cache.getOrSet("item", backendCall, twoHundredMillis, shouldUseCachedValue, canCacheValue).get());

        assertEquals(4, timesBackendCalled.get());

        assertEquals("bob4", cache.getOrSet("item", backendCall, twoHundredMillis, shouldUseCachedValue, canCacheValue, executorService).get());

        assertEquals(4, timesBackendCalled.get());

        assertEquals(4, canCacheBackendValueCounter.get());

        assertEquals(3, valueStoredInCachedIsToBeUsedCounter.get());
    }

    @Test
    public void testNoExpiryIsSetOnCachedItem() throws Exception {

        AtomicInteger timesBackendCalled = new AtomicInteger(0);


        Supplier<String> backendCall = () -> {
            int val = timesBackendCalled.incrementAndGet();
            return "bob" + val;
        };

        assertEquals("bob1",cache.getOrSet("item",backendCall).get());

        assertEquals("bob1",cache.getOrSet("item",backendCall,executorService).get());

        assertEquals("bob2", cache.getOrSet("item", backendCall, (String v) -> !v.equals("bob1")).get());

        assertEquals("bob3", cache.getOrSet("item", backendCall, (String v) -> !v.equals("bob2"), (String v) -> !v.equals("bob3")).get());

        assertEquals("bob2", cache.getOrSet("item", backendCall).get());

        assertEquals("bob2", cache.getOrSet("item", backendCall, (CachedItem<String> v) -> !v.hasExpired(1000),executorService).get());

    }

    @Test
    public void testNoExpiryIsSetOnCachedItemLargeValue() throws Exception {

        AtomicInteger timesBackendCalled = new AtomicInteger(0);


        Supplier<String> backendCall = () -> {
            int val = timesBackendCalled.incrementAndGet();
            return TestCacheValues.LARGE_CACHE_VALUE;
        };

        assertEquals(TestCacheValues.LARGE_CACHE_VALUE,cache.getOrSet("item",backendCall).get());

        assertEquals(TestCacheValues.LARGE_CACHE_VALUE,cache.getOrSet("item2",backendCall,executorService).get());



    }

}
