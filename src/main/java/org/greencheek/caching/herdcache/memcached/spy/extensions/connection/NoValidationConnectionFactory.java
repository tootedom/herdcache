package org.greencheek.caching.herdcache.memcached.spy.extensions.connection;

import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.HashAlgorithm;
import net.spy.memcached.MemcachedConnection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

/**
 *
 */
public class NoValidationConnectionFactory extends DefaultConnectionFactory {

    private final boolean doKeyValidation;
    public NoValidationConnectionFactory(boolean doKeyValidation) {
        this.doKeyValidation = doKeyValidation;
    }

    public MemcachedConnection createConnection(List<InetSocketAddress> addrs)
            throws IOException {
        if(doKeyValidation) {
            return new NoKeyValidationMemcachedConnection(getReadBufSize(), this, addrs,
                    getInitialObservers(), getFailureMode(), getOperationFactory());
        } else {
            return new StaticLoggerMemcachedConnection(getReadBufSize(),this,addrs,
                    getInitialObservers(),getFailureMode(),getOperationFactory());
        }
    }

    @Override
    public String toString() {
        return "Failure Mode: " + getFailureMode().name() + ", Hash Algorithm: "
                + ((HashAlgorithm)getHashAlg()).getClass().getName() + " Max Reconnect Delay: "
                + getMaxReconnectDelay() + ", Max Op Timeout: " + getOperationTimeout()
                + ", Op Queue Length: " + getOpQueueLen() + ", Op Max Queue Block Time"
                + getOpQueueMaxBlockTime() + ", Max Timeout Exception Threshold: "
                + getTimeoutExceptionThreshold() + ", Read Buffer Size: "
                + getReadBufSize() + ", Transcoder: " + getDefaultTranscoder()
                + ", Operation Factory: " + getOperationFactory() + " isDaemon: "
                + isDaemon() + ", Optimized: " + shouldOptimize() + ", Using Nagle: "
                + useNagleAlgorithm() + ", ConnectionFactory: " + getName();
    }
}
