package org.greencheek.caching.herdcache.memcached.factory;

import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.MemcachedClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * Created by dominictootell on 05/04/2015.
 */
public class SpyMemcachedReferencedClientFactory<V> implements ReferencedClientFactory<V>{
    private final ConnectionFactory factory;

    public SpyMemcachedReferencedClientFactory(ConnectionFactory factory) {
        this.factory = factory;
    }


    @Override
    public ReferencedClient createClient(List<InetSocketAddress> resolvedHosts) {
        try {
            return new SpyReferencedClient<V>(true,resolvedHosts,new MemcachedClient(factory,resolvedHosts));
        } catch (IOException e) {
            return SpyReferencedClient.UNAVAILABLE_REFERENCE_CLIENT;
        }
    }
}
