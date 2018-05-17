package org.greencheek.caching.herdcache.memcached.dns;

import java.net.InetAddress;

/**
 * TODO Document
 */
public interface AddressResolver {

    /**
     * Resolve a host name into one or more IPvX address types.
     *
     * @param host The host to resolve the IP addresses for.
     *
     * @return An array of one or more {@link InetAddress} details for a given host. If an error occurred (such as an
     *         unknown host, or a DNS collision) then an empty result should be returned.
     */
    InetAddress[] resolve(String host);

}