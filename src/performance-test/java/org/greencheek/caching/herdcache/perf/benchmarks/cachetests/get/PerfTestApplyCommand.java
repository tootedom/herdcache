package org.greencheek.caching.herdcache.perf.benchmarks.cachetests.get;

import org.greencheek.caching.herdcache.perf.benchmarks.cachetests.cacheobjects.BinaryAsciiOnlyKeysSpyMemcachedCache;
import org.greencheek.caching.herdcache.perf.benchmarks.cachetests.cacheobjects.JenkinsHashSpyMemcachedCache;
import org.greencheek.caching.herdcache.perf.benchmarks.cachetests.cacheobjects.XXHashAlgoSpyMemcachedCache;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;

import java.io.IOException;
import java.util.concurrent.ExecutionException;


/**
 * Created by dominictootell on 03/04/2015.
 */
public class PerfTestApplyCommand {

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public String applyDefaultKetamaHashAlgoTest(BinaryAsciiOnlyKeysSpyMemcachedCache cache) throws IOException, ExecutionException, InterruptedException {
        return cache.cache.apply("key",
                () -> {return "value1";}
        ).get();
    }


    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public String applyJenkinsHashAlgoTest(JenkinsHashSpyMemcachedCache cache) throws IOException, ExecutionException, InterruptedException {
        return cache.cache.apply("key",
                () -> {return "value1";}
        ).get();
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public String applyXXHashAlgoTest(XXHashAlgoSpyMemcachedCache cache) throws IOException, ExecutionException, InterruptedException {
        return cache.cache.apply("key",
                () -> {return "value1";}
        ).get();
    }
}
