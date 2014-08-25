package org.greencheek.caching.herdcache.memcached.dns.lookup;

import java.net.InetSocketAddress;

/**
 * Created by dominictootell on 29/03/2014.
 */
public interface AddressChecker {
    boolean isAvailable(InetSocketAddress address);
}
