package org.greencheek.caching.herdcache.memcached.elasticacheconfig.client;

import org.greencheek.caching.herdcache.memcached.config.Host;
import org.greencheek.caching.herdcache.memcached.config.hostparsing.CommaSeparatedHostAndPortStringParser;
import org.greencheek.caching.herdcache.memcached.config.hostparsing.HostStringParser;

import java.util.List;

/**
 */
public class ElastiCacheConfigHostsParser {
    public static ElastiCacheServerConnectionDetails[] parseElastiCacheConfigHosts(String hostsString) {
        HostStringParser hostStringParser  = new CommaSeparatedHostAndPortStringParser();
        String hosts = null;
        if(hostsString == null || hostsString.trim().length() == 0) {
            hosts = "localhost:11211";
        } else {
            hosts = hostsString;
        }

        List<Host> parsedHosts = hostStringParser.parseMemcachedNodeList(hosts);

        ElastiCacheServerConnectionDetails[] connectionDetails = (parsedHosts.size()>0) ?
                new ElastiCacheServerConnectionDetails[parsedHosts.size()] :
                new ElastiCacheServerConnectionDetails[0];

        int i = 0;
        while(i<parsedHosts.size()) {
            Host details = parsedHosts.get(i);
            connectionDetails[i] = new ElastiCacheServerConnectionDetails(details.getHost(),details.getPort());
            i++;
        }

        if(parsedHosts.size()==0) {
            connectionDetails[0] = new LocalhostElastiCacheServerConnectionDetails();
        }

        return connectionDetails;
    }
}
