package org.greencheek.caching.herdcache.perf.benchmarks.spy.extensions.memcached;


import net.spy.memcached.NodeLocator;
import org.greencheek.caching.herdcache.memcached.spy.extensions.hashing.JenkinsHash;
import org.greencheek.caching.herdcache.memcached.spy.extensions.locator.CeilingKeyKetamaNodeLocator;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.ArrayList;

/**
 *
 */
@State(Scope.Benchmark)
public class CeilingKeyNodeLocatorPerfObject {

    public NodeLocator locator;

    @Setup
    public void setUp() {
         locator = new CeilingKeyKetamaNodeLocator(new ArrayList(){{
             add(new DoNothingMemcachedNode(2345));
             add(new DoNothingMemcachedNode(3456));
             add(new DoNothingMemcachedNode(4567));
         }},new JenkinsHash());
    }

    @TearDown
    public void tearDown() {

    }
}
