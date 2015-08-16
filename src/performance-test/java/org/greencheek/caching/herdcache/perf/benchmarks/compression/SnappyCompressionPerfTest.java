package org.greencheek.caching.herdcache.perf.benchmarks.compression;

import org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.compression.Compression;
import org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.compression.LZ4NativeCompression;
import org.greencheek.caching.herdcache.memcached.spy.extensions.transcoders.compression.SnappyCompression;
import org.greencheek.caching.herdcache.memcached.util.TestCacheValues;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;

import java.io.IOException;


/**
 * Created by dominictootell on 03/04/2015.
 */
public class SnappyCompressionPerfTest {

    private final static Compression compression = new SnappyCompression();

//    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public byte[] compress() throws IOException {
        return compression.compress(TestCacheValues.LARGE_CACHE_VALUE_BYTES);
    }

//    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public byte[] compressAndDecompress() throws IOException {
        byte[] compressed = compression.compress(TestCacheValues.LARGE_CACHE_VALUE_BYTES);
        return compression.decompress(compressed);
    }


}
