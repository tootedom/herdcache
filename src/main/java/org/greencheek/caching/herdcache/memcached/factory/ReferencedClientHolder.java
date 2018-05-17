package org.greencheek.caching.herdcache.memcached.factory;

/**
 * Created by dominictootell on 17/05/2018.
 */
public interface ReferencedClientHolder {
    ReferencedClient getClient();
    default void shutdown() {};
}
