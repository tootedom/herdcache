package org.greencheek.caching.herdcache.memcached.elasticacheconfig.domain;

/**
 *
 */
public class ElastiCacheHost {
    private final String hostName;
    private final String ip;
    private final int port;
    private final boolean hasIP;

    public ElastiCacheHost(String hostName,String ip,int port,boolean hasIp) {
        this.hostName = hostName;
        this.ip = ip;
        this.port = port;
        this.hasIP = hasIp;
    }

    public String getHostName() {
        return hostName;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public boolean hasIP() {
        return hasIP;
    }
}
