package org.greencheek.caching.herdcache.memcached.dns.lookup;

import org.greencheek.caching.herdcache.memcached.config.Host;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by dominictootell on 24/08/2014.
 */
public class AddressByNameHostResolver implements HostResolver {

    private static Logger logger = LoggerFactory.getLogger(HostResolver.class);


    @Override
    public List<InetSocketAddress> returnSocketAddressesForHostNames(List<Host> nodes) {
        return returnSocketAddressesForHostNames(nodes,DEFAULT_DNS_TIMEOUT);
    }

    @Override
    public List<InetSocketAddress> returnSocketAddressesForHostNames(List<Host> nodes, Duration dnsLookupTimeout) {
        LookupService addressLookupService = LookupService.create();
        List<InetSocketAddress> workingNodes = new ArrayList<InetSocketAddress>(nodes.size());
        try {

            for (Host hostAndPort : nodes) {
                Future<InetAddress> future = null;
                String host = hostAndPort.getHost();
                int port = hostAndPort.getPort();
                try {
                    future = addressLookupService.getByName(host);
                    InetAddress ia = future.get(dnsLookupTimeout.getSeconds(), TimeUnit.SECONDS);
                    if (ia == null) {
                        logger.error("Unable to resolve dns entry for the host: {}", host);
                    } else {
                        try {
                            workingNodes.add(new InetSocketAddress(ia, port));
                        } catch (IllegalArgumentException e) {
                            logger.error("Invalid port number has been provided for the memcached node: host({}),port({})", host, port);
                        }
                    }
                } catch (TimeoutException e) {
                    logger.error("Problem resolving host name ({}) to an ip address in fixed number of seconds: {}", host, dnsLookupTimeout, e);
                } catch (Exception e) {
                    logger.error("Problem resolving host name to ip address: {}", host, e);
                } finally {
                    if (future != null) future.cancel(true);
                }
            }
        } finally {
            addressLookupService.shutdown();
        }
        return workingNodes;

    }
}
