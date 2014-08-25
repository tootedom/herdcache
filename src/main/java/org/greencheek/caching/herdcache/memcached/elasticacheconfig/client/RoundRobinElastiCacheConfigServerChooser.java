package org.greencheek.caching.herdcache.memcached.elasticacheconfig.client;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by dominictootell on 25/07/2014.
 */
public class RoundRobinElastiCacheConfigServerChooser implements ElastiCacheConfigServerChooser{
    private final AtomicInteger counter = new AtomicInteger(0);

    private final ElastiCacheServerConnectionDetails[] hosts;

    public RoundRobinElastiCacheConfigServerChooser(ElastiCacheServerConnectionDetails[] hosts) {
        this.hosts = hosts;

    }

    @Override
    public ElastiCacheServerConnectionDetails getServer() {
        return hosts[counter.getAndIncrement()%hosts.length];
    }
}
