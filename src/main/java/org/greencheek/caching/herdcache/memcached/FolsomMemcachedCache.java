package org.greencheek.caching.herdcache.memcached;

import org.greencheek.caching.herdcache.memcached.config.ElastiCacheCacheConfig;
import org.greencheek.caching.herdcache.memcached.factory.FolsomReferencedClientFactory;
import org.greencheek.caching.herdcache.memcached.factory.SpyMemcachedClientFactory;

import java.io.Serializable;

/**
 *
 */
public class FolsomMemcachedCache<V extends Serializable> extends BaseMemcachedCache<V> {
    public FolsomMemcachedCache(ElastiCacheCacheConfig config) {
        super(new SpyMemcachedClientFactory<V>(config.getMemcachedCacheConfig().getMemcachedHosts(),
                config.getMemcachedCacheConfig().getDnsConnectionTimeout(), config.getMemcachedCacheConfig().getHostStringParser(),
                config.getMemcachedCacheConfig().getHostResolver(), new FolsomReferencedClientFactory<V>(config)), config.getMemcachedCacheConfig());
    }
}