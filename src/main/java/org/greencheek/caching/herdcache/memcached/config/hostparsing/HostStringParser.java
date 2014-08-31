package org.greencheek.caching.herdcache.memcached.config.hostparsing;

import org.greencheek.caching.herdcache.memcached.config.Host;

import java.util.List;

/**
 * Created by dominictootell on 23/08/2014.
 */
public interface HostStringParser {

    public static int DEFAULT_MEMCACHED_PORT = 11211;
    /**
     * Takes a string:
     *
     * url:port,url:port
     *
     * converting it to a list of Host objtects consisting of:
     *
     * [url,port],[url,port]
     *
     * @param hosts
     * @return
     */
    public List<Host> parseMemcachedNodeList(String hosts);
}
