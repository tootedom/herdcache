package org.greencheek.caching.herdcache.memcached.elasticacheconfig.configprocessing;

import org.greencheek.caching.herdcache.memcached.elasticacheconfig.client.ClientClusterUpdateObserver;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.confighandler.ConfigInfoProcessor;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.configparsing.ElastiCacheConfigParser;
import org.greencheek.caching.herdcache.memcached.factory.ReferencedClient;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.connection.UpdateClientService;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.domain.ConfigInfo;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.domain.ElastiCacheHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 *
 */
public class ElastiCacheConfigInfoProcessor implements ConfigInfoProcessor {

    private Logger logger = LoggerFactory.getLogger(ElastiCacheConfigInfoProcessor.class);

    private final ElastiCacheConfigParser configParser;
    private final UpdateClientService updateClientService;
    private final boolean updateConfigVersionOnDnsTimeout;
    private final List<ClientClusterUpdateObserver> clientClusterUpdateObservers;
    private final boolean updateConfigOnlyOnVersionChange;

    private volatile long currentConfigVersionNumber = Long.MIN_VALUE;
    private volatile Map<String,ElastiCacheHost> currentElastiCacheHosts = Collections.emptyMap();

    public ElastiCacheConfigInfoProcessor(ElastiCacheConfigParser configParser,
                                          UpdateClientService updateClientService,
                                          boolean updateConfigVersionOnDnsTimeout,
                                          List<ClientClusterUpdateObserver> clientClusterUpdateObservers,
                                          boolean updateConfigOnlyOnVersionChange) {
        this.configParser = configParser;
        this.updateClientService = updateClientService;
        this.updateConfigVersionOnDnsTimeout = updateConfigVersionOnDnsTimeout;
        this.clientClusterUpdateObservers = clientClusterUpdateObservers;
        this.updateConfigOnlyOnVersionChange = updateConfigOnlyOnVersionChange;
    }


    @Override
    public void processConfig(ConfigInfo info) {
        boolean updated = false;
        if (info.isValid()) {
            long currentVersion = currentConfigVersionNumber;
            long latestConfigVersion = info.getVersion();

            logger.debug("Cluster Configuration: {}",info.getServers());
            List<ElastiCacheHost> parsedServers = configParser.parseServers(info.getServers());


            if (hasConfigChanged(currentVersion,latestConfigVersion,parsedServers)) {
                logger.info("Configuration cluster info has changed.  Reconfiguring client");

                ReferencedClient updatedClient = updateClientService.updateClientConnections(parsedServers);
                if (updateConfigVersionOnDnsTimeout == true || updatedClient.getResolvedHosts().size() == parsedServers.size()) {
                    currentConfigVersionNumber = latestConfigVersion;
                    currentElastiCacheHosts = toMapByHostName(parsedServers);
                }
                updated = true;
            } else if (latestConfigVersion == currentVersion) {
                logger.info("Configuration is up to date");
            } else {
                logger.warn("Supplied Configuration had a version lower than current configuration");
            }

        } else {
            logger.debug("Invalid configuration provided for elasticache configuration");
        }
        notifyObservers(updated);
    }

    private boolean hasConfigChanged(long currentVersion, long latestConfigVersion, List<ElastiCacheHost> parsedServers) {
        if(updateConfigOnlyOnVersionChange) {
            return latestConfigVersion > currentVersion;
        }
        else {
            return (latestConfigVersion > currentVersion || differentElastiCacheHosts(parsedServers));
        }
    }

    private Map<String,ElastiCacheHost> toMapByHostName(List<ElastiCacheHost> hosts) {
        return hosts.stream().collect(Collectors.toMap(ElastiCacheConfigInfoProcessor::createKey, (host)->(host)));
    }

    private static String createKey(ElastiCacheHost host) {
        return host.getHostName() +
                host.getIp() +
                host.getPort();
    }

    private boolean differentElastiCacheHosts(List<ElastiCacheHost> parsedHosts) {
        Map<String,ElastiCacheHost> currentHosts = currentElastiCacheHosts;
        int currentHostsSize = currentHosts.size();
        if(currentHostsSize!=parsedHosts.size()) {
            return true;
        }

        int foundHosts = 0;
        for(ElastiCacheHost host : parsedHosts) {
            ElastiCacheHost currentHost = currentElastiCacheHosts.get(createKey(host));
            if(currentHost==null) {
                return true;
            }
            else {
                if( currentHost.hasIP() != host.hasIP()
                ||  !currentHost.getIp().equals(host.getIp())
                ||  currentHost.getPort() != host.getPort())
                {
                    return true;
                }

            }
            foundHosts++;
        }

        if(foundHosts!=parsedHosts.size()) {
            return true;
        }

        return false;

    }

    private void notifyObservers(boolean updated) {
        for(ClientClusterUpdateObserver observer : clientClusterUpdateObservers) {
            observer.clusterUpdated(updated);
        }
    }


}
