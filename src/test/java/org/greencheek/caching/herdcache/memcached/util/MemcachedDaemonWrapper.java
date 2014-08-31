package org.greencheek.caching.herdcache.memcached.util;

import com.thimbleware.jmemcached.LocalCacheElement;
import com.thimbleware.jmemcached.MemCacheDaemon;

/**
 * Created by dominictootell on 20/04/2014.
 */
public class MemcachedDaemonWrapper {
    private final MemCacheDaemon<LocalCacheElement> daemon;
    private final int port;

    public MemcachedDaemonWrapper(MemCacheDaemon<LocalCacheElement> daemon,
                                  int port) {
        this.daemon = daemon;
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public MemCacheDaemon<LocalCacheElement> getDaemon() {
        return daemon;
    }
}