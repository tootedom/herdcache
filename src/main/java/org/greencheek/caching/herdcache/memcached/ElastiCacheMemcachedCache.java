package org.greencheek.caching.herdcache.memcached;

import org.greencheek.caching.herdcache.memcached.config.ElastiCacheCacheConfig;
import org.greencheek.caching.herdcache.memcached.config.Host;
import org.greencheek.caching.herdcache.memcached.config.MemcachedCacheConfig;
import org.greencheek.caching.herdcache.memcached.config.hostparsing.CommaSeparatedHostAndPortStringParser;
import org.greencheek.caching.herdcache.memcached.config.hostparsing.HostStringParser;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.client.ElastiCacheServerConnectionDetails;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.client.LocalhostElastiCacheServerConnectionDetails;
import org.greencheek.caching.herdcache.memcached.factory.ElastiCacheClientFactory;

import java.util.List;

/**
 *
 */
public class ElastiCacheMemcachedCache<V> extends BaseMemcachedCache<V> {


    public ElastiCacheMemcachedCache(ElastiCacheCacheConfig config) {
       this(config.getMemcachedCacheConfig(),config);
    }

    private ElastiCacheMemcachedCache(MemcachedCacheConfig mConfig,ElastiCacheCacheConfig config) {
        super(new ElastiCacheClientFactory(
             createMemcachedConnectionFactory(mConfig),
                parseElastiCacheConfigHosts(config.getElastiCacheConfigHosts()),
                config.getConfigPollingTime(),
                config.getInitialConfigPollingDelay(),
                config.getIdleReadTimeout(),
                config.getReconnectDelay(),
                config.getDelayBeforeClientClose(),
                mConfig.getHostResolver(),
                mConfig.getDnsConnectionTimeout(),
                config.isUpdateConfigVersionOnDnsTimeout(),
                config.getNumberOfConsecutiveInvalidConfigurationsBeforeReconnect(),
                config.getConnectionTimeoutInMillis()
        ),mConfig);
    }

    private static ElastiCacheServerConnectionDetails[] parseElastiCacheConfigHosts(String hostsString) {
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
