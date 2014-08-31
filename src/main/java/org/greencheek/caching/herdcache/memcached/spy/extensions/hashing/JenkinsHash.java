package org.greencheek.caching.herdcache.memcached.spy.extensions.hashing;

import net.spy.memcached.HashAlgorithm;

import java.io.UnsupportedEncodingException;

/**
 * Taken from xmemcached:
 * https://github.com/killme2008/xmemcached/blob/master/src/main/java/net/rubyeye/xmemcached/HashAlgorithm.java
 */
public class JenkinsHash implements HashAlgorithm {
    @Override
    public long hash(String k) {
        try {
            int hash = 0;
            for (byte bt : k.getBytes("UTF-8")) {
                hash += (bt & 0xFF);
                hash += (hash << 10);
                hash ^= (hash >>> 6);
            }
            hash += (hash << 3);
            hash ^= (hash >>> 11);
            hash += (hash << 15);

            // the hash variable in the original C code is a uint32.
            // convert the java signed int to an "unsigned",
            // represented via a long:
            return hash & 0xFFFFFFFFl;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Hash function error", e);
        }
    }
}