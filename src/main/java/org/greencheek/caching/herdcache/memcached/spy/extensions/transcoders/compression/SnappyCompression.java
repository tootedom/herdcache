package org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.compression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

/**
 *
 */
public class SnappyCompression implements Compression {

    private static Logger logger = LoggerFactory.getLogger(SnappyCompression.class);

    /**
     * Decompress the given array of bytes.
     *
     * @return null if the bytes cannot be decompressed
     */
    public byte[] decompress(byte[] in) {
        if(in == null) {
            throw new CompressionException("Cannot decompress null bytes");
        }

        byte[] decompressed;
        try {
            decompressed = Snappy.uncompress(in);
        } catch (Throwable e) {
            logger.warn("Failed to decompress data", e);
            return null;
        }

        return decompressed;
    }

    /**
     * Compress the given array of bytes.
     */
    public byte[] compress(byte[] in) {
        if (in == null) {
            throw new CompressionException("Cannot compress null bytes");
        }

        byte[] compressed;
        try {
            compressed = Snappy.compress(in);
        } catch (Throwable e) {
            throw new CompressionException("IO exception compressing data", e);
        }
        logger.debug("Compressed {} bytes to {}", in.length, compressed.length);
        return compressed;
    }
}
