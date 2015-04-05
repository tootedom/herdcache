package org.greencheek.caching.herdcache.memcached.config;

import org.greencheek.caching.herdcache.memcached.elasticacheconfig.client.ClientClusterUpdateObserver;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.client.ElastiCacheConfigServerUpdater;

import javax.swing.text.html.Option;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by dominictootell on 24/08/2014.
 */
public class ElastiCacheCacheConfig {

    private final String elastiCacheConfigHosts;
    private final MemcachedCacheConfig memcachedCacheConfig;
    private final Duration configPollingTime;
    private final Duration initialConfigPollingDelay;
    private final Duration connectionTimeoutInMillis;
    private final Duration idleReadTimeout;
    private final Duration reconnectDelay;
    private final Duration delayBeforeClientClose;
    private final int numberOfConsecutiveInvalidConfigurationsBeforeReconnect;
    private final boolean updateConfigVersionOnDnsTimeout;
    private final List<ClientClusterUpdateObserver> clusterUpdatedObservers;
    private final Optional<ElastiCacheConfigServerUpdater> configUrlUpdater;
    private final boolean updateConfigOnlyOnVersionChange;
    private final MemcachedClientType clientType;
    private final boolean useFolsomStringClient;
    private final Charset folsomStringClientCharset;
    private final int folsomConnections;
    private final long folsomRequestTimeout;
    private final int folsomOutstandingRequests;

    public ElastiCacheCacheConfig(MemcachedCacheConfig memcachedConf,
                                  String elastiCacheConfigHosts,
                                  Duration configPollingTime,
                                  Duration initialConfigPollingDelay,
                                  Duration connectionTimeoutInMillis,
                                  Duration idleReadTimeout,
                                  Duration reconnectDelay,
                                  Duration delayBeforeClientClose,
                                  int numberOfConsecutiveInvalidConfigurationsBeforeReconnect,
                                  boolean updateConfigVersionOnDnsTimeout,
                                  List<ClientClusterUpdateObserver> clusterUpdatedObservers,
                                  Optional<ElastiCacheConfigServerUpdater> configServerUpdater,
                                  boolean updateConfigOnlyOnVersionChange,
                                  MemcachedClientType clientType,
                                  boolean folsomStringClient,
                                  Charset folsomCharset,
                                  int folsomConnections,
                                  long folsomRequestTimeout,
                                  int folsomMaxOutstandingRequests) {
        this.memcachedCacheConfig = memcachedConf;
        this.elastiCacheConfigHosts = elastiCacheConfigHosts;
        this.configPollingTime = configPollingTime;
        this.initialConfigPollingDelay = initialConfigPollingDelay;
        this.connectionTimeoutInMillis = connectionTimeoutInMillis;
        this.idleReadTimeout = idleReadTimeout;
        this.reconnectDelay = reconnectDelay;
        this.delayBeforeClientClose = delayBeforeClientClose;
        this.numberOfConsecutiveInvalidConfigurationsBeforeReconnect = numberOfConsecutiveInvalidConfigurationsBeforeReconnect;
        this.updateConfigVersionOnDnsTimeout = updateConfigVersionOnDnsTimeout;
        this.clusterUpdatedObservers = new ArrayList<ClientClusterUpdateObserver>(clusterUpdatedObservers.size());
        this.configUrlUpdater = configServerUpdater;

        this.clusterUpdatedObservers.addAll(clusterUpdatedObservers);
        this.updateConfigOnlyOnVersionChange = updateConfigOnlyOnVersionChange;
        this.clientType = clientType;
        this.useFolsomStringClient = folsomStringClient;
        this.folsomStringClientCharset = folsomCharset;
        this.folsomConnections = folsomConnections;
        this.folsomRequestTimeout = folsomRequestTimeout;
        this.folsomOutstandingRequests = folsomMaxOutstandingRequests;
    }

    public MemcachedCacheConfig getMemcachedCacheConfig() {
        return memcachedCacheConfig;
    }

    public Duration getConfigPollingTime() {
        return configPollingTime;
    }

    public Duration getInitialConfigPollingDelay() {
        return initialConfigPollingDelay;
    }

    public Duration getConnectionTimeoutInMillis() {
        return connectionTimeoutInMillis;
    }

    public Duration getIdleReadTimeout() {
        return idleReadTimeout;
    }

    public Duration getReconnectDelay() {
        return reconnectDelay;
    }

    public Duration getDelayBeforeClientClose() {
        return delayBeforeClientClose;
    }

    public int getNumberOfConsecutiveInvalidConfigurationsBeforeReconnect() {
        return numberOfConsecutiveInvalidConfigurationsBeforeReconnect;
    }

    public boolean isUpdateConfigVersionOnDnsTimeout() {
        return updateConfigVersionOnDnsTimeout;
    }

    public String getElastiCacheConfigHosts() {
        return elastiCacheConfigHosts;
    }

    public List<ClientClusterUpdateObserver> getClusterUpdatedObservers() {
        return this.clusterUpdatedObservers;
    }

    public Optional<ElastiCacheConfigServerUpdater> getConfigUrlUpdater() {
        return this.configUrlUpdater;
    }

    public boolean isUpdateConfigOnlyOnVersionChange() {
        return updateConfigOnlyOnVersionChange;
    }

    public MemcachedClientType getClientType() {
        return clientType;
    }

    public boolean useFolsomStringClient() {
        return useFolsomStringClient;
    }

    public Charset getFolsomStringClientCharset() {
        return folsomStringClientCharset;
    }

    public int getFolsomConnections() {
        return folsomConnections;
    }

    public long getFolsomRequestTimeout() {
        return folsomRequestTimeout;
    }

    public int getFolsomOutstandingRequests() {
        return folsomOutstandingRequests;
    }
}
