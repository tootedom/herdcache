package org.greencheek.caching.herdcache.memcached;

import com.google.common.util.concurrent.ListenableFuture;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.HashAlgorithm;
import org.greencheek.caching.herdcache.CacheWithExpiry;
import org.greencheek.caching.herdcache.memcached.config.ElastiCacheCacheConfig;
import org.greencheek.caching.herdcache.memcached.config.MemcachedClientType;
import org.greencheek.caching.herdcache.memcached.config.builder.ElastiCacheCacheConfigBuilder;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.client.ClientClusterUpdateObserver;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.XXHashAlogrithm;
import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by dominictootell on 05/04/2015.
 */
public class TestFolsomMultipleHostsWithNonCachingConfigElastiCacheMemcachedCaching extends TestMultipleHostsWithNonCachingConfigElastiCacheMemcachedCaching {
    CacheWithExpiry<String> createCache(int configServerPort,HashAlgorithm algo,ClientClusterUpdateObserver observer) {
        return createCache(configServerPort,algo,observer,false,false);
    }

    CacheWithExpiry<String> createCache(int configServerPort,HashAlgorithm algo,ClientClusterUpdateObserver observer,
                                                  boolean useStringClient,boolean useFolsomClient) {
        ElastiCacheCacheConfig config = new ElastiCacheCacheConfigBuilder()
                .setElastiCacheConfigHosts("localhost:" + configServerPort)
                .setMemcachedHosts("localhost:" + configServerPort)
                .setConfigPollingTime(Duration.ofSeconds(9))
                .setInitialConfigPollingDelay(Duration.ofSeconds(0))
                .setTimeToLive(Duration.ofSeconds(2))
                .setProtocol(ConnectionFactoryBuilder.Protocol.TEXT)
                .setWaitForMemcachedSet(true)
                .setSetWaitDuration(Duration.ofSeconds(10))
                .setHashAlgorithm(algo)
                .setDelayBeforeClientClose(Duration.ofSeconds(1))
                .setDnsConnectionTimeout(Duration.ofSeconds(2))
                .setUseStaleCache(true)
                .setStaleCacheAdditionalTimeToLive(Duration.ofSeconds(4))
                .setRemoveFutureFromInternalCacheBeforeSettingValue(true)
                .addElastiCacheClientClusterUpdateObserver(observer)
                .setMemcachedClientType(MemcachedClientType.FOLSOM)
                .setUseFolsomStringClient(useStringClient)
                .buildElastiCacheMemcachedConfig();

        if(useFolsomClient) {
            return new FolsomMemcachedCache<>(
                    config
            );
        } else {
            return new ElastiCacheMemcachedCache<>(
                    config
            );
        }
    }

    @Test
    public void testStringClient() {

        cache = createCache(memcached1.getPort(),new XXHashAlogrithm(),(updated) -> {
            // nothing
        },true,true);

        ListenableFuture<String> val = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "value1";
        }, executorService);

        ListenableFuture<String> val2 = cache.apply("Key1", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "value2";
        }, executorService);


        assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val, null));
        assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val2, null));

        assertTrue(memcached1.getDaemon().getCache().getCurrentItems() > 0);

        ListenableFuture<String> val3 = cache.apply("Key1", () -> {
            return "value2";
        }, executorService);

        ListenableFuture<String> val4 = cache.apply("Key1", () -> {
            return "value2";
        }, executorService);

        ListenableFuture<String> val5 = cache.apply("Key1", () -> {
            return "value2";
        }, executorService);

        assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val3, null));
        assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val4, null));
        assertEquals("Value should be key1", "value1", cache.awaitForFutureOrElse(val5, null));

    }
}
