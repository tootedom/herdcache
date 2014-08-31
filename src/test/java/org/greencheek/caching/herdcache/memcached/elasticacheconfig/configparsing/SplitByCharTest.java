package org.greencheek.caching.herdcache.memcached.elasticacheconfig.configparsing;

import org.junit.Test;

import static org.junit.Assert.*;

public class SplitByCharTest {


    @Test
    public void testSplitingWithNoSplitChar() {
        assertEquals("test",SplitByChar.split("test",'|').get(0));
        assertEquals("test",SplitByChar.split("test",'|',false).get(0));
    }

    @Test
    public void testSplitingAtBeginning() {
        assertEquals("test",SplitByChar.split("|test",'|').get(0));
        assertEquals("",SplitByChar.split("|test",'|',false).get(0));
    }


    @Test
    public void testSplitingWithManyItems() {
        assertEquals("test",SplitByChar.split("test|test2|test3|test4|test5|",'|').get(0));
        assertEquals(5,SplitByChar.split("test|test2|test3|test4|test5|",'|').size());
        assertEquals("test2",SplitByChar.split("test|test2|test3|test4|test5|",'|').get(1));
        assertEquals("test3",SplitByChar.split("test|test2|test3|test4|test5|",'|').get(2));
        assertEquals("test4",SplitByChar.split("test|test2|test3|test4|test5|",'|').get(3));
        assertEquals("test5",SplitByChar.split("test|test2|test3|test4|test5|",'|').get(4));
    }

    @Test
    public void testSplitEmptyString() {
        assertEquals(0,SplitByChar.split("",'|').size());
        assertEquals(0,SplitByChar.split(null,'|').size());
    }

    @Test
    public void testSplitMultipleDelimetersTogetherAreCollapsedWhenRequested() {
        assertEquals("test",SplitByChar.split("test||||test2",'|').get(0));
        assertEquals("test2",SplitByChar.split("test||||test2",'|').get(1));

        assertEquals("test",SplitByChar.split("test||||test2",'|',true).get(0));
        assertEquals("test2",SplitByChar.split("test||||test2",'|',true).get(1));
    }

    @Test
    public void testSplitMultipleDelimetersTogetherAreNotCollapsedWhenRequested() {
        assertEquals("test",SplitByChar.split("test||||test2",'|',false).get(0));
        assertEquals(5,SplitByChar.split("test||||test2",'|',false).size());
    }
}