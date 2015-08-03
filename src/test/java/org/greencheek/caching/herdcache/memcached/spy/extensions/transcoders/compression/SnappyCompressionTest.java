package org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.compression;

import org.greencheek.caching.herdcache.memcached.util.TestCacheValues;
import org.junit.Test;

import static org.junit.Assert.*;

public class SnappyCompressionTest {

    @Test
    public void testCompression() throws Exception {
        SnappyCompression compression = new SnappyCompression();
        byte[] compressed = compression.compress(TestCacheValues.LARGE_CACHE_VALUE_BYTES);

        System.out.println("=============");
        System.out.println("Compression Length: " + compressed.length);
        System.out.println("=============");
        assertTrue(compressed.length < TestCacheValues.LARGE_CACHE_VALUE_BYTES.length);

        assertArrayEquals(TestCacheValues.LARGE_CACHE_VALUE_BYTES,compression.decompress(compressed));
    }

}