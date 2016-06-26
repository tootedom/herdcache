package org.greencheek.caching.herdcache.memcached.operations;

import org.greencheek.caching.herdcache.memcached.factory.ReferencedClient;

/**
 *
 */
public interface CacheWrite {
    public void writeToDistributedCache(ReferencedClient client,
                                         String key,
                                         Object valueToCache,
                                         int entryTTLInSeconds);

}
