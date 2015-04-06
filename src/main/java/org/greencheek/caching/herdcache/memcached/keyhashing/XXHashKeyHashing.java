package org.greencheek.caching.herdcache.memcached.keyhashing;

import net.jpountz.xxhash.XXHashFactory;

import java.io.UnsupportedEncodingException;

/**
 * Created by dominictootell on 04/05/2014.
 */
public class XXHashKeyHashing implements KeyHashing {

    private final XXHashFactory factory;
    private final boolean use64BitHashing;

    public XXHashKeyHashing(boolean allowNative) {
         this(true,false);
    }

    public XXHashKeyHashing(boolean allowNative, boolean use64BitHashing) {
        if(allowNative) {
            factory = XXHashFactory.fastestInstance();
        } else {
            factory = XXHashFactory.fastestJavaInstance();
        }

        this.use64BitHashing = use64BitHashing;
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
        if(use64BitHashing) {
            return Long.toString(factory.hash64().hash(bytes, offset, length, 0));
        } else {
            return Integer.toString(factory.hash32().hash(bytes, offset, length, 0));
        }
    }
}
