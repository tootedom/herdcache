package org.greencheek.caching.herdcache.memcached;

import org.greencheek.caching.herdcache.memcached.config.MemcachedCacheConfig;
import org.greencheek.caching.herdcache.memcached.factory.DynamicSpyMemcachedClientFactory;
import org.greencheek.caching.herdcache.memcached.factory.MemcachedClientFactory;
import org.greencheek.caching.herdcache.memcached.factory.SpyMemcachedClientFactory;
import org.greencheek.caching.herdcache.memcached.factory.SpyMemcachedReferencedClientFactory;

import java.io.Serializable;

/**
 * Created by dominictootell on 26/08/2014.
 */
public class SpyMemcachedCache<V extends Serializable> extends BaseMemcachedCache<V> {
    public SpyMemcachedCache(MemcachedCacheConfig config) {
        super(config);
    }

    public MemcachedClientFactory buildClientFactory(Object cfg) {
        MemcachedCacheConfig config = (MemcachedCacheConfig)cfg;
        if (config.resolveHostsFromDns()) {
            return new DynamicSpyMemcachedClientFactory<V>(config.getMemcachedHosts(),
                    config.getDurationForResolvingHostsFromDns(),config.getHostStringParser(),
                    new SpyMemcachedReferencedClientFactory<V>(createMemcachedConnectionFactory(config)));
        } else {
            return new SpyMemcachedClientFactory<V>(config.getMemcachedHosts(),
                    config.getDnsConnectionTimeout(),config.getHostStringParser(),
                    config.getHostResolver(),new SpyMemcachedReferencedClientFactory<V>(createMemcachedConnectionFactory(config)));
        }
    }
}
