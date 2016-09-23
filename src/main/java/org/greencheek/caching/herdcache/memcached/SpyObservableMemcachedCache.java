package org.greencheek.caching.herdcache.memcached;

import org.greencheek.caching.herdcache.memcached.config.MemcachedCacheConfig;
import org.greencheek.caching.herdcache.memcached.factory.SpyMemcachedClientFactory;
import org.greencheek.caching.herdcache.memcached.factory.SpyMemcachedReferencedClientFactory;

import java.io.Serializable;

/**
 * Created by dominictootell on 26/08/2014.
 */
public class SpyObservableMemcachedCache<V extends Serializable> extends BaseObservableMemcachedCache<V> {
    public SpyObservableMemcachedCache(MemcachedCacheConfig config) {
        super(new SpyMemcachedClientFactory<V>(config.getMemcachedHosts(),
                config.getDnsConnectionTimeout(),config.getHostStringParser(),
                config.getHostResolver(),new SpyMemcachedReferencedClientFactory<V>(createMemcachedConnectionFactory(config))), config);
    }
}
