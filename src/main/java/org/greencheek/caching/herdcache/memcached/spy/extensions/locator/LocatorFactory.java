package org.greencheek.caching.herdcache.memcached.spy.extensions.locator;

import net.spy.memcached.*;

import java.util.List;

/**
 *
 */
public interface LocatorFactory {
    public static LocatorFactory DO_NOTHING = (x,y) -> null;
    public static LocatorFactory ARRAY_MOD = (nodes,algorithm) -> new ArrayModNodeLocator(nodes,algorithm);
    public static LocatorFactory KETAMA = (nodes,algorithm) -> new KetamaNodeLocator(nodes,algorithm);
    public static LocatorFactory KETAMA_CEILING_ARRAY = (nodes,algorithm) -> new CeilingKeyKetamaNodeLocator(nodes,algorithm);


    public NodeLocator createNodeLocator(List<MemcachedNode> nodes,HashAlgorithm algorithm);
}
