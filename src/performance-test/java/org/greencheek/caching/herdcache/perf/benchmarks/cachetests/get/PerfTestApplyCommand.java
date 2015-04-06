package org.greencheek.caching.herdcache.perf.benchmarks.cachetests.get;

import org.greencheek.caching.herdcache.memcached.util.TestCacheValues;
import org.greencheek.caching.herdcache.perf.benchmarks.cachetests.cacheobjects.*;
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

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public String applyFolsomTest(FolsomMemcachedCache cache) throws IOException, ExecutionException, InterruptedException {
        return cache.cache.apply("key",
                () -> {return "value1";}
        ).get();
    }


    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public String applyNoKeyHashingJenkinsTest(NoKeyHashingJenkinsHashSpyMemcachedCache cache) throws IOException, ExecutionException, InterruptedException {
        return cache.cache.apply("key",
                () -> {return "value1";}
        ).get();
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public String applySHA256HashingJenkinsTest(SHA256KeyHashingJenkinsHashSpyMemcachedCache cache) throws IOException, ExecutionException, InterruptedException {
        return cache.cache.apply("key",
                () -> {return "value1";}
        ).get();
    }


    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public String applyDefaultKetamaHashAlgoTestLargeValue(BinaryAsciiOnlyKeysSpyMemcachedCache cache) throws IOException, ExecutionException, InterruptedException {
        return cache.cache.apply("key",
                () -> {return TestCacheValues.LARGE_CACHE_VALUE;}
        ).get();
    }


    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public String applyJenkinsHashAlgoTestLargeValue(JenkinsHashSpyMemcachedCache cache) throws IOException, ExecutionException, InterruptedException {
        return cache.cache.apply("key",
                () -> {return TestCacheValues.LARGE_CACHE_VALUE;}
        ).get();
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public String applyXXHashAlgoTestLargeValue(XXHashAlgoSpyMemcachedCache cache) throws IOException, ExecutionException, InterruptedException {
        return cache.cache.apply("key",
                () -> {return TestCacheValues.LARGE_CACHE_VALUE;}
        ).get();
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public String applyFolsomTestLargeValue(FolsomMemcachedCache cache) throws IOException, ExecutionException, InterruptedException {
        return cache.cache.apply("key",
                () -> {return TestCacheValues.LARGE_CACHE_VALUE;}
        ).get();
    }


    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public String applyNoKeyHashingJenkinsTestLargeValue(NoKeyHashingJenkinsHashSpyMemcachedCache cache) throws IOException, ExecutionException, InterruptedException {
        return cache.cache.apply("key",
                () -> {return TestCacheValues.LARGE_CACHE_VALUE;}
        ).get();
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public String applySHA256HashingJenkinsTestLargeValue(SHA256KeyHashingJenkinsHashSpyMemcachedCache cache) throws IOException, ExecutionException, InterruptedException {
        return cache.cache.apply("key",
                () -> {return TestCacheValues.LARGE_CACHE_VALUE;}
        ).get();
    }

}
