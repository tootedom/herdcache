package org.greencheek.caching.herdcache.memcached.factory;

import org.greencheek.caching.herdcache.memcached.config.Host;
import org.greencheek.caching.herdcache.memcached.config.hostparsing.HostStringParser;
import org.greencheek.caching.herdcache.memcached.dns.lookup.HostResolver;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

/**
 * Created by dominictootell on 24/08/2014.
 */
public class DynamicSpyMemcachedClientFactory<V> implements MemcachedClientFactory {

    private final ReferencedClientHolder memcached;
    private final HostStringParser hostStringParser;
    private final ReferencedClientFactory<V> connectionFactory;

    public DynamicSpyMemcachedClientFactory(String memcachedHosts,
                                            Duration pollingForAddressesTime,
                                            HostStringParser hostStringParser,
                                            ReferencedClientFactory<V> connnectionFactory) {
        this.hostStringParser = hostStringParser;
        this.connectionFactory = connnectionFactory;

        List<Host> parsedHosts  =  hostStringParser.parseMemcachedNodeList(memcachedHosts);

        if(parsedHosts==null || parsedHosts.size()==0) {
            throw new InstantiationError("Error Parsing Host String:"+memcachedHosts);
        }

        if (parsedHosts.size()>1){
            throw new InstantiationError("Only one host expected in Host String:"+memcachedHosts);
        }

        memcached = new BackgroundDnsResolver(parsedHosts.get(0),pollingForAddressesTime.toMillis(),connnectionFactory);

    }


    @Override
    public ReferencedClient getClient() {
        return memcached.getClient();
    }

    @Override
    public boolean isEnabled() {
        return memcached.getClient().isAvailable();
    }

    @Override
    public void shutdown() {
        if(isEnabled()) {
            memcached.shutdown();
        }
    }
}
