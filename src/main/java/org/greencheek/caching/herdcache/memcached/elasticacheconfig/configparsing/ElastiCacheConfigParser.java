package org.greencheek.caching.herdcache.memcached.elasticacheconfig.configparsing;

import org.greencheek.caching.herdcache.memcached.elasticacheconfig.domain.ElastiCacheHost;

import java.util.List;

/**
 * Created by dominictootell on 30/08/2014.
 */
public interface ElastiCacheConfigParser {
    List<ElastiCacheHost> parseServers(String serversString);
}
