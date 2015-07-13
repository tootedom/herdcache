package org.greencheek.caching.herdcache.memcached.factory;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * creates a ReferencedClient object
 */
public interface ReferencedClientFactory<V> {
    ReferencedClient createClient(List<InetSocketAddress> resolvedHosts);
}
