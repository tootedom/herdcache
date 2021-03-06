package org.greencheek.caching.herdcache.memcached.factory;

import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.MemcachedClient;
import org.greencheek.caching.herdcache.memcached.config.Host;
import org.greencheek.caching.herdcache.memcached.config.MemcachedCacheConfig;
import org.greencheek.caching.herdcache.memcached.config.builder.ElastiCacheCacheConfigBuilder;
import org.greencheek.caching.herdcache.memcached.dns.AddressResolver;
import org.greencheek.caching.herdcache.memcached.spy.extensions.connection.NoValidationConnectionFactory;
import org.greencheek.caching.herdcache.memcached.spyconnectionfactory.SpyConnectionFactoryBuilder;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class BackgroundDnsResolverTest {



    class SimpleCircularAddressResolver implements AddressResolver{
        private final CountDownLatch startSignal = new CountDownLatch(1);
        private AtomicInteger called = new AtomicInteger(0);
        private final InetAddress[][] addresses;

        public SimpleCircularAddressResolver(InetAddress[][] addressesToReturn) {
            this.addresses = addressesToReturn;
        }

        @Override
        public InetAddress[] resolve(String host) {
            startSignal.countDown();
            if(called.get()==addresses.length) {
                called.set(0);
            }

            return addresses[called.getAndIncrement()];
        }

        public CountDownLatch getStartSignal() {
            return startSignal;
        }
    }

    ReferencedClientFactory reffactory;

    @Before
    public void setUp() {
        MemcachedCacheConfig config = new ElastiCacheCacheConfigBuilder().buildMemcachedConfig();
        NoValidationConnectionFactory factory =  SpyConnectionFactoryBuilder.createConnectionFactory(
                config.getFailureMode(),
                config.getHashAlgorithm(), config.getSerializingTranscoder(),
                config.getProtocol(), config.getReadBufferSize(), config.getKeyHashType(),
                config.getLocatorFactory(), config.getKeyValidationType());

        reffactory = new SpyMemcachedReferencedClientFactory(factory);
    }

    @Test
    public void updateOfDnsIsCaptured() throws UnknownHostException, InterruptedException {

        InetAddress[] addresses1 = new InetAddress[3];

        addresses1[0] = InetAddress.getByName("127.0.0.100");
        addresses1[1] = InetAddress.getByName("127.0.0.2");
        addresses1[2] = InetAddress.getByName("127.0.0.1");

        InetAddress[] addresses2 = new InetAddress[3];
        addresses2[0] = InetAddress.getByName("127.0.0.101");
        addresses2[1] = InetAddress.getByName("127.0.0.2");
        addresses2[2] = InetAddress.getByName("127.0.0.3");

        InetAddress[] addresses3 = new InetAddress[3];
        addresses3[0] = InetAddress.getByName("127.0.0.101");
        addresses3[1] = InetAddress.getByName("127.0.0.2");
        addresses3[2] = InetAddress.getByName("127.0.0.3");

        SimpleCircularAddressResolver addressResolver = new SimpleCircularAddressResolver(new InetAddress[][]{addresses1,addresses2,addresses3});
        CountDownLatch latch = addressResolver.getStartSignal();
        BackgroundDnsResolver resolver = new BackgroundDnsResolver(new Host("bob.com",11211),10000,reffactory,addressResolver);
        try {
            latch.await(10000, TimeUnit.SECONDS);
            Thread.sleep(2000);
            ReferencedClient client = resolver.getClient();
            assertNotSame(client, SpyReferencedClient.UNAVAILABLE_REFERENCE_CLIENT);
            Thread.sleep(13000);
            ReferencedClient client2 = resolver.getClient();
            assertNotSame(client, SpyReferencedClient.UNAVAILABLE_REFERENCE_CLIENT);
            assertNotSame(client, client2);
            Thread.sleep(13000);
            ReferencedClient client3 = resolver.getClient();
            assertSame(client2, client3);
        }
        finally {
            resolver.shutdown();
        }

    }

    @Test
    public void updateOfDnsIsNotChangedWhenInvalidAddressesReturned() throws UnknownHostException, InterruptedException {

        InetAddress[] addresses1 = new InetAddress[3];

        addresses1[0] = InetAddress.getByName("127.0.0.100");
        addresses1[1] = InetAddress.getByName("127.0.0.2");
        addresses1[2] = InetAddress.getByName("127.0.0.1");

        InetAddress[] addresses2 = new InetAddress[3];
        addresses2[0] = InetAddress.getByName("127.0.53.53");
        addresses2[1] = InetAddress.getByName("127.0.53.53");
        addresses2[2] = InetAddress.getByName("127.0.53.53");

        InetAddress[] addresses3 = new InetAddress[3];
        addresses3[0] = InetAddress.getByName("127.0.53.53");
        addresses3[1] = InetAddress.getByName("127.0.53.53");
        addresses3[2] = InetAddress.getByName("127.0.53.53");

        SimpleCircularAddressResolver addressResolver = new SimpleCircularAddressResolver(new InetAddress[][]{addresses1,addresses2,addresses3});
        CountDownLatch latch = addressResolver.getStartSignal();
        BackgroundDnsResolver resolver = new BackgroundDnsResolver(new Host("bob.com",11211),10000,reffactory,addressResolver);
        try {
            latch.await(10000, TimeUnit.SECONDS);
            Thread.sleep(2000);
            ReferencedClient client = resolver.getClient();
            assertNotSame(client, SpyReferencedClient.UNAVAILABLE_REFERENCE_CLIENT);
            Thread.sleep(13000);
            ReferencedClient client2 = resolver.getClient();
            assertNotSame(client, SpyReferencedClient.UNAVAILABLE_REFERENCE_CLIENT);
            assertSame(client, client2);
            Thread.sleep(13000);
            ReferencedClient client3 = resolver.getClient();
            assertSame(client2, client3);
            assertSame(client, client3);
        }
        finally {
            resolver.shutdown();
        }

    }

    @Test
    public void updateOfDnsIsChangedAndFaultyAddressRemoved() throws UnknownHostException, InterruptedException {

        InetAddress[] addresses1 = new InetAddress[3];

        addresses1[0] = InetAddress.getByName("127.0.0.100");
        addresses1[1] = InetAddress.getByName("127.0.0.2");
        addresses1[2] = InetAddress.getByName("127.0.0.1");

        InetAddress[] addresses2 = new InetAddress[3];
        addresses2[0] = InetAddress.getByName("127.0.53.53");
        addresses2[1] = InetAddress.getByName("127.0.0.4");
        addresses2[2] = InetAddress.getByName("127.0.0.5");


        SimpleCircularAddressResolver addressResolver = new SimpleCircularAddressResolver(new InetAddress[][]{addresses1,addresses2});
        CountDownLatch latch = addressResolver.getStartSignal();
        BackgroundDnsResolver resolver = new BackgroundDnsResolver(new Host("bob.com",11211),10000,reffactory,addressResolver);
        try {
            latch.await(10000, TimeUnit.SECONDS);
            Thread.sleep(2000);
            ReferencedClient client = resolver.getClient();
            assertNotSame(client, SpyReferencedClient.UNAVAILABLE_REFERENCE_CLIENT);
            Thread.sleep(13000);
            ReferencedClient client2 = resolver.getClient();
            assertNotSame(client, SpyReferencedClient.UNAVAILABLE_REFERENCE_CLIENT);
            assertNotSame(client, client2);

            List<InetSocketAddress> sockAddys = client2.getResolvedHosts();

            assertEquals(2, sockAddys.size());
            for(InetSocketAddress addy : sockAddys) {
                System.out.println(addy.getAddress().getHostAddress());
            }

        }
        finally {
            resolver.shutdown();
        }

    }

    @Test
    public void updateOfDnsIsChangedAndFaultyAddressRemovedAndListenerIsAwake() throws UnknownHostException, InterruptedException {

        InetAddress[] addresses1 = new InetAddress[3];

        addresses1[0] = InetAddress.getByName("127.0.0.100");
        addresses1[1] = InetAddress.getByName("127.0.0.2");
        addresses1[2] = InetAddress.getByName("127.0.0.1");

        InetAddress[] addresses2 = new InetAddress[3];
        addresses2[0] = InetAddress.getByName("127.0.53.53");
        addresses2[1] = InetAddress.getByName("127.0.0.4");
        addresses2[2] = InetAddress.getByName("127.0.0.5");


        SimpleCircularAddressResolver addressResolver = new SimpleCircularAddressResolver(new InetAddress[][]{addresses1,addresses2});
        CountDownLatch latch = addressResolver.getStartSignal();
        BackgroundDnsResolver resolver = new BackgroundDnsResolver(new Host("bob.com",11211),10000,reffactory,addressResolver);
        try {
            latch.await(10000, TimeUnit.SECONDS);
            Thread.sleep(2000);
            ReferencedClient client = resolver.getClient();
            assertNotSame(client, SpyReferencedClient.UNAVAILABLE_REFERENCE_CLIENT);
            try {
                client.get("dom", 1000, TimeUnit.MILLISECONDS);
            } catch(Exception e) {
                throw e;
            }
            Thread.sleep(16500);
            ReferencedClient client2 = resolver.getClient();
            assertFalse("Listener should not be shutdown",((MemcachedClient)((SpyReferencedClient)client2).getMemcachedClient()).getExecutorService().isShutdown());
            try {
                client2.get("dom", 1000, TimeUnit.MILLISECONDS);
            } catch(Exception e) {
                throw e;
            }
            assertNotSame(client, SpyReferencedClient.UNAVAILABLE_REFERENCE_CLIENT);
            assertNotSame(client, client2);

            List<InetSocketAddress> sockAddys = client2.getResolvedHosts();

            assertEquals(2, sockAddys.size());
            for(InetSocketAddress addy : sockAddys) {
                System.out.println(addy.getAddress().getHostAddress());
            }

        }
        finally {
            resolver.shutdown();
        }

    }

}