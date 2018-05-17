package org.greencheek.caching.herdcache.memcached.dns.lookup;

import org.greencheek.caching.herdcache.memcached.config.Host;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;

/**
 * Created by dominictootell on 06/06/2014.
 */
public interface HostResolver {
    public static Duration DEFAULT_DNS_TIMEOUT = Duration.ofSeconds(3);

    public List<InetSocketAddress> returnSocketAddressesForHostNames(List<Host> nodes);

    public List<InetSocketAddress> returnSocketAddressesForHostNames(List<Host> nodes, Duration dnsLookupTimeout);

}
