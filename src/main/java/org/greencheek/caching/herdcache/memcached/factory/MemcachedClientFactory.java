package org.greencheek.caching.herdcache.memcached.factory;

import net.spy.memcached.MemcachedClientIF;

/**
 *
 */
public interface MemcachedClientFactory {
    public ReferencedClient getClient();
    public boolean isEnabled();
    public void shutdown();
}
