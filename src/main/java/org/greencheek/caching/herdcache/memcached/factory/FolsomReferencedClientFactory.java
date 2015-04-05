package org.greencheek.caching.herdcache.memcached.factory;

import com.google.common.net.HostAndPort;
import com.spotify.folsom.MemcacheClient;
import com.spotify.folsom.MemcacheClientBuilder;
import net.spy.memcached.ConnectionFactoryBuilder;
import org.greencheek.caching.herdcache.memcached.config.ElastiCacheCacheConfig;
import org.greencheek.caching.herdcache.memcached.folsom.transcoder.FastTranscoder;
import org.greencheek.caching.herdcache.memcached.folsom.transcoder.StringTranscoder;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by dominictootell on 05/04/2015.
 */
public class FolsomReferencedClientFactory<V> implements ReferencedClientFactory<V>{


    private final ElastiCacheCacheConfig config;

    public FolsomReferencedClientFactory(ElastiCacheCacheConfig config) {
        this.config = config;
    }


    @Override
    public ReferencedClient<V> createClient(List<InetSocketAddress> resolvedHosts) {

        List<HostAndPort> hosts = resolvedHosts.stream().map(x ->
                        HostAndPort.fromParts(x.getHostName(), x.getPort())
        ).collect(Collectors.toList());

        System.out.println(hosts);
        MemcacheClientBuilder builder = null;
        if(config.useFolsomStringClient()) {
            builder = new MemcacheClientBuilder<>(new StringTranscoder(config.getFolsomStringClientCharset()));

        } else {
            builder = new MemcacheClientBuilder(new FastTranscoder());
        }

        builder = builder.withAddresses(hosts)
                .withConnections(config.getFolsomConnections())
                .withRequestTimeoutMillis(config.getFolsomRequestTimeout());

        MemcacheClient<V> client;
        if(config.getMemcachedCacheConfig().getProtocol()== ConnectionFactoryBuilder.Protocol.BINARY) {
            client = builder.connectBinary();
        } else {
            client = builder.connectAscii();
        }


        return new FolsomReferencedClient<>(true,resolvedHosts,client);
    }
}
