package org.greencheek.caching.herdcache.memcached.factory;

import org.greencheek.caching.herdcache.memcached.config.Host;
import org.greencheek.caching.herdcache.memcached.dns.AddressResolver;
import org.greencheek.caching.herdcache.memcached.dns.DefaultAddressResolver;
import org.greencheek.caching.herdcache.memcached.dns.resolver.InetAddressComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Created by dominictootell on 17/05/2018.
 *
 * If the DNS resolver returns 127.0.53.53 (https://www.icann.org/resources/pages/name-collision-2013-12-06-en),
 * then it is ignored from the list of returned addresses.
 */
public class BackgroundDnsResolver implements ReferencedClientHolder {
    private static final Logger LOG = LoggerFactory.getLogger(BackgroundDnsResolver.class);
    private static final Holder EMPTY = new Holder(SpyReferencedClient.UNAVAILABLE_REFERENCE_CLIENT,new InetAddress[0]);

    private final AtomicReference<Holder> client = new AtomicReference(EMPTY);

    private final ScheduledExecutorService scheduledExecutorService;
    private final AddressResolver resolver;
    private final Host host;
    private final ReferencedClientFactory connnectionFactory;
    private final Comparator<InetAddress> sortingComparator = new InetAddressComparator();

    private static final ThreadFactory DEFAULT_THREAD_FACTORY = (r) -> {
        final Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    };


    public BackgroundDnsResolver(Host host,long backgroundPollingTimeInMillis,
                                 ReferencedClientFactory connnectionFactory) {

        this(host,backgroundPollingTimeInMillis,connnectionFactory,new DefaultAddressResolver());
    }

    public BackgroundDnsResolver(Host host,long backgroundPollingTimeInMillis,
                                 ReferencedClientFactory connnectionFactory, AddressResolver resolver) {
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(DEFAULT_THREAD_FACTORY);
        this.host = host;


        scheduledExecutorService.scheduleWithFixedDelay(
                backgroundResolver(),
                0,backgroundPollingTimeInMillis,
                TimeUnit.MILLISECONDS);


        this.connnectionFactory = connnectionFactory;
        this.resolver = resolver;
    }

    private Runnable backgroundResolver() {
        return () -> {
            String hostName = host.getHost();
            Holder currentResolvedAddresses = client.get();
            InetAddress[] addresses = resolver.resolve(hostName);

            addresses = checkForCollisionResponses(addresses, hostName);

            InetAddress[] existingAddresses = currentResolvedAddresses.addresses;

            if(addresses.length == 0) {
                if(existingAddresses.length==0) {
                    LOG.error("Failed to resolve address for '{}', no pre-cached addresses to re-use", host);
                } else {
                    LOG.error("Failed to resolve address for '{}', old pre-cached addresses will be kept",host);
                }
            } else {
                if (haveAddressesChanged(addresses,existingAddresses)) {
                    ReferencedClient referencedClient = connnectionFactory.createClient(toSocketAddresses(addresses));
                    if (referencedClient != SpyReferencedClient.UNAVAILABLE_REFERENCE_CLIENT) {
                        client.set(new Holder(referencedClient,addresses));
                    }
                    LOG.debug("[{}] Addresses available: {}", host, toCommaSeparated(addresses));
                } else {
                    LOG.debug("[{}] Has Same Addresses: {}", host, toCommaSeparated(addresses));
                }
            }
        };
    }

    private InetAddress[] checkForCollisionResponses(final InetAddress[] addresses, String host) {
        List<InetAddress> okResponses = new ArrayList<>(0);
        for (final InetAddress address : addresses) {
            if ((address instanceof Inet4Address) && isCollision(address)) {
                LOG.warn("DNS collision response found for {}", host);
                continue;
            }
            okResponses.add(address);
        }
        return okResponses.toArray(new InetAddress[okResponses.size()]);
    }

    private boolean isCollision(final InetAddress inetAddress) {
        final byte[] addressBytes = inetAddress.getAddress();

        return (addressBytes[0] == 127) && (addressBytes[1] == 0) && (addressBytes[2] == 53) && (addressBytes[3] == 53);
    }

    private List<InetSocketAddress> toSocketAddresses(InetAddress[] addresses) {
        List<InetSocketAddress> socketAddresses = new ArrayList<>(addresses.length);
        for(InetAddress addy : addresses) {
            socketAddresses.add(new InetSocketAddress(addy,host.getPort()));
        }
        return socketAddresses;
    }

    private String toCommaSeparated(InetAddress[] addresses) {
        return Arrays.stream(addresses)
                .map(x -> x.getHostAddress())
                .collect(Collectors.joining(","));
    }

    private boolean haveAddressesChanged(InetAddress[] newaddresses, InetAddress[] oldaddresses) {

        Arrays.sort(newaddresses, sortingComparator);
        if (newaddresses.length != oldaddresses.length) {
            return true;
        }

        for (int i =0;i<newaddresses.length;i++) {
            InetAddress oldaddy = oldaddresses[i];
            InetAddress newaddy = newaddresses[i];

            if(!oldaddy.equals(newaddy)) {
                return true;
            }
        }
        return false;
    }

    public ReferencedClient getClient(){
        return client.get().client;
    }

    public void shutdown() {
        scheduledExecutorService.shutdownNow();
        client.get().client.shutdown();
    }

    private static class Holder {
        final ReferencedClient client;
        final InetAddress[] addresses;
        Holder(ReferencedClient clientref, InetAddress[] addresses ) {
            this.client  = clientref;
            this.addresses = addresses;
        }
    }
}
