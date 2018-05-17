package org.greencheek.caching.herdcache.memcached.factory;

import org.greencheek.caching.herdcache.memcached.config.Host;

import java.time.Duration;
import java.util.List;

/**
 * Created by dominictootell on 17/05/2018.
 */
public interface ReferenceClientResolver {
    ReferencedClientHolder getClientHolder(List<Host> parsedHosts, Duration lookupTimeout);
}
