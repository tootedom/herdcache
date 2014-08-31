package org.greencheek.caching.herdcache.memcached.dns.lookup;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.*;

/**
 * As taken from http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6450279
 * In order to have a timeout on looking up the memcached ip address.
 *
 * Without this, the application could be waiting on a dns resolution for a while.
 * The amount of time to wait for the DNS resolution is now configurable via this class.
 *
 */
public class LookupService {
    private ExecutorService executor;

    public LookupService() {
        executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
        });
    }

    public static LookupService create() {
        return new LookupService();
    }

    public Future<InetAddress> getByName(final String host) {
        FutureTask<InetAddress> future = new FutureTask<InetAddress>(
                new Callable<InetAddress>() {
                    public InetAddress call() throws UnknownHostException {
                        return InetAddress.getByName(host);
                    }
                }
        );
        executor.execute(future);
        return future;
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}