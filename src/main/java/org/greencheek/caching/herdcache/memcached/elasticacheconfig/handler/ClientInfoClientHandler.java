package org.greencheek.caching.herdcache.memcached.elasticacheconfig.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.client.ElastiCacheConfigServerChooser;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.client.PeriodicConfigRetrievalClient;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.confighandler.AsyncConfigInfoMessageHandler;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.decoder.InvalidConfigVersionException;
import org.greencheek.caching.herdcache.memcached.elasticacheconfig.domain.ConfigInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Keep reconnecting to the server while printing out the current uptime and
 * connection attempt getStatus.
 */
@Sharable
public class ClientInfoClientHandler extends SimpleChannelInboundHandler<ConfigInfo> {

    private static final Logger log = LoggerFactory.getLogger(ClientInfoClientHandler.class);

    private final RequestConfigInfoScheduler obtainConfigComand;
    private final AsyncConfigInfoMessageHandler asyncConfigInfoMessageHandler;


    private final TimeUnit reconnectTimeUnit;
    private final long reconnectDelay;
    private final TimeUnit idleReadTimeUnit;
    private final long idleReadTimeout;
    private final int connectionTimeoutInMillis;

    private final ElastiCacheConfigServerChooser configServerChooser;

    private final AtomicInteger invalidConsecutiveConfigs = new AtomicInteger(0);
    private final int maxConsecutiveInvalidConfigsBeforeReconnect;

    public ClientInfoClientHandler(RequestConfigInfoScheduler getConfigCommand,
                                   AsyncConfigInfoMessageHandler handler,
                                   TimeUnit reconnectTimeUnit,long reconnectDelay,
                                   TimeUnit idleReadTimeUnit, long idleReadTimeout,
                                   ElastiCacheConfigServerChooser configServerChooser,
                                   int noInvalidConfigsBeforeReconnect,
                                   int connectionTimeoutInMillis) {
        this.obtainConfigComand = getConfigCommand;
        this.asyncConfigInfoMessageHandler = handler;
        this.reconnectTimeUnit = reconnectTimeUnit;
        this.reconnectDelay = reconnectDelay;
        this.idleReadTimeUnit = idleReadTimeUnit;
        this.idleReadTimeout = idleReadTimeout;
        this.configServerChooser = configServerChooser;
        this.maxConsecutiveInvalidConfigsBeforeReconnect = noInvalidConfigsBeforeReconnect;
        this.connectionTimeoutInMillis = connectionTimeoutInMillis;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("Connected to: {}",ctx.channel().remoteAddress());
        obtainConfigComand.schedule(ctx.executor(),ctx);
    }


    @Override
    public void channelRead0(ChannelHandlerContext ctx, ConfigInfo msg) throws Exception {
        asyncConfigInfoMessageHandler.processConfig(msg);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (!(evt instanceof IdleStateEvent)) {
            return;
        }

        IdleStateEvent e = (IdleStateEvent) evt;
        if (e.state() == IdleState.READER_IDLE) {
            // The connection was OK but there was no traffic for last period.
            log.info("Disconnecting due to no response on connection for config retrieval.  A reconnect will occur.");
            ctx.close();
        } else if(e.state() == IdleState.WRITER_IDLE) {
            // The connection was OK but there was no traffic for last period.
            log.info("Disconnecting due to no traffic on connection, either read or write.  A reconnect will occur.");
            ctx.close();
        }
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        log.warn("Disconnected from: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelUnregistered(final ChannelHandlerContext ctx) throws Exception {
        if(log.isInfoEnabled()) {
            log.info("Sleeping for {}s before reconnect.", reconnectTimeUnit.toSeconds(reconnectDelay));
        }

        final ClientInfoClientHandler handler = this;
        final EventLoop loop = ctx.channel().eventLoop();
        loop.schedule(new Runnable() {
            @Override
            public void run() {
                log.info("Reconnecting");
                PeriodicConfigRetrievalClient.configureBootstrap(configServerChooser, handler, new Bootstrap(), loop,
                        idleReadTimeUnit,idleReadTimeout,connectionTimeoutInMillis);
            }
        }, reconnectDelay, reconnectTimeUnit);
    }





    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if(cause instanceof InvalidConfigVersionException) {
            log.warn("invalid config supplied by elasticache");
            if(maxConsecutiveInvalidConfigsBeforeReconnect==-1) return;

            if(invalidConsecutiveConfigs.incrementAndGet() >= maxConsecutiveInvalidConfigsBeforeReconnect) {
                invalidConsecutiveConfigs.set(0);
                ctx.close();
            }
        }
        else {
            super.exceptionCaught(ctx, cause);
            ctx.close();

        }
    }


    public void shutdown() {
        asyncConfigInfoMessageHandler.shutdown();
    }

}