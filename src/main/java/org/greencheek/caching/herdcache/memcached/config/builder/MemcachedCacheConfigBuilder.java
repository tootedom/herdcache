package org.greencheek.caching.herdcache.memcached.config.builder;

import net.spy.memcached.*;
import net.spy.memcached.transcoders.Transcoder;
import org.greencheek.caching.herdcache.memcached.config.MemcachedCacheConfig;
import org.greencheek.caching.herdcache.memcached.config.hostparsing.CommaSeparatedHostAndPortStringParser;
import org.greencheek.caching.herdcache.memcached.config.hostparsing.HostStringParser;
import org.greencheek.caching.herdcache.memcached.dns.lookup.AddressByNameHostResolver;
import org.greencheek.caching.herdcache.memcached.dns.lookup.HostResolver;
import org.greencheek.caching.herdcache.memcached.factory.MemcachedClientFactory;
import org.greencheek.caching.herdcache.memcached.factory.SpyMemcachedClientFactory;
import org.greencheek.caching.herdcache.memcached.keyhashing.KeyHashingType;
import org.greencheek.caching.herdcache.memcached.spy.extensions.FastSerializingTranscoder;
import org.greencheek.caching.herdcache.memcached.spyconnectionfactory.SpyConnectionFactoryBuilder;

import java.time.Duration;
import java.util.Optional;

/**
 * Created by dominictootell on 23/08/2014.
 */
public abstract class MemcachedCacheConfigBuilder implements CacheConfigBuilder {

    private Duration timeToLive =  Duration.ofSeconds(60);
    private int maxCapacity = 1000;
    private  String memcachedHosts = "localhost:11211";
    private  ConnectionFactoryBuilder.Locator hashingType  = ConnectionFactoryBuilder.Locator.CONSISTENT;
    private  FailureMode failureMode = FailureMode.Redistribute;
    private  HashAlgorithm hashAlgorithm = DefaultHashAlgorithm.KETAMA_HASH;
    private  Transcoder<Object> serializingTranscoder = new FastSerializingTranscoder();
    private  ConnectionFactoryBuilder.Protocol protocol = ConnectionFactoryBuilder.Protocol.BINARY;
    private  int readBufferSize = DefaultConnectionFactory.DEFAULT_READ_BUFFER_SIZE;
    private  Duration memcachedGetTimeout  = Duration.ofMillis(2500);
    private  Duration dnsConnectionTimeout = Duration.ofSeconds(3);
    private  boolean waitForMemcachedSet  = false;
    private  Duration setWaitDuration = Duration.ofSeconds(2);
    private  boolean allowFlush = false;
    private  boolean waitForMemcachedRemove = false;
    private  Duration removeWaitDuration = Duration.ofSeconds(2);
    private  KeyHashingType keyHashType = KeyHashingType.JAVA_XXHASH;
    private  Optional<String> keyPrefix = Optional.empty();
    private  boolean asciiOnlyKeys = false;
    private HostStringParser hostStringParser = new CommaSeparatedHostAndPortStringParser();
    private  HostResolver hostResolver = new AddressByNameHostResolver();
    private  boolean useStaleCache = false;
    private  Duration staleCacheAdditionalTimeToLive = Duration.ZERO;
    private  String staleCachePrefix = "stale";
    private  int staleMaxCapacity = -1;
    private  Duration staleCacheMemachedGetTimeout = Duration.ZERO;
    private MemcachedClientFactory clientFactory;

    public MemcachedCacheConfig buildMemcachedConfig()
    {
       return new MemcachedCacheConfig(
               createClientFactory(),
               timeToLive,
               maxCapacity,memcachedHosts,
               hashingType,
               failureMode,
               hashAlgorithm,serializingTranscoder,
               protocol,readBufferSize,memcachedGetTimeout,
               dnsConnectionTimeout,waitForMemcachedSet,
               setWaitDuration,allowFlush,waitForMemcachedRemove,
               removeWaitDuration,keyHashType,keyPrefix,asciiOnlyKeys,
               hostStringParser,hostResolver,
               useStaleCache,staleCacheAdditionalTimeToLive,staleCachePrefix,
               staleMaxCapacity,staleCacheMemachedGetTimeout);
    }


    public MemcachedClientFactory createClientFactory() {
        if(clientFactory==null) {
            clientFactory = new SpyMemcachedClientFactory(memcachedHosts,
                    dnsConnectionTimeout,hostStringParser,hostResolver,createMemcachedConnectionFactory());
        }
        return clientFactory;
    }

    public ConnectionFactory createMemcachedConnectionFactory() {
        return SpyConnectionFactoryBuilder.createConnectionFactory(
                hashingType,failureMode,hashAlgorithm,serializingTranscoder,protocol,
                readBufferSize,keyHashType);
    }

    public MemcachedCacheConfigBuilder setTimeToLive(Duration timeToLive) {
        this.timeToLive = timeToLive;
        return this;
    }

    public MemcachedCacheConfigBuilder setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
        return this;
    }

    public MemcachedCacheConfigBuilder setMemcachedHosts(String memcachedHosts) {
        this.memcachedHosts = memcachedHosts;
        return this;
    }

    public MemcachedCacheConfigBuilder setHashingType(ConnectionFactoryBuilder.Locator hashingType) {
        this.hashingType = hashingType;
        return this;
    }

    public MemcachedCacheConfigBuilder setFailureMode(FailureMode failureMode) {
        this.failureMode = failureMode;
        return this;
    }

    public MemcachedCacheConfigBuilder setHashAlgorithm(HashAlgorithm hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
        return this;
    }

    public MemcachedCacheConfigBuilder setSerializingTranscoder(Transcoder<Object> serializingTranscoder) {
        this.serializingTranscoder = serializingTranscoder;
        return this;
    }

    public MemcachedCacheConfigBuilder setProtocol(ConnectionFactoryBuilder.Protocol protocol) {
        this.protocol = protocol;
        return this;
    }

    public MemcachedCacheConfigBuilder setReadBufferSize(int readBufferSize) {
        this.readBufferSize = readBufferSize;
        return this;
    }

    public MemcachedCacheConfigBuilder setMemcachedGetTimeout(Duration memcachedGetTimeout) {
        this.memcachedGetTimeout = memcachedGetTimeout;
        return this;
    }

    public MemcachedCacheConfigBuilder setDnsConnectionTimeout(Duration dnsConnectionTimeout) {
        this.dnsConnectionTimeout = dnsConnectionTimeout;
        return this;
    }

    public MemcachedCacheConfigBuilder setWaitForMemcachedSet(boolean waitForMemcachedSet) {
        this.waitForMemcachedSet = waitForMemcachedSet;
        return this;
    }

    public MemcachedCacheConfigBuilder setSetWaitDuration(Duration setWaitDuration) {
        this.setWaitDuration = setWaitDuration;
        return this;
    }

    public MemcachedCacheConfigBuilder setAllowFlush(boolean allowFlush) {
        this.allowFlush = allowFlush;
        return this;
    }

    public MemcachedCacheConfigBuilder setWaitForMemcachedRemove(boolean waitForMemcachedRemove) {
        this.waitForMemcachedRemove = waitForMemcachedRemove;
        return this;
    }

    public MemcachedCacheConfigBuilder setRemoveWaitDuration(Duration removeWaitDuration) {
        this.removeWaitDuration = removeWaitDuration;
        return this;
    }

    public MemcachedCacheConfigBuilder setKeyHashType(KeyHashingType keyHashType) {
        this.keyHashType = keyHashType;
        return this;
    }

    public MemcachedCacheConfigBuilder setKeyPrefix(Optional<String> keyPrefix) {
        this.keyPrefix = keyPrefix;
        return this;
    }

    public MemcachedCacheConfigBuilder setAsciiOnlyKeys(boolean asciiOnlyKeys) {
        this.asciiOnlyKeys = asciiOnlyKeys;
        return this;
    }

    public MemcachedCacheConfigBuilder setHostStringParser(HostStringParser hostStringParser) {
        this.hostStringParser = hostStringParser;
        return this;
    }

    public MemcachedCacheConfigBuilder setHostResolver(HostResolver hostResolver) {
        this.hostResolver = hostResolver;
        return this;
    }

    public MemcachedCacheConfigBuilder setUseStaleCache(boolean useStaleCache) {
        this.useStaleCache = useStaleCache;
        return this;
    }

    public MemcachedCacheConfigBuilder setStaleCacheAdditionalTimeToLive(Duration staleCacheAdditionalTimeToLive) {
        this.staleCacheAdditionalTimeToLive = staleCacheAdditionalTimeToLive;
        return this;
    }

    public MemcachedCacheConfigBuilder setStaleCachePrefix(String staleCachePrefix) {
        this.staleCachePrefix = staleCachePrefix;
        return this;
    }

    public MemcachedCacheConfigBuilder setStaleMaxCapacity(int staleMaxCapacity) {
        this.staleMaxCapacity = staleMaxCapacity;
        return this;
    }

    public MemcachedCacheConfigBuilder setStaleCacheMemachedGetTimeout(Duration staleCacheMemachedGetTimeout) {
        this.staleCacheMemachedGetTimeout = staleCacheMemachedGetTimeout;
        return this;
    }
}
