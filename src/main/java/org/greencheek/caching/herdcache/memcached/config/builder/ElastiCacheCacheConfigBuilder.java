package org.greencheek.caching.herdcache.memcached.config.builder;

import net.spy.memcached.ConnectionFactory;
import org.greencheek.caching.herdcache.memcached.config.ElastiCacheCacheConfig;
import org.greencheek.caching.herdcache.memcached.config.MemcachedCacheConfig;
import org.greencheek.caching.herdcache.memcached.config.MemcachedClientType;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.client.ClientClusterUpdateObserver;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.client.ElastiCacheConfigServerUpdater;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 *
 */
public class ElastiCacheCacheConfigBuilder extends MemcachedCacheConfigBuilder<ElastiCacheCacheConfigBuilder> {

    private String elastiCacheConfigHosts = "localhost:11211";
    private Duration configPollingTime  = Duration.ofSeconds(60);
    private Duration initialConfigPollingDelay = Duration.ZERO;
    private Duration connectionTimeoutInMillis = Duration.ofMillis(3000);
    private Duration idleReadTimeout = Duration.ofSeconds(125);
    private Duration reconnectDelay = Duration.ofSeconds(5);
    private Duration delayBeforeClientClose = Duration.ofSeconds(300);
    private int numberOfConsecutiveInvalidConfigurationsBeforeReconnect = 3;
    private boolean updateConfigVersionOnDnsTimeout = true;
    private List<ClientClusterUpdateObserver> clusterUpdatedObservers = new ArrayList<>();
    private Optional<ElastiCacheConfigServerUpdater> configUrlUpdater = Optional.empty();
    private boolean updateConfigOnlyOnVersionChange = false;
    private MemcachedClientType memcachedClientType = MemcachedClientType.SPY;
    private boolean useFolsomStringClient = false;
    Charset folsomCharset = Charset.forName("UTF-8");
    int folsomConnections = 10;
    long folsomRequestTimeout = 3000;
    int folsomMaxOutstandingRequests = 1000;


    public ElastiCacheCacheConfigBuilder setElastiCacheConfigHosts(String urls) {
        this.elastiCacheConfigHosts = urls;
        return self();
    }

    public ElastiCacheCacheConfigBuilder setConfigUrlUpdater(ElastiCacheConfigServerUpdater configUrlUpdater) {
        this.configUrlUpdater = Optional.of(configUrlUpdater);
        return self();
    }

    public ElastiCacheCacheConfigBuilder setConfigPollingTime(Duration configPollingTime) {
        this.configPollingTime = configPollingTime;
        return self();
    }

    public ElastiCacheCacheConfigBuilder setInitialConfigPollingDelay(Duration initialConfigPollingDelay) {
        this.initialConfigPollingDelay = initialConfigPollingDelay;
        return self();
    }

    public ElastiCacheCacheConfigBuilder setConnectionTimeoutInMillis(Duration connectionTimeoutInMillis) {
        this.connectionTimeoutInMillis = connectionTimeoutInMillis;
        return self();
    }

    public ElastiCacheCacheConfigBuilder setIdleReadTimeout(Duration idleReadTimeout) {
        this.idleReadTimeout = idleReadTimeout;
        return this;
    }

    public ElastiCacheCacheConfigBuilder setReconnectDelay(Duration reconnectDelay) {
        this.reconnectDelay = reconnectDelay;
        return self();
    }

    public ElastiCacheCacheConfigBuilder setDelayBeforeClientClose(Duration delayBeforeClientClose) {
        this.delayBeforeClientClose = delayBeforeClientClose;
        return self();
    }

    public ElastiCacheCacheConfigBuilder setNumberOfConsecutiveInvalidConfigurationsBeforeReconnect(int numberOfConsecutiveInvalidConfigurationsBeforeReconnect) {
        this.numberOfConsecutiveInvalidConfigurationsBeforeReconnect = numberOfConsecutiveInvalidConfigurationsBeforeReconnect;
        return self();
    }

    public ElastiCacheCacheConfigBuilder setUpdateConfigVersionOnDnsTimeout(boolean updateConfigVersionOnDnsTimeout) {
        this.updateConfigVersionOnDnsTimeout = updateConfigVersionOnDnsTimeout;
        return self();
    }

    public ElastiCacheCacheConfigBuilder addElastiCacheClientClusterUpdateObserver(ClientClusterUpdateObserver observer) {
        clusterUpdatedObservers.add(observer);
        return self();
    }

    public ElastiCacheCacheConfigBuilder setUpdateConfigOnlyOnVersionChange(boolean updateConfigOnlyOnVersionChange) {
        this.updateConfigOnlyOnVersionChange = updateConfigOnlyOnVersionChange;
        return self();
    }

    public ElastiCacheCacheConfigBuilder setMemcachedClientType(MemcachedClientType clientType) {
        this.memcachedClientType = clientType;
        return self();
    }

    public ElastiCacheCacheConfigBuilder setUseFolsomStringClient(boolean useStringClient) {
        this.useFolsomStringClient = useStringClient;
        return self();
    }

    public ElastiCacheCacheConfigBuilder setFolsomStringClientCharset(Charset charset) {
        this.folsomCharset = charset;
        return self();
    }

    public ElastiCacheCacheConfigBuilder setFolsomClientConnections(int connections) {
        this.folsomConnections = connections;
        return self();
    }

    public ElastiCacheCacheConfigBuilder setFolsomRequestTimeout(long timeout) {
        this.folsomRequestTimeout = timeout;
        return self();
    }

    public ElastiCacheCacheConfigBuilder setFolsomMaxOutstandingRequests(int maxRequests) {
        this.folsomMaxOutstandingRequests = maxRequests;
        return self();
    }

    @Override
    public ElastiCacheCacheConfig buildElastiCacheMemcachedConfig() {
        return new ElastiCacheCacheConfig(buildMemcachedConfig(),
                elastiCacheConfigHosts,
                configPollingTime,
                initialConfigPollingDelay,
                connectionTimeoutInMillis,
                idleReadTimeout,
                reconnectDelay,
                delayBeforeClientClose,
                numberOfConsecutiveInvalidConfigurationsBeforeReconnect,
                updateConfigVersionOnDnsTimeout,
                clusterUpdatedObservers,
                configUrlUpdater,
                updateConfigOnlyOnVersionChange,
                memcachedClientType,
                useFolsomStringClient,
                folsomCharset,
                folsomConnections,
                folsomRequestTimeout,
                folsomMaxOutstandingRequests
        );

    }

    @Override
    public MemcachedCacheConfig buildMemcachedConfig() {
        return super.buildMemcachedConfig();
    }

}
