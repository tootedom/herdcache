package org.greencheek.caching.herdcache.memcached.spy.extensions.connection;

import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.HashAlgorithm;
import net.spy.memcached.MemcachedConnection;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.*;

/**
 *
 */
public class NoValidationConnectionFactory extends DefaultConnectionFactory {

    private final boolean doKeyValidation;
    private final ExecutorService executorService;
    private final ExecutorService customExecutorService;
    private final boolean usingCustomExecutor;
    private final CustomConnectionFactoryBuilder builder;

    public NoValidationConnectionFactory(boolean doKeyValidation, ExecutorService customExecutor, CustomConnectionFactoryBuilder builder) {
        this.builder = builder;
        this.doKeyValidation = doKeyValidation;
        this.customExecutorService = customExecutor;
        this.usingCustomExecutor = customExecutor != null;
        if (customExecutor == null) {
            ThreadFactory threadFactory = new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "FutureNotifyListener");
                }
            };

            executorService = new ThreadPoolExecutor(
                    0,
                    Runtime.getRuntime().availableProcessors(),
                    60L,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<Runnable>(),
                    threadFactory
            );
        } else {
            executorService = null;
        }
    }


    public NoValidationConnectionFactory clone() {
        return builder.build(doKeyValidation);
    }

    public boolean isDefaultExecutorService() {
        return !usingCustomExecutor;
    }

    public ExecutorService getCustomExecutorService() {
        return customExecutorService;
    }

    @Override
    public ExecutorService getListenerExecutorService() {
        if (isDefaultExecutorService()) {
            return getExecutorService();
        } else {
            return getCustomExecutorService();
        }
    }


    public ExecutorService getExecutorService() {
        return executorService;
    }

    public boolean doKeyValidation() {
        return this.doKeyValidation;
    }

    public MemcachedConnection createConnection(List<InetSocketAddress> addrs)
            throws IOException {
        if(doKeyValidation) {
            return new StaticLoggerMemcachedConnection(getReadBufSize(),this,addrs,
                    getInitialObservers(),getFailureMode(),getOperationFactory());
        } else {
            return new NoKeyValidationMemcachedConnection(getReadBufSize(), this, addrs,
                    getInitialObservers(), getFailureMode(), getOperationFactory());
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
