package org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.compression;


/**
 * Interface that is used by the {@link org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.BaseSerializingTranscoder}
 * for performing compression
 */
public interface Compression {

    public static final Compression NONE = new Compression() {};
    /**
     * Compress the given array of bytes.
     */
    default public byte[] compress(byte[] in) {
        return in;
    }

    /**
     * Decompress the given array of bytes.
     *
     * @return null if the bytes cannot be decompressed
     */
    default public byte[] decompress(byte[] in) {
        return in;
    }

}
