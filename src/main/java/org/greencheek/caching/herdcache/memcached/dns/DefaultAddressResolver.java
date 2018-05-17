package org.greencheek.caching.herdcache.memcached.dns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Default {@link AddressResolver} that defers to the {@link InetAddress#getAllByName(String)} method for address
 * resolution.
 *
 * If there are no addresses, then an Array with 0 elements is returned.
 */
public class DefaultAddressResolver implements AddressResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAddressResolver.class);

    public static final InetAddress[] EMPTY = new InetAddress[0];

    @Override
    public InetAddress[] resolve(final String host) {

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);

            if (addresses.length == 0) {
                return EMPTY;
            }

            return addresses;
        } catch (UnknownHostException e) {
            LOGGER.warn("Failed to resolve address for '{}'", host, e);
            return EMPTY;
        }
    }
}
