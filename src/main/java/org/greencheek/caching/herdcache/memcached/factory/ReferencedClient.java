package org.greencheek.caching.herdcache.memcached.factory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by dominictootell on 05/04/2015.
 */
public interface ReferencedClient {

    boolean isAvailable();
    List<InetSocketAddress> getResolvedHosts();

    Object get(String key, long time,TimeUnit unit);
    Future set(String key, int ttlInSeconds, Object value);
    Future delete(String key);
    Future flush();

    void shutdown();
}
