package org.greencheek.caching.herdcache.memcached.elasticacheconfig.domain;

import org.greencheek.caching.herdcache.memcached.elasticacheconfig.decoder.ConfigInfoDecodingState;

/**
 * Created by dominictootell on 15/07/2014.
 */
public class ClusterConfigurationBuilder {
    private String header;
    private long versionNumber;
    private String hostsString;


    public ClusterConfigurationBuilder setHeader(String header) {
        init();
        this.header = header;
        return this;
    }

    public ClusterConfigurationBuilder setVersionNumber(long version) {
        this.versionNumber = version;
        return this;
    }

    public ClusterConfigurationBuilder setHostsString(String hosts) {
        this.hostsString = hosts;
        return this;
    }

    public ClusterConfigurationBuilder setValue(long value, ConfigInfoDecodingState state) {
        setVersionNumber(value);
        return this;
    }

    public ClusterConfigurationBuilder setValue(String value, ConfigInfoDecodingState state) {
        switch(state) {
            case HEADER:
                setHeader(value);
                break;
            case NODES:
                setHostsString(value);
                break;
        }
        return this;
    }

    public void init() {
        this.header = null;
        this.versionNumber = Long.MIN_VALUE;
        this.hostsString = null;
    }

    public ConfigInfo build() {
        if(header == null || hostsString == null ) {
            return ConfigInfo.INVALID_CONFIG;
        }
        else if (versionNumber== Long.MIN_VALUE) {
            return new ConfigInfo(header, versionNumber, hostsString, false);
        }
        else {
            return new ConfigInfo(header, versionNumber, hostsString, true);

        }
    }
}
