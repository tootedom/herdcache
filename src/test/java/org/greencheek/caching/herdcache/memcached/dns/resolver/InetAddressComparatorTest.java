package org.greencheek.caching.herdcache.memcached.dns.resolver;


import org.junit.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Comparator;

import static org.junit.Assert.assertEquals;

public class InetAddressComparatorTest {

    Comparator<InetAddress> comparator = new InetAddressComparator();


    @Test
    public void testSorting() throws UnknownHostException {
        InetAddress[] addresses = new InetAddress[3];

        addresses[0] = InetAddress.getByName("127.0.0.100");
        addresses[1] = InetAddress.getByName("127.0.0.2");
        addresses[2] = InetAddress.getByName("127.0.0.1");

        Arrays.sort(addresses, comparator);
        assertEquals("127.0.0.1",addresses[0].getHostAddress());
        assertEquals("127.0.0.2",addresses[1].getHostAddress());
        assertEquals("127.0.0.100",addresses[2].getHostAddress());
    }

}