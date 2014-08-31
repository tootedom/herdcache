package org.greencheek.caching.herdcache.memcached.config.hostparsing;

import org.greencheek.caching.herdcache.memcached.config.Host;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by dominictootell on 23/08/2014.
 */
public class CommaSeparatedHostAndPortStringParser implements HostStringParser {

    private static Logger logger = LoggerFactory.getLogger(HostStringParser.class);

    @Override
    public List<Host> parseMemcachedNodeList(String urls) {
        if (urls == null) return Collections.EMPTY_LIST;
        String hostUrls = urls.trim();
        List<Host> memcachedNodes = new ArrayList<Host>(4);
        for (String url : hostUrls.split(",")) {
            int port = DEFAULT_MEMCACHED_PORT;
            int indexOfPort = url.indexOf(':');
            String host;
            if(indexOfPort==-1) {
                host = url.trim();
            } else {
                host = url.substring(0,indexOfPort).trim();
            }

            try {
                port = Integer.parseInt(url.substring(indexOfPort + 1, url.length()));
                if(port > 65535) {
                    port = DEFAULT_MEMCACHED_PORT;
                }
            }
            catch ( NumberFormatException e) {
                logger.info("Unable to parse memcached port number, not an integer");
            }

            if ( host.length() != 0 ) {
                memcachedNodes.add(new Host(host, port));
            }
        }
        return memcachedNodes;
    }
}
