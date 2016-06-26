package org.greencheek.caching.herdcache.util;

import org.greencheek.caching.herdcache.memcached.config.MemcachedCacheConfig;
import org.greencheek.caching.herdcache.memcached.keyhashing.*;
import org.greencheek.caching.herdcache.util.keycreators.HashAfterPrefixPrependedCacheKeyCreator;
import org.greencheek.caching.herdcache.util.keycreators.HashBeforePrefixPrependedCacheKeyCreator;
import org.greencheek.caching.herdcache.util.keycreators.CacheKeyCreator;
import org.greencheek.caching.herdcache.util.keycreators.NoPrefixCacheKeyCreator;

/**
 *
 */
public interface CacheKeyCreatorFactory {
    public static final CacheKeyCreatorFactory DEFAULT_INSTANCE = new CacheKeyCreatorFactory() {};

    default CacheKeyCreator create(MemcachedCacheConfig config) {
        KeyHashing hasher;
        switch (config.getKeyHashType()) {
            case NONE:
                hasher = new NoKeyHashing();
                break;
            case NATIVE_XXHASH:
                hasher = new FastestXXHashKeyHashing();
                break;
            case NATIVE_XXHASH_64:
                hasher = new XXHashKeyHashing(true,true);
                break;
            case JAVA_XXHASH:
                hasher = new JavaXXHashKeyHashing();
                break;
            case JAVA_XXHASH_64:
                hasher = new XXHashKeyHashing(false,true);
                break;
            case MD5_UPPER:
                hasher = new MessageDigestHashing(KeyHashing.MD5,Runtime.getRuntime().availableProcessors()*2,true);
                break;
            case SHA256_UPPER:
                hasher = new MessageDigestHashing(KeyHashing.SHA256,Runtime.getRuntime().availableProcessors()*2,true);
                break;
            case MD5_LOWER:
                hasher = new MessageDigestHashing(KeyHashing.MD5,Runtime.getRuntime().availableProcessors()*2,false);
                break;
            case SHA256_LOWER:
                hasher = new MessageDigestHashing(KeyHashing.SHA256,Runtime.getRuntime().availableProcessors()*2,false);
                break;
            default:
                hasher = new FastestXXHashKeyHashing();
        }

        return create(config,hasher);
    }

    default CacheKeyCreator create(MemcachedCacheConfig config, KeyHashing hasher) {
        if(config.hasKeyPrefix()) {
            if(config.isHashKeyPrefix()) {
                return new HashAfterPrefixPrependedCacheKeyCreator(hasher,config.getKeyPrefix());
            } else {
                return new HashBeforePrefixPrependedCacheKeyCreator(hasher,config.getKeyPrefix());
            }
        } else {
            return new NoPrefixCacheKeyCreator(hasher);
        }
    }
}
