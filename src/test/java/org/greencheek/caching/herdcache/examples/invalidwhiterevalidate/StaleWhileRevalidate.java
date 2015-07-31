package org.greencheek.caching.herdcache.examples.invalidwhiterevalidate;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import net.spy.memcached.ConnectionFactoryBuilder;
import org.greencheek.caching.herdcache.IsSupplierValueCachable;
import org.greencheek.caching.herdcache.RequiresShutdown;
import org.greencheek.caching.herdcache.RevalidateInBackgroundCapableCache;
import org.greencheek.caching.herdcache.memcached.SpyMemcachedCache;
import org.greencheek.caching.herdcache.memcached.config.builder.ElastiCacheCacheConfigBuilder;
import org.greencheek.caching.herdcache.memcached.keyhashing.KeyHashingType;

import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 * Example for background refresh.
 */
public class StaleWhileRevalidate {


    /**
     * Wraps a user supplied cache value.  It is the CachedItemWithCreationDate object that is
     * then stored in the cache (i.e. memcached).  The CachedItemWithCreationDate has one extra
     * instance member, that of a {@link java.time.Instant} that represents the time the
     * CachedItemWithCreationDate was created ({@link #getCreationDate()}.
     * The instant is recorded in UTC ({@link java.time.Clock#systemUTC()}).
     */
    public static class CacheableItemWithCreationDate<V extends Serializable> implements Serializable {

        /**
         * Serialization version.
         */
        private static final long serialVersionUID = -695733675816600378L;
        private static Clock UTC = Clock.systemUTC();

        private final V cachedItem;
        private final Instant creationDate;

        /**
         * Boolean to mark if this object was generated as a
         * Default Item, in response to an error condition
         */
        private final boolean fallback;


        /**
         * When returning a fallback item, should this indicate that
         * we check a cache for an overriding item, or use this fallback item.
         * @param item
         */
        private final boolean checkForCachedItemWhenFallback;

        public CacheableItemWithCreationDate(V item) {
            this(item,false,false);
        }

        public CacheableItemWithCreationDate(V item, boolean fallback) {
            this(item,fallback, fallback==true ? true : false);
        }
        public CacheableItemWithCreationDate(V item, boolean fallback, boolean checkForCachedItem) {
            this.cachedItem = item;
            creationDate = Instant.now(UTC);
            this.fallback = fallback;
            if(!fallback) {
                this.checkForCachedItemWhenFallback = true;
            } else {
                this.checkForCachedItemWhenFallback = checkForCachedItem;
            }
        }

        /**
         * Is this instance actually a generic object that represents a failure condition, and
         * as a result contains a generic piece of content that will be returned by {@link #getCachedItem()}
         * @return
         */
        public boolean isFallback() {
            return fallback;
        }

        public boolean shouldCheckForCachedItemWhenFallback() {
            return checkForCachedItemWhenFallback;
        }

        public V getCachedItem() {
            return cachedItem;
        }

        /**
         * Given a time in millis, checks if the CacheItem's age is less that the given millis old.
         * The millis is basically added to the time when the
         * cached item was created and compares it against current time (in UTC)
         *
         * Returns true if the item is still less than given ttl in millis (i.e. is still alive)
         *
         * @param ttlInMillis
         * @return
         */
        public boolean isLive(long ttlInMillis) {
            Instant now = Instant.now(UTC);
            return creationDate.plusMillis(ttlInMillis).isAfter(now);
        }

        /**
         * Given a duration, this is converted to millis and {@link #isLive(long)} is called
         * @param ttl
         * @return
         */
        public boolean isLive(Duration ttl) {
            return isLive(ttl.toMillis());
        }

        /**
         * Returns true if the age of the CachedItem is more than the given ttlInMillis. i.e. The item is Expired
         * @param ttlInMillis
         * @return
         */
        public boolean hasExpired(long ttlInMillis) {
            return !isLive(ttlInMillis);
        }

        /**
         * Returns true if the age of the CachedItem is more than the given Duration. i.e. The item is Expired
         * @param ttl
         * @return
         */
        public boolean hasExpired(Duration ttl) {
            return hasExpired(ttl.toMillis());
        }

        public Instant getCreationDate() {
            return creationDate;
        }
    }



    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1)));

        RevalidateInBackgroundCapableCache<CacheableItemWithCreationDate<String>> cache = null;

        cache = new SpyMemcachedCache<>(
                new ElastiCacheCacheConfigBuilder()
                        .setMemcachedHosts("localhost:11211")
                        .setTimeToLive(Duration.ofSeconds(0))
                        .setProtocol(ConnectionFactoryBuilder.Protocol.BINARY)
                        .setKeyHashType(KeyHashingType.NONE)
                        .setUseStaleCache(false)
                        .setWaitForMemcachedSet(true)
                        .buildMemcachedConfig()
        );

        // Store KEY1 with value of KEY1_VALUE1 for ever in the cache
        ListenableFuture<CacheableItemWithCreationDate<String>> future = cache.apply("KEY1",
                () -> new CacheableItemWithCreationDate<>("KEY1_VALUE1"),
                Duration.ZERO,
                executor,
                IsSupplierValueCachable.GENERATED_VALUE_IS_ALWAYS_CACHABLE,
                (CacheableItemWithCreationDate<String> cachedItem) -> cachedItem.isLive(Duration.ofSeconds(3)),
                true);

        System.out.println(future.get().getCachedItem());

        // Lets say some time passes, so that we can now say:
        // If the cached item is older than 3 seconds, the item should be refreshed in the background
        Thread.sleep(5000);


        // Store KEY1 with value of KEY1_VALUE2 for ever in the cache, but return the previous value KEY1_VALUE1
        ListenableFuture<CacheableItemWithCreationDate<String>> stalefuture = cache.apply("KEY1",
                () -> new CacheableItemWithCreationDate<>("KEY1_VALUE2"),
                Duration.ZERO,executor,
                IsSupplierValueCachable.GENERATED_VALUE_IS_ALWAYS_CACHABLE,
                (CacheableItemWithCreationDate<String> cachedItem) -> cachedItem.isLive(Duration.ofSeconds(3)),true);

        System.out.println(stalefuture.get().getCachedItem());

        // Wait for the value to be refreshed in the background
        Thread.sleep(200);


        System.out.println(cache.get("KEY1").get().getCachedItem());

        // output will be:
        // KEY1_VALUE1
        // KEY1_VALUE1
        // KEY1_VALUE2


        // Shutdown the cache and the executor
        executor.shutdownNow();
        ((RequiresShutdown)cache).shutdown();
    }
}
