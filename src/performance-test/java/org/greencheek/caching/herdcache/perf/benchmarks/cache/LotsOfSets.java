package org.greencheek.caching.herdcache.perf.benchmarks.cache;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Created by dominictootell on 16/08/2015.
 */
public class LotsOfSets {

    private final static List<String> strings;
    private static int NUM = 100;
    static {
        List<String> randoms = new ArrayList<>(NUM);
        for(int i=0;i<NUM;i++) {
            randoms.add(UUID.randomUUID().toString());
        }

        strings = new ArrayList<>(randoms);
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public List<String> applySets(CacheObject cache) throws IOException, ExecutionException, InterruptedException {
        List<String> list = new ArrayList<>(NUM*2);
        for(int i =0;i<NUM;i++) {
            final String item = strings.get(i);
            list.add(cache.cache.apply(item,
                    () -> item
            ).get());
        }
        return list;
    }
}
