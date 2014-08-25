package org.greencheek.caching.herdcache.memcached.elasticacheconfig.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.confighandler.AsyncConfigInfoMessageHandler;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.decoder.ConfigInfoDecoder;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.handler.ClientInfoClientHandler;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.handler.RequestConfigInfoScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by dominictootell on 20/07/2014.
 */
public class PeriodicConfigRetrievalClient
{

    private static final Logger log = LoggerFactory.getLogger(PeriodicConfigRetrievalClient.class);


    private final TimeUnit idleTimeoutTimeUnit;
    private final long readTimeout;
    private final ClientInfoClientHandler handler;
    private final int connectionTimeoutInMillis;
    private final ElastiCacheConfigServerChooser configServerChooser;
    private NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup();


    public PeriodicConfigRetrievalClient(ConfigRetrievalSettings settings)
    {
        ElastiCacheServerConnectionDetails[] configServers = settings.getElasticacheConfigHosts();
        if(configServers.length==1) {
            this.configServerChooser = new SingleElastiCacheConfigServerChooser(configServers[0]);
        } else {
            this.configServerChooser = new RoundRobinElastiCacheConfigServerChooser(configServers);
        }


        this.idleTimeoutTimeUnit = settings.getIdleTimeoutTimeUnit();
        this.readTimeout = settings.getIdleReadTimeout();
        this.connectionTimeoutInMillis = settings.getConnectionTimeoutInMillis();

        this.handler = createHandler(settings.getScheduledConfigRetrieval(),settings.getConfigInfoMessageHandler(),
                settings.getReconnectDelayTimeUnit(),settings.getReconnectDelay(),idleTimeoutTimeUnit,readTimeout,
                settings.getNumberOfConsecutiveInvalidConfigsBeforeReconnect(),
                connectionTimeoutInMillis);



    }

    public ClientInfoClientHandler createHandler(RequestConfigInfoScheduler scheduledRequester,
                                                 AsyncConfigInfoMessageHandler configReadHandler,
                                                 TimeUnit reconnectTimeUnit,
                                                 long reconnectionDelay,
                                                 TimeUnit idleTimeoutTimeUnit,
                                                 long idleReadTimeout,
                                                 int noConsecutiveInvalidConfigsBeforeReconnect,
                                                 int connectionTimeoutInMillis) {
        return new ClientInfoClientHandler(scheduledRequester,configReadHandler,reconnectTimeUnit,reconnectionDelay,
                idleTimeoutTimeUnit,idleReadTimeout,this.configServerChooser,noConsecutiveInvalidConfigsBeforeReconnect,
                connectionTimeoutInMillis);
    }


    public void start() {
        configureBootstrap(this.configServerChooser,handler,new Bootstrap(),nioEventLoopGroup,idleTimeoutTimeUnit,readTimeout,connectionTimeoutInMillis);
    }

    public void stop() {
        nioEventLoopGroup.shutdownGracefully();
        handler.shutdown();
    }

    public static ChannelFuture configureBootstrap(ElastiCacheConfigServerChooser configServerService,final ClientInfoClientHandler handler,
                                                   Bootstrap b, EventLoopGroup g, final TimeUnit idleTimeoutTimeUnit, final long idleTimeout,
                                                   final int connectionTimeoutInMillis) {
        final ElastiCacheServerConnectionDetails configServer = configServerService.getServer();
        b.group(g)
                .channel(NioSocketChannel.class)
                .remoteAddress(configServer.getHost(),configServer.getPort())

                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.config().setConnectTimeoutMillis(connectionTimeoutInMillis);
                        ch.pipeline().addLast(new ConfigInfoDecoder());
                        ch.pipeline().addLast(new IdleStateHandler(idleTimeout, 0, 0,idleTimeoutTimeUnit));
                        ch.pipeline().addLast(handler);
                    }
                });

        return b.connect().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.cause() != null) {
                    log.warn("Failed to connect: {}:{}",configServer.getHost(),configServer.getPort(), future.cause());
                }
            }
        });

    }
}
