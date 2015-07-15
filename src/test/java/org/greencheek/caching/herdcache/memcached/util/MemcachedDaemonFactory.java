package org.greencheek.caching.herdcache.memcached.util;

import com.thimbleware.jmemcached.*;
import com.thimbleware.jmemcached.storage.CacheStorage;
import com.thimbleware.jmemcached.storage.hash.ConcurrentLinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.ServerSocket;

/**
 * Created by dominictootell on 20/04/2014.
 */
public class MemcachedDaemonFactory {
    private static Logger logger = LoggerFactory.getLogger("MemcachedDaemonFactory");

    public static MemcachedDaemonWrapper createMemcachedDaemon(boolean binary) {
        ServerSocket portServerSocket = PortUtil.findFreePort();
        return startMemcachedDaemon(binary,portServerSocket);
    }

    private static MemcachedDaemonWrapper startMemcachedDaemon(boolean binary,ServerSocket portServerSocket) {
        int port = -1;
        try {
            MemCacheDaemon<LocalCacheElement> daemon = new MemCacheDaemon<LocalCacheElement>();


            CacheStorage<Key, LocalCacheElement> cacheStorage = ConcurrentLinkedHashMap.create(ConcurrentLinkedHashMap.EvictionPolicy.LRU, 1000, 512000);
            Cache<LocalCacheElement> cacheImpl = new CacheImpl(cacheStorage);
            daemon.setCache(cacheImpl);
            daemon.setAddr(new InetSocketAddress("localhost", portServerSocket.getLocalPort()));
            daemon.setIdleTime(100000);
            daemon.setBinary(binary);
            daemon.setVerbose(true);
            port =  PortUtil.getPort(portServerSocket);
            daemon.start();
            Thread.sleep(500);
            return new MemcachedDaemonWrapper(daemon, port);
        } catch (Exception e) {
            logger.error("Error starting memcached", e);
            return new MemcachedDaemonWrapper(null, port);
        }

    }

    public static void stopMemcachedDaemon(MemcachedDaemonWrapper memcachedDaemon) {
        if (memcachedDaemon.getDaemon() != null) {
            if (memcachedDaemon.getDaemon().isRunning()) {
                System.out.println("Shutting down the Memcached Daemon");
                memcachedDaemon.getDaemon().stop();

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

