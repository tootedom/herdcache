package org.greencheek.caching.herdcache.memcached.elasticacheconfig.client;

/**
 * Created by dominictootell on 25/07/2014.
 */
public interface ElastiCacheConfigServerChooser {
    public ElastiCacheServerConnectionDetails getServer();
}
