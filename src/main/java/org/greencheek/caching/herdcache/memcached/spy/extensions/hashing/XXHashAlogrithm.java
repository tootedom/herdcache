package org.greencheek.caching.herdcache.memcached.spy.extensions.hashing;

import net.jpountz.xxhash.XXHashFactory;
import net.spy.memcached.HashAlgorithm;

/**
 * Created by dominictootell on 28/05/2014.
 */
public class XXHashAlogrithm implements HashAlgorithm {
    private static final XXHashFactory factory = XXHashFactory.fastestJavaInstance();

    @Override
    public long hash(String k) {
        byte[] b = k.getBytes();
        return factory.hash32().hash(b,0,b.length,0) & 0xFFFFFFFFl;
    }
}
