package org.greencheek.caching.herdcache.perf.benchmarks.spy.extensions.memcached;

import net.spy.memcached.*;
import net.spy.memcached.ops.Operation;
import org.greencheek.caching.herdcache.RequiresShutdown;
import org.greencheek.caching.herdcache.memcached.config.builder.ElastiCacheCacheConfigBuilder;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.JenkinsHash;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 */
@State(Scope.Benchmark)
public class KetamaNodeLocatorPerfObject {

    public NodeLocator locator;

    @Setup
    public void setUp() {
         locator = new KetamaNodeLocator(new ArrayList(){{
             add(new DoNothingMemcachedNode(2345));
             add(new DoNothingMemcachedNode(3456));
             add(new DoNothingMemcachedNode(4567));
         }},new JenkinsHash());
    }

    @TearDown
    public void tearDown() {

    }


}
