package org.greencheek.caching.herdcache.memcached;

import org.greencheek.caching.herdcache.memcached.config.MemcachedCacheConfig;
import org.greencheek.caching.herdcache.memcached.factory.SpyMemcachedClientFactory;

/**
 * Created by dominictootell on 26/08/2014.
 */
public class SpyMemcachedCache<V> extends BaseMemcachedCache<V> {
    public SpyMemcachedCache(MemcachedCacheConfig config) {
        super(new SpyMemcachedClientFactory(config.getMemcachedHosts(),
                config.getDnsConnectionTimeout(),config.getHostStringParser(),
                config.getHostResolver(),createMemcachedConnectionFactory(config)), config);
    }
}
