package org.greencheek.caching.herdcache.perf.benchmarks.spy.extensions.memcached;


import net.spy.memcached.MemcachedNode;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 */
public class NodeLocatorPerfTest {

    private static List<String> strings;
    private static int NUM = 10000;
    static {
        List<String> randoms = new ArrayList<>(NUM);
        for(int i=0;i<NUM;i++) {
            randoms.add(UUID.randomUUID().toString());
        }

        strings = new ArrayList<>(randoms);
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public List<MemcachedNode> testKetemaNodeLocator(KetamaNodeLocatorPerfObject locatorPerfObject) {
        List<MemcachedNode> builder = new ArrayList<>(NUM + 1);
        for (int i = 0; i < NUM; i++) {
            builder.add(locatorPerfObject.locator.getPrimary(strings.get(i)));
        }
        return builder;
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public List<MemcachedNode> testCeilingKeyNodeLocator(CeilingKeyNodeLocatorPerfObject locatorPerfObject) {
        List<MemcachedNode> builder = new ArrayList<>(NUM+1);
        for(int i =0 ; i<NUM;i++) {
            builder.add(locatorPerfObject.locator.getPrimary(strings.get(i)));
        }
        return builder;
    }


    public static void main(String[] args) {
        System.out.println(UUID.randomUUID().toString().length());
    }
}
