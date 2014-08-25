package org.greencheek.caching.herdcache.memcached.elasticacheconfig;

import org.greencheek.caching.herdcache.memcached.elasticacheconfig.client.ConfigRetrievalSettingsBuilder;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.client.PeriodicConfigRetrievalClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by dominictootell on 20/07/2014.
 */
public class Client {

    private static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) {
        final PeriodicConfigRetrievalClient client = new PeriodicConfigRetrievalClient(new ConfigRetrievalSettingsBuilder().build());
        client.start();

        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                System.out.println("========");
                System.out.println("Stopping");
                System.out.println("========");
                client.stop();

                executorService.shutdownNow();
            }
        },10,60, TimeUnit.SECONDS);
    }
}
