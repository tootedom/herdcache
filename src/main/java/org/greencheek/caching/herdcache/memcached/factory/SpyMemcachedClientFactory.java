package org.greencheek.caching.herdcache.memcached.factory;

import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.MemcachedClientIF;
import org.greencheek.caching.herdcache.memcached.config.Host;
import org.greencheek.caching.herdcache.memcached.config.hostparsing.HostStringParser;
import org.greencheek.caching.herdcache.memcached.dns.lookup.HostResolver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Created by dominictootell on 24/08/2014.
 */
public class SpyMemcachedClientFactory implements MemcachedClientFactory {

    private final ReferencedClient memcached;
    private final HostStringParser hostStringParser;
    private final Duration dnsConnectionTimeout;
    private final HostResolver hostResolver;
    private final ConnectionFactory connectionFactory;

    public SpyMemcachedClientFactory(String memcachedHosts,
                                     Duration dnsConnectionTimeout,
                                     HostStringParser hostStringParser,
                                     HostResolver hostResolver,
                                     ConnectionFactory connnectionFactory) {
        this.hostStringParser = hostStringParser;
        this.dnsConnectionTimeout = dnsConnectionTimeout;
        this.hostResolver = hostResolver;
        this.connectionFactory = connnectionFactory;
        this.memcached = createMemcachedClient(memcachedHosts);


    }


    private ReferencedClient createMemcachedClient(String hosts) {
        List<Host> parsedHosts  =  hostStringParser.parseMemcachedNodeList(hosts);

        if(parsedHosts==null || parsedHosts.size()==0) {
            return ReferencedClient.UNAVAILABLE_REFERENCE_CLIENT;
        } else {
            List<InetSocketAddress> resolvedHosts = hostResolver.returnSocketAddressesForHostNames(parsedHosts,dnsConnectionTimeout);
            if(resolvedHosts==null || resolvedHosts.size()==0) {
                return ReferencedClient.UNAVAILABLE_REFERENCE_CLIENT;
            } else {
                try {
                    return new ReferencedClient(true,resolvedHosts,new MemcachedClient(connectionFactory,resolvedHosts));
                } catch (IOException e) {
                    return ReferencedClient.UNAVAILABLE_REFERENCE_CLIENT;
                }
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
            memcached.getClient().shutdown();
        }
    }
}
