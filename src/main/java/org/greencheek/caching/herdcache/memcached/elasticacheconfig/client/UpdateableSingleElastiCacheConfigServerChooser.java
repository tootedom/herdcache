package org.greencheek.caching.herdcache.memcached.elasticacheconfig.client;

/**
 *
 */
public class UpdateableSingleElastiCacheConfigServerChooser implements UpdateableElastiCacheConfigServerChooser {

    private volatile ElastiCacheServerConnectionDetails server;

    public UpdateableSingleElastiCacheConfigServerChooser(ElastiCacheServerConnectionDetails server) {
        this.server = server;
    }

    @Override
    public ElastiCacheServerConnectionDetails getServer() {
        return server;
    }

    @Override
    public ElastiCacheServerConnectionDetails setServer(ElastiCacheServerConnectionDetails connectionDetails) {
        return server = connectionDetails;
    }
}
