package org.greencheek.caching.herdcache.memcached.elasticacheconfig.decoder;

public enum ConfigInfoDecodingState {
    HEADER,
    VERSION,
    NODES,
    BLANK,
    END
}