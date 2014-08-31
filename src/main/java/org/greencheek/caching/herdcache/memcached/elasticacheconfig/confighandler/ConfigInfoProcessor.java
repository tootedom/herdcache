package org.greencheek.caching.herdcache.memcached.elasticacheconfig.confighandler;

import org.greencheek.caching.herdcache.memcached.elasticacheconfig.domain.ConfigInfo;

/**
 * Created by dominictootell on 20/07/2014.
 */
public interface ConfigInfoProcessor {
    // Will be called on a single thread.
    public void processConfig(ConfigInfo info);
}
