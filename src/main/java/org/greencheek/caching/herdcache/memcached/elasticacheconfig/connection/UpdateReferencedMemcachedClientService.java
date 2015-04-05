package org.greencheek.caching.herdcache.memcached.elasticacheconfig.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.MemcachedClient;
import org.greencheek.caching.herdcache.memcached.config.Host;
import org.greencheek.caching.herdcache.memcached.dns.lookup.HostResolver;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.domain.ElastiCacheHost;
import org.greencheek.caching.herdcache.memcached.factory.ReferencedClient;
import org.greencheek.caching.herdcache.memcached.factory.ReferencedClientFactory;
import org.greencheek.caching.herdcache.memcached.factory.SpyReferencedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by dominictootell on 22/07/2014.
 */
public class UpdateReferencedMemcachedClientService implements UpdateClientService {

    private final HostResolver dnsLookupService;
    private final Duration dnsConnectionTimeout;
    private final ReferencedClientFactory memcachedConnectionFactory;
    private final Duration delayBeforeOldClientClose;

    private final static Logger logger = LoggerFactory.getLogger(UpdateReferencedMemcachedClientService.class);
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    private volatile ReferencedClient referencedClient = SpyReferencedClient.UNAVAILABLE_REFERENCE_CLIENT;

    public UpdateReferencedMemcachedClientService(HostResolver dnsLookupService,
                                                  Duration dnsConnectionTimeout,
                                                  ReferencedClientFactory memcachedConnectionFactory,
                                                  Duration delayBeforeOldClientClose) {

        this.dnsConnectionTimeout = dnsConnectionTimeout;
        this.dnsLookupService = dnsLookupService;
        this.memcachedConnectionFactory = memcachedConnectionFactory;
        this.delayBeforeOldClientClose = delayBeforeOldClientClose;
    }


    @Override
    public ReferencedClient updateClientConnections(List<ElastiCacheHost> hosts) {
        boolean shutdown = isShutdown.get();
        if (shutdown) {
            return referencedClient;
        }

        ReferencedClient currentClient = referencedClient;
        if (hosts.size() == 0) {
            logger.warn("No cache hosts available.  Marking cache as disabled.");
            referencedClient = SpyReferencedClient.UNAVAILABLE_REFERENCE_CLIENT;
            return SpyReferencedClient.UNAVAILABLE_REFERENCE_CLIENT;
        } else {
            List<InetSocketAddress> resolvedHosts = getSocketAddresses(hosts);
            if (resolvedHosts.size() == 0) {
                logger.warn("No resolvable cache hosts available.  Marking cache as disabled.");
                referencedClient = SpyReferencedClient.UNAVAILABLE_REFERENCE_CLIENT;
                return SpyReferencedClient.UNAVAILABLE_REFERENCE_CLIENT;
            } else {
                logger.info("New client being created for new cache hosts.");
                try {
                    ReferencedClient newClient = memcachedConnectionFactory.createClient(resolvedHosts);

                    referencedClient = newClient;
                    if (currentClient.isAvailable()) {
                        logger.debug("Scheduling shutdown of old cache client in {}ms", delayBeforeOldClientClose.toMillis());
                        scheduledExecutor.schedule(new Runnable() {
                            @Override
                            public void run() {
                                logger.info("Shutting down old cache client.");
                                currentClient.shutdown();
                            }
                        }, delayBeforeOldClientClose.toMillis(), TimeUnit.MILLISECONDS);
                    }

                    // A shutdown may have occurred mid creation of the new client,
                    // so we shutdown the old one
                    if (isShutdown.get() != shutdown) {
                        shutdown();
                    }
                    return newClient;
                } catch (Exception e) {
                    logger.warn("Unable to create new MemcachedClient, exception during client creation.  Marking cache as disabled.",e);
                    referencedClient = SpyReferencedClient.UNAVAILABLE_REFERENCE_CLIENT;
                    return SpyReferencedClient.UNAVAILABLE_REFERENCE_CLIENT;
                }
            }
        }

    }

    @Override
    public ReferencedClient getClient() {
        return referencedClient;
    }

    @Override
    public void shutdown() {
        isShutdown.set(true);
        ReferencedClient currentClient = referencedClient;

        if (currentClient.isAvailable()) {
            try {
                scheduledExecutor.shutdown();
                currentClient.shutdown();
            } finally {
                referencedClient = SpyReferencedClient.UNAVAILABLE_REFERENCE_CLIENT;
            }
        }
    }

    private List<InetSocketAddress> getSocketAddresses(List<ElastiCacheHost> hosts) {
        List<InetSocketAddress> resolvedHosts = new ArrayList<InetSocketAddress>();
        int size = hosts.size();
        int i = 0;

        while (i < size) {
            ElastiCacheHost host = hosts.get(i);
            if (host.hasIP()) {
                try {
                    InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(host.getIp()), host.getPort());
                    resolvedHosts.add(socketAddress);
                } catch (UnknownHostException e) {

                }
            } else {
                Host singleHost = new Host(host.getHostName(), host.getPort());
                List<InetSocketAddress> socketAddress = dnsLookupService.returnSocketAddressesForHostNames(Collections.singletonList(singleHost), dnsConnectionTimeout);
                if (socketAddress.size() == 1) {
                    resolvedHosts.add(socketAddress.get(0));
                }
            }
            i += 1;
        }

        return resolvedHosts;
    }

}
