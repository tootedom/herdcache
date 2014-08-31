package org.greencheek.caching.herdcache.memcached.keyhashing;

/**
 * Created by dominictootell on 08/04/2014.
 */
public interface ToHexString {

    public String bytesToHex(byte[] data);
}
