package org.greencheek.caching.herdcache.memcached.elasticacheconfig.connection;

import net.spy.memcached.MemcachedClientIF;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

/**
 * Created by dominictootell on 26/08/2014.
 */
public class ReferencedClient {


    public static final ReferencedClient UNAVAILABLE_REFERENCE_CLIENT = new ReferencedClient(false, Collections.<InetSocketAddress>emptyList(),null);

    private final boolean isAvailable;
    private final List<InetSocketAddress> resolvedHosts;
    private final MemcachedClientIF client;


    public ReferencedClient(boolean isAvailable,
                            List<InetSocketAddress> resolvedHosts,
                            MemcachedClientIF client) {
        this.isAvailable = isAvailable;
        this.resolvedHosts = resolvedHosts;
        this.client = client;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public List<InetSocketAddress> getResolvedHosts() {
        return resolvedHosts;
    }

    public MemcachedClientIF getClient() {
        return client;
    }
}
