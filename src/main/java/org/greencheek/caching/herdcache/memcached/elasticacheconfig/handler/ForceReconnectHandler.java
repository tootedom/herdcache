package org.greencheek.caching.herdcache.memcached.elasticacheconfig.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
@ChannelHandler.Sharable
public class ForceReconnectHandler extends ChannelHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(ForceReconnectHandler.class);

    private volatile ChannelHandlerContext ctx;


    /**
     * Do nothing by default, sub-classes may override this method.
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        log.info("force reconnect handler added");
        this.ctx = ctx;
    }

    /**
     * Do nothing by default, sub-classes may override this method.
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        // NOOP
        log.info("force reconnect handler removed");
        this.ctx = null;
    }


    public void forceReconnect() {
        ChannelHandlerContext ctx = this.ctx;
        if(ctx!=null) {
            ctx.fireUserEventTriggered(new ReconnectEvent());
        }
    }
}
