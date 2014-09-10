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

import java.util.List;


/**
 *
 */
public class ElastiCacheConfigInfoProcessor implements ConfigInfoProcessor {

    private Logger logger = LoggerFactory.getLogger(ElastiCacheConfigInfoProcessor.class);

    private final ElastiCacheConfigParser configParser;
    private final UpdateClientService updateClientService;
    private final boolean updateConfigVersionOnDnsTimeout;
    private final List<ClientClusterUpdateObserver> clientClusterUpdateObservers;

    private volatile long currentConfigVersionNumber = Long.MIN_VALUE;

    public ElastiCacheConfigInfoProcessor(ElastiCacheConfigParser configParser,
                                          UpdateClientService updateClientService,
                                          boolean updateConfigVersionOnDnsTimeout,
                                          List<ClientClusterUpdateObserver> clientClusterUpdateObservers) {
        this.configParser = configParser;
        this.updateClientService = updateClientService;
        this.updateConfigVersionOnDnsTimeout = updateConfigVersionOnDnsTimeout;
        this.clientClusterUpdateObservers = clientClusterUpdateObservers;
    }


    @Override
    public void processConfig(ConfigInfo info) {
        boolean updated = false;
        if (info.isValid()) {
            long currentVersion = currentConfigVersionNumber;
            long latestConfigVersion = info.getVersion();

            if (latestConfigVersion > currentVersion) {
                logger.info("Configuration version has increased.  Reconfiguring client");

                List<ElastiCacheHost> parsedServers = configParser.parseServers(info.getServers());
                ReferencedClient updatedClient = updateClientService.updateClientConnections(parsedServers);
                if (updateConfigVersionOnDnsTimeout == true || updatedClient.getResolvedHosts().size() == parsedServers.size()) {
                    currentConfigVersionNumber = latestConfigVersion;
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

    private void notifyObservers(boolean updated) {
        for(ClientClusterUpdateObserver observer : clientClusterUpdateObservers) {
            observer.clusterUpdated(updated);
        }
    }


}
