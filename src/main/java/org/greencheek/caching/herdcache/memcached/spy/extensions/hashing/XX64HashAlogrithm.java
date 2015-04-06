package org.greencheek.caching.herdcache.memcached.spy.extensions.hashing;

import net.jpountz.xxhash.XXHashFactory;
import net.spy.memcached.HashAlgorithm;

import java.io.UnsupportedEncodingException;

/**
 * Created by dominictootell on 28/05/2014.
 */
public class XX64HashAlogrithm implements HashAlgorithm {
    private static final XXHashFactory factory = XXHashFactory.fastestJavaInstance();

    @Override
    public long hash(String k) {
        try {
            byte[] b = k.getBytes("UTF-8");
            return factory.hash64().hash(b, 0, b.length, 0) & 0xFFFFFFFFl;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Hash function error", e);
        }
    }
}
