package org.greencheek.caching.herdcache.memcached.spy.extensions.connection;

import net.spy.memcached.*;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.metrics.MetricCollector;
import net.spy.memcached.metrics.MetricType;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.transcoders.Transcoder;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

/**
 * Created by dominictootell on 03/05/2014.
 */
public class CustomConnectionFactoryBuilder extends ConnectionFactoryBuilder {

    /**
     * Get the ConnectionFactory set up with the provided parameters.
     */
    public ConnectionFactory build() {
        return new NoValidationConnectionFactory() {

            @Override
            public BlockingQueue<Operation> createOperationQueue() {
                return opQueueFactory == null ? super.createOperationQueue()
                        : opQueueFactory.create();
            }

            @Override
            public BlockingQueue<Operation> createReadOperationQueue() {
                return readQueueFactory == null ? super.createReadOperationQueue()
                        : readQueueFactory.create();
            }

            @Override
            public BlockingQueue<Operation> createWriteOperationQueue() {
                return writeQueueFactory == null ? super.createReadOperationQueue()
                        : writeQueueFactory.create();
            }

            @Override
            public NodeLocator createLocator(List<MemcachedNode> nodes) {
                switch (locator) {
                    case ARRAY_MOD:
                        return new ArrayModNodeLocator(nodes, getHashAlg());
                    case CONSISTENT:
                        return new KetamaNodeLocator(nodes, getHashAlg());
                    default:
                        throw new IllegalStateException("Unhandled locator type: " + locator);
                }
            }

            @Override
            public Transcoder<Object> getDefaultTranscoder() {
                return transcoder == null ? super.getDefaultTranscoder() : transcoder;
            }

            @Override
            public FailureMode getFailureMode() {
                return failureMode == null ? super.getFailureMode() : failureMode;
            }

            @Override
            public HashAlgorithm getHashAlg() {
                return hashAlg == null ? super.getHashAlg() : hashAlg;
            }

            public Collection<ConnectionObserver> getInitialObservers() {
                return initialObservers;
            }

            @Override
            public OperationFactory getOperationFactory() {
                return opFact == null ? super.getOperationFactory() : opFact;
            }

            @Override
            public long getOperationTimeout() {
                return opTimeout == -1 ? super.getOperationTimeout() : opTimeout;
            }

            @Override
            public int getReadBufSize() {
                return readBufSize == -1 ? super.getReadBufSize() : readBufSize;
            }

            @Override
            public boolean isDaemon() {
                return isDaemon;
            }

            @Override
            public boolean shouldOptimize() {
                return shouldOptimize;
            }

            @Override
            public boolean useNagleAlgorithm() {
                return useNagle;
            }

            @Override
            public long getMaxReconnectDelay() {
                return maxReconnectDelay;
            }

            @Override
            public AuthDescriptor getAuthDescriptor() {
                return authDescriptor;
            }

            @Override
            public long getOpQueueMaxBlockTime() {
                return opQueueMaxBlockTime > -1 ? opQueueMaxBlockTime
                        : super.getOpQueueMaxBlockTime();
            }

            @Override
            public int getTimeoutExceptionThreshold() {
                return timeoutExceptionThreshold;
            }

            @Override
            public MetricType enableMetrics() {
                return metricType == null ? super.enableMetrics() : metricType;
            }

            @Override
            public MetricCollector getMetricCollector() {
                return collector == null ? super.getMetricCollector() : collector;
            }

            @Override
            public ExecutorService getListenerExecutorService() {
                return executorService == null ? super.getListenerExecutorService() : executorService;
            }

            @Override
            public boolean isDefaultExecutorService() {
                return executorService == null;
            }
        };

    }

}
