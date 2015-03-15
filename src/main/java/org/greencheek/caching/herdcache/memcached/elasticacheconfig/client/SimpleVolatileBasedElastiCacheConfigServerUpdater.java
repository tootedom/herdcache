package org.greencheek.caching.herdcache.memcached.elasticacheconfig.client;

import java.util.function.Consumer;

/**
 *
 */
public class SimpleVolatileBasedElastiCacheConfigServerUpdater implements ElastiCacheConfigServerUpdater{
    private volatile Consumer<String> listener;

    public void setUpdateConsumer(Consumer<String> l) {
        listener = l;
    }


    public void connectionUpdated(String connectionString) {
        listener.accept(connectionString);
    }
}
