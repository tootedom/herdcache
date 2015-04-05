package org.greencheek.caching.herdcache.memcached;

import org.greencheek.caching.herdcache.memcached.config.MemcachedCacheConfig;
import org.greencheek.caching.herdcache.memcached.factory.SpyMemcachedClientFactory;
import org.greencheek.caching.herdcache.memcached.factory.SpyMemcachedReferencedClientFactory;

/**
 * Created by dominictootell on 26/08/2014.
 */
public class SpyMemcachedCache<V> extends BaseMemcachedCache<V> {
    public SpyMemcachedCache(MemcachedCacheConfig config) {
        super(new SpyMemcachedClientFactory<V>(config.getMemcachedHosts(),
                config.getDnsConnectionTimeout(),config.getHostStringParser(),
                config.getHostResolver(),new SpyMemcachedReferencedClientFactory<V>(createMemcachedConnectionFactory(config))), config);
    }
}
