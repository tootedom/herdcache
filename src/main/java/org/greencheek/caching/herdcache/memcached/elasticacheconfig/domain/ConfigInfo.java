package org.greencheek.caching.herdcache.memcached.elasticacheconfig.domain;

/**
 * Created by dominictootell on 14/07/2014.
 */
public class ConfigInfo {
    public static ConfigInfo INVALID_CONFIG = new ConfigInfo("", Long.MIN_VALUE,"",false);

    private final String header;
    private final long version;
    private final String servers;
    private final boolean valid;

    public ConfigInfo(String header, long version, String servers, boolean valid) {
        this.header = header;
        this.version = version;
        this.servers = servers;
        this.valid = valid;
    }

    public String getHeader() {
        return header;
    }

    public long getVersion() {
        return version;
    }

    public String getServers() {
        return servers;
    }

    public boolean isValid() {
        return valid;
    }


    public String toString() {
        StringBuilder b = new StringBuilder(256);
        b.append("header:").append(header).append(',');
        b.append("version:").append(version).append(',');
        b.append("servers:").append(servers).append(',');
        b.append("valid:").append(valid);
        return b.toString();
    }
}
