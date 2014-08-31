package org.greencheek.caching.herdcache.memcached.config.builder;

import net.spy.memcached.ConnectionFactory;
import org.greencheek.caching.herdcache.memcached.config.ElastiCacheCacheConfig;
import org.greencheek.caching.herdcache.memcached.config.MemcachedCacheConfig;
import org.greencheek.caching.herdcache.memcached.factory.MemcachedClientFactory;

/**
 * Created by dominictootell on 24/08/2014.
 */
public interface CacheConfigBuilder<T extends CacheConfigBuilder<T>> {
    public ElastiCacheCacheConfig buildElastiCacheMemcachedConfig();
    public MemcachedCacheConfig buildMemcachedConfig();
    public ConnectionFactory createMemcachedConnectionFactory();
    public default T self() {
        return (T)this;
    }
}