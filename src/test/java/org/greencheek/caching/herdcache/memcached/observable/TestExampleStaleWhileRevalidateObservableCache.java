package org.greencheek.caching.herdcache.memcached.observable;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import net.spy.memcached.ConnectionFactoryBuilder;
import org.greencheek.caching.herdcache.ObservableCache;
import org.greencheek.caching.herdcache.RequiresShutdown;
import org.greencheek.caching.herdcache.domain.CacheItem;
import org.greencheek.caching.herdcache.memcached.SpyObservableMemcachedCache;
import org.greencheek.caching.herdcache.memcached.config.KeyValidationType;
import org.greencheek.caching.herdcache.memcached.config.builder.ElastiCacheCacheConfigBuilder;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonFactory;
import org.greencheek.caching.herdcache.memcached.util.MemcachedDaemonWrapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import rx.Scheduler;
import rx.Single;
import rx.schedulers.Schedulers;

import java.time.Duration;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * Example of Stale While Revalidate with RxJava Observable cache
 */
public class TestExampleStaleWhileRevalidateObservableCache {


    private MemcachedDaemonWrapper memcached;
    private ObservableCache<String> cache;

    private String key = "Key";
    private String staleValue = "StaleValue";
    private String newValue = "newValue";
    private Predicate<String> staleValueChecker = (value) -> staleValue.equals(value);
    private StaleRevalidator revalidator;


    @Before
    public void setUp() {

        memcached = MemcachedDaemonFactory.createMemcachedDaemon(false);

        if(memcached.getDaemon()==null) {
            throw new RuntimeException("Unable to start local memcached");
        }

        cache = new SpyObservableMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:" + memcached.getPort())
                        .setTimeToLive(Duration.ofSeconds(10))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                        .setWaitForMemcachedSet(true)
                        .setKeyValidationType(KeyValidationType.NONE)
                        .buildMemcachedConfig()
        );

        revalidator = new StaleRevalidator(cache,staleValueChecker);

    }

    @After
    public void tearDown() {
        if(memcached!=null) {
            memcached.getDaemon().stop();
        }
        cache.shutdown();
    }

    class StaleRevalidator {
        private final AtomicInteger timesRevalidationRun = new AtomicInteger(0);
        private final ConcurrentMap<String,String> validationRunning =  new ConcurrentLinkedHashMap.Builder()
                                                                        .initialCapacity(10)
                                                                        .maximumWeightedCapacity(10)
                                                                        .build();

        private final ObservableCache<String> cache;
        private final Predicate<String> isStaleChecker;

        public StaleRevalidator(ObservableCache<String> cache, Predicate<String> isStaleChecker) {
            this.cache = cache;
            this.isStaleChecker = isStaleChecker;
        }

        public int timesRevalidationRun() {
            return timesRevalidationRun.get();
        }

        public void revalidate(String keyString,
                               String value,
                               Supplier<String> supplierProvidingNewValueToCache,
                               Scheduler scheduler) {

            if(isStaleChecker.test(value)) {
                if (validationRunning.putIfAbsent(keyString, keyString) == null) {
                    Single<CacheItem<String>> revalidation = cache.set(keyString, supplierProvidingNewValueToCache, Duration.ZERO);
                    // Where we run the supplier calculation that "refreshes the cache"
                    revalidation = revalidation.observeOn(scheduler);
                    // run the subscriber on a different thread to main
                    revalidation = revalidation.subscribeOn(scheduler);
                    revalidation.subscribe(newValue -> {
                        System.out.println(Thread.currentThread().getName());
                        timesRevalidationRun.incrementAndGet();
                        validationRunning.remove(keyString);
                    },(throwable) -> {
                        validationRunning.remove(keyString);
                    });
                }
            }
        }
    }

    private void simulateManyRequests(final String key, final AtomicInteger counter) {
        Single<CacheItem<String>> item = cache.get(key);
        //
        // The Gets run on a different thread to that of the revalidate.
        // As a result the re-validation is occurring in the background, separately from the gets (requests)
        //
        item = item.observeOn(Schedulers.computation());
        item.subscribeOn(Schedulers.computation()).subscribe(value -> {
            final String cachedValue = value.value();

            if (staleValueChecker.test(cachedValue)) {
                counter.incrementAndGet();
            }

            revalidator.revalidate(key, cachedValue, () -> {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName());
                return newValue;
            },Schedulers.io());

        });
    }

    @Test
    public void testStaleWhileRevalidate() {


        // Write an item to the cache (setup)
        Single<CacheItem<String>> val = cache.set(key, staleValue,Duration.ZERO);
        assertEquals("Value should be 'value1'", staleValue, val.toBlocking().value().value());

        final AtomicInteger originalStaleValueFound = new AtomicInteger(0);

        // Example of many requests flowing into the application
        for (int i = 0; i<100; i++) {
            // These all happen in a background thread.
            simulateManyRequests(key,originalStaleValueFound);
        }

        // We sleep
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertEquals("We expect that the stale value is always found",100,originalStaleValueFound.get());
        assertEquals("We expect that the stale value refreshed once", 1, revalidator.timesRevalidationRun());
        assertEquals("The stale value should have been updated to:"+newValue, newValue, cache.get(key).toBlocking().value().value());


    }

}
