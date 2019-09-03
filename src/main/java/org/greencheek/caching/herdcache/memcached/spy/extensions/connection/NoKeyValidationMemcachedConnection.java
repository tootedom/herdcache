package org.greencheek.caching.herdcache.memcached.spy.extensions.connection;

import net.spy.memcached.*;
import net.spy.memcached.compat.log.Logger;
import net.spy.memcached.compat.log.SLF4JLogger;
import net.spy.memcached.ops.Operation;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedSelectorException;
import java.util.Collection;
import java.util.ConcurrentModificationException;
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

    /**
     * Get a Logger instance for this class.
     *
     * @return an appropriate logger instance.
     */
    protected Logger getLogger() {
        return (logger);
    }


    /**
     * Handle IO as long as the application is running.
     */
    @Override
    public void run() {
        while (running) {
            try {
                handleIO();
            } catch (IOException e) {
                logRunException(e);
            } catch (CancelledKeyException e) {
                logRunException(e);
            } catch (ClosedSelectorException e) {
                logRunException(e);
            } catch (IllegalStateException e) {
                logRunException(e);
            } catch (ConcurrentModificationException e) {
                logRunException(e);
            } catch (Throwable e) {
                logThrowable(e);
            }
        }
        getLogger().info("Shut down memcached client");
    }

    /**
     * Log a exception to different levels depending on the state.
     *
     * Exceptions get logged at debug level when happening during shutdown, but
     * at warning level when operating normally.
     *
     * @param e the exception to log.
     */
    private void logRunException(final Exception e) {
        if (shutDown) {
            getLogger().debug("Exception occurred during shutdown", e);
        } else {
            getLogger().warn("Problem handling memcached IO", e);
        }
    }
    /**
     * Log a exception to different levels depending on the state.
     *
     * Exceptions get logged at debug level when happening during shutdown, but
     * at warning level when operating normally.
     *
     * @param e the exception to log.
     */
    private void logThrowable(final Throwable e) {
        if (shutDown) {
            getLogger().debug("Throwable, Exception occurred during shutdown", e);
        } else {
            getLogger().warn("Throwable, Problem handling memcached IO", e);
        }
    }

}
