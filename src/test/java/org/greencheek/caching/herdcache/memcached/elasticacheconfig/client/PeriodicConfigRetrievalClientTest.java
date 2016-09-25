package org.greencheek.caching.herdcache.memcached.elasticacheconfig.client;

import org.greencheek.caching.herdcache.memcached.elasticacheconfig.annotations.ConfigMessage;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.annotations.DelayConfigResponse;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.confighandler.ConfigInfoProcessor;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.domain.ConfigInfo;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.server.StringServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Created by dominictootell on 20/07/2014.
 */
public class PeriodicConfigRetrievalClientTest {
    String message = new String("CONFIG cluster 0 147\r\n" +
            "12\r\n" +
            "myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211\r\n" +
            "\n"+
            "END\r\n");

    @Rule
    public StringServer server = new StringServer(message);


    PeriodicConfigRetrievalClient client;

    @Before
    public void setUp() {

    }


    @Test
    public void testConfigInfoIsReturned() {
        ConfigRetrievalSettingsBuilder builder = new ConfigRetrievalSettingsBuilder();
        final CountDownLatch latch = new CountDownLatch(2);
        ConfigInfoProcessor processor = new ConfigInfoProcessor() {
            @Override
            public void processConfig(ConfigInfo info) {
                System.out.println(info);
                latch.countDown();
            }
        };

        builder.setConfigInfoProcessor(processor);
        builder.setConfigPollingTime(0,5, TimeUnit.SECONDS);
        builder.setIdleReadTimeout(70, TimeUnit.SECONDS);
        builder.addElastiCacheHost(new ElastiCacheServerConnectionDetails("localhost",server.getPort()));


        client = new PeriodicConfigRetrievalClient(builder.build());
        client.start();

        boolean ok=false;
        try {
            ok = latch.await(20, TimeUnit.SECONDS);
        } catch(InterruptedException e) {
            fail("problem waiting for config retrieval");
        }

        assertTrue(ok);
    }

    @Test
    @ConfigMessage(message = {"CONFIG cluster 0 147\r\n" +
            "INVALID_VERSION\r\n" +
            "myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211\r\n" +
            "\nEND\r\n","CONFIG cluster 0 147\r\n" +
            "12\r\n" +
            "myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211\r\n" +
            "\nEND\r\n"})
    public void testInvalidConfigInfoIsNotReturned() {
        ConfigRetrievalSettingsBuilder builder = new ConfigRetrievalSettingsBuilder();
        final CountDownLatch latch = new CountDownLatch(2);
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


        client = new PeriodicConfigRetrievalClient(builder.build());
        client.start();

        boolean ok=false;
        try {
            ok = latch.await(15, TimeUnit.SECONDS);
        } catch(InterruptedException e) {
            fail("problem waiting for config retrieval");
        }

        assertTrue(ok);
        assertEquals(1,invalid.get());
    }




    @Test
    @ConfigMessage(message = {"CONFIG cluster 0 147\r\n" +
            "1.5\r\n" +
            "myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211\r\n" +
            "END\r\n",
            "CONFIG cluster 0 147\r\n" +
                    "-1.5\r\n" +
                    "myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211\r\n" +
                    "\nEND\r\n",
            "CONFIG cluster 0 147\r\n" +
                    "0xf\r\n" +
                    "myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211\r\n" +
                    "\nEND\r\n",
            "CONFIG cluster 0 147\r\n" +
                    "-\r\n" +
                    "myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211\r\n" +
                    "\nEND\r\n",
            "CONFIG cluster 0 147\r\n" +
                    ".\r\n" +
                    "myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211\r\n" +
                    "\nEND\r\n",
            "CONFIG cluster 0 147\r\n" +
                    " \r\n" +
                    "myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211\r\n" +
                    "\nEND\r\n",
            "CONFIG cluster 0 147\r\n" +
                    (char)1+"\r\n" +
                    "myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211\r\n" +
                    "\nEND\r\n"
    })
    public void testVersionConfigInfo() {
        ConfigRetrievalSettingsBuilder builder = new ConfigRetrievalSettingsBuilder();
        final CountDownLatch latch = new CountDownLatch(7);
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
        builder.setConfigPollingTime(0,2, TimeUnit.SECONDS);
        builder.setIdleReadTimeout(70, TimeUnit.SECONDS);
        builder.addElastiCacheHost(new ElastiCacheServerConnectionDetails("localhost",server.getPort()));
        builder.setNumberOfInvalidConfigsBeforeReconnect(10);

        client = new PeriodicConfigRetrievalClient(builder.build());
        client.start();

        boolean ok=false;
        try {
            ok = latch.await(30, TimeUnit.SECONDS);
        } catch(InterruptedException e) {
            fail("problem waiting for config retrieval");
        }

        assertTrue(ok);
        assertEquals(7,invalid.get());
    }


    @Test
    @ConfigMessage(message = {"CONFIG cluster 0 147\r\n" +
            "1.5\r\n" +
            "myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211\r\n" +
            "END\r\n",
            "CONFIG cluster 0 147\r\n" +
                    "-1.5\r\n" +
                    "myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211\r\n" +
                    "\nEND\r\n",
            "CONFIG cluster 0 147\r\n" +
                    "0xf\r\n" +
                    "myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211\r\n" +
                    "\nEND\r\n",
            "CONFIG cluster 0 147\r\n" +
                    "-\r\n" +
                    "myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211\r\n" +
                    "\nEND\r\n",
            "CONFIG cluster 0 147\r\n" +
                    ".\r\n" +
                    "myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211\r\n" +
                    "\nEND\r\n",
            "CONFIG cluster 0 147\r\n" +
                    " \r\n" +
                    "myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211\r\n" +
                    "\nEND\r\n",
            "CONFIG cluster 0 147\r\n" +
                    (char)1+"\r\n" +
                    "myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211\r\n" +
                    "\nEND\r\n"
    })
    public void testUpdateConfiguration() {

        ScheduledExecutorService sexec = Executors.newSingleThreadScheduledExecutor();
        try {
            ConfigRetrievalSettingsBuilder builder = new ConfigRetrievalSettingsBuilder();
            final CountDownLatch latch = new CountDownLatch(7);
            final AtomicInteger invalid = new AtomicInteger(0);
            ConfigInfoProcessor processor = new ConfigInfoProcessor() {
                @Override
                public void processConfig(ConfigInfo info) {
                    System.out.println(info);
                    latch.countDown();
                    if (!info.isValid()) invalid.incrementAndGet();
                }
            };

            final ElastiCacheConfigServerUpdater configUpdator = new SimpleVolatileBasedElastiCacheConfigServerUpdater();

            builder.setConfigInfoProcessor(processor);
            builder.setConfigPollingTime(0, 2, TimeUnit.SECONDS);
            builder.setIdleReadTimeout(70, TimeUnit.SECONDS);
            builder.setReconnectDelay(1000, TimeUnit.MILLISECONDS);
            builder.addElastiCacheHost(new ElastiCacheServerConnectionDetails("localhost", server.getPort()));
            builder.setNumberOfInvalidConfigsBeforeReconnect(10);
            builder.setConfigUrlUpdater(Optional.of(configUpdator));

            client = new PeriodicConfigRetrievalClient(builder.build());
            client.start();

            boolean ok = false;
            try {
                sexec.scheduleWithFixedDelay(() -> {
                    configUpdator.connectionUpdated("localhost:" + server.getPort());
                }, 0, 11, TimeUnit.SECONDS);
                ok = latch.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                fail("problem waiting for config retrieval");
            }

            assertTrue(ok);
            assertEquals(7, invalid.get());
        } finally {
            sexec.shutdownNow();
        }
    }

    @Test
    @ConfigMessage(message = {"CONFIG cluster 0 147\r\n" +
            "INVALID_VERSION\r\n" +
            "myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211${REMOTE_ADDR}\r\n" +
            "\nEND\r\n"
            ,"bob\nbob\nbob\n\nbob\n"
            ,"bob\r\nbob\nbob\n\nbob\n"
            ,"bob\r\nbob\nbob\n\nbob\n"
            ,"bob\r\nbob\nbob\n\nbob\n",
            "CONFIG cluster 0 147\r\n" +
            "12\r\n" +
            "myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211${REMOTE_ADDR}\r\n" +
            "\nEND\r\n"})
    public void testReconnectAfterInvalidConfigInfoIsReturned() {
        ConfigRetrievalSettingsBuilder builder = new ConfigRetrievalSettingsBuilder();
        final CountDownLatch latch = new CountDownLatch(6);
        final AtomicInteger invalid = new AtomicInteger(0);
        final Set<String> addresses = new HashSet<String>();
        ConfigInfoProcessor processor = new ConfigInfoProcessor() {
            @Override
            public void processConfig(ConfigInfo info) {
                System.out.println(info);
                int index = info.getServers().indexOf("/127.0.0.1");
                if(index>-1) {
                    addresses.add(info.getServers().substring(index + 11));
                }
                latch.countDown();
                if(!info.isValid()) invalid.incrementAndGet();
            }
        };

        builder.setConfigInfoProcessor(processor);
        builder.setConfigPollingTime(0,1, TimeUnit.SECONDS);
        builder.setIdleReadTimeout(70, TimeUnit.SECONDS);
        builder.addElastiCacheHost(new ElastiCacheServerConnectionDetails("localhost",server.getPort()));
        builder.setNumberOfInvalidConfigsBeforeReconnect(5);

        client = new PeriodicConfigRetrievalClient(builder.build());
        client.start();

        boolean ok=false;
        try {
            ok = latch.await(15, TimeUnit.SECONDS);
        } catch(InterruptedException e) {
            fail("problem waiting for config retrieval");
        }

        assertTrue(ok);
        assertEquals(5,invalid.get());
        assertEquals(2,addresses.size());
    }

    @Test
    @DelayConfigResponse(delayedForTimeUnit = TimeUnit.SECONDS,delayFor = 20)
    @ConfigMessage(message = {
            "CONFIG cluster 0 147\r\n" +
                    "12\r\n" +
                    "myCluster.pc4ldq.0001.use1.cache.amazonaws.com|10.82.235.120|11211 myCluster.pc4ldq.0002.use1.cache.amazonaws.com|10.80.249.27|11211${REMOTE_ADDR}\r\n" +
                    "\nEND\r\n"})
    public void testIdleTimeout() {
        ConfigRetrievalSettingsBuilder builder = new ConfigRetrievalSettingsBuilder();
        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicInteger invalid = new AtomicInteger(0);
        final Set<String> addresses = new HashSet<String>();
        ConfigInfoProcessor processor = new ConfigInfoProcessor() {
            @Override
            public void processConfig(ConfigInfo info) {
                System.out.println(info);
                int index = info.getServers().indexOf("/127.0.0.1");
                if(index>-1) {
                    addresses.add(info.getServers().substring(index+11));
                }
                latch.countDown();
                if(!info.isValid()) invalid.incrementAndGet();
            }
        };

        builder.setConfigInfoProcessor(processor);
        builder.setConfigPollingTime(0, 20, TimeUnit.SECONDS);
        builder.setIdleReadTimeout(10, TimeUnit.SECONDS);
        builder.addElastiCacheHost(new ElastiCacheServerConnectionDetails("localhost",server.getPort()));
        builder.setNumberOfInvalidConfigsBeforeReconnect(5);

        client = new PeriodicConfigRetrievalClient(builder.build());
        client.start();

        boolean ok=false;
        try {
            ok = latch.await(25, TimeUnit.SECONDS);
        } catch(InterruptedException e) {
            fail("problem waiting for config retrieval");
        }

        assertFalse(ok);
        assertEquals(0,invalid.get());
        assertEquals(0,addresses.size());
    }

    @After
    public void tearDown() {
        client.stop();
    }
}
