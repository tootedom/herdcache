package org.greencheek.caching.herdcache.memcached.elasticacheconfig.client;

/**
 *
 */
public interface UpdateableElastiCacheConfigServerChooser extends ElastiCacheConfigServerChooser {
    /**
     * returns the old connection details, and set new connection details
     * @param connectionDetails
     * @return
     */
    public ElastiCacheServerConnectionDetails setServer(ElastiCacheServerConnectionDetails connectionDetails);
}
