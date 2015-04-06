package org.greencheek.caching.herdcache.memcached.keyhashing;

/**
 * Created by dominictootell on 08/04/2014.
 */
public enum KeyHashingType {
    NONE,
    NATIVE_XXHASH,
    NATIVE_XXHASH_64,
    JAVA_XXHASH,
    JAVA_XXHASH_64,
    MD5_UPPER,
    SHA256_UPPER,
    MD5_LOWER,
    SHA256_LOWER
}
