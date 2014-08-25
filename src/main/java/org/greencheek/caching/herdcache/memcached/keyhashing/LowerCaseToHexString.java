package org.greencheek.caching.herdcache.memcached.keyhashing;

/**
 * Created by dominictootell on 09/04/2014.
 */
public class LowerCaseToHexString implements ToHexString {
    public static final char[] DIGITS_LOWER = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
    public static final LowerCaseToHexString INSTANCE = new LowerCaseToHexString();

    @Override
    public String bytesToHex(byte[] data) {
        return ToHexStringUtil.bytesToHex(data,DIGITS_LOWER);
    }
}
