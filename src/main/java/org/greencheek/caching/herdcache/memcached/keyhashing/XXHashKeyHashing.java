package org.greencheek.caching.herdcache.memcached.keyhashing;

import net.jpountz.xxhash.XXHashFactory;

import java.io.UnsupportedEncodingException;

/**
 * Created by dominictootell on 04/05/2014.
 */
public class XXHashKeyHashing implements KeyHashing {

    private final XXHashFactory factory;

    public XXHashKeyHashing(boolean allowNative) {
        if(allowNative) {
            factory = XXHashFactory.fastestInstance();
        } else {
            factory = XXHashFactory.fastestJavaInstance();
        }
    }

    @Override
    public String hash(String key) {
        byte[] bytes;
        try {
            bytes = key.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            bytes = key.getBytes();
        }

        return hash(bytes,0,bytes.length);
    }

    @Override
    public String hash(byte[] bytes, int offset, int length) {
        return Integer.toString(factory.hash32().hash(bytes, offset, length, 0));
    }
}
