package org.greencheek.caching.herdcache.memcached.factory;

import net.spy.memcached.MemcachedClientIF;

/**
 * Created by dominictootell on 24/08/2014.
 */
public interface MemcachedClientFactory {
    public MemcachedClientIF getClient();
    public boolean isEnabled();
    public void shutdown();
}
