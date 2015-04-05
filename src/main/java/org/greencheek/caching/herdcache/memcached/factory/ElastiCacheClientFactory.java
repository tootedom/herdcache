package org.greencheek.caching.herdcache.memcached.factory;

import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.MemcachedClientIF;
import org.greencheek.caching.herdcache.memcached.dns.lookup.HostResolver;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.client.*;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.confighandler.ConfigInfoProcessor;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.configparsing.DefaultElastiCacheConfigParser;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.configprocessing.ElastiCacheConfigInfoProcessor;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.connection.UpdateClientService;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.connection.UpdateReferencedMemcachedClientService;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Created by dominictootell on 26/08/2014.
 */
public class ElastiCacheClientFactory implements MemcachedClientFactory {

    private final ReferencedClientFactory connnectionFactory;
    private final ElastiCacheServerConnectionDetails[] elastiCacheConfigHosts;
    private final Duration configPollingTime;
    private final Duration initialConfigPollingDelay;
    private final Duration idleReadTimeout;
    private final Duration reconnectDelay;
    private final int numberOfConsecutiveInvalidConfigurationsBeforeReconnect;
    private final Duration connectionTimeout;
    private final ConfigInfoProcessor suppliedConfigInfoProcessor;

    private final UpdateClientService memcachedClientHolder;
    private final PeriodicConfigRetrievalClient configRetrievalClient;

    public ElastiCacheClientFactory(ReferencedClientFactory connnectionFactory,
                                    ElastiCacheServerConnectionDetails[] elastiCacheConfigHosts,
                                    Duration configPollingTime,
                                    Duration initialConfigPollingDelay,
                                    Duration idleReadTimeout,
                                    Duration reconnectDelay,
                                    Duration delayBeforeClientClose,
                                    HostResolver dnsLookupService,
                                    Duration dnsLookupTimeout,
                                    boolean updateConfigVersionOnDnsTimeout,
                                    int numberOfConsecutiveInvalidConfigurationsBeforeReconnect,
                                    Duration connectionTimeout,
                                    List<ClientClusterUpdateObserver> clusterUpdateObservers,
                                    Optional<ElastiCacheConfigServerUpdater> configUrlUpdater,
                                    boolean updateConfigOnlyOnVersionChange) {
        this.connnectionFactory = connnectionFactory;
        this.elastiCacheConfigHosts = elastiCacheConfigHosts;
        this.configPollingTime = configPollingTime;
        this.initialConfigPollingDelay = initialConfigPollingDelay;
        this.idleReadTimeout = idleReadTimeout;
        this.reconnectDelay = reconnectDelay;
        this.numberOfConsecutiveInvalidConfigurationsBeforeReconnect = numberOfConsecutiveInvalidConfigurationsBeforeReconnect;
        this.connectionTimeout = connectionTimeout;


        memcachedClientHolder = new UpdateReferencedMemcachedClientService(dnsLookupService,
                dnsLookupTimeout, connnectionFactory, delayBeforeClientClose);

        suppliedConfigInfoProcessor = new ElastiCacheConfigInfoProcessor(new DefaultElastiCacheConfigParser(),
                memcachedClientHolder, updateConfigVersionOnDnsTimeout,clusterUpdateObservers,updateConfigOnlyOnVersionChange);
        ConfigRetrievalSettings elastiCacheConfigPeriodicConfigRetrievalSettings = createConfigRetrievalSettings(configUrlUpdater);

        configRetrievalClient = new PeriodicConfigRetrievalClient(elastiCacheConfigPeriodicConfigRetrievalSettings);
        configRetrievalClient.start();

    }

    @Override
    public ReferencedClient getClient() {
        return memcachedClientHolder.getClient();
    }

    @Override
    public boolean isEnabled() {
        return memcachedClientHolder.getClient().isAvailable();
    }

    @Override
    public void shutdown() {
        if(isEnabled()) {
            memcachedClientHolder.shutdown();
        }
       configRetrievalClient.stop();
    }


    private ConfigRetrievalSettings createConfigRetrievalSettings(Optional<ElastiCacheConfigServerUpdater> configUrlUpdater) {
        ConfigRetrievalSettingsBuilder builder = new ConfigRetrievalSettingsBuilder();

        builder.addElastiCacheHosts(elastiCacheConfigHosts)
                .setConfigPollingTime(initialConfigPollingDelay.toMillis(), configPollingTime.toMillis(), TimeUnit.MILLISECONDS)
                .setIdleReadTimeout(idleReadTimeout.toMillis(), TimeUnit.MILLISECONDS)
                .setReconnectDelay(reconnectDelay.toMillis(), TimeUnit.MILLISECONDS)
                .setNumberOfInvalidConfigsBeforeReconnect(numberOfConsecutiveInvalidConfigurationsBeforeReconnect)
                .setConfigInfoProcessor(suppliedConfigInfoProcessor)
                .setConnectionTimeoutInMillis((int) connectionTimeout.toMillis())
                .setConfigUrlUpdater(configUrlUpdater);

        return builder.build();
    }


}
