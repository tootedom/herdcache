package org.greencheek.caching.herdcache.memcached.keyhashing;

/**
 * Created by dominictootell on 25/08/2014.
 */
public class NoKeyHashing implements KeyHashing {
    @Override
    public String hash(String key) {
        return key;
    }

    @Override
    public String hash(byte[] bytes, int offset, int length) {
        return new String(bytes,offset,length);
    }
}
