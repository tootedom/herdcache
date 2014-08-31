package org.greencheek.caching.herdcache.memcached.elasticacheconfig.connection;

import org.greencheek.caching.herdcache.memcached.elasticacheconfig.domain.ElastiCacheHost;

import java.util.List;

/**
 * Created by dominictootell on 26/08/2014.
 */
public interface UpdateClientService {
    public ReferencedClient getClient();
    public ReferencedClient updateClientConnections(List<ElastiCacheHost> hosts);
    public void shutdown();
}
