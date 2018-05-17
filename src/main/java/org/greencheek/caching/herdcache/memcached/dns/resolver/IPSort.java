package org.greencheek.caching.herdcache.memcached.dns.resolver;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

public class IPSort {
    private static String[] TESTS = {"0:0:0:0:0:0:fff:ffff","::FFFF:222.1.41.90",":8:","::::5:6::8","::::5:6::7","::::5:6::8","123..245.23","1...","..1.","123...23",".1..","123..245.23", "123..245.23", "104.244.253.29", "1.198.3.93", "32.183.93.40", "32.183.93.40", "104.30.244.2", "104.244.4.1","0.0.0.1",":a:","::5:3:4:5:6:78","1::2:3","1::2:3:4","1::5:256.2.3.4","1:1:3000.30.30.30","ae80::217:f2ff:254:7:237:98","::2:3:4:5:6:7","2:3:4:5:6:7","::5:3:4:5:6:7:8","::5:3:4:5:6:7:8:9:0","1::8","1::2:3","1::2:3:4","1::5:256.2.3.4","1:1:3000.30.30.30","ae80::217:f2ff:254.7.237.98","1:2:3:4::5:1.2.3.4","2001:0000:1234:0000:0000:C1C0:ABCD:0876","12345::6:7:8","1::1.2.900.4","fe80::","::ffff:0:0"};

    public static class InetAddressComparator implements Comparator<InetAddress> {
        @Override
        public int compare(InetAddress a, InetAddress b) {
            byte[] aOctets = a.getAddress(),
                    bOctets = b.getAddress();
            int len = Math.max(aOctets.length, bOctets.length);
            for (int i = 0; i < len; i++) {
                byte aOctet = (i >= len - aOctets.length) ?
                        aOctets[i - (len - aOctets.length)] : 0;
                byte bOctet = (i >= len - bOctets.length) ?
                        bOctets[i - (len - bOctets.length)] : 0;
                if (aOctet != bOctet) return (0xff & aOctet) - (0xff & bOctet);
            }
            return 0;
        }
    }

    public static Optional<InetAddress> toInetAddress(String s) {
        try {
            return Optional.of(InetAddress.getByName(s));
        } catch (UnknownHostException badAddress) {
            return Optional.empty();
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Valid 32-bit addresses");
        Arrays.stream(TESTS)
              .map(IPSort::toInetAddress)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .filter((addr) -> addr instanceof Inet4Address)
              .map(InetAddress::getHostAddress)
              .forEach(System.out::println);

        System.out.println("\nValid 128-bit addresses");
        Arrays.stream(TESTS)
              .map(IPSort::toInetAddress)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .filter((addr) -> addr instanceof Inet6Address)
              .map(InetAddress::getHostAddress)
              .forEach(System.out::println);

        System.out.println("\nInvalid addresses");
        Arrays.stream(TESTS)
              .filter((s) -> !toInetAddress(s).isPresent())
              .forEach(System.out::println);

        System.out.println("\nSorted addresses");
        Arrays.stream(TESTS)
              .map(IPSort::toInetAddress)
              .filter(Optional::isPresent)
              .map(Optional::get)
              .sorted(new InetAddressComparator())
              .map(InetAddress::getHostAddress)
              .forEach(System.out::println);
    }
}


