package org.greencheek.caching.herdcache.memcached.elasticacheconfig.client;

import org.greencheek.caching.herdcache.memcached.elasticacheconfig.annotations.ConfigMessage;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.annotations.SendAllMessages;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.confighandler.ConfigInfoProcessor;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.domain.ConfigInfo;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.server.StringServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Created by dominictootell on 21/07/2014.
 */
public class SlowStartupPeriodicConfigRetrievalClientTest {


    @Rule
    public StringServer server = new StringServer("",2, TimeUnit.SECONDS);


    PeriodicConfigRetrievalClient client;

    @Before
    public void setUp() {

    }

    @Test
    @SendAllMessages
    @ConfigMessage(message = {"CONFIG"," cluster 0 ","147\r\n" +
            "+","1","00","\r\n" +
            "myCluster.","pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211${REMOTE_ADDR}\r\n" +
            "\nEND\r\n"
    })
    public void testServerNotAvailableStraightAway() {
        ConfigRetrievalSettingsBuilder builder = new ConfigRetrievalSettingsBuilder();
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger invalid = new AtomicInteger(0);
        ConfigInfoProcessor processor = new ConfigInfoProcessor() {
            @Override
            public void processConfig(ConfigInfo info) {
                System.out.println(info);
                latch.countDown();
                if(!info.isValid()) invalid.incrementAndGet();
            }
        };

        builder.setConfigInfoProcessor(processor);
        builder.setConfigPollingTime(0,5, TimeUnit.SECONDS);
        builder.setIdleReadTimeout(70, TimeUnit.SECONDS);
        builder.addElastiCacheHost(new ElastiCacheServerConnectionDetails("localhost",server.getPort()));

        builder.setNumberOfInvalidConfigsBeforeReconnect(5);
        builder.setReconnectDelay(2, TimeUnit.SECONDS);

        client = new PeriodicConfigRetrievalClient(builder.build());
        client.start();

        boolean ok=false;
        try {
            ok = latch.await(10, TimeUnit.SECONDS);
        } catch(InterruptedException e) {
            fail("problem waiting for config retrieval");
        }

        assertTrue(ok);
        assertEquals(0,invalid.get());
    }


    @After
    public void tearDown() {
        client.stop();
    }
}
