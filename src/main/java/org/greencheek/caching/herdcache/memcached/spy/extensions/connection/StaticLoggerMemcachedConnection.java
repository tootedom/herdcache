package org.greencheek.caching.herdcache.memcached.spy.extensions.connection;

import net.spy.memcached.*;
import net.spy.memcached.compat.log.Logger;
import net.spy.memcached.compat.log.SLF4JLogger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;

/**
 */
public class StaticLoggerMemcachedConnection extends MemcachedConnection {
    private static final Logger logger = new SLF4JLogger(MemcachedConnection.class.getName());

    /**
     * Construct a {@link net.spy.memcached.MemcachedConnection}.
     *
     * @param bufSize   the size of the buffer used for reading from the server.
     * @param f         the factory that will provide an operation queue.
     * @param a         the addresses of the servers to connect to.
     * @param obs       the initial observers to add.
     * @param fm        the failure mode to use.
     * @param opfactory the operation factory.
     * @throws java.io.IOException if a connection attempt fails early
     */
    public StaticLoggerMemcachedConnection(int bufSize, ConnectionFactory f, List<InetSocketAddress> a, Collection<ConnectionObserver> obs, FailureMode fm, OperationFactory opfactory) throws IOException {
        super(bufSize, f, a, obs, fm, opfactory);
    }


    /**
     * Get a Logger instance for this class.
     *
     * @return an appropriate logger instance.
     */
    protected Logger getLogger() {
        return (logger);
    }
}
