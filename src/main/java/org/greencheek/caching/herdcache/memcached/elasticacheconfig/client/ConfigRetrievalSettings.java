package org.greencheek.caching.herdcache.memcached.elasticacheconfig.client;

import org.greencheek.caching.herdcache.memcached.elasticacheconfig.confighandler.AsyncConfigInfoMessageHandler;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.handler.RequestConfigInfoScheduler;

import java.util.concurrent.TimeUnit;

/**
 * Created by dominictootell on 20/07/2014.
 */
public class ConfigRetrievalSettings {

    private final RequestConfigInfoScheduler scheduledConfigRetrieval;
    private final AsyncConfigInfoMessageHandler configInfoMessageHandler;

    private final TimeUnit idleTimeoutTimeUnit;
    private final long idleReadTimeout;

    private final TimeUnit reconnectDelayTimeUnit;
    private final long reconnectDelay;

    private final int connectionTimeoutInMillis;

    private final int numberOfConsecutiveInvalidConfigsBeforeReconnect;

    private final ElastiCacheServerConnectionDetails[] elasticacheConfigHosts;

    public ConfigRetrievalSettings(RequestConfigInfoScheduler scheduledConfigRetrieval,
                                   AsyncConfigInfoMessageHandler obtainedConfigHandler,
                                   ElastiCacheServerConnectionDetails[] configurationServerConnectionDetails,
                                   TimeUnit idleTimeoutTimeUnit,
                                   long idleReadTimeout,
                                   TimeUnit reconnectDelayTimeUnit,
                                   long reconnectDelay,
                                   int noInvalidConfigsBeforeReconnect,
                                   int connectionTimeoutInMillis) {
        this.scheduledConfigRetrieval = scheduledConfigRetrieval;
        this.configInfoMessageHandler = obtainedConfigHandler;
        this.elasticacheConfigHosts = new ElastiCacheServerConnectionDetails[configurationServerConnectionDetails.length];
        int i = 0;
        for(ElastiCacheServerConnectionDetails host : configurationServerConnectionDetails) {
            elasticacheConfigHosts[i++] = host;
        }
        this.idleTimeoutTimeUnit = idleTimeoutTimeUnit;
        this.idleReadTimeout = idleReadTimeout;
        this.reconnectDelayTimeUnit = reconnectDelayTimeUnit;
        this.reconnectDelay = reconnectDelay;
        this.numberOfConsecutiveInvalidConfigsBeforeReconnect = noInvalidConfigsBeforeReconnect;
        this.connectionTimeoutInMillis = connectionTimeoutInMillis;
    }

    public RequestConfigInfoScheduler getScheduledConfigRetrieval() {
        return scheduledConfigRetrieval;
    }

    public AsyncConfigInfoMessageHandler getConfigInfoMessageHandler() {
        return configInfoMessageHandler;
    }

    public ElastiCacheServerConnectionDetails[] getElasticacheConfigHosts() {
        return elasticacheConfigHosts;
    }

    public TimeUnit getIdleTimeoutTimeUnit() {
        return idleTimeoutTimeUnit;
    }

    public long getIdleReadTimeout() {
        return idleReadTimeout;
    }

    public TimeUnit getReconnectDelayTimeUnit() {
        return reconnectDelayTimeUnit;
    }

    public long getReconnectDelay() {
        return reconnectDelay;
    }

    public int getNumberOfConsecutiveInvalidConfigsBeforeReconnect() {
        return numberOfConsecutiveInvalidConfigsBeforeReconnect;
    }

    public int getConnectionTimeoutInMillis() {
        return connectionTimeoutInMillis;
    }
}
