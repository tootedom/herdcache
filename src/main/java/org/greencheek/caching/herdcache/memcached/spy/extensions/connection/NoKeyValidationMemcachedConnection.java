package org.greencheek.caching.herdcache.memcached.spy.extensions.connection;

import net.spy.memcached.*;
import net.spy.memcached.ops.Operation;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;

/**
 * Created by dominictootell on 03/05/2014.
 *
 * The filter uses hashing to create the cache key.  Therefore,
 * the key is ALWAYS valid.  As a result, the validate key operation
 * in the original spy memcached is not required.  This
 * extends the MemcachedConnection to remove the key validation from the enqueueOperation
 */
public class NoKeyValidationMemcachedConnection extends MemcachedConnection {


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
    public NoKeyValidationMemcachedConnection(int bufSize, ConnectionFactory f, List<InetSocketAddress> a, Collection<ConnectionObserver> obs, FailureMode fm, OperationFactory opfactory) throws IOException {
        super(bufSize, f, a, obs, fm, opfactory);
    }

    /**
     * Enqueue the given {@link net.spy.memcached.ops.Operation} with the used key.
     *
     * @param key the key to use.
     * @param o the {@link net.spy.memcached.ops.Operation} to enqueue.
     */
    public void enqueueOperation(final String key, final Operation o) {
        checkState();
        addOperation(key, o);
    }
}
