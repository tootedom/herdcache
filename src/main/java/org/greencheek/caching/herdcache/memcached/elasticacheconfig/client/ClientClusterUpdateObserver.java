package org.greencheek.caching.herdcache.memcached.elasticacheconfig.client;

import org.greencheek.caching.herdcache.memcached.factory.ReferencedClient;

/**
 * clusterUpdate is executed when the elasticache cluster has been updated
 */
public interface ClientClusterUpdateObserver{
    void clusterUpdated(boolean updated);
}
