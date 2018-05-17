package org.greencheek.caching.herdcache.memcached.dns.resolver.domain;

import java.net.InetAddress;

/**
 * {@link ResolvedAddresses} holds a collection of resolved {@link InetAddress} details for a single host.
 */
public interface ResolvedAddresses {

    /**
     * Fetch the number of addresses available that can be used to connect to a host.
     *
     * @return The number of available {@link InetAddress} entries for a host.
     */
    int available();

    /**
     * Check whether or not this {@link ResolvedAddresses} has {@link InetAddress} details available.
     *
     * @return {@code true} if the this instance has at least one {@link InetAddress} available to be used, or
     *         {@code false} if there are no {@link InetAddress} details available.
     */
    boolean isEmpty();

    /**
     * Fetch the next available {@link InetAddress}(es) to use for a host.
     *
     * <p>The implementation is free to decide whether to return a single address, or multiple addresses if they are
     * available.</p>
     *
     * @return The next available {@link InetAddress}(es) in the pool, or an empty array if <em>none</em> are available.
     */
    InetAddress[] next();
}
