package org.greencheek.caching.herdcache.memcached.elasticacheconfig.client;

import java.util.function.Consumer;

/**
 * Interface that represents that the ElastiCache cluster
 * may change during the lifetime of the cache.  As a result
 * the config server may change, i.e. the url or the actual
 * config cluster node(s) themselves
 */
public interface ElastiCacheConfigServerUpdater {

    /**
     * Set the consumer that is notified when the connection is
     * Updated
     * @param l
     */
    public void setUpdateConsumer(Consumer<String> l);

    /**
     * Called when the ElastiCache cluster config url has changed.
     * @param connectionString
     */
    public void connectionUpdated(String connectionString);
}
