package org.greencheek.caching.herdcache.memcached.elasticacheconfig.configprocessing;

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
 * Created by dominictootell on 22/07/2014.
 */
public class ElastiCacheConfigInfoProcessor implements ConfigInfoProcessor {

    private Logger logger = LoggerFactory.getLogger(ElastiCacheConfigInfoProcessor.class);

    private final ElastiCacheConfigParser configParser;
    private final UpdateClientService updateClientService;
    private final boolean updateConfigVersionOnDnsTimeout;

    private volatile long currentConfigVersionNumber = Long.MIN_VALUE;

    public ElastiCacheConfigInfoProcessor(ElastiCacheConfigParser configParser,
                                          UpdateClientService updateClientService,
                                          boolean updateConfigVersionOnDnsTimeout) {
        this.configParser = configParser;
        this.updateClientService = updateClientService;
        this.updateConfigVersionOnDnsTimeout = updateConfigVersionOnDnsTimeout;
    }



  @Override
  public void processConfig(ConfigInfo info)  {
    if(info.isValid()) {
      long currentVersion = currentConfigVersionNumber;
      long latestConfigVersion = info.getVersion();

      if(latestConfigVersion>currentVersion) {
        logger.info("Configuration version has increased.  Reconfiguring client");

        List<ElastiCacheHost> parsedServers = configParser.parseServers(info.getServers());
        ReferencedClient updatedClient = updateClientService.updateClientConnections(parsedServers);
        if(updateConfigVersionOnDnsTimeout == true || updatedClient.getResolvedHosts().size()==parsedServers.size()) {
          currentConfigVersionNumber = latestConfigVersion;
        }
      }
      else if(latestConfigVersion==currentVersion) {
        logger.info("Configuration is up to date");
      } else {
        logger.warn("Supplied Configuration had a version lower than current configuration");
      }

    } else {
      logger.debug("Invalid configuration provided for elasticache configuration");
    }
  }
}
