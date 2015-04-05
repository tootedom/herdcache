package org.greencheek.caching.herdcache.memcached.factory;

import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.MemcachedClient;
import org.greencheek.caching.herdcache.memcached.config.Host;
import org.greencheek.caching.herdcache.memcached.config.hostparsing.HostStringParser;
import org.greencheek.caching.herdcache.memcached.dns.lookup.HostResolver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

/**
 * Created by dominictootell on 24/08/2014.
 */
public class SpyMemcachedClientFactory<V> implements MemcachedClientFactory {

    private final ReferencedClient memcached;
    private final HostStringParser hostStringParser;
    private final Duration dnsConnectionTimeout;
    private final HostResolver hostResolver;
    private final ReferencedClientFactory<V> connectionFactory;

    public SpyMemcachedClientFactory(String memcachedHosts,
                                     Duration dnsConnectionTimeout,
                                     HostStringParser hostStringParser,
                                     HostResolver hostResolver,
                                     ReferencedClientFactory<V> connnectionFactory) {
        this.hostStringParser = hostStringParser;
        this.dnsConnectionTimeout = dnsConnectionTimeout;
        this.hostResolver = hostResolver;
        this.connectionFactory = connnectionFactory;
        this.memcached = createMemcachedClient(memcachedHosts);


    }


    private ReferencedClient createMemcachedClient(String hosts) {
        List<Host> parsedHosts  =  hostStringParser.parseMemcachedNodeList(hosts);

        if(parsedHosts==null || parsedHosts.size()==0) {
            return SpyReferencedClient.UNAVAILABLE_REFERENCE_CLIENT;
        } else {
            List<InetSocketAddress> resolvedHosts = hostResolver.returnSocketAddressesForHostNames(parsedHosts,dnsConnectionTimeout);
            if(resolvedHosts==null || resolvedHosts.size()==0) {
                return SpyReferencedClient.UNAVAILABLE_REFERENCE_CLIENT;
            } else {
                return connectionFactory.createClient(resolvedHosts);
            }
        }
    }

    @Override
    public ReferencedClient getClient() {
        return memcached;
    }

    @Override
    public boolean isEnabled() {
        return memcached.isAvailable();
    }

    @Override
    public void shutdown() {
        if(isEnabled()) {
            memcached.shutdown();
        }
    }
}
