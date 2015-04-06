package org.greencheek.caching.herdcache.perf.benchmarks.compression;

import org.greencheek.caching.herdcache.memcached.util.TestCacheValues;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;

import java.io.IOException;


/**
 * Created by dominictootell on 03/04/2015.
 */
public class SnappyPerfTest {

//    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public byte[] xerialCompress() throws IOException {
        return org.xerial.snappy.Snappy.compress(TestCacheValues.LARGE_CACHE_VALUE_BYTES);
    }

//    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public byte[] iq80Compresss() {
        return org.iq80.snappy.Snappy.compress(TestCacheValues.LARGE_CACHE_VALUE_BYTES);
    }


//    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public byte[] xerialDecompress() throws IOException {
        return org.xerial.snappy.Snappy.uncompress(TestCacheValues.LARGE_COMPRESSED_BYTES);
    }

//    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public byte[] iq80Decompresss() {
        return org.iq80.snappy.Snappy.uncompress(TestCacheValues.LARGE_COMPRESSED_BYTES,0,TestCacheValues.LARGE_COMPRESSED_BYTES.length);
    }

}
