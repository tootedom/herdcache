package org.greencheek.caching.herdcache.memcached.spy.extensions.hashing;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class XXHashAlogrithmTest {

    XXHashAlogrithm algo = new XXHashAlogrithm();

    @Test
    public void testHashing() {
        assertEquals(1401757748, algo.hash("Hello "));

        assertEquals(-234433905 & 0x00000000FFFFFFFFl, algo.hash("Hello"));

        assertEquals(-2032373643 & 0x00000000FFFFFFFFl, algo.hash("AB"));

        assertEquals(2072705615, algo.hash("CD"));

        assertEquals(46947589, algo.hash(""));
    }


}