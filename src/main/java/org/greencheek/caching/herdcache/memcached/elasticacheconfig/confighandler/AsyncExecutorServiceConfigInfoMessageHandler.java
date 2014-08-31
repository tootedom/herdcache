package org.greencheek.caching.herdcache.memcached.elasticacheconfig.confighandler;

import org.greencheek.caching.herdcache.memcached.elasticacheconfig.domain.ConfigInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by dominictootell on 20/07/2014.
 */
public class AsyncExecutorServiceConfigInfoMessageHandler implements AsyncConfigInfoMessageHandler {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ConfigInfoProcessor processor;

    public AsyncExecutorServiceConfigInfoMessageHandler(ConfigInfoProcessor configInfoProcessor) {
        processor = configInfoProcessor;
    }

    @Override
    public void processConfig(final ConfigInfo info) {
        executor.submit(new Runnable() {
                @Override
                public void run() {
                    processor.processConfig(info);
                }
            });
    }

    @Override
    public void shutdown() {
        executor.shutdownNow();
    }
}


