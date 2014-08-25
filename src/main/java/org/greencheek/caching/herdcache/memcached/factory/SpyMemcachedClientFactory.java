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

    private final Optional<MemcachedClientIF> memcached;
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


    private Optional<MemcachedClientIF> createMemcachedClient(String hosts) {
        List<Host> parsedHosts  =  hostStringParser.parseMemcachedNodeList(hosts);

        if(parsedHosts==null || parsedHosts.size()==0) {
            return Optional.empty();
        } else {
            List<InetSocketAddress> resolvedHosts = hostResolver.returnSocketAddressesForHostNames(parsedHosts,dnsConnectionTimeout);
            if(resolvedHosts==null || resolvedHosts.size()==0) {
                return Optional.empty();
            } else {
                try {
                    return Optional.of(new MemcachedClient(connectionFactory,resolvedHosts));
                } catch (IOException e) {
                    return Optional.empty();
                }
            }
        }
    }

    @Override
    public MemcachedClientIF getClient() {
        return memcached.orElse(null);
    }

    @Override
    public boolean isEnabled() {
        return memcached.isPresent();
    }

    @Override
    public void shutdown() {
        if(isEnabled()) {
            memcached.get().shutdown();
        }
    }
}
