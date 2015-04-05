package org.greencheek.caching.herdcache.memcached.factory;

/**
 *
 */
public interface MemcachedClientFactory {
    public ReferencedClient getClient();
    public boolean isEnabled();
    public void shutdown();
}
