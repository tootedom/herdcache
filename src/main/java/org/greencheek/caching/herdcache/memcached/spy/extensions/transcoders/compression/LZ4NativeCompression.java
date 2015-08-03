package org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.compression;

import net.jpountz.lz4.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 *
 */
public class LZ4NativeCompression implements Compression {
    private static final Logger logger = LoggerFactory.getLogger(LZ4NativeCompression.class);

    final LZ4Compressor compressor = LZ4Factory.nativeInstance().fastCompressor();
    final LZ4FastDecompressor decompressor = LZ4Factory.nativeInstance().fastDecompressor();

    public static int byteArrayToInt(byte[] b)
    {
        return   b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }

    public static byte[] intToByteArray(int a)
    {
        return new byte[] {
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }

    /**
     * Compress the given array of bytes.
     */
    public byte[] compress(byte[] in) {
        if (in == null) {
            throw new CompressionException("Cannot compress null bytes");
//            return null;
        }
        final int srcLen = in.length;
        final int maxCompressedLength = compressor.maxCompressedLength(srcLen) + 4;
        final byte[] compressed = new byte[maxCompressedLength];

        final byte[] lengthArray = intToByteArray(srcLen);
        System.arraycopy(lengthArray, 0, compressed, 0, 4);

        final int compressedLength = compressor.compress(in, 0, srcLen, compressed, 4);
        return Arrays.copyOf(compressed, compressedLength + 4);
    }


    /**
     * Decompress the given array of bytes.
     *
     * @return null if the bytes cannot be decompressed
     */
    public byte[] decompress(byte[] in) {
        if(in == null || in.length<5) {
            throw new CompressionException("Cannot decompress null bytes");
        }

        int originalLength = byteArrayToInt(in);
        if(originalLength>0) {
            try {
                return decompressor.decompress(in, 4, originalLength);
            } catch(Throwable e) {
                logger.warn("Error during decompression", e);
                return null;
            }
        } else {
            logger.warn("Invalid initial byte array denoting original content length");
            return null;
        }
    }
}
