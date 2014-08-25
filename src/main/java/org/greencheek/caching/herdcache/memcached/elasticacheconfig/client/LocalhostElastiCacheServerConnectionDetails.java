package org.greencheek.caching.herdcache.memcached.elasticacheconfig.client;

/**
 * Created by dominictootell on 25/07/2014.
 */
public class LocalhostElastiCacheServerConnectionDetails extends ElastiCacheServerConnectionDetails {

    public LocalhostElastiCacheServerConnectionDetails() {
        super("localhost", 11211);
    }
}
