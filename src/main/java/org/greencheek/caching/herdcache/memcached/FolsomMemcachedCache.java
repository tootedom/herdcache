package org.greencheek.caching.herdcache.memcached;

import org.greencheek.caching.herdcache.memcached.config.ElastiCacheCacheConfig;
import org.greencheek.caching.herdcache.memcached.config.MemcachedCacheConfig;
import org.greencheek.caching.herdcache.memcached.factory.FolsomReferencedClientFactory;
import org.greencheek.caching.herdcache.memcached.factory.MemcachedClientFactory;
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

    @Override
    public MemcachedClientFactory buildClientFactory(Object cfg) {
        ElastiCacheCacheConfig config = (ElastiCacheCacheConfig) cfg;

        return new SpyMemcachedClientFactory<V>(config.getMemcachedCacheConfig().getMemcachedHosts(),
                config.getMemcachedCacheConfig().getDnsConnectionTimeout(), config.getMemcachedCacheConfig().getHostStringParser(),
                config.getMemcachedCacheConfig().getHostResolver(), new FolsomReferencedClientFactory<V>(config));
    }
}