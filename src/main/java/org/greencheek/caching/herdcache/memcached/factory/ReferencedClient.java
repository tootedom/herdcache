package org.greencheek.caching.herdcache.memcached.factory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by dominictootell on 05/04/2015.
 */
public interface ReferencedClient<V> {

    boolean isAvailable();
    List<InetSocketAddress> getResolvedHosts();

    V get(String key, long time,TimeUnit unit);
    Future set(String key, int ttlInSeconds, V value);
    Future delete(String key);
    Future flush();

    void shutdown();
}
