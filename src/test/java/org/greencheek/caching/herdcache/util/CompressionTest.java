package org.greencheek.caching.herdcache.util;

import org.greencheek.caching.herdcache.memcached.util.TestCacheValues;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by dominictootell on 03/04/2015.
 */
public class CompressionTest {

    @Test
    public void testCompressionViaSnappyIsDecompressable() throws IOException {
        byte[] compressed = org.iq80.snappy.Snappy.compress(TestCacheValues.LARGE_CACHE_VALUE.getBytes("UTF-8"));

        String value = new String(org.xerial.snappy.Snappy.uncompress(compressed));

        assertEquals(TestCacheValues.LARGE_CACHE_VALUE,value);
    }
}
