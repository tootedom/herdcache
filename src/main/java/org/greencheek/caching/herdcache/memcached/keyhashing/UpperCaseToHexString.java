package org.greencheek.caching.herdcache.memcached.keyhashing;

/**
 * Created by dominictootell on 09/04/2014.
 */
public class UpperCaseToHexString implements ToHexString {
    public static final char[] DIGITS_UPPER = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    public static final UpperCaseToHexString INSTANCE = new UpperCaseToHexString();

    @Override
    public String bytesToHex(byte[] data) {
        return ToHexStringUtil.bytesToHex(data,DIGITS_UPPER);
    }
}
