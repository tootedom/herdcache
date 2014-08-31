package org.greencheek.caching.herdcache.memcached.elasticacheconfig.client;

/**
 * Created by dominictootell on 25/07/2014.
 */
public class ElastiCacheServerConnectionDetails {
    private final int port;
    private final String host;

    public ElastiCacheServerConnectionDetails(String host,int port) {
        this.port = port;
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }
}
