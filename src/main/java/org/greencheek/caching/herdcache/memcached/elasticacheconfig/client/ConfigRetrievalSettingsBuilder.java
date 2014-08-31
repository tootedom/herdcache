package org.greencheek.caching.herdcache.memcached.elasticacheconfig.client;

import org.greencheek.caching.herdcache.memcached.elasticacheconfig.confighandler.AsyncConfigInfoMessageHandler;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.confighandler.AsyncExecutorServiceConfigInfoMessageHandler;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.confighandler.ConfigInfoProcessor;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.confighandler.SystemOutConfigInfoProcessor;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.handler.AsciiRequestConfigInfoScheduler;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.handler.RequestConfigInfoScheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by dominictootell on 20/07/2014.
 */
public class ConfigRetrievalSettingsBuilder {

    public static final String ELASTICACHE_HOST = "localhost";
    public static final int ELASTICACHE_PORT = 11211;


    public static final TimeUnit RECONNECT_DELAY_TIMEUNIT = TimeUnit.SECONDS;
    public static final long RECONNECT_DELAY = 5;

    public static final TimeUnit GET_CONFIG_POLLING_TIMEUNIT = TimeUnit.SECONDS;
    public static final long GET_CONFIG_POLLING_TIME = 60;
    public static final long GET_CONFIG_POLLING_INITIAL_DELAY = 0;

    public static final int CONNECTION_TIMEOUT_IN_MILLIS = 3000;

    // If not response recieved for the last poll, then re-establish the connection
    public static final TimeUnit IDLE_TIMEOUT_TIMEUNIT = TimeUnit.SECONDS;
    public static final long IDLE_READ_TIMEOUT = 70;

    public static final int NO_CONSECUTIVE_INVALID_CONFIGS_BEFORE_RECONNECT = 5;


    private RequestConfigInfoScheduler clientConfigInfoScheduler = null;
    private AsyncConfigInfoMessageHandler configInfoMessageHandler = null;
    private ConfigInfoProcessor configInfoProcessor = null;
//
//
    private TimeUnit idleTimeoutTimeUnit = IDLE_TIMEOUT_TIMEUNIT;
    private long idleReadTimeout = IDLE_READ_TIMEOUT;

    private TimeUnit reconnectDelayTimeUnit = RECONNECT_DELAY_TIMEUNIT;
    private long reconnectDelay = RECONNECT_DELAY;

    private int connectionTimeoutInMillis = CONNECTION_TIMEOUT_IN_MILLIS;

    private TimeUnit configPollingTimeUnit = GET_CONFIG_POLLING_TIMEUNIT;
    private long configPollingTime = GET_CONFIG_POLLING_TIME;
    private long configPollingInitialDelay = GET_CONFIG_POLLING_INITIAL_DELAY;

    private int numberOfInvalidConfigsBeforeReconnect = NO_CONSECUTIVE_INVALID_CONFIGS_BEFORE_RECONNECT;

    private List<ElastiCacheServerConnectionDetails> elasticCacheHosts = new ArrayList<ElastiCacheServerConnectionDetails>();

    public  ConfigRetrievalSettingsBuilder addElastiCacheHost(ElastiCacheServerConnectionDetails details) {
        elasticCacheHosts.add(details);
        return this;
    }

    public  ConfigRetrievalSettingsBuilder addElastiCacheHosts(ElastiCacheServerConnectionDetails[] details) {
        for(ElastiCacheServerConnectionDetails host : details) {
            elasticCacheHosts.add(host);
        }
        return this;
    }



    public ConfigRetrievalSettingsBuilder setIdleReadTimeout(long idleReadTimeout,TimeUnit idleTimeoutTimeUnit) {
        this.idleReadTimeout = idleReadTimeout;
        this.idleTimeoutTimeUnit = idleTimeoutTimeUnit;
        return this;
    }

    public ConfigRetrievalSettingsBuilder setReconnectDelay(long reconnectDelay,TimeUnit reconnectDelayTimeUnit) {
        this.reconnectDelay = reconnectDelay;
        this.reconnectDelayTimeUnit = reconnectDelayTimeUnit;
        return this;
    }


    public ConfigRetrievalSettingsBuilder setConfigPollingTime(long configPollingInitialDelay, long configPollingTime,TimeUnit configPollingTimeUnit) {
        this.configPollingTime = configPollingTime;
        this.configPollingTimeUnit = configPollingTimeUnit;
        this.configPollingInitialDelay = configPollingInitialDelay;
        return this;
    }

    public ConfigRetrievalSettingsBuilder setClientConfigInfoScheduler(RequestConfigInfoScheduler periodicConfigObtainer) {
        this.clientConfigInfoScheduler = periodicConfigObtainer;
        return this;
    }

    public ConfigRetrievalSettingsBuilder setConfigInfoMessageHandler(AsyncConfigInfoMessageHandler newConfigMessageHandler) {
        this.configInfoMessageHandler = newConfigMessageHandler;
        return this;
    }

    public ConfigRetrievalSettingsBuilder setConfigInfoProcessor(ConfigInfoProcessor configInfoProcessor) {
        this.configInfoProcessor = configInfoProcessor;
        return this;
    }

    public ConfigRetrievalSettingsBuilder setNumberOfInvalidConfigsBeforeReconnect(int numberOfInvalidConfigsBeforeReconnect) {
        this.numberOfInvalidConfigsBeforeReconnect = numberOfInvalidConfigsBeforeReconnect;
        return this;
    }

    public ConfigRetrievalSettingsBuilder setConnectionTimeoutInMillis(int connectionTimeoutInMillis) {
        this.connectionTimeoutInMillis = connectionTimeoutInMillis;
        return this;
    }

    public ConfigRetrievalSettings build() {
        AsyncConfigInfoMessageHandler messageHandler;
        RequestConfigInfoScheduler periodicConfigObtainer;
        ConfigInfoProcessor configInfoProcessor;

        if(clientConfigInfoScheduler==null) {
            periodicConfigObtainer = new AsciiRequestConfigInfoScheduler(configPollingTimeUnit, configPollingInitialDelay, configPollingTime);
        } else {
            periodicConfigObtainer = clientConfigInfoScheduler;
        }

        if(this.configInfoProcessor==null) {
            configInfoProcessor = new SystemOutConfigInfoProcessor();
        } else {
            configInfoProcessor = this.configInfoProcessor;
        }

        if(configInfoMessageHandler==null) {
            messageHandler = new AsyncExecutorServiceConfigInfoMessageHandler(configInfoProcessor);
        } else {
            messageHandler = configInfoMessageHandler;
        }


        return new ConfigRetrievalSettings(periodicConfigObtainer,messageHandler,
                elasticCacheHosts.toArray(new ElastiCacheServerConnectionDetails[elasticCacheHosts.size()]),idleTimeoutTimeUnit,idleReadTimeout,
                reconnectDelayTimeUnit,reconnectDelay, numberOfInvalidConfigsBeforeReconnect,connectionTimeoutInMillis);
    }



}
